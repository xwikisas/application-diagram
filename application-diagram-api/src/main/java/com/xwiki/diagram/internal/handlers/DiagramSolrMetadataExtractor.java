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

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.solr.common.SolrInputDocument;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.search.solr.SolrEntityMetadataExtractor;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Handles the backlinks of a diagram. Ensure that the diagram links are updated and point to the correct page
 * whenever a page is moved or renamed.
 *
 * @version $Id$
 * @since 1.20.8
 */
@Component
@Singleton
public class DiagramSolrMetadataExtractor implements SolrEntityMetadataExtractor<XWikiDocument>
{
    @Inject
    private DiagramContentHandler diagramContentHandler;

    @Inject
    private LinkRegistry linkRegistry;

    @Override
    public boolean extract(XWikiDocument document, SolrInputDocument solrDocument)
    {
        // Updates for the diagram directly.
        if (document.getXObject(DiagramContentHandler.DIAGRAM_CLASS) != null) {
            List<EntityReference> references =
                diagramContentHandler.getLinkedPages(document.getContent(), document.getDocumentReference());
            return linkRegistry.registerBacklinks(solrDocument, references);
        }
        return false;
    }
}
