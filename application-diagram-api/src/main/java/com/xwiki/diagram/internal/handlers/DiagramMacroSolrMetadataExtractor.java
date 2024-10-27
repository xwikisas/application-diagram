package com.xwiki.diagram.internal.handlers;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.solr.common.SolrInputDocument;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.match.MacroBlockMatcher;
import org.xwiki.search.solr.SolrEntityMetadataExtractor;

import com.xpn.xwiki.doc.XWikiDocument;

public class DiagramMacroSolrMetadataExtractor implements SolrEntityMetadataExtractor<XWikiDocument>
{

    @Inject
    @Named("explicit")
    private DocumentReferenceResolver<String> explicitDocumentReferenceResolver;

    @Override
    public boolean extract(XWikiDocument entity, SolrInputDocument solrDocument)
    {

        List<Block> macroBlocks = entity.getXDOM().getBlocks(new MacroBlockMatcher("diagram"), Block.Axes.CHILD);

        if (macroBlocks != null && !macroBlocks.isEmpty())
        {
            for (Block macroBlock : macroBlocks)
            {
                DocumentReference documentReference = explicitDocumentReferenceResolver.resolve(
                    macroBlock.getParameter("reference"), entity.getDocumentReference());


            }
        }

        return false;
    }
}
