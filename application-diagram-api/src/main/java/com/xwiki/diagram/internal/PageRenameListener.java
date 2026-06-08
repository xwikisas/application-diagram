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
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.phase.Disposable;
import org.xwiki.job.JobContext;
import org.xwiki.job.event.JobStartedEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.ObservationContext;
import org.xwiki.observation.event.Event;
import org.xwiki.refactoring.event.DocumentRenamingEvent;
import org.xwiki.refactoring.event.EntitiesRenamedEvent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

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
    protected Provider<XWikiContext> contextProvider;

    @Inject
    private DiagramRenameStateManager renameStateManager;

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
        // The former event is responsible for handling the actual updates, while the latter is responsible for
        // cleaning up the memory so we don't create leaks.
        super(ROLE_HINT, new DocumentRenamingEvent(), new EntitiesRenamedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (this.diagramLinksThread == null || this.diagramMacroThread == null) {
            startThreads();
        }

        // When the job ends, mark it finished and clean up if all entries are done.
        if (event instanceof EntitiesRenamedEvent) {
            String jobId = getCurrentJobId();
            if (jobId != null) {
                renameStateManager.markJobFinished(jobId);
            }
            return;
        }

        if (observationContext.isIn(new JobStartedEvent("refactoring/rename"))) {
            DocumentRenamingEvent renamedEvent = (DocumentRenamingEvent) event;

            DocumentReference originalDocRef = renamedEvent.getSourceReference();
            DocumentReference destinationRef = renamedEvent.getTargetReference();
            logger.info("Original document {}, destination {}", originalDocRef, destinationRef);
            String jobId = getCurrentJobId();
            // Because the event if fired in sequence, and we don't know when we might reach a page that interests us
            // we always record the new mapping so the other entires runnables can resolve it to the new reference if
            // it appears as a backlink somewhere.
            renameStateManager.recordRename(jobId, originalDocRef, destinationRef);

            XWikiContext context = contextProvider.get();
            try {

                XWikiDocument originalDoc = context.getWiki().getDocument(originalDocRef, context);
                // We check the original document because the new one doesn't have any object yet.
                List<DocumentReference> backlinks = originalDoc.getBackLinkedReferences(context);
                if (!backlinks.isEmpty()) {
                    JobRenameState state = renameStateManager.getOrCreateState(jobId);
                    DiagramQueueEntry queueEntry =
                        new DiagramQueueEntry(originalDocRef, destinationRef, backlinks, jobId, state.renameMap);
                    state.pendingEntries.incrementAndGet();
                    diagramLinksRunnable.addToQueue(queueEntry);
                }
            } catch (XWikiException e) {

                logger.error("Error when getting backlinks of renamed document [{}]", originalDocRef, e);
            }
        }
    }

    /**
     * Multiple rename jobs could be started at very close dates in the moment when the threads were not initialized yet
     * (for example, at installation step) and we need to be sure that only a single instance of each thread is created.
     */
    public synchronized void startThreads()
    {
        if (this.diagramLinksThread == null) {
            this.diagramLinksThread = startThread(this.diagramLinksRunnable, "Update Diagram Links Thread");
        }
        if (this.diagramMacroThread == null) {
            this.diagramMacroThread = startThread(this.diagramMacroRunnable, "Update Diagram Macro Thread");
        }
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
            logger.warn("Diagram backlinks update thread interruped", e);
        }
    }

    private String getCurrentJobId()
    {
        if (jobContext.getCurrentJob() == null) {
            return null;
        }
        return jobContext.getCurrentJob().getStatus().getRequest().getId().toString();
    }
}
