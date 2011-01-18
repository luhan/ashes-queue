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
package jk.ashes.queues;

import jk.ashes.states.NormalState;
import jk.ashes.states.OverflowState;
import jk.ashes.states.OffloaderState;
import jk.ashes.QueueState;
import jk.ashes.Queue;
import jk.ashes.PersistentMessageListener;
import jk.ashes.util.MemoryMonitoringService;
import jk.ashes.util.Range;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * The Alias Queue which internally manages the states and doing the swapping between file and memory
 * <p/>
 * $LastChangedDate$
 * $LastChangedBy$
 * $LastChangedRevision$
 */
public class AshesQueue<T extends Serializable> implements Queue<T> {

    private final static Logger logger = LoggerFactory.getLogger(AshesQueue.class);

    private QueueState<T> currentState;

    private NormalState<T> normalState;
    private OverflowState<T> overflowState;
    private OffloaderState<T> offloaderState;

    private MemoryQueue<T> inMemoryQueue;

    public AshesQueue(int mainMemorySize, int stagingMemorySize, String fileName, PersistentMessageListener<T> listener) {

        inMemoryQueue = new MemoryQueue<T>(mainMemorySize);
        MemoryQueue<T> inMemoryStagingQueue = new MemoryQueue<T>(stagingMemorySize);
        PersistentQueue<T> persistentQueue = new PersistentQueue<T>(fileName, listener);

        normalState = new NormalState<T>(inMemoryQueue);
        overflowState = new OverflowState<T>(inMemoryQueue, persistentQueue);
        offloaderState = new OffloaderState<T>(inMemoryQueue, inMemoryStagingQueue, persistentQueue);

        initialiseState(persistentQueue);
    }

    public AshesQueue(int mainMemorySize, int stagingMemorySize, String fileName) {
        this(mainMemorySize, stagingMemorySize, fileName, null);
    }    

    public void addMemoryMonitoring(Map<Range, Integer> memoryUtilMap) {
        final MemoryMonitoringService service = new MemoryMonitoringService(memoryUtilMap);
        service.init(inMemoryQueue);
        service.start();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                service.stop();
            }
        });
    }

    private void initialiseState(PersistentQueue persistentQueue) {
        if (persistentQueue.isBacklogAvailable()) {
            moveFromNormalToOverflowState(null); // the file contains messages already, clear them first
        } else {
            currentState = normalState;    // move to normal
        }
    }

    public boolean produce(T t) {
        return currentState.produce(t, this);
    }

    public T consume() {
        return currentState.consume();
    }

    public boolean produce(List<T> list) {
        boolean b;
        boolean success = true;
        for (T t : list) {
            b = produce(t);
            if (!b) {
                logger.error("Message Failed " + t);
                success = false;
            }
        }
        return success;
    }

    public boolean moveFromNormalToOverflowState(T t) {
        logger.debug("Moving to overflow state from normal state ...");
        overflowState.start();
        currentState = overflowState;
        return null == t || currentState.produce(t, this);
    }

    public boolean moveFromOffLoaderToOverflowState(T t, MemoryQueue<T> stagingMemoryQueue) {
        logger.debug("Moving to overflow state from Offloader state ...");
        overflowState.start();
        currentState = overflowState;
        return overflowState.produce(t, stagingMemoryQueue, this);
    }

    public void moveFromOverflowToOffLoaderState() {
        logger.debug("Moving to offloader state from over from over flow state ...");
        offloaderState.start();
        currentState = offloaderState;
    }

    public boolean moveToNormalState() {
        logger.debug("Moving to normal state...");
        currentState = normalState;
        return true;
    }

    public int remainingCapacity() {
        return inMemoryQueue.remainingCapacity();
    }

    public int capacity() {
        return inMemoryQueue.capacity();
    }

    public void shutdown() {
        offloaderState.shutdown();
    }

    public boolean isEmpty() {
        return remainingCapacity() == capacity();
    }
}
