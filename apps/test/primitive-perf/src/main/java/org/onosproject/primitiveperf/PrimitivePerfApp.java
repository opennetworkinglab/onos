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

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.collect.Lists;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.AtomicValue;
import org.onosproject.store.service.AtomicValueEventListener;
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

    private static final int DEFAULT_NUM_CLIENTS = 8;
    private static final int DEFAULT_WRITE_PERCENTAGE = 100;

    private static final int DEFAULT_NUM_KEYS = 100_000;
    private static final int DEFAULT_KEY_LENGTH = 32;
    private static final int DEFAULT_NUM_UNIQUE_VALUES = 100;
    private static final int DEFAULT_VALUE_LENGTH = 1024;
    private static final boolean DEFAULT_INCLUDE_EVENTS = false;
    private static final boolean DEFAULT_DETERMINISTIC = true;

    private static final int REPORT_PERIOD = 1_000; //ms

    private static final char[] CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();

    @Property(
        name = "numClients",
        intValue = DEFAULT_NUM_CLIENTS,
        label = "Number of clients to use to submit writes")
    private int numClients = DEFAULT_NUM_CLIENTS;

    @Property(
        name = "writePercentage",
        intValue = DEFAULT_WRITE_PERCENTAGE,
        label = "Percentage of operations to perform as writes")
    private int writePercentage = DEFAULT_WRITE_PERCENTAGE;

    @Property(
        name = "numKeys",
        intValue = DEFAULT_NUM_KEYS,
        label = "Number of unique keys to write")
    private int numKeys = DEFAULT_NUM_KEYS;

    @Property(
        name = "keyLength",
        intValue = DEFAULT_KEY_LENGTH,
        label = "Key length")
    private int keyLength = DEFAULT_KEY_LENGTH;

    @Property(
        name = "numValues",
        intValue = DEFAULT_NUM_UNIQUE_VALUES,
        label = "Number of unique values to write")
    private int numValues = DEFAULT_NUM_UNIQUE_VALUES;

    @Property(
        name = "valueLength",
        intValue = DEFAULT_VALUE_LENGTH,
        label = "Value length")
    private int valueLength = DEFAULT_VALUE_LENGTH;

    @Property(
        name = "includeEvents",
        boolValue = DEFAULT_INCLUDE_EVENTS,
        label = "Whether to include events in test")
    private boolean includeEvents = DEFAULT_INCLUDE_EVENTS;

    @Property(
        name = "deterministic",
        boolValue = DEFAULT_DETERMINISTIC,
        label = "Whether to deterministically populate entries")
    private boolean deterministic = DEFAULT_DETERMINISTIC;

    @Reference(cardinality = MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = MANDATORY_UNARY)
    protected ComponentConfigService configService;

    @Reference(cardinality = MANDATORY_UNARY)
    protected PrimitivePerfCollector sampleCollector;

    private ExecutorService messageHandlingExecutor;

    private ExecutorService workers;

    // Tracks whether the test has been started in the cluster.
    private AtomicValue<Boolean> started;
    private AtomicValueEventListener<Boolean> startedListener = event -> {
        if (event.newValue()) {
            startTestRun();
        } else {
            stopTestRun();
        }
    };

    // Tracks whether local clients are running.
    private volatile boolean running;

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
        started = storageService.<Boolean>atomicValueBuilder()
            .withName("perf-test-started")
            .withSerializer(Serializer.using(KryoNamespaces.BASIC))
            .build()
            .asAtomicValue();
        started.addListener(startedListener);

        reportTimer = new Timer("onos-primitive-perf-reporter");

        messageHandlingExecutor = Executors.newSingleThreadExecutor(
                groupedThreads("onos/perf", "command-handler"));

        // TODO: investigate why this seems to be necessary for configs to get picked up on initial activation
        modify(context);
    }

    @Deactivate
    public void deactivate() {
        stopTestRun();

        configService.unregisterProperties(getClass(), false);
        messageHandlingExecutor.shutdown();
        started.removeListener(startedListener);

        if (reportTimer != null) {
            reportTimer.cancel();
            reportTimer = null;
        }
    }

    private int parseInt(Dictionary<?, ?> properties, String name, int currentValue, int defaultValue) {
        try {
            String s = get(properties, name);
            return isNullOrEmpty(s) ? currentValue : Integer.parseInt(s.trim());
        } catch (NumberFormatException | ClassCastException e) {
            log.warn("Malformed configuration detected; using defaults", e);
            return defaultValue;
        }
    }

    @Modified
    public void modify(ComponentContext context) {
        if (context == null) {
            logConfig("Reconfigured");
            return;
        }

        Dictionary<?, ?> properties = context.getProperties();
        int newNumClients = parseInt(properties, "numClients", numClients, DEFAULT_NUM_CLIENTS);
        int newWritePercentage = parseInt(properties, "writePercentage", writePercentage, DEFAULT_WRITE_PERCENTAGE);
        int newNumKeys = parseInt(properties, "numKeys", numKeys, DEFAULT_NUM_KEYS);
        int newKeyLength = parseInt(properties, "keyLength", keyLength, DEFAULT_KEY_LENGTH);
        int newNumValues = parseInt(properties, "numValues", numValues, DEFAULT_NUM_UNIQUE_VALUES);
        int newValueLength = parseInt(properties, "valueLength", valueLength, DEFAULT_VALUE_LENGTH);

        String includeEventsString = get(properties, "includeEvents");
        boolean newIncludeEvents = isNullOrEmpty(includeEventsString)
            ? includeEvents
            : Boolean.parseBoolean(includeEventsString.trim());

        String deterministicString = get(properties, "deterministic");
        boolean newDeterministic = isNullOrEmpty(deterministicString)
            ? includeEvents
            : Boolean.parseBoolean(deterministicString.trim());

        if (newNumClients != numClients
            || newWritePercentage != writePercentage
            || newNumKeys != numKeys
            || newKeyLength != keyLength
            || newNumValues != numValues
            || newValueLength != valueLength
            || newIncludeEvents != includeEvents
            || newDeterministic != deterministic) {
            numClients = newNumClients;
            writePercentage = newWritePercentage;
            numKeys = newNumKeys;
            keyLength = newKeyLength;
            numValues = newNumValues;
            valueLength = newValueLength;
            includeEvents = newIncludeEvents;
            deterministic = newDeterministic;
            logConfig("Reconfigured");
            Boolean started = this.started.get();
            if (started != null && started) {
                stop();
                start();
            }
        } else {
            Boolean started = this.started.get();
            if (started != null && started) {
                if (running) {
                    stopTestRun();
                }
                startTestRun();
            }
        }
    }

    /**
     * Starts a new test.
     */
    public void start() {
        // To stop the test, we simply update the "started" value. Events from the change will notify all
        // nodes to start the test.
        Boolean started = this.started.get();
        if (started == null || !started) {
            this.started.set(true);
        }
    }

    /**
     * Stops a test.
     */
    public void stop() {
        // To stop the test, we simply update the "started" value. Events from the change will notify all
        // nodes to stop the test.
        Boolean started = this.started.get();
        if (started != null && started) {
            this.started.set(false);
        }
    }

    private void logConfig(String prefix) {
        log.info("{} with " +
            "numClients = {}; " +
            "writePercentage = {}; " +
            "numKeys = {}; " +
            "keyLength = {}; " +
            "numValues = {}; " +
            "valueLength = {}; " +
            "includeEvents = {}; " +
            "deterministic = {}",
            prefix,
            numClients,
            writePercentage,
            numKeys,
            keyLength,
            numValues,
            valueLength,
            includeEvents,
            deterministic);
    }

    private void startTestRun() {
        if (running) {
            return;
        }

        sampleCollector.clearSamples();

        startTime = System.currentTimeMillis();
        currentStartTime = startTime;
        currentCounter = new AtomicLong();
        overallCounter = new AtomicLong();

        reporterTask = new ReporterTask();
        reportTimer.scheduleAtFixedRate(
            reporterTask, REPORT_PERIOD - currentTimeMillis() % REPORT_PERIOD, REPORT_PERIOD);

        running = true;

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
            String[] keys = createStrings(keyLength, numKeys);
            String[] values = createStrings(valueLength, numValues);

            workers = Executors.newFixedThreadPool(workerCount, groupedThreads("onos/primitive-perf", "worker-%d"));
            for (int i = 0; i < workerCount; i++) {
                if (deterministic) {
                    workers.submit(new DeterministicRunner(keys, values));
                } else {
                    workers.submit(new NonDeterministicRunner(keys, values));
                }
            }
        }
        log.info("Started test run");
    }

    /**
     * Creates a deterministic array of strings to write to the cluster.
     *
     * @param length the string lengths
     * @param count the string count
     * @return a deterministic array of strings
     */
    private String[] createStrings(int length, int count) {
        Random random = new Random(length);
        List<String> stringsList = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            stringsList.add(randomString(length, random));
        }
        return stringsList.toArray(new String[stringsList.size()]);
    }

    /**
     * Creates a deterministic string based on the given seed.
     *
     * @param length the seed from which to create the string
     * @param random the random object from which to create the string characters
     * @return the string
     */
    private String randomString(int length, Random random) {
        char[] buffer = new char[length];
        for (int i = 0; i < length; i++) {
            buffer[i] = CHARS[random.nextInt(CHARS.length)];
        }
        return new String(buffer);
    }

    private void stopTestRun() {
        if (!running) {
            return;
        }

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
                Thread.currentThread().interrupt();
            }
        }

        sampleCollector.recordSample(0, 0);
        sampleCollector.recordSample(0, 0);

        running = false;

        log.info("Stopped test run");
    }

    // Submits primitive operations.
    abstract class Runner implements Runnable {
        final String[] keys;
        final String[] values;
        final Random random = new Random();
        ConsistentMap<String, String> map;

        Runner(String[] keys, String[] values) {
            this.keys = keys;
            this.values = values;
        }

        @Override
        public void run() {
            setup();
            while (running) {
                try {
                    submit();
                } catch (Exception e) {
                    log.warn("Exception during cycle", e);
                }
            }
            teardown();
        }

        void setup() {
            map = storageService.<String, String>consistentMapBuilder()
                    .withName("perf-test")
                    .withSerializer(Serializer.using(KryoNamespaces.BASIC))
                    .build();
            if (includeEvents) {
                map.addListener(event -> {
                });
            }
        }

        abstract void submit();

        void teardown() {
            //map.destroy();
        }
    }

    private class NonDeterministicRunner extends Runner {
        NonDeterministicRunner(String[] keys, String[] values) {
            super(keys, values);
        }

        @Override
        void submit() {
            currentCounter.incrementAndGet();
            String key = keys[random.nextInt(keys.length)];
            if (random.nextInt(100) < writePercentage) {
                map.put(key, values[random.nextInt(values.length)]);
            } else {
                map.get(key);
            }
        }
    }

    private class DeterministicRunner extends Runner {
        private int index;

        DeterministicRunner(String[] keys, String[] values) {
            super(keys, values);
        }

        @Override
        void submit() {
            currentCounter.incrementAndGet();
            if (random.nextInt(100) < writePercentage) {
                map.put(keys[index++ % keys.length], values[random.nextInt(values.length)]);
            } else {
                map.get(keys[random.nextInt(keys.length)]);
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
