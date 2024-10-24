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
package com.xwiki.diagram.internal;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.search.solr.internal.api.FieldUtils;
import org.xwiki.search.solr.internal.api.SolrIndexerException;
import org.xwiki.search.solr.internal.metadata.DocumentSolrMetadataExtractor;
import org.xwiki.search.solr.internal.metadata.LengthSolrInputDocument;

/**
 * Handler for diagram content operations.
 *
 * @version $Id$
 * @since 1.13
 */
@Component
@Named("diagram")
@Singleton
public class DiagramSolrMetadataExtractor extends DocumentSolrMetadataExtractor
{
    /**
     * Returns the solr document with updated links.
     * @param sourceDocument orgin of solr document
     * @param referencedDocument link to be added
     * @return solr document.
     */
    public LengthSolrInputDocument updateSolrDocument(EntityReference sourceDocument,
        EntityReference referencedDocument)
    {
        try {
            LengthSolrInputDocument solrInputDocument = super.getSolrDocument(sourceDocument);
            // Append our link to the already existing links.
            appendLink(referencedDocument, solrInputDocument);
            return solrInputDocument;
        } catch (SolrIndexerException e) {
            return null;
        }
    }
    private void appendLink(EntityReference referencedDocument, LengthSolrInputDocument solrInputDocument)
    {
        Set<String> link = new HashSet<>(1);
        Set<String> linkExtened =  new HashSet<>(3);

        String referenceString = super.linkSerializer.serialize(referencedDocument);
        link.add(referenceString);
        linkExtened.add(referenceString);
        extendLink(referencedDocument, linkExtened);

        solrInputDocument.addField(FieldUtils.LINKS, link);

        for (String linkEx : linkExtened)
        {
            solrInputDocument.addField(FieldUtils.LINKS_EXTENDED, linkExtened);
        }
    }
}
