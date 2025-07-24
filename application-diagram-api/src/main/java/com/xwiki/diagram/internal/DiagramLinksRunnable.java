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

import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xwiki.diagram.internal.handlers.DiagramContentHandler;

/**
 * Updates content and attachment of a diagram after the rename of backlinked pages.
 * 
 * @version $Id$
 * @since 1.13
 */
@Component(roles = DiagramLinksRunnable.class)
@Singleton
public class DiagramLinksRunnable extends AbstractDiagramRunnable
{
    @Inject
    private Logger logger;

    @Inject
    private DiagramContentHandler contentHandler;

    @Inject
    private Provider<XWikiContext> contextProvider;

    /**
     * @see com.xpn.xwiki.util.AbstractXWikiRunnable#runInternal()
     */
    @Override
    public void runInternal()
    {
        while (!Thread.interrupted()) {
            DiagramQueueEntry queueEntry = getNextDiagramQueueEntry();

            if (queueEntry == STOP_RUNNABLE_ENTRY) {
                break;
            }

            XWikiContext context = contextProvider.get();
            DocumentReference originalDocRef = queueEntry.originalDocRef;
            DocumentReference currentDocRef = queueEntry.currentDocRef;

            try {
                // We need to take backlinks from the original document because at this step they are not loaded on
                // the new document.
                List<DocumentReference> backlinks =
                    context.getWiki().getDocument(originalDocRef, context).getBackLinkedReferences(context);

                XWikiDocument backlinkDoc;
                for (DocumentReference backlinkRef : backlinks) {
                    backlinkDoc = context.getWiki().getDocument(backlinkRef, context).clone();

                    if (backlinkDoc.getXObject(DiagramContentHandler.DIAGRAM_CLASS) != null) {
                        contentHandler.updateDiagramContent(backlinkDoc, originalDocRef, currentDocRef, context);

                        contentHandler.updateAttachment(backlinkDoc, originalDocRef, currentDocRef);
                    }
                }
            } catch (Exception e) {
                logger.warn("Update diagram backlinks thread interrupted", e);
            }
        }
    }
}
