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
import jk.ashes.queues.PersistentQueue;
import jk.ashes.Queue;
import jk.ashes.QueueState;

import java.io.Serializable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.List;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Overflow state where a reloader will get the object from file and
 * put it in the remaining inmemory queue untill it gets full
 * <p/>
 * $LastChangedDate$
 * $LastChangedBy$
 * $LastChangedRevision$
 */
public class OverflowState<T extends Serializable> implements QueueState<T> {

    private final static Logger logger = LoggerFactory.getLogger(OverflowState.class);

    private MemoryQueue<T> inMemoryQueue;
    private PersistentQueue<T> persistentQueue;
    private Reloader reloader;

    private ExecutorService executorService;

    public OverflowState(MemoryQueue<T> inMemoryQueue, PersistentQueue<T> persistentQueue) {
        this.inMemoryQueue = inMemoryQueue;
        this.persistentQueue = persistentQueue;
        reloader = new Reloader(inMemoryQueue, persistentQueue);
        executorService = Executors.newSingleThreadExecutor();
        persistentQueue.init();
    }

    public synchronized boolean produce(T t, AshesQueue<T> ashesQueue) {
        final boolean b = persistentQueue.produce(t);
        if (!b) {
            logger.error("Persistent queue is full or in abnormal state, very funny, please check " + t);
        } else {
            if (inMemoryQueue.remainingCapacity() > inMemoryQueue.capacity() / 2) { //TODO analyse this later
                ashesQueue.moveFromOverflowToOffLoaderState();
                stop();
            }
        }
        return b;
    }

    /**
     * This is when moving from offloader to overloader
     * @param t - object to produce
     * @param stagingMemoryQueue -
     * @param ashesQueue -
     * @return - true if success
     */
    public synchronized boolean produce(T t, MemoryQueue<T> stagingMemoryQueue, AshesQueue<T> ashesQueue) {
        List<T> list = new ArrayList<T>();
        stagingMemoryQueue.inMemoryQueue().drainTo(list);
        persistentQueue.produce(list);
        return produce(t, ashesQueue);
    }

    public T consume() {
        T consume = inMemoryQueue.consume();
        if (consume == null) {
            reloader.awake();
        }
        return consume;
    }

    public int remainingCapacity() {
        return inMemoryQueue.remainingCapacity();
    }

    public void start() {          
        inMemoryQueue.setReady(false);
        if (persistentQueue.isBacklogAvailable()) {  // This when starting the system, clearing the objects
            executorService.execute(reloader);
        } else if (!persistentQueue.isEmpty()) {
            logger.debug("Overload Reloader is already running, hence just resuming ....");
            reloader.resume();
        } else {
            logger.debug("Overload Reloader is starting ....");
            executorService.execute(reloader);
        }
    }

    public void stop() {
        reloader.pause();
    }
    
    class Reloader implements Runnable {
        private Queue<T> persistenQueue;
        private MemoryQueue<T> inMemoryQueue;
        private Boolean halt = false;

        Reloader(MemoryQueue<T> inMemoryQueue, Queue<T> persistenQueue) {
            this.persistenQueue = persistenQueue;
            this.inMemoryQueue = inMemoryQueue;
        }

        public void run() {
            while (true) {
                T t = null;
                if (inMemoryQueue.remainingCapacity() > 0) {
                    t = persistenQueue.consume();
                    if (null != t) {
                        inMemoryQueue.produce(t);
                    }
                } else {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        logger.error("Received interupted exception, exiting the reloader ");
                        inMemoryQueue.setReady(true);
                        halt = false;
                        return;
                    }
                }
                if (halt && persistenQueue.isEmpty()) {
                    inMemoryQueue.setReady(true);
                    logger.debug("Overflow messages are cleared, exiting the reloader ");
                    if (persistenQueue instanceof PersistentQueue) {
                        ((PersistentQueue) persistenQueue).cleanUp();
                    }
                    halt = false;
                    return;
                }
            }
        }

        public void pause() {
            halt = true;
        }

        public void resume() {
            halt = false;
        }

        public void awake() {
            synchronized (this) {
                this.notify();
            }
        }
    }
}
