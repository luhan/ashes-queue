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
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import static java.nio.channels.FileChannel.MapMode.READ_WRITE;
import java.io.*;
import java.util.List;

import jk.ashes.Queue;
import jk.ashes.PersistentMessageListener;

/**
 * This is the Persistent Queue where the objects are produced and consumed in a FIFO basis
 *
 * two mapped byte buffers will be created both for reading and writing
 *
 * The data packet format will be [1bye][4bytes][data bytes] where firs header is to mention whether
 * the packet is already consumed or not. second header is 4 bytes size, specifying the length of the packet,
 * and the rest is the actual data
 *
 * The maximum Data size is less than (WRITE_PAGE_SIZE & READ_PAGE_SIZE - headerLength)
 *
 * The backlog is also taken care. Too bored to write the document, please check the code or drop me an email ;)
 *
 *
 * $LastChangedDate$
 * $LastChangedBy$
 * $LastChangedRevision$
 */
public class PersistentQueue implements Queue {
    private final static Logger logger = LoggerFactory.getLogger(PersistentQueue.class);

    protected static int WRITE_PAGE_SIZE = 1024 * 1024; // 1 MB Page Size
    protected static int READ_PAGE_SIZE = 1024 * 1024; // 1 MB Page Size

    private String fileName = "store.dat"; // the default name of the persistent file
    private RandomAccessFile file;//random accessfile
    private FileChannel channel; //channel returned from random accessfile
    private MappedByteBuffer readMbb; // buffer used to read
    private MappedByteBuffer writeMbb; // buffer used to wirte

    private boolean backlogAvailable = false; // If the persistent file has backlog already

    private PersistentMessageListener persistentMessageListener; //Listener to be used to notify when a message is persisted

    final private ByteBuffer header = ByteBuffer.allocateDirect(5);   //1 byte for the status of the message, 4 bytes length of the payload
    final private ByteBuffer data = ByteBuffer.allocateDirect(1024); //1KB Message Packet
    final private int packetCapacity = header.capacity() + data.capacity();
    final private int headerLength = 5;

    private int fileReadPosition = 0;  //absolute file read position
    private int fileWritePosition = 0; //absolute file write position

    public PersistentQueue(String fileName, PersistentMessageListener persistentMessageListener) {
        this.fileName = fileName;
        this.persistentMessageListener = persistentMessageListener;
    }

    public PersistentQueue(String fileName) {
        this(fileName, null);
    }

    public PersistentQueue(PersistentMessageListener persistentMessageListener) {
        this("store.dat", persistentMessageListener);
    }

    public void init() {
        try {
            file = new RandomAccessFile(fileName, "rw");
            channel = file.getChannel();
            initialiseReadBuffer();
            initialiseWriteBuffer();
        } catch (Throwable e) {
            logger.error("FATAL : Couldn't initialise the persistent queue, overload protection won't work ", e);
        }
    }

    private void initialiseReadBuffer() throws IOException {
        readMbb = channel.map(READ_WRITE, fileReadPosition, READ_PAGE_SIZE);  //create the read buffer with fileReadPosition 0 initially
        int position = readMbb.position();
        byte active = readMbb.get(); //first byte to see whether the message is already read or not
        int length = readMbb.getInt();//next four bytes to see the data length

        while (active == 0 && length > 0) {// message is non active means, its read,so skipping it
            if (position + packetCapacity > readMbb.capacity()) { // Bytebuffer is out of capacity, hence changing the buffer
                fileReadPosition = fileReadPosition + position + headerLength + length;
                readMbb = channel.map(READ_WRITE, fileReadPosition, READ_PAGE_SIZE);
            } else {
                readMbb.position(position + headerLength + length); // skipping the read bytes
            }
            position = readMbb.position();
            active = readMbb.get();
            length = readMbb.getInt();
        }
        if (active == 1) {
            backlogAvailable = true; // the file has unconsumed message(s)
        }
        readMbb.position(position);
    }


    private void initialiseWriteBuffer() throws IOException {
        int position;
        byte active;
        int length;
        writeMbb = channel.map(READ_WRITE, fileReadPosition, WRITE_PAGE_SIZE); //start from the readposition, since we dont overwrite!
        position = writeMbb.position();
        active = writeMbb.get();
        length = writeMbb.getInt();
        while (length > 0) { // message is there, so skip it, keep doing until u get the end
            if (position + packetCapacity > readMbb.capacity()) { // over run capacity, hence move the paging
                fileWritePosition = fileWritePosition + position + headerLength + length;
                writeMbb = channel.map(READ_WRITE, fileWritePosition, WRITE_PAGE_SIZE);
            } else {
                writeMbb.position(position + headerLength + length);
            }
            position = writeMbb.position();
            active = writeMbb.get();
            length = writeMbb.getInt();
        }
        writeMbb.position(position);
    }


    /**
     * Changing the read write positiong back to earlier
     */
    public synchronized void cleanUp() {
        try {
            channel.truncate(0);
            readMbb = channel.map(READ_WRITE, 0, READ_PAGE_SIZE);
            writeMbb = channel.map(READ_WRITE, 0, WRITE_PAGE_SIZE);
            backlogAvailable = false;
        } catch (IOException e) {
            logger.error("Error while trying to truncate the file ", e);
        }
        fileReadPosition = readMbb.position();
        fileWritePosition = writeMbb.position();
    }

    /**
     * This queue has to be synchronised with consume, otherwise it gets corrupted
     */
    public synchronized boolean produce(Object o) {
        try {
            byte[] oBytes = getBytes(o);
            int length = oBytes.length;
            //prepare the header
            header.clear();
            header.put((byte) 1);
            header.putInt(length);
            header.flip();

            //prepare the data
            data.clear();
            data.put(oBytes);
            data.flip();

            int currentPosition = writeMbb.position();
            if (writeMbb.remaining() < packetCapacity) { //check weather current buffer is enuf, otherwise we need to chance the buffer
                writeMbb.force();
                fileWritePosition = fileWritePosition + currentPosition;
                writeMbb = channel.map(READ_WRITE, fileWritePosition, WRITE_PAGE_SIZE);
            }

            writeMbb.put(header); //write header
            writeMbb.put(data); //write data
            if (null != persistentMessageListener) { //notify listener
                persistentMessageListener.onMessagePersistent(o, true);
            }
            return true;
        } catch (Throwable e) {
            logger.error("Issue in dumping the object into persistent " + e);
            logger.error("The object missed is :" + o);
            if (null != persistentMessageListener) {
                persistentMessageListener.onMessagePersistent(o, true);
            }
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
        try {
            int currentPosition = readMbb.position();
            if (readMbb.remaining() < packetCapacity) {
                fileReadPosition = fileReadPosition + currentPosition;
                readMbb = channel.map(READ_WRITE, fileReadPosition, READ_PAGE_SIZE);
                currentPosition = readMbb.position();
            }
            final byte active = readMbb.get();
            final int length = readMbb.getInt();
            if (active == 0 && length > 0) {
                readMbb.position(currentPosition + headerLength + length);
                return consume();
            }

            if (length <= 0) {
                readMbb.position(currentPosition);
                return null; //the queue is empty
            }
            byte[] bytes = new byte[length];
            readMbb.get(bytes);

            readMbb.put(currentPosition, (byte) 0); //making it not active (deleted)

            return toObject(bytes);
        } catch (Throwable e) {
            logger.error("Issue in reading the persistent queue : ", e);
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
            } catch (Throwable e) {/*Do nothing*/}
            try {
                bos.close();
            } catch (Throwable e) {/*Do nothing*/}
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
            } catch (Throwable e) {/*Do Nothing*/}
            try {
                bis.close();
            } catch (Throwable e) {/*Do Nothing*/}
        }
    }

    public int remainingCapacity() {
        return Integer.MAX_VALUE;  //fake still
    }

    public int capacity() {
        return Integer.MAX_VALUE; //fake still
    }

    public synchronized boolean isEmpty() {
        int pos = readMbb.position();
        final byte b;
        final int length;
        try {
            b = readMbb.get(pos);
            length = readMbb.getInt(pos + 1);
        } catch (Throwable e) {
            return true;
        }
        if (b == 0 && length == 0) {
            backlogAvailable = false;
            return true;
        } else {
            return false;
        }

    }

    public boolean isBacklogAvailable() {
        return backlogAvailable;
    }

    public void close() {
        writeMbb.force();
        try {
            file.close();
        } catch (Throwable e) {
            //do nothing
        }
    }
}
