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
import javax.inject.Singleton;

import org.hibernate.Session;
import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xwiki.diagram.internal.handlers.DiagramContentHandler;
import com.xwiki.diagram.internal.handlers.StoreHandler;

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
    private DiagramContentHandler contentHandler;

    @Inject
    private StoreHandler storeHandler;

    @Inject
    private Logger logger;

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
                storeHandler.deleteLinks(document.getId(), context);

                storeHandler.getStore().executeWrite(context, new HibernateCallback<Object>()
                {
                    @Override
                    public Object doInHibernate(Session session) throws XWikiException
                    {
                        // Is necessary to blank links from doc.
                        context.remove("links");

                        // Add backlinks from pages linked by this diagram.
                        for (DocumentReference linkedDocRef : contentHandler.getLinkedPages(document.getContent(),
                            document.getDocumentReference())) {
                            storeHandler.addXWikiLink(session, document, linkedDocRef);
                        }

                        return Boolean.TRUE;
                    }
                });
            } catch (Exception e) {
                logger.warn("Failed to update backlinks from diagram content", e);
            }
        }
    }
}
