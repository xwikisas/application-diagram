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

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.search.solr.internal.api.SolrIndexerException;
import org.xwiki.search.solr.internal.api.SolrInstance;
import org.xwiki.search.solr.internal.metadata.SolrMetadataExtractor;

import com.xwiki.diagram.internal.DiagramSolrMetadataExtractor;

/**
 * Handles the storage of the backlinks of a diagram.
 *
 * @version $Id$
 * @since 1.20.8
 */
@Component(roles = BackLinkHandler.class)
@Singleton
public class BackLinkHandler
{
    @Inject
    @Named("diagram")
    private SolrMetadataExtractor diagramSolrDocumentUpdated;

    @Inject
    private SolrInstance solrInstance;

    @Inject
    private Logger logger;

    /**
     * Creates a backreference between two documents.
     * @param sourceDocument source document
     * @param referencedDocument referenced document
     */
    public void createBackReference(DocumentReference sourceDocument, DocumentReference referencedDocument)

    {

        EntityReference soruceEntityReference =
            new EntityReference(sourceDocument.getName(), sourceDocument.getType(), sourceDocument.getParent());
        EntityReference referencedEntityReference =
            new EntityReference(referencedDocument.getName(), referencedDocument.getType(),
                referencedDocument.getParent());

        try {
            SolrInputDocument solrInputDocument =
                ((DiagramSolrMetadataExtractor) diagramSolrDocumentUpdated).updateSolrDocument(
                    soruceEntityReference, referencedEntityReference);
            solrInstance.add(solrInputDocument);
            solrInstance.commit();
        } catch (SolrServerException | IOException e) {
            logger.warn(
                "Failed to add backreference. Source document: {}, Referenced document: {}. Exception message: {}",
                sourceDocument, referencedDocument, e.getMessage(), e);
            try{
                solrInstance.rollback();
            }catch (Exception e2)
            {
                logger.warn("Failed to rollback the index change.", e2);
            }

        }
    }

    /**
     * Clears all the backlinks of the @sourceDocument.
     * @param sourceDocument the document for witch we want to clear the backlinks.
     */
    public void clearBackLinks(DocumentReference sourceDocument)
    {
        EntityReference sourceEntityReference = new EntityReference(sourceDocument.getName(),
            sourceDocument.getType(), sourceDocument.getParent());
        try {
            SolrInputDocument solrInputDocument = diagramSolrDocumentUpdated.getSolrDocument(sourceEntityReference);
            solrInstance.delete(solrInputDocument.get("id").getValue().toString());
            solrInstance.commit();
        } catch (SolrServerException | IOException | SolrIndexerException e) {
            logger.warn(
                "Failed to clear the backreferences. Source document: {}, Exception message: "
                    + "{}",
                sourceDocument, e.getMessage(), e);
        }
    }
}
