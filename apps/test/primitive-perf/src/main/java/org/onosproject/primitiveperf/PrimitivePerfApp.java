/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.primitiveperf;

import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import com.google.common.collect.Lists;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.cluster.messaging.MessageSubject;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.System.currentTimeMillis;
import static org.apache.felix.scr.annotations.ReferenceCardinality.MANDATORY_UNARY;
import static org.onlab.util.Tools.get;
import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Application to test sustained primitive throughput.
 */
@Component(immediate = true)
@Service(value = PrimitivePerfApp.class)
public class PrimitivePerfApp {

    private final Logger log = getLogger(getClass());

    private static final int DEFAULT_NUM_CLIENTS = 64;
    private static final int DEFAULT_WRITE_PERCENTAGE = 100;

    private static final int REPORT_PERIOD = 1_000; //ms

    private static final String START = "start";
    private static final String STOP = "stop";
    private static final MessageSubject CONTROL = new MessageSubject("primitive-perf-ctl");

    @Property(name = "numClients", intValue = DEFAULT_NUM_CLIENTS,
            label = "Number of clients to use to submit writes")
    private int numClients = DEFAULT_NUM_CLIENTS;

    @Property(name = "writePercentage", intValue = DEFAULT_WRITE_PERCENTAGE,
            label = "Percentage of operations to perform as writes")
    private int writePercentage = DEFAULT_WRITE_PERCENTAGE;

    @Reference(cardinality = MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = MANDATORY_UNARY)
    protected ComponentConfigService configService;

    @Reference(cardinality = MANDATORY_UNARY)
    protected PrimitivePerfCollector sampleCollector;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterCommunicationService communicationService;

    private ExecutorService messageHandlingExecutor;

    private ExecutorService workers;
    private boolean stopped = true;

    private Timer reportTimer;

    private NodeId nodeId;
    private TimerTask reporterTask;

    private long startTime;
    private long currentStartTime;
    private AtomicLong overallCounter;
    private AtomicLong currentCounter;

    @Activate
    public void activate(ComponentContext context) {
        configService.registerProperties(getClass());

        nodeId = clusterService.getLocalNode().id();

        reportTimer = new Timer("onos-primitive-perf-reporter");

        messageHandlingExecutor = Executors.newSingleThreadExecutor(
                groupedThreads("onos/perf", "command-handler"));

        communicationService.addSubscriber(CONTROL, String::new, new InternalControl(),
                                           messageHandlingExecutor);

        // TODO: investigate why this seems to be necessary for configs to get picked up on initial activation
        modify(context);
    }

    @Deactivate
    public void deactivate() {
        stopTestRun();

        configService.unregisterProperties(getClass(), false);
        messageHandlingExecutor.shutdown();
        communicationService.removeSubscriber(CONTROL);

        if (reportTimer != null) {
            reportTimer.cancel();
            reportTimer = null;
        }
    }

    @Modified
    public void modify(ComponentContext context) {
        if (context == null) {
            logConfig("Reconfigured");
            return;
        }

        Dictionary<?, ?> properties = context.getProperties();
        int newNumClients;
        try {
            String s = get(properties, "numClients");
            newNumClients = isNullOrEmpty(s) ? numClients : Integer.parseInt(s.trim());
        } catch (NumberFormatException | ClassCastException e) {
            log.warn("Malformed configuration detected; using defaults", e);
            newNumClients = DEFAULT_NUM_CLIENTS;
        }

        int newWritePercentage;
        try {
            String s = get(properties, "writePercentage");
            newWritePercentage = isNullOrEmpty(s) ? writePercentage : Integer.parseInt(s.trim());
        } catch (NumberFormatException | ClassCastException e) {
            log.warn("Malformed configuration detected; using defaults", e);
            newWritePercentage = DEFAULT_WRITE_PERCENTAGE;
        }

        if (newNumClients != numClients || newWritePercentage != writePercentage) {
            numClients = newNumClients;
            writePercentage = newWritePercentage;
            logConfig("Reconfigured");
            if (!stopped) {
                stop();
                start();
            }
        }
    }

    public void start() {
        if (stopped) {
            stopped = false;
            communicationService.broadcast(START, CONTROL, str -> str.getBytes());
            startTestRun();
        }
    }

    public void stop() {
        if (!stopped) {
            communicationService.broadcast(STOP, CONTROL, str -> str.getBytes());
            stopTestRun();
        }
    }

    private void logConfig(String prefix) {
        log.info("{} with numClients = {}; writePercentage = {}", prefix, numClients, writePercentage);
    }

    private void startTestRun() {
        sampleCollector.clearSamples();

        startTime = System.currentTimeMillis();
        currentStartTime = startTime;
        currentCounter = new AtomicLong();
        overallCounter = new AtomicLong();

        reporterTask = new ReporterTask();
        reportTimer.scheduleAtFixedRate(reporterTask,
                                        REPORT_PERIOD - currentTimeMillis() % REPORT_PERIOD,
                                        REPORT_PERIOD);

        stopped = false;

        Map<String, ControllerNode> nodes = new TreeMap<>();
        for (ControllerNode node : clusterService.getNodes()) {
            nodes.put(node.id().id(), node);
        }

        // Compute the index of the local node in a sorted list of nodes.
        List<String> sortedNodes = Lists.newArrayList(nodes.keySet());
        int nodeCount = nodes.size();
        int index = sortedNodes.indexOf(nodeId.id());

        // Count the number of workers assigned to this node.
        int workerCount = 0;
        for (int i = 1; i <= numClients; i++) {
            if (i % nodeCount == index) {
                workerCount++;
            }
        }

        // Create a worker pool and start the workers for this node.
        if (workerCount > 0) {
            workers = Executors.newFixedThreadPool(workerCount, groupedThreads("onos/primitive-perf", "worker-%d"));
            for (int i = 0; i < workerCount; i++) {
                workers.submit(new Runner(UUID.randomUUID().toString(), UUID.randomUUID().toString()));
            }
        }
        log.info("Started test run");
    }

    private void stopTestRun() {
        if (reporterTask != null) {
            reporterTask.cancel();
            reporterTask = null;
        }

        if (workers != null) {
            workers.shutdown();
            try {
                workers.awaitTermination(10, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                log.warn("Failed to stop worker", e);
            }
        }

        sampleCollector.recordSample(0, 0);
        sampleCollector.recordSample(0, 0);
        stopped = true;

        log.info("Stopped test run");
    }

    // Submits primitive operations.
    final class Runner implements Runnable {
        private final String key;
        private final String value;
        private ConsistentMap<String, String> map;

        private Runner(String key, String value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public void run() {
            setup();
            while (!stopped) {
                try {
                    submit();
                } catch (Exception e) {
                    log.warn("Exception during cycle", e);
                }
            }
            teardown();
        }

        private void setup() {
            map = storageService.<String, String>consistentMapBuilder()
                    .withName("perf-test")
                    .withSerializer(Serializer.using(KryoNamespaces.BASIC))
                    .build();
        }

        private void submit() {
            if (currentCounter.incrementAndGet() % 100 < writePercentage) {
                map.put(key, value);
            } else {
                map.get(key);
            }
        }

        private void teardown() {
            map.destroy();
        }
    }

    private class InternalControl implements Consumer<String> {
        @Override
        public void accept(String cmd) {
            log.info("Received command {}", cmd);
            if (cmd.equals(START)) {
                startTestRun();
            } else {
                stopTestRun();
            }
        }
    }

    private class ReporterTask extends TimerTask {
        @Override
        public void run() {
            long endTime = System.currentTimeMillis();
            long overallTime = endTime - startTime;
            long currentTime = endTime - currentStartTime;
            long currentCount = currentCounter.getAndSet(0);
            long overallCount = overallCounter.addAndGet(currentCount);
            sampleCollector.recordSample(overallTime > 0 ? overallCount / (overallTime / 1000d) : 0,
                    currentTime > 0 ? currentCount / (currentTime / 1000d) : 0);
            currentStartTime = System.currentTimeMillis();
        }
    }

}
