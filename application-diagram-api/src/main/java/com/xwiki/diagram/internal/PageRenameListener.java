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
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Disposable;
import org.xwiki.job.Job;
import org.xwiki.job.JobContext;
import org.xwiki.job.event.JobStartedEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.ObservationContext;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Listens to rename of pages and starts a thread that will update backlinked diagrams.
 * 
 * @version $Id$
 * @since 1.13
 */
@Component
@Named(PageRenameListener.ROLE_HINT)
@Singleton
public class PageRenameListener extends AbstractEventListener implements Disposable
{
    /**
     * The role hint of the document.
     */
    protected static final String ROLE_HINT = "PageRenameEventListener";

    /**
     * Thread that will handle updating diagram's content after a page rename.
     */
    public Thread diagramThread;

    @Inject
    protected ObservationContext observationContext;

    @Inject
    protected JobContext jobContext;

    @Inject
    protected ComponentManager componentManager;

    @Inject
    private Logger logger;

    @Inject
    private DiagramRunnable diagramRunnable;

    /**
     * Constructor.
     */
    public PageRenameListener()
    {
        super(ROLE_HINT, new DocumentCreatedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (this.diagramThread == null) {
            this.diagramThread = new Thread(this.diagramRunnable);
            this.diagramRunnable.initilizeQueue();
            this.diagramThread.setName("Update Diagram Links Thread");
            this.diagramThread.setDaemon(true);
            this.diagramThread.start();
        }

        if (observationContext.isIn(new JobStartedEvent("refactoring/rename"))) {
            Job job = jobContext.getCurrentJob();
            DocumentReference destinationRef = job.getRequest().getProperty("destination");
            List<DocumentReference> references = job.getRequest().getProperty("entityReferences");

            if (references != null && references.size() > 0) {
                XWikiDocument currentDoc = (XWikiDocument) source;

                DocumentReference originalDocRef = references.get(0);
                DocumentReference currentDocRef = currentDoc.getDocumentReference();

                if (destinationRef.equals(currentDocRef)) {
                    this.diagramRunnable.addToQueue(new DiagramQueueEntry(originalDocRef, currentDocRef));
                }
            }
        }
    }

    /**
     * Actions for closing the thread.
     * 
     * @throws InterruptedException if any thread has interrupted the current thread
     */
    public void stopUpdateDiagramLinksThread() throws InterruptedException
    {
        if (this.diagramThread != null) {
            this.diagramRunnable.addToQueue(DiagramRunnable.STOP_RUNNABLE_ENTRY);
            this.diagramThread.interrupt();
            this.diagramThread.join();
        }
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        try {
            stopUpdateDiagramLinksThread();
        } catch (InterruptedException e) {
            logger.debug("Diagram update links thread interruped", e);
        }
    }
}
