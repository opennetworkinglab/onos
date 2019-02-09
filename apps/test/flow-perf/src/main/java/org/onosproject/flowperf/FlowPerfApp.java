/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.flowperf;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.onlab.packet.MacAddress;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.Device;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleEvent;
import org.onosproject.net.flow.FlowRuleListener;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.Instructions;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;

import java.util.Dictionary;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onlab.util.Tools.get;
import static org.onosproject.flowperf.OsgiPropertyConstants.BATCH_SIZE;
import static org.onosproject.flowperf.OsgiPropertyConstants.BATCH_SIZE_DEFAULT;
import static org.onosproject.flowperf.OsgiPropertyConstants.TOTAL_FLOWS;
import static org.onosproject.flowperf.OsgiPropertyConstants.TOTAL_FLOWS_DEFAULT;
import static org.onosproject.flowperf.OsgiPropertyConstants.TOTAL_THREADS;
import static org.onosproject.flowperf.OsgiPropertyConstants.TOTAL_THREADS_DEFAULT;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Application for measuring flow installation performance.
 * <p>
 * This application installs a bunch of flows, validates that all those flows have
 * been successfully added and immediately proceeds to remove all the added flows.
 */
@Component(
    immediate = true,
    service = FlowPerfApp.class,
    property = {
        TOTAL_FLOWS + ":Integer=" + TOTAL_FLOWS_DEFAULT,
        BATCH_SIZE + ":Integer=" + BATCH_SIZE_DEFAULT,
        TOTAL_THREADS + ":Integer=" + TOTAL_THREADS_DEFAULT
    }
)
public class FlowPerfApp {
    private final Logger log = getLogger(getClass());

    @Reference(cardinality = MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = MANDATORY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = MANDATORY)
    protected ComponentConfigService configService;

    protected ApplicationId appId;

    private AtomicInteger pendingBatchCount;
    private CountDownLatch installationLatch;
    private CountDownLatch uninstallationLatch;
    private Iterator<Device> devices;
    private AtomicLong macIndex;

    List<FlowRule> addedRules = Lists.newArrayList();

    /** Total number of flows. */
    private int totalFlows = TOTAL_FLOWS_DEFAULT;

    /** Number of flows per batch. */
    private int batchSize = BATCH_SIZE_DEFAULT;

    /** Number of installer threads. */
    private int totalThreads = TOTAL_THREADS_DEFAULT;

    private ExecutorService installer;
    private ExecutorService testRunner =
            Executors.newSingleThreadExecutor(Tools.groupedThreads("app/flow-perf-test-runner", ""));

    @Activate
    public void activate(ComponentContext context) {
        appId = coreService.registerApplication("org.onosproject.flowperf");
        configService.registerProperties(getClass());
        installer = Executors.newFixedThreadPool(totalThreads, Tools.groupedThreads("app/flow-perf-worker", "%d"));
        testRunner.submit(this::runTest);
        log.info("Started");
    }

    @Deactivate
    public void deactivate(ComponentContext context) {
        installer.shutdown();
        testRunner.shutdown();
        configService.unregisterProperties(getClass(), false);
        log.info("Stopped.");
    }

    private void runTest() {
        pendingBatchCount = new AtomicInteger(totalFlows / batchSize);
        installationLatch = new CountDownLatch(totalFlows);
        List<Device> deviceList = Lists.newArrayList();
        deviceService.getAvailableDevices().forEach(deviceList::add);
        devices = Iterables.cycle(deviceList).iterator();
        log.info("Starting installation. Total flows: {}, Total threads: {}, "
                + "Batch Size: {}", totalFlows, totalThreads, batchSize);

        macIndex = new AtomicLong(0);
        FlowRuleListener addMonitor = event -> {
            if (event.type() == FlowRuleEvent.Type.RULE_ADDED) {
                installationLatch.countDown();
            }
        };

        flowRuleService.addListener(addMonitor);
        long addStartTime = System.currentTimeMillis();
        for (int i = 0; i < totalThreads; ++i) {
            installer.submit(() -> {
                while (pendingBatchCount.getAndDecrement() > 0) {
                    List<FlowRule> batch = nextBatch(batchSize);
                    addedRules.addAll(batch);
                    flowRuleService.applyFlowRules(batch.toArray(new FlowRule[]{}));
                }
            });
        }

        // Wait till all the flows are in ADDED state.
        try {
            installationLatch.await();
        } catch (InterruptedException e) {
            Thread.interrupted();
        }
        log.info("Time to install {} flows: {} ms", totalFlows, System.currentTimeMillis() - addStartTime);
        flowRuleService.removeListener(addMonitor);


        uninstallationLatch = new CountDownLatch(totalFlows);
        FlowRuleListener removeListener = event -> {
            if (event.type() == FlowRuleEvent.Type.RULE_REMOVED) {
                uninstallationLatch.countDown();
            }
        };
        AtomicInteger currentIndex = new AtomicInteger(0);
        long removeStartTime = System.currentTimeMillis();
        flowRuleService.addListener(removeListener);
        // Uninstallation runs on a single thread.
        installer.submit(() -> {
            while (currentIndex.get() < addedRules.size()) {
                List<FlowRule> removeBatch = addedRules.subList(currentIndex.get(),
                        Math.min(currentIndex.get() + batchSize, addedRules.size()));
                currentIndex.addAndGet(removeBatch.size());
                flowRuleService.removeFlowRules(removeBatch.toArray(new FlowRule[]{}));
            }
        });
        try {
            uninstallationLatch.await();
        } catch (InterruptedException e) {
            Thread.interrupted();
        }
        log.info("Time to uninstall {} flows: {} ms", totalFlows, System.currentTimeMillis() - removeStartTime);
        flowRuleService.removeListener(removeListener);
    }

    private List<FlowRule> nextBatch(int size) {
        List<FlowRule> rules = Lists.newArrayList();
        for (int i = 0; i < size; ++i) {
            Device device = devices.next();
            long srcMac = macIndex.incrementAndGet();
            long dstMac = srcMac + 1;
            TrafficSelector selector = DefaultTrafficSelector.builder()
                    .matchEthSrc(MacAddress.valueOf(srcMac))
                    .matchEthDst(MacAddress.valueOf(dstMac))
                    .matchInPort(PortNumber.portNumber(2))
                    .build();
            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .add(Instructions.createOutput(PortNumber.portNumber(3))).build();
            FlowRule rule = DefaultFlowRule.builder()
                .forDevice(device.id())
                .withSelector(selector)
                .withTreatment(treatment)
                .withPriority(100)
                .fromApp(appId)
                .withHardTimeout(50000)
                .makePermanent()
                .build();

            rules.add(rule);
        }
        return rules;
    }

    @Modified
    public void modified(ComponentContext context) {
        if (context == null) {
            totalFlows = TOTAL_FLOWS_DEFAULT;
            batchSize = BATCH_SIZE_DEFAULT;
            totalThreads = TOTAL_THREADS_DEFAULT;
            return;
        }

        Dictionary properties = context.getProperties();

        int newTotalFlows = totalFlows;
        int newBatchSize = batchSize;
        int newTotalThreads = totalThreads;
        try {
            String s = get(properties, TOTAL_FLOWS);
            newTotalFlows = isNullOrEmpty(s)
                    ? totalFlows : Integer.parseInt(s.trim());

            s = get(properties, BATCH_SIZE);
            newBatchSize = isNullOrEmpty(s)
                    ? batchSize : Integer.parseInt(s.trim());

            s = get(properties, TOTAL_THREADS);
            newTotalThreads = isNullOrEmpty(s)
                    ? totalThreads : Integer.parseInt(s.trim());

        } catch (NumberFormatException | ClassCastException e) {
            return;
        }

        boolean modified = newTotalFlows != totalFlows || newTotalThreads != totalThreads ||
                newBatchSize != batchSize;

        // If nothing has changed, simply return.
        if (!modified) {
            return;
        }

        totalFlows = newTotalFlows;
        batchSize = newBatchSize;
        if (totalThreads != newTotalThreads) {
            totalThreads = newTotalThreads;
            installer.shutdown();
            installer = Executors.newFixedThreadPool(totalThreads, Tools.groupedThreads("flow-perf-worker", "%d"));
        }
    }
}
