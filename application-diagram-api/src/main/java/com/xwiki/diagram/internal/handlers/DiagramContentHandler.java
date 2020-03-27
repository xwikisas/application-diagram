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
package com.xwiki.diagram.internal.handlers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.xml.XMLUtils;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Handler for diagram content operations.
 * 
 * @version $Id$
 * @since 1.13
 */
@Component(roles = DiagramContentHandler.class)
@Singleton
public class DiagramContentHandler
{
    /**
     * View action.
     */
    public static final String VIEW_ACTION = "view";

    /**
     * Reference to Diagram's class.
     */
    public static final LocalDocumentReference DIAGRAM_CLASS = new LocalDocumentReference("Diagram", "DiagramClass");

    @Inject
    private GetDiagramLinksHandler getDiagramLinksHandler;

    @Inject
    private DiagramLinkHandler linkHandler;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private Logger logger;

    /**
     * Update the attachments content and save the new version.
     * 
     * @param backlinkDoc document that has a backlink to diagram
     * @param oldDocRef reference of the page before rename
     * @param newDocRef reference of the page after rename
     * @throws XWikiException when an error occurs while accessing documents.
     * @throws IOException if something goes wrong while setting attachment's content.
     */
    public void updateAttachment(XWikiDocument backlinkDoc, DocumentReference oldDocRef, DocumentReference newDocRef)
        throws XWikiException, IOException
    {
        XWikiAttachment attachment = backlinkDoc.getAttachment("diagram.svg");
        XWikiContext context = contextProvider.get();

        if (attachment != null) {
            backlinkDoc.addAttachment(getUpdatedAttachment(attachment, oldDocRef, newDocRef));
            context.getWiki().saveDocument(backlinkDoc, "Updated attachment after page rename", context);
        }
    }

    /**
     * Migrate links inside diagram's attachment to the new name.
     * 
     * @param attachment attachment to be processed
     * @param oldDocRef reference of the page before rename
     * @param newDocRef reference of the page after rename
     * @return the modified attachment
     * @throws XWikiException when an error occurs while accessing documents.
     * @throws IOException if something goes wrong while setting attachment's content.
     */
    private XWikiAttachment getUpdatedAttachment(XWikiAttachment attachment, DocumentReference oldDocRef,
        DocumentReference newDocRef) throws XWikiException, IOException
    {
        XWikiContext context = contextProvider.get();
        XWikiDocument oldDoc = context.getWiki().getDocument(oldDocRef, context);
        XWikiDocument newDoc = context.getWiki().getDocument(newDocRef, context);

        String attachmentContent = IOUtils.toString(attachment.getContentInputStream(context), "UTF-8");

        String regex = "\"%s\"";
        String oldAbsoluteURL = String.format(regex, oldDoc.getExternalURL(VIEW_ACTION, context));
        String newAbsoluteURL = String.format(regex, newDoc.getExternalURL(VIEW_ACTION, context));
        String oldURL = String.format(regex, oldDoc.getURL(VIEW_ACTION, context));
        String newURL = String.format(regex, newDoc.getURL(VIEW_ACTION, context));

        String newContent = attachmentContent.replaceAll(oldAbsoluteURL, newAbsoluteURL).replaceAll(oldURL, newURL);

        attachment.setContent(new ByteArrayInputStream(newContent.getBytes()));

        return attachment;
    }

    /**
     * Update content of the diagram with new links.
     * 
     * @param backlinkDoc document that has a backlink to the diagram
     * @param originalDocRef reference of the document before rename
     * @param currentDocRef reference of the document after rename
     * @param context context of the execution
     * @throws ParserConfigurationException if a DocumentBuilder cannot be created
     * @throws SAXException if parsing the document fails
     * @throws IOException if any IO errors occur
     * @throws XWikiException if saving the document fails
     */
    public void updateDiagramContent(XWikiDocument backlinkDoc, DocumentReference originalDocRef,
        DocumentReference currentDocRef, XWikiContext context)
        throws ParserConfigurationException, SAXException, IOException, XWikiException
    {
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(new ByteArrayInputStream(backlinkDoc.getContent().getBytes()));

        NodeList userObjectList = document.getElementsByTagName("UserObject");
        for (int i = 0; i < userObjectList.getLength(); i++) {
            linkHandler.updateUserObjectNode(userObjectList.item(i), currentDocRef, originalDocRef);
        }

        NodeList mxCellList = document.getElementsByTagName("mxCell");
        for (int i = 0; i < mxCellList.getLength(); i++) {
            linkHandler.updateMxCellNode(mxCellList.item(i), currentDocRef, originalDocRef);
        }

        backlinkDoc.setContent(XMLUtils.serialize(document));
        context.getWiki().saveDocument(backlinkDoc, "Updated diagram after page rename", context);
    }

    /**
     * Search referenced pages inside the content of a diagram.
     * 
     * @param content the content of the diagram
     * @param diagramReference the reference of current diagram
     * @return linkedPages list of pages linked in this content
     */
    public List<DocumentReference> getLinkedPages(String content, DocumentReference diagramReference)
    {
        try {
            SAXParserFactory.newInstance().newSAXParser().parse(new ByteArrayInputStream(content.getBytes()),
                getDiagramLinksHandler);

            return getDiagramLinksHandler.getLinkedPages(diagramReference);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            logger.warn("Failed while getting diagram linked pages", e);
        }

        return Collections.emptyList();
    }
}
