/*
 *   (C) Copyright 2009-2010 hSenid Software International (Pvt) Limited.
 *   All Rights Reserved.
 *
 *   These materials are unpublished, proprietary, confidential source code of
 *   hSenid Software International (Pvt) Limited and constitute a TRADE SECRET
 *   of hSenid Software International (Pvt) Limited.
 *
 *   hSenid Software International (Pvt) Limited retains all title to and intellectual
 *   property rights in these materials.
 */
package jk.ashes.queues;

import org.testng.annotations.Test;
import org.testng.annotations.AfterMethod;
import jk.ashes.example.Message;
import org.testng.Assert;
import static org.testng.Assert.*;

import java.io.File;

/**
 * $LastChangedDate$
 * $LastChangedBy$
 * $LastChangedRevision$
 */
public class PersistentQueueTest {

    @AfterMethod
    public void teardown() {
        new File("jk.store").delete();
    }


    @Test
    public void testProduceConsumeSingleMessage() {
        PersistentQueue queue = new PersistentQueue("jk.store");
        queue.init();
        final Message actual = new Message(1);
        queue.produce(actual);
        Message expected = (Message) queue.consume();
        Assert.assertEquals(actual.index(), expected.index(), "Comparing index value");
        Assert.assertEquals(actual.value(), expected.value(), "Comparing message value");
        Assert.assertNull(queue.consume(), "Already Popped, so it should not be available");
        queue = new PersistentQueue("jk.store");
        queue.init();
        Assert.assertNull(queue.consume(), "Already Popped, so it should not be available");
    }

    @Test
    public void testProduceConsumeMultiMessage() {
        PersistentQueue queue = new PersistentQueue("jk.store");
        queue.init();
        for (int i = 0; i < 10; i++) {
            assertTrue(queue.produce(new Message(i)), "always success");
        }
        for (int i = 0; i < 10; i++) {
            Message expected = (Message) queue.consume();
            Assert.assertEquals(i, expected.index(), "Comparing index value");
            Assert.assertEquals("My name is JK and I am a crap living in Singapore !@#$$%^^&&", expected.value(), "Comparing message value");
        }
        Assert.assertNull(queue.consume(), "Already Popped, so it should not be available");
        queue = new PersistentQueue("jk.store");
        queue.init();
        Assert.assertNull(queue.consume(), "Already Popped, so it should not be available");
        ////////////////
        queue = new PersistentQueue("jk.store");
        queue.init();
        for (int i = 11; i <= 20; i++) {
            assertTrue(queue.produce(new Message(i)), "always success");
        }
        for (int i = 11; i <= 20; i++) {
            Message expected = (Message) queue.consume();
            Assert.assertEquals(i, expected.index(), "Comparing index value");
            Assert.assertEquals("My name is JK and I am a crap living in Singapore !@#$$%^^&&", expected.value(), "Comparing message value");
        }
    }

    @Test
    public void testProduceConsumeMultiMessageTwoFileOpens() {
        PersistentQueue queue = new PersistentQueue("jk.store");
        queue.init();
        for (int i = 0; i < 10; i++) {
            assertTrue(queue.produce(new Message(i)), "always success");
        }
        queue.close();

        queue = new PersistentQueue("jk.store");
        queue.init();
        for (int i = 0; i < 10; i++) {
            Message expected = (Message) queue.consume();
            Assert.assertEquals(i, expected.index(), "Comparing index value");
            Assert.assertEquals("My name is JK and I am a crap living in Singapore !@#$$%^^&&", expected.value(), "Comparing message value");
        }
        for (int i = 11; i <= 20; i++) {
            assertTrue(queue.produce(new Message(i)), "always success");
        }
        for (int i = 11; i <= 20; i++) {
            Message expected = (Message) queue.consume();
            Assert.assertEquals(i, expected.index(), "Comparing index value");
            Assert.assertEquals("My name is JK and I am a crap living in Singapore !@#$$%^^&&", expected.value(), "Comparing message value");
        }
    }

    @Test
    public void testFinish() {
        PersistentQueue queue = new PersistentQueue("jk.store");
        queue.init();
        for (int i = 0; i < 10; i++) {
            assertTrue(queue.produce(new Message(i)), "always success");
        }
        for (int i = 0; i < 10; i++) {
            Message expected = (Message) queue.consume();
            Assert.assertEquals(i, expected.index(), "Comparing index value");
            Assert.assertEquals("My name is JK and I am a crap living in Singapore !@#$$%^^&&", expected.value(), "Comparing message value");
        }
        Assert.assertNull(queue.consume(), "Already Popped, so it should not be available");
        queue.cleanUp();
        Assert.assertNull(queue.consume(), "Already Popped, so it should not be available");

        for (int i = 11; i <= 20; i++) {
            assertTrue(queue.produce(new Message(i)), "always success");
        }
        for (int i = 11; i <= 20; i++) {
            Message expected = (Message) queue.consume();
            Assert.assertEquals(i, expected.index(), "Comparing index value");
            Assert.assertEquals("My name is JK and I am a crap living in Singapore !@#$$%^^&&", expected.value(), "Comparing message value");
        }
        Assert.assertNull(queue.consume(), "Already Popped, so it should not be available");
        queue.cleanUp();
        Assert.assertNull(queue.consume(), "Already Popped, so it should not be available");
    }

    @Test
    public void testLoad() {
        final PersistentQueue queue = new PersistentQueue("jk.store");
        queue.init();
        Boolean success = true;
        final Thread producer = new Thread() {
            @Override
            public void run() {
                for (int i = 0; i < 100000; i++) {
                    queue.produce(new Message(i));
                }
            }
        };
        producer.setDaemon(false);
        producer.start();

        final Thread consumer = new Thread() {
            @Override
            public void run() {
                    for (int i = 0; i < 100000; i++) {
                        final Object message = queue.consume();
                        if (message == null) {
                            i--;
                            continue;
                        }
                        Assert.assertEquals(((Message) message).index(), i, "Comparing the integer value");
                    }

            }
        };
        consumer.setDaemon(false);
        consumer.start();


        while (producer.isAlive() || consumer.isAlive()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                //do nothing
            }
        }        
    }

}
