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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Query;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiLink;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.XWikiStoreInterface;

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

    /**
     * Add a backlink between 2 documents.
     * 
     * @param session the Hibernate Session
     * @param document document that contains a reference to another document
     * @param linkedDocRef reference of the linked document
     */
    public void addXWikiLink(Session session, XWikiDocument document, DocumentReference linkedDocRef)
    {
        XWikiLink wikiLink = new XWikiLink();
        String serializedParentDocRef = localEntityReferenceSerializer.serialize(document.getDocumentReference());
        String serializedLinkedDocRef = compactwikiEntityReferenceSerializer.serialize(linkedDocRef);

        wikiLink.setDocId(document.getId());
        wikiLink.setFullName(serializedParentDocRef);
        wikiLink.setLink(serializedLinkedDocRef);

        // Verify that the link reference isn't larger than 253 characters since otherwise that would lead to a DB
        // error that would result in a fatal error. 253 is used instead of 255 since this is a combined identifier
        // along with docId. The size of the field was increased starting with https://jira.xwiki.org/browse/XWIKI-18429
        // to 768 and a new API was added to get this size programmatically, but the HibernateStore#getLimitSize has
        // problems for combined identifiers, as stated in https://jira.xwiki.org/browse/XWIKI-18678.
        // TODO: Modify the conditions to have the limit at 766 when the XWiki version is > 13.2 and start using
        // HibernateStore#getLimitSize when XWiki version > 12.10.8 / 13.4.
        if (StringUtils.length(wikiLink.getLink()) > 253) {
            logger.warn("Failed to add a backlink to [{}] since [{}] exceeds the 253 characters limit",
                serializedParentDocRef, serializedLinkedDocRef);
        } else {
            session.saveOrUpdate(wikiLink);
        }
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
    public XWikiHibernateBaseStore getStore() throws ComponentLookupException
    {
        return (XWikiHibernateBaseStore) hibernateStoreProvider.get();
    }
}
