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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.solr.common.SolrInputDocument;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.search.solr.SolrEntityMetadataExtractor;
import org.xwiki.search.solr.internal.api.FieldUtils;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Test to register component.
 *
 * @version $Id$
 * @since 1.13
 */
@Component
@Singleton
public class DiagramSolrMetadataExtractor implements SolrEntityMetadataExtractor<XWikiDocument>
{
    private static final String ENTITY_PREFIX = "entity:";

    @Inject
    private DiagramContentHandler diagramContentHandler;

    @Inject
    @Named("withtype/withparameters")
    private EntityReferenceSerializer<String> entitySerializer;

    @Override
    public boolean extract(XWikiDocument entity, SolrInputDocument solrDocument)
    {
        // Updates for the diagram directly.
        if (entity.getXObject(DiagramContentHandler.DIAGRAM_CLASS) != null) {
            registerBacklinks(entity, solrDocument);
            return true;
        }
        return false;
    }

    private void registerBacklinks(XWikiDocument document, SolrInputDocument solrDocument)
    {

        List<DocumentReference> references =
            diagramContentHandler.getLinkedPages(document.getContent(), document.getDocumentReference());

        Set<String> links = new HashSet<>(references.size());
        Set<String> extenedLinks = new HashSet<>(references.size());
        for (DocumentReference reference : references) {
            EntityReference entityReference =
                new EntityReference(reference.getName(), reference.getType(), reference.getParent());
            String serializedEntitiy = entitySerializer.serialize(entityReference);
            links.add(serializedEntitiy);
            extenedLinks.add(serializedEntitiy);
            this.extendLink(entityReference, extenedLinks);
        }
        for (String link : links) {
            solrDocument.addField(FieldUtils.LINKS, link);
        }
        for (String linkExtended : extenedLinks) {
            solrDocument.addField(FieldUtils.LINKS_EXTENDED, linkExtended);
        }
    }

    private String serialize(EntityReference reference)
    {
        return ENTITY_PREFIX + this.entitySerializer.serialize(reference);
    }

    private void extendLink(EntityReference reference, Set<String> linksExtended)
    {
        for (EntityReference parent = reference.getParameters().isEmpty() ? reference
            : new EntityReference(reference.getName(), reference.getType(), reference.getParent(), null);
            parent != null; parent = parent.getParent()) {
            linksExtended.add(this.serialize(parent));
        }
    }
}
