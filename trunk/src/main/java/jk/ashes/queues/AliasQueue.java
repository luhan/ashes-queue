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

import java.util.List;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * $LastChangedDate$
 * $LastChangedBy$
 * $LastChangedRevision$
 */
public class AliasQueue implements Queue {
    private final static Logger logger = LoggerFactory.getLogger(AliasQueue.class);
    private QueueState currentQueue;
    private NormalState normalState;
    private OverflowState overflowState;
    private OffloaderState offloaderState;
    private MemoryQueue inMemoryQueue;
    private MemoryQueue inMemoryStatgingQueue;
    private PersistentQueue persistentQueue;

    public AliasQueue() {
        inMemoryQueue = new MemoryQueue(1000);
        inMemoryStatgingQueue = new MemoryQueue(400);
        persistentQueue = new PersistentQueue();
        normalState = new NormalState(inMemoryQueue);
        overflowState = new OverflowState(inMemoryQueue, persistentQueue);
        offloaderState = new OffloaderState(inMemoryQueue, inMemoryStatgingQueue);
        if (persistentQueue.isBacklogAvailable()) {
            moveFromNormalToOverflowState(null);
        } else {
            currentQueue = normalState;
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
        logger.debug("Moving to overflow state...");
        overflowState.start();
        currentQueue = overflowState;
        return null == a || currentQueue.produce(a, this);
    }

    public boolean moveFromOffLoaderToOverflowState(Object a, MemoryQueue stagingMemoryQueue) {
        logger.debug("Moving to overflow state from Offloader state");
        overflowState.start();
        currentQueue = overflowState;
        return overflowState.produce(a, stagingMemoryQueue, this);
    }

    public void moveFromOverflowToOffLoaderState() {
        logger.debug("Moving to offloader state from over from over flow state");
        offloaderState.start();
        currentQueue = offloaderState;
    }

    public boolean moveToNormalState() {
        logger.debug("Moving to normal state");
        currentQueue = normalState;
        return true;
    }

    public int remainingCapacity() {
        return inMemoryQueue.remainingCapacity();
    }

    public int size() {
        return inMemoryQueue.size();
    }

    public boolean isEmpty() {
        return remainingCapacity() == size();
    }

}
