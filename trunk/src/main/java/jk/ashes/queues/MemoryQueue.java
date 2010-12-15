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

import jk.ashes.Queue;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.List;

/**
 * This is the primary memory queue where consumer take messages from. 
 * $LastChangedDate$
 * $LastChangedBy$
 * $LastChangedRevision$
 */
public class MemoryQueue implements Queue {
    private BlockingQueue inMemoryQueue;
    private boolean ready = true;
    private int size;

    public MemoryQueue(int size) {
        this.size = size;
        this.inMemoryQueue = new ArrayBlockingQueue(size, true); // FIFO queue
    }

    public boolean produce(Object a) {
        return inMemoryQueue.offer(a); // return false if full
    }

    /*
    *
    * This when a list of objects is dumbed into file
    *
    * */
    public synchronized boolean produce(List list) {
        for (Object obj : list) {
            produce(obj);
        }
        return true;
    }

    public Object consume() {
        return inMemoryQueue.poll(); // return null if empty
    }

    public int remainingCapacity() {
        return inMemoryQueue.remainingCapacity();
    }

    public int size() {
        return size;
    }

    public BlockingQueue inMemoryQueue() {
        return inMemoryQueue;
    }

    public boolean isEmpty() {
        return remainingCapacity() == size();
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }
}
