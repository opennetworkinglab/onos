/*
 * Copyright 2015 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.databaseperf;

import static org.apache.felix.scr.annotations.ReferenceCardinality.MANDATORY_UNARY;
import static org.onlab.util.Tools.delay;
import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.apache.commons.lang.math.RandomUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.onlab.util.KryoNamespace;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;

/**
 * Application to measure partitioned database performance.
 */
@Component(immediate = true)
public class DatabasePerfInstaller {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = MANDATORY_UNARY)
    protected StorageService storageService;

    private boolean stopped;

    private ApplicationId appId;

    private static final long REPORT_PERIOD = 5000L; //ms
    private Timer reportTimer;

    private AtomicInteger successCount = new AtomicInteger(0);
    private AtomicInteger failureCount = new AtomicInteger(0);

    private ConsistentMap<String, String> cmap;

    private ControllerNode localNode;

    private long reportStartTime = System.currentTimeMillis();

    private static final int NUM_TASK_THREADS = 2;
    private ExecutorService taskExecutor;

    private static final Serializer SERIALIZER = new Serializer() {

        KryoNamespace kryo = new KryoNamespace.Builder().build();

        @Override
        public <T> byte[] encode(T object) {
            return kryo.serialize(object);
        }

        @Override
        public <T> T decode(byte[] bytes) {
            return kryo.deserialize(bytes);
        }

    };

    @Activate
    public void activate() {
        localNode = clusterService.getLocalNode();
        String nodeId = localNode.ip().toString();
        appId = coreService.registerApplication("org.onosproject.nettyperf."
                                                        + nodeId);

        cmap = storageService.createConsistentMap("onos-app-database-perf-test-map", SERIALIZER);
        taskExecutor = Executors.newFixedThreadPool(NUM_TASK_THREADS, groupedThreads("onos/database-perf", "worker"));
        log.info("Started with Application ID {}", appId.id());
        start();
    }

    @Deactivate
    public void deactivate() {
        stop();
        log.info("Stopped");
    }

    public void start() {
        long delay = System.currentTimeMillis() % REPORT_PERIOD;
        reportTimer = new Timer("onos-netty-perf-reporter");
        reportTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                report();
            }
        }, delay, REPORT_PERIOD);

        stopped = false;
        IntStream.range(0, NUM_TASK_THREADS).forEach(i -> {
            taskExecutor.submit(() -> {
                delay(2000); // take a breath to start
                while (!stopped) {
                    performDBOperation();
                    delay(2); // take a breather
                }
            });
        });
    }

    private void performDBOperation() {
        String key = String.format("test%d", RandomUtils.nextInt(1000));
        try {
            if (RandomUtils.nextBoolean()) {
                cmap.put(key, UUID.randomUUID().toString());
            } else {
                cmap.get(key);
            }
            successCount.incrementAndGet();
        } catch (Exception e) {
            failureCount.incrementAndGet();
        }
    }

    private void report() {
        long delta = System.currentTimeMillis() - reportStartTime;
        if (delta > 0) {
            int rate = (int) Math.round(((successCount.get() * 1000.0) / delta));
            log.info("Passed: {}, Failed: {}, Rate: {}",
                    successCount.getAndSet(0), failureCount.getAndSet(0), rate);
            reportStartTime = System.currentTimeMillis();
        }
    }

    public void stop() {
        reportTimer.cancel();
        reportTimer = null;
        stopped = true;
        try {
            taskExecutor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("Failed to stop worker.");
        }
    }
}
