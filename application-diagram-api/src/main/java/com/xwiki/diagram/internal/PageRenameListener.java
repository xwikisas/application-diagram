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
import com.xwiki.diagram.internal.handlers.DiagramContentHandler;

/**
 * Listens to rename of pages and starts a thread that will update the content of backlinked diagrams. Also, for diagram
 * pages it will start a thread for updating references of the diagram macro.
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
     * Thread that will handle updating diagram's content and attachment after a page rename.
     */
    public Thread diagramLinksThread;

    /**
     * Thread that will handle updating the reference of a diagram macro after a diagram rename.
     */
    public Thread diagramMacroThread;

    @Inject
    protected ObservationContext observationContext;

    @Inject
    protected JobContext jobContext;

    @Inject
    protected ComponentManager componentManager;

    @Inject
    private Logger logger;

    @Inject
    private DiagramLinksRunnable diagramLinksRunnable;

    @Inject
    private DiagramMacroRunnable diagramMacroRunnable;

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
        if (this.diagramLinksThread == null) {
            this.diagramLinksThread = startThread(this.diagramLinksRunnable, "Update Diagram Links Thread");
        }
        if (this.diagramMacroThread == null) {
            this.diagramMacroThread = startThread(this.diagramMacroRunnable, "Update Diagram Macro Thread");
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
                    startContentUpdating(currentDoc, new DiagramQueueEntry(originalDocRef, currentDocRef));
                }
            }
        }
    }

    /**
     * Add entries in the queue of threads that will update the content of pages after rename, in just two cases: update
     * content of a diagram after renaming pages that are linked in the content, update reference parameter of diagram
     * macro after renaming a diagram.
     *
     * @param currentDoc current document
     * @param queueEntry entry to be added in the thread queue
     */
    public void startContentUpdating(XWikiDocument currentDoc, DiagramQueueEntry queueEntry)
    {
        if (currentDoc.getXObject(DiagramContentHandler.DIAGRAM_CLASS) != null) {
            this.diagramMacroRunnable.addToQueue(queueEntry);
        }
        this.diagramLinksRunnable.addToQueue(queueEntry);
    }

    /**
     * Actions for starting a thread.
     *
     * @param diagramRunnable runnable object that implements the run method
     * @param threadName name of the thread
     * @return thread that was started
     */
    public Thread startThread(AbstractDiagramRunnable diagramRunnable, String threadName)
    {
        Thread diagramThread = new Thread(diagramRunnable);
        diagramRunnable.initilizeQueue();
        diagramThread.setName(threadName);
        diagramThread.setDaemon(true);
        diagramThread.start();

        return diagramThread;
    }

    /**
     * Actions for closing a thread.
     * 
     * @param diagramThread thread to be stopped
     * @param diagramRunnable runnable object of the thread
     * @throws InterruptedException if any thread has interrupted the current thread
     */
    public void stopThread(Thread diagramThread, AbstractDiagramRunnable diagramRunnable) throws InterruptedException
    {
        if (diagramThread != null) {
            diagramRunnable.addToQueue(AbstractDiagramRunnable.STOP_RUNNABLE_ENTRY);
            diagramThread.join();
        }
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        try {
            stopThread(this.diagramLinksThread, this.diagramLinksRunnable);
            stopThread(this.diagramMacroThread, this.diagramMacroRunnable);
        } catch (InterruptedException e) {
            logger.debug("Diagram backlinks update thread interruped", e);
        }
    }
}
