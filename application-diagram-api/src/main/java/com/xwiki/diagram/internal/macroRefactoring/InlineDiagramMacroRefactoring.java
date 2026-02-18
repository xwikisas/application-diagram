/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.xwiki.diagram.internal.macroRefactoring;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.macro.MacroRefactoringException;
import org.xwiki.stability.Unstable;
import org.xwiki.xml.XMLUtils;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xwiki.diagram.internal.handlers.DiagramContentHandler;
import com.xwiki.diagram.internal.handlers.DiagramLinkHandler;

/**
 * Responsible for updating the diagram content when a reference is moved for the inline diagram macro. The code will be
 * executed on move/rename actions.
 * <p>
 *
 * @version $Id$
 * @since 1.22.11
 */
@Component
@Named("inlineDiagram")
@Singleton
@Unstable
public class InlineDiagramMacroRefactoring extends AbstractInlineDiagramMacroRefactoring
{
    @Inject
    protected DiagramContentHandler contentHandler;

    @Inject
    private DiagramLinkHandler linkHandler;

    /**
     * The name might be a bit miss leading, but on XWiki Document renames we are not interested in updating the macro,
     * but the attachment created by the macro which can have backlinks.
     *
     * @param macroBlock the macro block in which to replace the reference.
     * @param currentDocumentReference the reference of the document in which the block is located
     * @param sourceReference the reference to replace.
     * @param targetReference the reference to use as replacement.
     * @param relative if {@code true} indicate that the reference should be resolved relatively to the current
     *     document
     * @return will always return an empty optional since we will handle the update and saving of the document here.
     */
    @Override
    public Optional<MacroBlock> replaceReference(MacroBlock macroBlock, DocumentReference currentDocumentReference,
        DocumentReference sourceReference, DocumentReference targetReference, boolean relative)
        throws MacroRefactoringException
    {
        try {
            // Get the name of the attachment
            String diagramName = String.format(FORMAT_NAME, macroBlock.getParameter(DIAGRAM_NAME), ATTACHMENT_SUFFIX);
            // Get the actual document
            XWikiContext context = contextProvider.get();
            XWiki xwiki = context.getWiki();
            XWikiDocument documentContainingMacro = xwiki.getDocument(currentDocumentReference, context).clone();
            XWikiAttachment diagramAttachment = documentContainingMacro.getExactAttachment(diagramName);
            if (diagramAttachment == null) {
                logger.debug(
                    "Could not find the inline diagram attachment with the name [{}] on the document [{}]. The "
                        + "refactoring was attempted because the document [{}] was renamed/moved to [{}]", diagramName,
                    currentDocumentReference, sourceReference, targetReference);
                return Optional.empty();
            }
            Optional<String> updatedContent =
                updateLinkedDocuments(diagramAttachment, sourceReference, targetReference, context);
            if (updatedContent.isEmpty()) {
                return Optional.empty();
            }
            String newContent = updatedContent.get();
            diagramAttachment.setContent(new ByteArrayInputStream(newContent.getBytes(StandardCharsets.UTF_8)));
            xwiki.saveDocument(documentContainingMacro, "Refactor diagram attachment", true, context);
            // Since we handle the save ourselves there is no need return a block.
            return Optional.empty();
        } catch (XWikiException e) {
            throw new MacroRefactoringException("Something went wrong while trying to retrieve the XWiki document", e);
        } catch (IOException e) {
            throw new MacroRefactoringException("Something went wrong while trying to read the attachment", e);
        }
    }

    /**
     * Handles the update of the diagram links.
     *
     * @param attachment the attachment that contains the XML of the diagram.
     * @param originalRef the original reference of the link
     * @param newRef the new reference of the link
     * @param context the current context
     * @return an optional with the content if it was updated, otherwise an empty optional.
     */
    private Optional<String> updateLinkedDocuments(XWikiAttachment attachment, DocumentReference originalRef,
        DocumentReference newRef, XWikiContext context)
    {
        try (InputStream is = attachment.getContentInputStream(context)) {
            if (is == null) {
                return Optional.empty();
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);

            Document document = factory.newDocumentBuilder().parse(is);

            if (document.getDocumentElement() == null) {
                logger.error("XML Root Element is missing for {}", attachment.getFilename());
                return Optional.empty();
            }

            boolean updated = false;
            NodeList userObjectList = document.getElementsByTagName("UserObject");
            for (int i = 0; i < userObjectList.getLength(); i++) {
                updated |= linkHandler.updateUserObjectNode(userObjectList.item(i), newRef, originalRef);
            }

            NodeList mxCellList = document.getElementsByTagName("mxCell");
            for (int i = 0; i < mxCellList.getLength(); i++) {
                updated |= linkHandler.updateMxCellNode(mxCellList.item(i), newRef, originalRef);
            }

            return updated ? Optional.of(XMLUtils.serialize(document)) : Optional.empty();
        } catch (Exception e) {
            logger.error("Failed to update the links.", e);
            return Optional.empty();
        }
    }
}
