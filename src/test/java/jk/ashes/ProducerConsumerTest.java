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
package jk.ashes;

import jk.ashes.queues.AliasQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.apache.log4j.BasicConfigurator;

import java.util.Arrays;

/**
 * $LastChangedDate$
 * $LastChangedBy$
 * $LastChangedRevision$
 */
public class ProducerConsumerTest {
    private static final Logger logger = LoggerFactory.getLogger(ProducerConsumerTest.class);


    //@Test
    public static void main(String[] args) {
        final AliasQueue q = new AliasQueue();
        Runnable producer = new Runnable() {
            public void run() {
                for (int i = 1; i <= 20000; i++) {
                    final Message o = new Message(i);
                    try {
                        if (Arrays.asList(100, 300, 450, 600, 800).contains(i)) {
                            Thread.sleep(1);
                        } else {
                            Thread.sleep((int) (Math.random() * 1));
                        }
                    } catch (InterruptedException e) {}
                    logger.info("Producing Message(" + i + ") : ************ " +  o.ck());
                    q.produce(o);
                }
            }
        };
        Runnable consumer = new Runnable() {
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
                        Thread.sleep((int) (Math.random() * 10));
                    } catch (InterruptedException e) {
                    }
                }
            }
        };
        new Thread(producer).start();
        new Thread(consumer).start();
    }


}
