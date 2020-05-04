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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.xpn.xwiki.util.AbstractXWikiRunnable;

/**
 * Base class for diagram Runnable. It provides tools for working with diagramsQueue.
 * 
 * @version $Id$
 * @since 1.14
 */
public abstract class AbstractDiagramRunnable extends AbstractXWikiRunnable
{
    /**
     * Stop runnable entry.
     */
    public static final DiagramQueueEntry STOP_RUNNABLE_ENTRY = new DiagramQueueEntry(null, null);

    /**
     * Entries to be processed by this thread.
     */
    private BlockingQueue<DiagramQueueEntry> diagramsQueue;

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

    /**
     * Get the entries to be processed by the thread.
     * 
     * @return queue with the entries for the thread
     */
    public BlockingQueue<DiagramQueueEntry> getDiagramsQueue()
    {
        return this.diagramsQueue;
    }
}
