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
package org.onosproject.transactionperf;

import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.CommitStatus;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.TransactionContext;
import org.onosproject.store.service.TransactionalMap;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;

import java.util.Comparator;
import java.util.Dictionary;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onlab.util.Tools.get;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.transactionperf.OsgiPropertyConstants.MAP_NAME;
import static org.onosproject.transactionperf.OsgiPropertyConstants.MAP_NAME_DEFAULT;
import static org.onosproject.transactionperf.OsgiPropertyConstants.READ_PERCENTAGE;
import static org.onosproject.transactionperf.OsgiPropertyConstants.READ_PERCENTAGE_DEFAULT;
import static org.onosproject.transactionperf.OsgiPropertyConstants.REPORT_INTERVAL_SECONDS;
import static org.onosproject.transactionperf.OsgiPropertyConstants.REPORT_INTERVAL_SECONDS_DEFAULT;
import static org.onosproject.transactionperf.OsgiPropertyConstants.TOTAL_OPERATIONS;
import static org.onosproject.transactionperf.OsgiPropertyConstants.TOTAL_OPERATIONS_DEFAULT;
import static org.onosproject.transactionperf.OsgiPropertyConstants.WITH_CONTENTION;
import static org.onosproject.transactionperf.OsgiPropertyConstants.WITH_CONTENTION_DEFAULT;
import static org.onosproject.transactionperf.OsgiPropertyConstants.WITH_RETRIES;
import static org.onosproject.transactionperf.OsgiPropertyConstants.WITH_RETRIES_DEFAULT;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Application for measuring transaction performance.
 */
@Component(
    immediate = true,
    service = TransactionPerfApp.class,
    property = {
        MAP_NAME + "=" + MAP_NAME_DEFAULT,
        READ_PERCENTAGE + ":Double=" + READ_PERCENTAGE_DEFAULT,
        TOTAL_OPERATIONS + ":Integer=" + TOTAL_OPERATIONS_DEFAULT,
        WITH_CONTENTION + ":Boolean=" + WITH_CONTENTION_DEFAULT,
        WITH_RETRIES + ":Boolean=" + WITH_RETRIES_DEFAULT,
        REPORT_INTERVAL_SECONDS + ":Integer=" + REPORT_INTERVAL_SECONDS_DEFAULT
    }
)
public class TransactionPerfApp {
    private final Logger log = getLogger(getClass());

    @Reference(cardinality = MANDATORY)
    protected StorageService storageService;

    @Reference(cardinality = MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = MANDATORY)
    protected ComponentConfigService configService;

    private static final String KEY_PREFIX = "key";

    /** The name of the map to use for testing. */
    protected String mapName = MAP_NAME_DEFAULT;

    /** Percentage of reads to perform. */
    protected double readPercentage = READ_PERCENTAGE_DEFAULT;

    /** Number of operations to perform within each transaction. */
    protected int totalOperationsPerTransaction = TOTAL_OPERATIONS_DEFAULT;

    /** Whether to test transactions with contention from all nodes. */
    protected boolean withContention = WITH_CONTENTION_DEFAULT;

    /** Whether to retry transactions until success. */
    protected boolean withRetries = WITH_RETRIES_DEFAULT;

    /** The frequency with which to report performance in seconds. */
    protected int reportIntervalSeconds = REPORT_INTERVAL_SECONDS_DEFAULT;

    private ExecutorService testRunner =
            Executors.newSingleThreadExecutor(Tools.groupedThreads("app/transaction-perf-test-runner", ""));

    private ScheduledExecutorService reporter =
            Executors.newSingleThreadScheduledExecutor(
                    groupedThreads("onos/transaction-perf-test", "reporter"));

    private Serializer serializer = Serializer.using(KryoNamespaces.BASIC);

    private AtomicInteger attempted = new AtomicInteger(0);
    private AtomicInteger succeeded = new AtomicInteger(0);
    private AtomicInteger iteration = new AtomicInteger(0);

    @Activate
    public void activate(ComponentContext context) {
        configService.registerProperties(getClass());
        if (isParticipant()) {
            startTest();
            reporter.scheduleWithFixedDelay(this::reportPerformance,
                    reportIntervalSeconds,
                    reportIntervalSeconds,
                    TimeUnit.SECONDS);
            logConfig("Started");
        }
    }

    @Modified
    public void modified(ComponentContext context) {
        if (context == null) {
            mapName = MAP_NAME_DEFAULT;
            readPercentage = READ_PERCENTAGE_DEFAULT;
            totalOperationsPerTransaction = TOTAL_OPERATIONS_DEFAULT;
            withContention = WITH_CONTENTION_DEFAULT;
            withRetries = WITH_RETRIES_DEFAULT;
            reportIntervalSeconds = REPORT_INTERVAL_SECONDS_DEFAULT;
            return;
        }

        Dictionary properties = context.getProperties();

        String newMapName = mapName;
        double newReadPercentage = readPercentage;
        int newTotalOperationsPerTransaction = totalOperationsPerTransaction;
        boolean newWithContention = withContention;
        boolean newWithRetries = withRetries;
        int newReportIntervalSeconds = reportIntervalSeconds;

        try {
            String s;

            s = get(properties, MAP_NAME);
            if (!isNullOrEmpty(s)) {
                newMapName = s;
            }

            s = get(properties, READ_PERCENTAGE);
            if (!isNullOrEmpty(s)) {
                newReadPercentage = Double.parseDouble(s);
            }

            s = get(properties, TOTAL_OPERATIONS);
            if (!isNullOrEmpty(s)) {
                newTotalOperationsPerTransaction = Integer.parseInt(s);
            }

            s = get(properties, WITH_CONTENTION);
            if (!isNullOrEmpty(s)) {
                newWithContention = Boolean.parseBoolean(s);
            }

            s = get(properties, WITH_RETRIES);
            if (!isNullOrEmpty(s)) {
                newWithRetries = Boolean.parseBoolean(s);
            }

            s = get(properties, REPORT_INTERVAL_SECONDS);
            if (!isNullOrEmpty(s)) {
                newReportIntervalSeconds = Integer.parseInt(s);
            }
        } catch (NumberFormatException | ClassCastException e) {
            return;
        }

        boolean modified = newMapName != mapName
                || newReadPercentage != readPercentage
                || newTotalOperationsPerTransaction != totalOperationsPerTransaction
                || newWithContention != withContention
                || newWithRetries != withRetries
                || newReportIntervalSeconds != reportIntervalSeconds;

        // If no configuration options have changed, skip restarting the test.
        if (!modified) {
            return;
        }

        mapName = newMapName;
        readPercentage = newReadPercentage;
        totalOperationsPerTransaction = newTotalOperationsPerTransaction;
        withContention = newWithContention;
        withRetries = newWithRetries;
        reportIntervalSeconds = newReportIntervalSeconds;

        // Restart the test.
        stopTest();
        testRunner = Executors.newSingleThreadExecutor(
                groupedThreads("app/transaction-perf-test-runner", ""));
        reporter = Executors.newSingleThreadScheduledExecutor(
                groupedThreads("onos/transaction-perf-test", "reporter"));
        startTest();
        reporter.scheduleWithFixedDelay(this::reportPerformance,
                reportIntervalSeconds,
                reportIntervalSeconds,
                TimeUnit.SECONDS);
        logConfig("Restarted");
    }

    @Deactivate
    public void deactivate(ComponentContext context) {
        configService.unregisterProperties(getClass(), false);
        stopTest();
        log.info("Stopped");
    }

    private void logConfig(String prefix) {
        log.info("{} with mapName = {}; readPercentage = {}; totalOperationsPerTransaction = {}; " +
                        "withContention = {}; withRetries = {}; reportIntervalSeconds = {}",
                prefix, mapName, readPercentage, totalOperationsPerTransaction,
                withContention, withRetries, reportIntervalSeconds);
    }

    /**
     * Returns whether this node is a participant in the test.
     *
     * @return whether this node is a participant in the test
     */
    private boolean isParticipant() {
        return withContention || clusterService.getLocalNode().id().equals(clusterService.getNodes().stream()
                .map(ControllerNode::id)
                .min(Comparator.naturalOrder()).get());
    }

    /**
     * Initializes the map.
     */
    private void initializeMap() {
        TransactionContext context = storageService.transactionContextBuilder().build();
        context.begin();
        try {
            TransactionalMap<String, String> map = context.getTransactionalMap(mapName, serializer);
            for (int i = 0; i < totalOperationsPerTransaction; i++) {
                map.put(KEY_PREFIX + i, KEY_PREFIX + i);
            }
            context.commit().join();
        } catch (Exception e) {
            context.abort();
            log.warn("An exception occurred during initialization: {}", e);
        }
    }

    /**
     * Starts the test.
     */
    private void startTest() {
        logConfig("Started");
        initializeMap();
        runTest(iteration.getAndIncrement());
    }

    /**
     * Runs the test.
     */
    private void runTest(int iteration) {
        testRunner.execute(() -> {
            // Attempt the transaction until successful if retries are enabled.
            CommitStatus status = null;
            do {
                TransactionContext context = storageService.transactionContextBuilder().build();
                context.begin();

                try {
                    TransactionalMap<String, String> map = context.getTransactionalMap(mapName, serializer);
                    int reads = (int) (totalOperationsPerTransaction * readPercentage);
                    for (int i = 0; i < reads; i++) {
                        map.get(KEY_PREFIX + i);
                    }

                    int writes = (int) (totalOperationsPerTransaction * (1 - readPercentage));
                    for (int i = 0; i < writes; i++) {
                        map.put(KEY_PREFIX + i, KEY_PREFIX + iteration + i);
                    }

                    status = context.commit().join();
                    attempted.incrementAndGet();
                } catch (Exception e) {
                    context.abort();
                    log.warn("An exception occurred during a transaction: {}", e);
                }
            } while (withRetries && status != CommitStatus.SUCCESS);

            // If the transaction was successful, increment succeeded transactions.
            if (status == CommitStatus.SUCCESS) {
                succeeded.incrementAndGet();
            }

            runTest(this.iteration.getAndIncrement());
        });
    }

    /**
     * Reports transaction performance.
     */
    private void reportPerformance() {
        log.info("Attempted: {} Succeeded: {} Total iterations: {}",
                attempted.getAndSet(0),
                succeeded.getAndSet(0),
                iteration.get());
    }

    /**
     * Stops a test.
     */
    private void stopTest() {
        testRunner.shutdown();
        reporter.shutdown();
    }
}
