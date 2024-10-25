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

import org.hibernate.Session;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.XWikiHibernateStore;
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
    @Named("hibernate")
    private Provider<XWikiStoreInterface> hibernateStoreProvider;

    @Inject
    private BackLinkHandler backLinkHandler;

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
        backLinkHandler.createBackReference(documentReference, linkedDocRef);
    }

    /**
     * Delete backlinks from this document.
     *
     * @param doc the id of the document
     * @param context the current request context
     * @throws XWikiException fail during delete action
     */
    public void deleteLinks(DocumentReference doc, XWikiContext context) throws XWikiException
    {
        backLinkHandler.clearBackLinks(doc);
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
