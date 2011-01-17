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
import jk.ashes.util.MemoryMonitoringService;

import java.io.Serializable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.List;

/**
 * This is the primary memory queue where consumer take messages from.
 * <p/>
 * $LastChangedDate$
 * $LastChangedBy$
 * $LastChangedRevision$
 */
public class MemoryQueue<T extends Serializable> implements Queue<T> {
    private BlockingQueue<T> inMemoryQueue;
    private boolean ready = true;
    private int capacity;

    public MemoryQueue(int capacity) {
        this.capacity = capacity;
        this.inMemoryQueue = new LinkedBlockingQueue<T>(); // FIFO queue
    }

    public synchronized boolean produce(T t) {
        return inMemoryQueue.size() < capacity && inMemoryQueue.offer(t);
    }

    /*
    *
    * This when a list of objects is dumbed into file
    *
    * */
    public synchronized boolean produce(List<T> list) {
        for (T t : list) {
            produce(t);
        }
        return true;
    }

    public T consume() {
        return inMemoryQueue.poll(); // return null if empty
    }

    public int remainingCapacity() {
        return capacity - inMemoryQueue.size();
    }

    public int capacity() {
        return capacity;
    }

    public int resize(int capacity) {
        return this.capacity = capacity;
    }

    public BlockingQueue<T> inMemoryQueue() {
        return inMemoryQueue;
    }

    public boolean isEmpty() {
        return remainingCapacity() == capacity();
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }
}
