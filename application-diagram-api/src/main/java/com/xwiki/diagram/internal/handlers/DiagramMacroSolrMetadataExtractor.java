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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.validation.EntityNameValidationConfiguration;
import org.xwiki.model.validation.EntityNameValidationManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.MacroBlockMatcher;
import org.xwiki.search.solr.SolrEntityMetadataExtractor;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Handles the backlinks of the diagram macro. Ensure that the diagram macro always has a valid reference even if the
 * page is renamed or moved.
 *
 * @version $Id$
 * @since 1.21
 */
@Component
@Named("macrodiagram")
@Singleton
public class DiagramMacroSolrMetadataExtractor implements SolrEntityMetadataExtractor<XWikiDocument>
{
    private static final String PARAMETER_NAME = "reference";

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    @Named("explicit")
    private DocumentReferenceResolver<String> explicitDocumentReferenceResolver;

    @Inject
    private LinkRegistry linkRegistry;

    @Inject
    private EntityNameValidationManager entityNameValidationManager;

    @Inject
    private EntityNameValidationConfiguration entityNameValidationConfiguration;

    @Inject
    private Logger logger;

    @Override
    public boolean extract(XWikiDocument document, SolrInputDocument solrDocument)
    {

        XDOM xdom = document.getXDOM();
        List<Block> macroBlocks = xdom.getBlocks(new MacroBlockMatcher("diagram"), Block.Axes.CHILD);
        if (macroBlocks != null && !macroBlocks.isEmpty() && !updateMacroReference(
            document, xdom, macroBlocks))
        {
            List<EntityReference> macroReferences = new ArrayList<>();
            for (Block macroBlock : macroBlocks) {

                DocumentReference macroReference =
                    explicitDocumentReferenceResolver.resolve(macroBlock.getParameter(PARAMETER_NAME),
                        document.getDocumentReference());
                macroReferences.add(macroReference);
            }
            return linkRegistry.registerBacklinks(solrDocument, macroReferences);
        }
        return false;
    }

    /**
     * Checks and updates the references of all the diagram macro calls to make sure that all of them respect the
     * current name strategy.
     *
     * @param document document with all the macro calls
     * @param xdom of the @document
     * @param macroBlocks list of all the diagram macro calls
     * @return true if any reference was invalid and has been updated, false if there weren't any invalid references or
     *     if an error occurred while updating the document
     */
    public boolean updateMacroReference(XWikiDocument document, XDOM xdom, List<Block> macroBlocks)
    {
        try {

            XWikiContext context = contextProvider.get();
            boolean modified = false;
            for (Block macroBlock : macroBlocks) {
                boolean addDefaultValue = (macroBlock.getParameter(PARAMETER_NAME) == null);
                String referenceName = addDefaultValue ? "Diagram" : macroBlock.getParameter(PARAMETER_NAME);
                // For backwards compatibility we check if the page already exists so we won't modify it.
                DocumentReference macroReference =
                    explicitDocumentReferenceResolver.resolve(referenceName, document.getDocumentReference());
                boolean diagramAlreadyExists = context.getWiki().exists(macroReference, context);
                if (!diagramAlreadyExists) {
                    // First we check if the name is valid in the current naming strategy.
                    boolean isValid = this.isValid(referenceName);
                    // If the name is valid then we can use it, otherwise we transform the name in a valid
                    // one and update the macro block. If the macro reference is empty we also update it with the
                    // right reference.
                    if (!isValid || addDefaultValue) {
                        String transformedName = this.transformName(referenceName);
                        logger.debug("The reference [{}] was updated to [{}] to respect the current name strategy. "
                            + "Document: [{}]", referenceName, transformedName, document.getDocumentReference());
                        macroBlock.setParameter(PARAMETER_NAME, transformedName);
                        modified = true;
                    }
                } else if (addDefaultValue) {
                    // If the page already exists, but the macro parameter is empty we should still add a value to it
                    // because when we try to move it will fail otherwise.
                    macroBlock.setParameter(PARAMETER_NAME, referenceName);
                    modified = true;
                }
            }
            if (modified) {
                document.setContent(xdom);
                context.getWiki()
                    .saveDocument(document, "Updated diagram macro references to respect the name strategy.", context);
                return modified;
            }
        } catch (XWikiException e) {
            logger.error("Failed to update diagram macro references of [{}] to respect the naming strategy.", document,
                e);
            return false;
        }
        return false;
    }

    private String transformName(String name)
    {
        // this.entityNameValidationConfiguration.useTransformation() is a property that MUST be set by the user in the
        // Administration -> Editing -> Name Strategies -> transform names automatically, if the property is disabled
        // the code will always return the original name and not the transformed one.
        if (this.entityNameValidationConfiguration.useTransformation()
            && this.entityNameValidationManager.getEntityReferenceNameStrategy() != null)
        {
            return this.entityNameValidationManager.getEntityReferenceNameStrategy().transform(name);
        } else {
            return name;
        }
    }

    private boolean isValid(String name)
    {
        // this.entityNameValidationConfiguration.useValidation() is a property that MUST be set by the user in the
        // Administration -> Editing -> Name Strategies -> validate names before saving, if the property is disabled
        // this code will always return false.
        if (this.entityNameValidationConfiguration.useValidation()
            && this.entityNameValidationManager.getEntityReferenceNameStrategy() != null)
        {
            return this.entityNameValidationManager.getEntityReferenceNameStrategy().isValid(name);
        } else {
            return true;
        }
    }
}
