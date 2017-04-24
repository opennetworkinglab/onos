/*
 * Copyright 2017-present Open Networking Laboratory
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

import java.util.Comparator;
import java.util.Dictionary;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
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
import org.slf4j.Logger;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.apache.felix.scr.annotations.ReferenceCardinality.MANDATORY_UNARY;
import static org.onlab.util.Tools.get;
import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Application for measuring transaction performance.
 */
@Component(immediate = true, enabled = true)
@Service(value = TransactionPerfApp.class)
public class TransactionPerfApp {
    private final Logger log = getLogger(getClass());

    @Reference(cardinality = MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService configService;

    private static final String DEFAULT_MAP_NAME = "transaction-perf";
    private static final double DEFAULT_READ_PERCENTAGE = .9;
    private static final int DEFAULT_TOTAL_OPERATIONS = 1000;
    private static final boolean DEFAULT_WITH_CONTENTION = false;
    private static final boolean DEFAULT_WITH_RETRIES = false;
    private static final int DEFAULT_REPORT_INTERVAL_SECONDS = 1;
    private static final String KEY_PREFIX = "key";

    @Property(name = "mapName", value = DEFAULT_MAP_NAME,
            label = "The name of the map to use for testing")
    protected String mapName = DEFAULT_MAP_NAME;

    @Property(name = "readPercentage", doubleValue = DEFAULT_READ_PERCENTAGE,
            label = "Percentage of reads to perform")
    protected double readPercentage = DEFAULT_READ_PERCENTAGE;

    @Property(name = "totalOperationsPerTransaction", intValue = DEFAULT_TOTAL_OPERATIONS,
            label = "Number of operations to perform within each transaction")
    protected int totalOperationsPerTransaction = DEFAULT_TOTAL_OPERATIONS;

    @Property(name = "withContention", boolValue = DEFAULT_WITH_CONTENTION,
            label = "Whether to test transactions with contention from all nodes")
    protected boolean withContention = DEFAULT_WITH_CONTENTION;

    @Property(name = "withRetries", boolValue = DEFAULT_WITH_RETRIES,
            label = "Whether to retry transactions until success")
    protected boolean withRetries = DEFAULT_WITH_RETRIES;

    @Property(name = "reportIntervalSeconds", intValue = DEFAULT_REPORT_INTERVAL_SECONDS,
            label = "The frequency with which to report performance in seconds")
    protected int reportIntervalSeconds = 1;

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
            mapName = DEFAULT_MAP_NAME;
            readPercentage = DEFAULT_READ_PERCENTAGE;
            totalOperationsPerTransaction = DEFAULT_TOTAL_OPERATIONS;
            withContention = DEFAULT_WITH_CONTENTION;
            withRetries = DEFAULT_WITH_RETRIES;
            reportIntervalSeconds = DEFAULT_REPORT_INTERVAL_SECONDS;
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

            s = get(properties, "mapName");
            if (!isNullOrEmpty(s)) {
                newMapName = s;
            }

            s = get(properties, "readPercentage");
            if (!isNullOrEmpty(s)) {
                newReadPercentage = Double.parseDouble(s);
            }

            s = get(properties, "totalOperationsPerTransaction");
            if (!isNullOrEmpty(s)) {
                newTotalOperationsPerTransaction = Integer.parseInt(s);
            }

            s = get(properties, "withContention");
            if (!isNullOrEmpty(s)) {
                newWithContention = Boolean.parseBoolean(s);
            }

            s = get(properties, "withRetries");
            if (!isNullOrEmpty(s)) {
                newWithRetries = Boolean.parseBoolean(s);
            }

            s = get(properties, "reportIntervalSeconds");
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
