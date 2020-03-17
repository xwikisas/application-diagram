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

import java.io.IOException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.xml.sax.SAXException;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.util.AbstractXWikiRunnable;
import com.xwiki.diagram.internal.handlers.DiagramContentHandler;

/**
 * Updates content and attachment of diagram for rename of backlinked pages.
 * 
 * @version $Id$
 * @since 1.13
 */
@Component(roles = DiagramRunnable.class)
@Singleton
public class DiagramRunnable extends AbstractXWikiRunnable
{
    /**
     * Reference to Diagram's class.
     */
    public static final LocalDocumentReference DIAGRAM_CLASS = new LocalDocumentReference("Diagram", "DiagramClass");

    /**
     * Stop runnable entry.
     */
    public static final DiagramQueueEntry STOP_RUNNABLE_ENTRY = new DiagramQueueEntry(null, null);

    @Inject
    private Execution execution;

    @Inject
    private Logger logger;

    /**
     * Entries to be processed by this thread.
     */
    private BlockingQueue<DiagramQueueEntry> diagramsQueue;

    /**
     * Backlinks of the document.
     */
    private List<DocumentReference> backlinks;

    /**
     * Handler for operation performed on diagram content.
     */
    private DiagramContentHandler contentHandler;

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.util.AbstractXWikiRunnable#runInternal()
     */
    @Override
    public void runInternal()
    {
        while (!Thread.interrupted()) {
            DiagramQueueEntry queueEntry;
            try {
                queueEntry = diagramsQueue.take();
            } catch (InterruptedException e) {
                logger.warn("Diagrams update thread has been interrupted", e);
                queueEntry = STOP_RUNNABLE_ENTRY;
            }

            if (queueEntry == STOP_RUNNABLE_ENTRY) {
                diagramsQueue.clear();
                diagramsQueue.offer(STOP_RUNNABLE_ENTRY);
                break;
            }

            XWikiContext context = (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
            DocumentReference originalDocRef = queueEntry.originalDocRef;
            DocumentReference currentDocRef = queueEntry.currentDocRef;

            contentHandler = new DiagramContentHandler();

            try {
                // We need to take backlinks from the original document because at this step they are not loaded on
                // the new document.
                backlinks = context.getWiki().getDocument(originalDocRef, context).getBackLinkedReferences(context);

                XWikiDocument backlinkDoc;
                for (DocumentReference backlinkRef : backlinks) {
                    backlinkDoc = context.getWiki().getDocument(backlinkRef, context);

                    if (backlinkDoc.getXObject(DIAGRAM_CLASS) != null) {
                        contentHandler.updateDiagramContent(backlinkDoc, originalDocRef, currentDocRef, context);

                        contentHandler.updateAttachment(backlinkDoc, context, originalDocRef, currentDocRef);
                    }
                }
            } catch (XWikiException e) {
                logger.warn(e.getMessage(), e);
            } catch (IOException e) {
                logger.warn(e.getMessage(), e);
            } catch (ParserConfigurationException e) {
                logger.warn(e.getMessage(), e);
            } catch (SAXException e) {
                logger.warn(e.getMessage(), e);
            }
        }
    }

    /**
     * Add entries to the thread's queue.
     * 
     * @param queueEntry the entry to be added
     */
    public void addToQueue(DiagramQueueEntry queueEntry)
    {
        this.diagramsQueue.add(queueEntry);
    }

    /**
     * Create an empty blocking queue.
     */
    public void initilizeQueue()
    {
        this.diagramsQueue = new LinkedBlockingQueue<DiagramQueueEntry>();
    }
}
