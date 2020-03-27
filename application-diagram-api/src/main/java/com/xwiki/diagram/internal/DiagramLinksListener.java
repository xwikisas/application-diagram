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

import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Query;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiLink;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xwiki.diagram.internal.handlers.DiagramContentHandler;

/**
 * Listens to created or updated diagrams and adds backlinks from pages linked inside them.
 * 
 * @version $Id$
 * @since 1.13
 */
@Component
@Named(DiagramLinksListener.ROLE_HINT)
@Singleton
public class DiagramLinksListener extends AbstractEventListener
{
    /**
     * The role hint used for this component.
     */
    protected static final String ROLE_HINT = "DiagramCreatedListener";

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
    private DiagramContentHandler contentHandler;

    /**
     * Constructor.
     */
    public DiagramLinksListener()
    {
        super(ROLE_HINT, Arrays.<Event>asList(new DocumentUpdatedEvent(), new DocumentCreatedEvent()));
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument document = (XWikiDocument) source;
        XWikiContext context = (XWikiContext) data;

        if (document.getXObject(DiagramContentHandler.DIAGRAM_CLASS) != null) {
            try {
                // We need to delete existing links before saving the page's ones.
                deleteLinks(document.getId(), context);

                getStore().executeWrite(context, new HibernateCallback<Object>()
                {
                    @Override
                    public Object doInHibernate(Session session) throws XWikiException
                    {
                        // Is necessary to blank links from doc.
                        context.remove("links");

                        // Get pages linked by this diagram.
                        for (DocumentReference linkedDocRef : contentHandler.getLinkedPages(document.getContent(),
                            document.getDocumentReference())) {
                            XWikiLink wikiLink = getXWikiLink(document, linkedDocRef);
                            session.save(wikiLink);
                        }

                        return Boolean.TRUE;
                    }
                });
            } catch (Exception e) {
                logger.warn("Failed to update backlinks from diagram content", e);
            }
        }
    }

    /**
     * Get a document as XWikiLink.
     * 
     * @param document diagram document
     * @param linkedDocRef reference of the linked document
     * @return document casted to XWikiLink
     */
    private XWikiLink getXWikiLink(XWikiDocument document, DocumentReference linkedDocRef)
    {
        XWikiLink wikiLink = new XWikiLink();

        wikiLink.setDocId(document.getId());
        wikiLink.setFullName(localEntityReferenceSerializer.serialize(document.getDocumentReference()));
        wikiLink.setLink(compactwikiEntityReferenceSerializer.serialize(linkedDocRef));

        // Verify that the link reference isn't larger than 255 characters (and truncate it if
        // that's the case) since otherwise that would lead to a DB error that would result in
        // a fatal error, and the user would have a hard time understanding why his page failed
        // to be saved.
        wikiLink.setLink(StringUtils.substring(wikiLink.getLink(), 0, 255));

        return wikiLink;
    }

    /**
     * Delete backlinks from this document.
     * 
     * @param docId the id of the document
     * @param context the current request context
     * @throws XWikiException fail during delete action
     */
    private void deleteLinks(long docId, XWikiContext context) throws XWikiException
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
     * @return store system for execute store-specific actions.
     * @throws ComponentLookupException if the store could not be reached
     */
    private XWikiHibernateBaseStore getStore() throws ComponentLookupException
    {
        return (XWikiHibernateBaseStore) hibernateStoreProvider.get();
    }
}
