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
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.hibernate.Query;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.QueryManager;
import org.xwiki.search.solr.internal.SolrSearchCoreUtils;
import org.xwiki.search.solr.internal.api.SolrInstance;
import org.xwiki.search.solr.internal.metadata.SolrLinkSerializer;
import org.xwiki.search.solr.internal.metadata.SolrMetadataExtractor;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xwiki.diagram.internal.DiagramSolrMetadataExtractor;

/**
 * Handler for XWikiStore operations.
 *
 * @version $Id$
 * @since 1.13
 */
@Component(roles = StoreHandler.class)
@Singleton
public class StoreHandler
{
    @Inject
    protected SolrLinkSerializer linkSerializer;

    @Inject
    private SolrSearchCoreUtils seachUtils;

    @Inject
    private ComponentManager manager;

    @Inject
    @Named("diagram")
    private SolrMetadataExtractor diagramSolrDocumentUpdated;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    private QueryManager queryManager;

    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> localEntityReferenceSerializer;

    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> compactwikiEntityReferenceSerializer;

    @Inject
    @Named("hibernate")
    private Provider<XWikiStoreInterface> hibernateStoreProvider;

    @Inject
    private Logger logger;

    @Inject
    private SolrInstance solrInstance;
    @Inject
    private SolrSearchCoreUtils solrSearchCoreUtils;

    /**
     * Add a backlink between 2 documents.
     *
     * @param session the Hibernate Session
     * @param document document that contains a reference to another document
     * @param linkedDocRef reference of the linked document
     * @param context the context in which the code is executed.
     */
    public void addXWikiLink(Session session, XWikiDocument document, DocumentReference linkedDocRef,
        XWikiContext context)
    {

        DocumentReference documentReference = document.getDocumentReference();
        EntityReference originDocEntity =  new EntityReference(documentReference.getName(),
            documentReference.getType(), documentReference.getParent());
        EntityReference linkedDocEntity =  new EntityReference(linkedDocRef.getName(), linkedDocRef.getType(),
            linkedDocRef.getParent());

        SolrInputDocument solrInputDocument =
            ((DiagramSolrMetadataExtractor) diagramSolrDocumentUpdated).updateSolrDocument(originDocEntity,
            linkedDocEntity);
        try {
            solrInstance.add(solrInputDocument);
            solrInstance.commit();

        } catch (SolrServerException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

//        XWikiLink wikiLink = new XWikiLink();
//        String serializedParentDocRef = localEntityReferenceSerializer.serialize(document.getDocumentReference());
//        String serializedLinkedDocRef = compactwikiEntityReferenceSerializer.serialize(linkedDocRef);
//
//        wikiLink.setDocId(document.getId());
//        wikiLink.setFullName(serializedParentDocRef);
//        wikiLink.setLink(serializedLinkedDocRef);
//
//        try {
//            if (StringUtils.length(wikiLink.getLink()) > getStore().getLimitSize(context, XWikiLink.class, "link")) {
//                logger.warn("Failed to add a backlink to [{}] since [{}] exceeds the 253 characters limit",
//                    serializedParentDocRef, serializedLinkedDocRef);
//            } else {
//                session.saveOrUpdate(wikiLink);
//            }
//        } catch (ComponentLookupException e) {
//            logger.warn("Failed to add a backlink to [{}]. Cause [{}].", serializedParentDocRef,
//                ExceptionUtils.getRootCauseMessage(e));
//        }
    }

    /**
     * Delete backlinks from this document.
     *
     * @param docId the id of the document
     * @param context the current request context
     * @throws XWikiException fail during delete action
     */
    public void deleteLinks(long docId, XWikiContext context) throws XWikiException
    {
        try {
            getStore().executeWrite(context, new HibernateCallback<Object>()
            {
                @Override
                public Object doInHibernate(Session session) throws XWikiException
                {
                    Query query = session.createQuery("delete from XWikiLink as link where link.id.docId = :docId");
                    query.setParameter("docId", docId);
                    query.executeUpdate();

                    return Boolean.TRUE;
                }
            });
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_DELETING_LINKS, "Exception while deleting links", e);
        }
    }

    /**
     * Get instance of XWikiHibernateStore.
     *
     * @return store system for execute store-specific actions.
     * @throws ComponentLookupException if the store could not be reached
     */
    public XWikiHibernateStore getStore() throws ComponentLookupException
    {
        return (XWikiHibernateStore) hibernateStoreProvider.get();
    }

}
