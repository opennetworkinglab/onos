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

import com.google.common.collect.Lists;
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
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;

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

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.System.currentTimeMillis;
import static org.onlab.util.Tools.get;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.primitiveperf.OsgiPropertyConstants.DETERMINISTIC;
import static org.onosproject.primitiveperf.OsgiPropertyConstants.DETERMINISTIC_DEFAULT;
import static org.onosproject.primitiveperf.OsgiPropertyConstants.INCLUDE_EVENTS;
import static org.onosproject.primitiveperf.OsgiPropertyConstants.INCLUDE_EVENTS_DEFAULT;
import static org.onosproject.primitiveperf.OsgiPropertyConstants.KEY_LENGTH;
import static org.onosproject.primitiveperf.OsgiPropertyConstants.KEY_LENGTH_DEFAULT;
import static org.onosproject.primitiveperf.OsgiPropertyConstants.NUM_CLIENTS;
import static org.onosproject.primitiveperf.OsgiPropertyConstants.NUM_CLIENTS_DEFAULT;
import static org.onosproject.primitiveperf.OsgiPropertyConstants.NUM_KEYS;
import static org.onosproject.primitiveperf.OsgiPropertyConstants.NUM_KEYS_DEFAULT;
import static org.onosproject.primitiveperf.OsgiPropertyConstants.NUM_UNIQUE_VALUES;
import static org.onosproject.primitiveperf.OsgiPropertyConstants.NUM_UNIQUE_VALUES_DEFAULT;
import static org.onosproject.primitiveperf.OsgiPropertyConstants.VALUE_LENGTH;
import static org.onosproject.primitiveperf.OsgiPropertyConstants.VALUE_LENGTH_DEFAULT;
import static org.onosproject.primitiveperf.OsgiPropertyConstants.WRITE_PERCENTAGE;
import static org.onosproject.primitiveperf.OsgiPropertyConstants.WRITE_PERCENTAGE_DEFAULT;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Application to test sustained primitive throughput.
 */
@Component(
    immediate = true,
    service = PrimitivePerfApp.class,
    property = {
        NUM_CLIENTS + ":Integer=" + NUM_CLIENTS_DEFAULT,
        WRITE_PERCENTAGE + ":Integer=" + WRITE_PERCENTAGE_DEFAULT,
        NUM_KEYS + ":Integer=" + NUM_KEYS_DEFAULT,
        KEY_LENGTH + ":Integer=" + KEY_LENGTH_DEFAULT,
        NUM_UNIQUE_VALUES + ":Integer=" + NUM_UNIQUE_VALUES_DEFAULT,
        VALUE_LENGTH + ":Integer=" + VALUE_LENGTH_DEFAULT,
        INCLUDE_EVENTS + ":Boolean=" + INCLUDE_EVENTS_DEFAULT,
        DETERMINISTIC + ":Boolean=" + DETERMINISTIC_DEFAULT,
    }
)
public class PrimitivePerfApp {

    private final Logger log = getLogger(getClass());

    private static final int REPORT_PERIOD = 1_000; //ms

    private static final char[] CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();

    /** Number of clients to use to submit writes. */
    private int numClients = NUM_CLIENTS_DEFAULT;

    /** Percentage of operations to perform as writes. */
    private int writePercentage = WRITE_PERCENTAGE_DEFAULT;

    /** Number of unique keys to write. */
    private int numKeys = NUM_KEYS_DEFAULT;

    /** Key length. */
    private int keyLength = KEY_LENGTH_DEFAULT;

    /** Number of unique values to write. */
    private int numValues = NUM_UNIQUE_VALUES_DEFAULT;

    /** Value length. */
    private int valueLength = VALUE_LENGTH_DEFAULT;

    /** Whether to include events in test. */
    private boolean includeEvents = INCLUDE_EVENTS_DEFAULT;

    /** Whether to deterministically populate entries. */
    private boolean deterministic = DETERMINISTIC_DEFAULT;

    @Reference(cardinality = MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = MANDATORY)
    protected StorageService storageService;

    @Reference(cardinality = MANDATORY)
    protected ComponentConfigService configService;

    @Reference(cardinality = MANDATORY)
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
        int newNumClients = parseInt(properties, NUM_CLIENTS, numClients, NUM_CLIENTS_DEFAULT);
        int newWritePercentage = parseInt(properties, WRITE_PERCENTAGE, writePercentage, WRITE_PERCENTAGE_DEFAULT);
        int newNumKeys = parseInt(properties, NUM_KEYS, numKeys, NUM_KEYS_DEFAULT);
        int newKeyLength = parseInt(properties, KEY_LENGTH, keyLength, KEY_LENGTH_DEFAULT);
        int newNumValues = parseInt(properties, NUM_UNIQUE_VALUES, numValues, NUM_UNIQUE_VALUES_DEFAULT);
        int newValueLength = parseInt(properties, VALUE_LENGTH, valueLength, VALUE_LENGTH_DEFAULT);

        String includeEventsString = get(properties, INCLUDE_EVENTS);
        boolean newIncludeEvents = isNullOrEmpty(includeEventsString)
            ? includeEvents
            : Boolean.parseBoolean(includeEventsString.trim());

        String deterministicString = get(properties, DETERMINISTIC);
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
