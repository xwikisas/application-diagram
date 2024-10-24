package com.xwiki.diagram.internal.handlers;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.search.solr.internal.api.SolrInstance;
import org.xwiki.search.solr.internal.metadata.SolrMetadataExtractor;

public class BackLinkHandler
{
    @Inject
    @Named("diagram")
    private SolrMetadataExtractor diagramSolrDocumentUpdated;


    @Inject
    private SolrInstance solrInstance;

    public void createBackReference(DocumentReference source, )
    {

    }
}
