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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * $LastChangedDate$
 * $LastChangedBy$
 * $LastChangedRevision$
 */
public class OffloaderState implements QueueState {
    private final static Logger logger = LoggerFactory.getLogger(Offloader.class);
    private MemoryQueue inMemoryQueue;
    private MemoryQueue stagingMemoryQueue;
    private Offloader offloader;
    private ExecutorService executorService;

    public OffloaderState(MemoryQueue inMemoryQueue, MemoryQueue stagingMemoryQueue) {
        this.inMemoryQueue = inMemoryQueue;
        this.stagingMemoryQueue = stagingMemoryQueue;
        offloader = new Offloader(inMemoryQueue, stagingMemoryQueue);
        executorService = Executors.newSingleThreadExecutor();
    }

    public boolean produce(Object a, AshesQueue ashesQueue) {
        boolean b = stagingMemoryQueue.produce(a);
        if (!b) {
            logger.debug("Not enough space in staging memory, going back to persistent queue again");
            b = ashesQueue.moveFromOffLoaderToOverflowState(a, stagingMemoryQueue);
        } else {
            if (inMemoryQueue.remainingCapacity() > stagingMemoryQueue.inMemoryQueue().size() && inMemoryQueue.isReady()) {
                logger.debug("Time to move to Normal state, moving..");
                b = moveToNormal(ashesQueue, b);
            }
        }
        return b;
    }

    private synchronized boolean moveToNormal(AshesQueue ashesQueue, boolean b) {
        stop();
        Object object = null;
        while ((object = stagingMemoryQueue.consume()) != null) {
            inMemoryQueue.produce(object);
        }
        b = ashesQueue.moveToNormalState();
        return b;
    }

    private synchronized void moveToNormal() {
        if (inMemoryQueue.remainingCapacity() > stagingMemoryQueue.inMemoryQueue().size() && inMemoryQueue.isReady()) {
            Object object = null;
            while ((object = stagingMemoryQueue.consume()) != null) {
                inMemoryQueue.produce(object);
            }
        }
    }

    public Object consume() {
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
    }


    class Offloader implements Runnable {
        private MemoryQueue stagingMemoryQueue;
        private MemoryQueue inMemoryQueue;
        private boolean halt = false;

        Offloader(MemoryQueue inMemoryQueue, MemoryQueue stagingMemoryQueue) {
            this.stagingMemoryQueue = stagingMemoryQueue;
            this.inMemoryQueue = inMemoryQueue;
        }

        public void run() {
            while (true) {

                if (inMemoryQueue.remainingCapacity() > 0 && inMemoryQueue.isReady()) {
                    Object o = stagingMemoryQueue.consume();
                    if (null != o) {
                        inMemoryQueue.produce(o);
                    }
                } else {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        logger.error("Interupted exception : offloader exiting");
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
