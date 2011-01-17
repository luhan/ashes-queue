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

/**
 * This is the normal state where message comes to inmemory and passed to consumer.
 * incase the inmemory is full, it moves to overflow state
 * $LastChangedDate$
 * $LastChangedBy$
 * $LastChangedRevision$
 */
public class NormalState<T extends Serializable> implements QueueState<T> {

    private MemoryQueue<T> inMemoryQueue;

    public NormalState(MemoryQueue<T> inMemoryQueue) {
        this.inMemoryQueue = inMemoryQueue;
    }

    public boolean produce(T t, AshesQueue<T> ashesQueue) {
        boolean b = inMemoryQueue.produce(t);
        if (!b) {
            b = ashesQueue.moveFromNormalToOverflowState(t);
        }
        return b;
    }

    public T consume() {
        return inMemoryQueue.consume();
    }

    public int remainingCapacity() {
        return inMemoryQueue.remainingCapacity();
    }
}
