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

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import static java.nio.channels.FileChannel.MapMode.READ_WRITE;
import java.io.*;
import java.util.List;

import jk.ashes.Queue;

/**
 * $LastChangedDate$
 * $LastChangedBy$
 * $LastChangedRevision$
 */
public class PersistentQueue implements Queue {
    private final static Logger logger = LoggerFactory.getLogger(PersistentQueue.class);
    private final static int length = 0x8FFFFFF; // 128 Mb
    private MappedByteBuffer mbb;
    private FileChannel channel;
    private RandomAccessFile file;
    private static int PAGE_SIZE = 1024 * 1024; // 1 megabyte page size //TODO
    private int readPosition = 0;
    private int writePosition = 0;
    private boolean backlogAvailable = false;

    public void begin() {
        try {
            final String fileName = "store.dat";
            file = new RandomAccessFile(fileName, "rw");
            channel = file.getChannel();
            mbb = channel.map(READ_WRITE, 0, length);
            readPosition = (mbb.getInt(0) == 0 ? 8 : mbb.getInt(0));
            writePosition = (mbb.getInt(4) == 0 ? 8 : mbb.getInt(4));
            System.out.println("READ POSITION : " + readPosition);
            if (readPosition != 8) {
                backlogAvailable = true;
            }
        } catch (Throwable e) {
            logger.error("FATAL : Couldn't initialise the persistent queue, overload protection won't work ", e);
        }
    }


    /**
     * Changing the read write positiong back to earlier
     */
    public void finish() {
        mbb.clear();
        mbb.putInt(0, 8);
        mbb.putInt(4, 8);
        readPosition = 8;
        writePosition = 8;
    }

    /**
     * This queue has to be synchronised with consume, otherwise it gets corrupted
     */
    public synchronized boolean produce(Object o) {
        try {
            byte[] oBytes = getBytes(o);
            int length = oBytes.length;
            mbb.putInt(writePosition, length);
            writePosition = writePosition + 4;
            mbb.position(writePosition);
            mbb.put(oBytes);
            writePosition = writePosition + length;
            mbb.putInt(4, writePosition);
            return true;
        } catch (Throwable e) {
            logger.error("Issue in dumping the object into persistent " + e);
            logger.error("The object missed is :" + o);
            return false;
        }
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

    public synchronized Object consume() {
        final int length = mbb.getInt(readPosition);
        if (length == 0) {
            return null; //the queue is empty
        }
        mbb.putInt(readPosition, 0); //clear it after reading
        readPosition = readPosition + 4; // move to data from header of four bytes

        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = mbb.get(readPosition);
            mbb.put(readPosition, (byte) 0);// clear after reading it
            readPosition++;
        }
        mbb.putInt(0, readPosition); // for next object
        try {
            return toObject(bytes);
        } catch (Throwable e) {
            logger.error("Issue in reading the persistent queue : ", e);
            logger.error("Length of the errored packet  : " + length);
            logger.error("Current Read Position         : " + readPosition);
            logger.error("Object which has error        : " + bytes.toString());
            return null;
        }
    }

    public static byte[] getBytes(Object o) throws IOException {
        ByteArrayOutputStream bos = null;
        ObjectOutputStream oos = null;
        try {
            bos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(bos);
            oos.writeObject(o);
            oos.flush();

            return bos.toByteArray();
        } finally {
            try {
                oos.close();
            } catch (Throwable e) {}
            try {
                bos.close();
            } catch (Throwable e) {}
        }
    }

    public static Object toObject(byte[] bytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = null;
        ObjectInputStream ois = null;
        try {
            bis = new ByteArrayInputStream(bytes);
            ois = new ObjectInputStream(bis);
            return ois.readObject();
        } finally {
            try {
                ois.close();
            } catch (Throwable e) {}
            try {
                bis.close();
            } catch (Throwable e) {}
        }
    }

    public int remainingCapacity() {
       return Integer.MAX_VALUE;  //fake still
    }

    public int size() {
        return Integer.MAX_VALUE; //fake still
    }

    public boolean isEmpty() {
        final int b = mbb.getInt(readPosition);
        backlogAvailable = false;
        return b == 0;
    }

    public boolean isBacklogAvailable() {
        return backlogAvailable;
    }
}
