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
package jk.ashes.util;

import jk.ashes.queues.MemoryQueue;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.Map;
import java.util.HashMap;
import java.lang.management.MemoryMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import static java.util.concurrent.TimeUnit.SECONDS;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * $LastChangedDate$
 * $LastChangedBy$
 * $LastChangedRevision$
 */
public class MemoryMonitoringService {
    private final static Logger logger = LoggerFactory.getLogger(MemoryMonitoringService.class);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private Runnable monitor;
    private Map<Range, Integer> memoryUtilMap;

    public MemoryMonitoringService(Map memoryUtilMap) {
        this.memoryUtilMap = memoryUtilMap;
    }

    public MemoryMonitoringService() {
        memoryUtilMap = new HashMap<Range, Integer>();
        memoryUtilMap.put(new Range(0, 100), 1000);
        memoryUtilMap.put(new Range(201, 300), 2000);
        memoryUtilMap.put(new Range(101, 200), 3000);
        memoryUtilMap.put(new Range(301, 400), 4000);
        memoryUtilMap.put(new Range(401, Long.MAX_VALUE), 5000);
    }

    public void init(final MemoryQueue memoryQueue) {
        monitor = new Runnable() {
            public void run() {
                MemoryMXBean memorymbean = ManagementFactory.getMemoryMXBean();
                final MemoryUsage heapMemoryUsage = memorymbean.getHeapMemoryUsage();
                long freeMemory = (heapMemoryUsage.getCommitted() - heapMemoryUsage.getUsed()) / (1024*1024);
                final Integer estimatedCapacity = memoryUtilMap.get(new Range(freeMemory));
                int currentCapacity = memoryQueue.capacity();
                if (estimatedCapacity == currentCapacity) {
                    return;
                }
                if (estimatedCapacity < currentCapacity) {
                    logger.info("Less free memory " + freeMemory / 1024 + " KB hence decreasing the capacity to "
                            + estimatedCapacity);
                } else if (estimatedCapacity > currentCapacity) {
                    logger.info("More free memory " + freeMemory / 1024 + " KB hence increasing the capacity to "
                            + estimatedCapacity);
                }
                memoryQueue.resize(estimatedCapacity);
            }
        };
    }

    public void start(){
        scheduler.scheduleAtFixedRate(monitor, 10, 10, SECONDS);
    }

    public void stop(){
        scheduler.shutdown();
    }


}
