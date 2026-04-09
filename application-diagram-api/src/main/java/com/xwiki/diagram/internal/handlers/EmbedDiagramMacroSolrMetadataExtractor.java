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
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.MacroBlockMatcher;
import org.xwiki.search.solr.SolrEntityMetadataExtractor;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xwiki.diagram.internal.InlineDiagramManager;

/**
 * Responsible for registering the backlink to the source document.
 *
 * @version $Id$
 * @since 1.22.12
 */
@Component
@Named("macroembeddiagram")
@Singleton
public class EmbedDiagramMacroSolrMetadataExtractor implements SolrEntityMetadataExtractor<XWikiDocument>
{
    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    @Named("current")
    private EntityReferenceResolver<String> entityReferenceResolver;

    @Inject
    private LinkRegistry linkRegistry;

    @Override
    public boolean extract(XWikiDocument document, SolrInputDocument solrDocument)
    {
        XDOM xdom = document.getXDOM();
        List<Block> macroBlocks = xdom.getBlocks(new MacroBlockMatcher("embedDiagram"), Block.Axes.CHILD);
        if (macroBlocks != null && !macroBlocks.isEmpty()) {
            List<EntityReference> macroReferences = new ArrayList<>();
            for (Block macroBlock : macroBlocks) {
                // Handle the reference to the source document.
                String macroReference = macroBlock.getParameter("diagramSource");
                if (macroReference != null && !macroReference.isEmpty()) {
                    boolean isAttachment =
                        macroReference.endsWith(InlineDiagramManager.DIAGRAM_SUFFIX) && macroReference.contains("@");
                    if (isAttachment) {
                        // Handle the reference to the actual attachment. Yes, attachments have backreferences.
                        macroReferences.add(entityReferenceResolver.resolve(macroReference, EntityType.ATTACHMENT));
                    } else {
                        macroReferences.add(entityReferenceResolver.resolve(macroReference, EntityType.DOCUMENT));
                    }
                }
            }
            return linkRegistry.registerBacklinks(solrDocument, macroReferences);
        }
        return false;
    }
}
