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
public class AshesQueue implements Queue {
    private final static Logger logger = LoggerFactory.getLogger(AshesQueue.class);

    private QueueState currentQueue;

    private NormalState normalState;
    private OverflowState overflowState;
    private OffloaderState offloaderState;

    private MemoryQueue inMemoryQueue;

    public AshesQueue(int mainMemorySize, int stagingMemorySize, String fileName, PersistentMessageListener listener) {
        inMemoryQueue = new MemoryQueue(mainMemorySize);
        MemoryQueue inMemoryStatgingQueue = new MemoryQueue(stagingMemorySize);
        PersistentQueue persistentQueue = new PersistentQueue(fileName, listener);

        normalState = new NormalState(inMemoryQueue);
        overflowState = new OverflowState(inMemoryQueue, persistentQueue);
        offloaderState = new OffloaderState(inMemoryQueue, inMemoryStatgingQueue);

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
                if (service != null) {
                    service.stop();
                }
            }
        });
    }

    private void initialiseState(PersistentQueue persistentQueue) {
        if (persistentQueue.isBacklogAvailable()) {
            moveFromNormalToOverflowState(null); // the file contains messages already, clear them first
        } else {
            currentQueue = normalState;    // move to normal
        }
    }

    public boolean produce(Object o) {
        return currentQueue.produce(o, this);
    }

    public Object consume() {
        return currentQueue.consume();
    }

    public boolean produce(List list) {
        boolean b;
        boolean success = true;
        for (Object o : list) {
            b = produce(o);
            if (!b) {
                logger.error("Message Failed " + o);
                success = false;
            }
        }
        return success;
    }

    public boolean moveFromNormalToOverflowState(Object a) {
        logger.debug("Moving to overflow state from normal state ...");
        overflowState.start();
        currentQueue = overflowState;
        return null == a || currentQueue.produce(a, this);
    }

    public boolean moveFromOffLoaderToOverflowState(Object a, MemoryQueue stagingMemoryQueue) {
        logger.debug("Moving to overflow state from Offloader state ...");
        overflowState.start();
        currentQueue = overflowState;
        return overflowState.produce(a, stagingMemoryQueue, this);
    }

    public void moveFromOverflowToOffLoaderState() {
        logger.debug("Moving to offloader state from over from over flow state ...");
        offloaderState.start();
        currentQueue = offloaderState;
    }

    public boolean moveToNormalState() {
        logger.debug("Moving to normal state...");
        currentQueue = normalState;
        return true;
    }

    public int remainingCapacity() {
        return inMemoryQueue.remainingCapacity();
    }

    public int capacity() {
        return inMemoryQueue.capacity();
    }

    public boolean isEmpty() {
        return remainingCapacity() == capacity();
    }
}
