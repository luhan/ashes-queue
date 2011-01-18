/*
*   Copyright 2010 JK
*
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
*/
package jk.ashes.states;

import jk.ashes.queues.MemoryQueue;
import jk.ashes.queues.AshesQueue;
import jk.ashes.QueueState;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jk.ashes.queues.PersistentQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * $LastChangedDate$
 * $LastChangedBy$
 * $LastChangedRevision$
 */
public class OffloaderState<T extends Serializable> implements QueueState<T> {

    private final static Logger logger = LoggerFactory.getLogger(OffloaderState.class);

    private MemoryQueue<T> inMemoryQueue;
    private MemoryQueue<T> stagingMemoryQueue;
    private PersistentQueue<T> persistentQueue;

    private Offloader offloader;
    private ExecutorService executorService;

    public OffloaderState(MemoryQueue<T> inMemoryQueue, MemoryQueue<T> stagingMemoryQueue, PersistentQueue<T> persistentQueue) {
        this.persistentQueue = persistentQueue;
        this.inMemoryQueue = inMemoryQueue;
        this.stagingMemoryQueue = stagingMemoryQueue;
        offloader = new Offloader(inMemoryQueue, stagingMemoryQueue);
        executorService = Executors.newSingleThreadExecutor();
    }

    public boolean produce(T t, AshesQueue<T> ashesQueue) {
        boolean b = stagingMemoryQueue.produce(t);
        if (!b) {
            logger.debug("Not enough space in staging memory, going back to persistent queue again");
            b = ashesQueue.moveFromOffLoaderToOverflowState(t, stagingMemoryQueue);
        } else {
            if (inMemoryQueue.remainingCapacity() > stagingMemoryQueue.inMemoryQueue().size() && inMemoryQueue.isReady()) {
                logger.debug("Time to move to Normal state, moving..");
                b = moveToNormal(ashesQueue, b);
            }
        }
        return b;
    }

    private synchronized boolean moveToNormal(AshesQueue<T> ashesQueue, boolean b) {
        stop();
        T t = null;
        while ((t = stagingMemoryQueue.consume()) != null) {
            inMemoryQueue.produce(t);
        }
        b = ashesQueue.moveToNormalState();
        return b;
    }

    private synchronized void moveToNormal() {
        if (inMemoryQueue.remainingCapacity() > stagingMemoryQueue.inMemoryQueue().size() && inMemoryQueue.isReady()) {
            T t= null;
            while ((t = stagingMemoryQueue.consume()) != null) {
                inMemoryQueue.produce(t);
            }
        }
    }

    public T consume() {
        return inMemoryQueue.consume();
    }

    public int remainingCapacity() {
        return inMemoryQueue.remainingCapacity();
    }

    public void start() {
        executorService.execute(offloader);
    }

    public void stop() {
        offloader.halt();
        executorService.shutdown();
    }

    public void shutdown() {
        stop();
        final ArrayList<T> list = new ArrayList<T>();
        persistentQueue.produce(list);
    }

    class Offloader implements Runnable {
        private MemoryQueue<T> stagingMemoryQueue;
        private MemoryQueue<T> inMemoryQueue;
        private boolean halt = false;

        Offloader(MemoryQueue<T> inMemoryQueue, MemoryQueue<T> stagingMemoryQueue) {
            this.stagingMemoryQueue = stagingMemoryQueue;
            this.inMemoryQueue = inMemoryQueue;
        }

        public void run() {
            while (true) {

                if (inMemoryQueue.remainingCapacity() > 0 && inMemoryQueue.isReady()) {
                    T t = stagingMemoryQueue.consume();
                    if (null != t) {
                        inMemoryQueue.produce(t);
                    }
                } else {
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        logger.error("Interrupted exception : offloader exiting");
                        return;
                    }
                }
                if (halt) {
                    moveToNormal();
                    return;
                }
            }
        }

        public void halt() {
            halt = true;
        }
    }
}
