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
import javax.inject.Singleton;

import org.apache.solr.common.SolrInputDocument;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.match.MacroBlockMatcher;
import org.xwiki.search.solr.SolrEntityMetadataExtractor;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Handles the backlinks of the diagram macro.
 *
 * @version $Id$
 * @since 1.20.8
 */
@Component
@Named("macrodiagram")
@Singleton
public class DiagramMacroSolrMetadataExtractor implements SolrEntityMetadataExtractor<XWikiDocument>
{
    @Inject
    @Named("explicit")
    private DocumentReferenceResolver<String> explicitDocumentReferenceResolver;

    @Inject
    private LinkRegistry linkRegistry;

    @Override
    public boolean extract(XWikiDocument document, SolrInputDocument solrDocument)
    {


        List<Block> macroBlocks =
            document.getXDOM().getBlocks(new MacroBlockMatcher("diagram"), Block.Axes.CHILD);
        if (macroBlocks != null && macroBlocks.size() > 0)
        {
            List<DocumentReference> macroReferences = new ArrayList<>();
            for (Block macroBlock : macroBlocks) {
                DocumentReference macroReference = explicitDocumentReferenceResolver.resolve(
                    macroBlock.getParameter("reference"), document.getDocumentReference()
                );
                macroReferences.add(macroReference);
            }
            return linkRegistry.registerBacklinks(solrDocument, macroReferences);


        }
        return false;
    }
}
