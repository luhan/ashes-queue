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
package jk.ashes.example;

import jk.ashes.queues.AshesQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;

/**
 * $LastChangedDate$
 * $LastChangedBy$
 * $LastChangedRevision$
 */
public class ProducerConsumerSample {
    private static final Logger logger = LoggerFactory.getLogger(ProducerConsumerSample.class);


    public static void main(String[] args) throws IOException {
        Properties props = new Properties();
        InputStream inputStream = ClassLoader.getSystemResource("pc.properties").openStream();
        props.load(inputStream);

        //Reading props from the file
        final int noOfMessages = Integer.parseInt(props.getProperty("no.messages"));
        final int productionDelay = Integer.parseInt(props.getProperty("producing.delay"));
        final int consumptionDelay = Integer.parseInt(props.getProperty("consuming.delay"));
        int memoryQueueSize = Integer.parseInt(props.getProperty("memory.queue.size"));
        int statgingMemorySize = Integer.parseInt(props.getProperty("statging.queue.size"));
        String fileName = props.getProperty("persistent.file.name");

        //create the FIFO AshesQueue
        final AshesQueue q = new AshesQueue(memoryQueueSize, statgingMemorySize, fileName);

        Runnable producer = createProducer(noOfMessages, productionDelay, q);
        Runnable consumer = createConsumer(consumptionDelay, q);

        new Thread(producer).start();
        new Thread(consumer).start();
    }

    private static Runnable createConsumer(final int consumptionDelay, final AshesQueue q) {
        return new Runnable() {
            public void run() {
                int count = 1;
                while (true) {
                    Object o = q.consume();
                    if (null != o) {
                        logger.info("Consumed Message(" + count + ") : *********** " + o);
                        //Assert.assertEquals(((Message) o).ck(), count);
                        count++;
                    }
                    try {
                        Thread.sleep(consumptionDelay);
                        //Thread.sleep((int) (Math.random() * 10));
                    } catch (InterruptedException e) {
                    }
                }
            }
        };
    }

    private static Runnable createProducer(final int noOfMessages, final int productionDelay, final AshesQueue q) {
        return new Runnable() {
            public void run() {
                for (int i = 1; i <= noOfMessages; i++) {
                    final Message o = new Message(i);
                    try {
                        Thread.sleep(productionDelay);
                    } catch (InterruptedException e) {/*do nothing*/}
                    logger.info("Producing Message(" + i + ") : ************ " + o.index());
                    q.produce(o);
                }

                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    //do nothing
                }

                for (int i = noOfMessages + 1; i <= 2 * noOfMessages; i++) {
                    final Message o = new Message(i);
                    try {
                        Thread.sleep(productionDelay);
                        //    Thread.sleep((int) (Math.random() * 1));

                    } catch (InterruptedException e) {
                    }
                    logger.info("Producing Message(" + i + ") : ************ " + o.index());
                    q.produce(o);
                }

            }
        };
    }


}
