/*
 * Copyright 2020-present Open Networking Foundation
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
package org.onosproject.diagnosis.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.service.component.ComponentContext;
import org.apache.karaf.bundle.core.BundleService;
import org.apache.karaf.system.SystemService;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cluster.ClusterAdminService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.cluster.messaging.ClusterMessage;
import org.onosproject.store.cluster.messaging.ClusterMessageHandler;
import org.onosproject.store.cluster.messaging.MessageSubject;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Constants;
import org.osgi.framework.wiring.FrameworkWiring;
import org.slf4j.Logger;

import java.util.Dictionary;
import java.io.File;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Collections;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.ProcFd;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.SharedExecutors.getPoolThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;

import static org.onlab.util.Tools.getIntegerProperty;
import static org.onlab.util.Tools.isPropertyEnabled;
import static org.onosproject.diagnosis.impl.OsgiPropertyConstants.INITIAL_POLL_DELAY_MINUTE;
import static org.onosproject.diagnosis.impl.OsgiPropertyConstants.POLL_FREQUENCY_MINUTE;
import static org.onosproject.diagnosis.impl.OsgiPropertyConstants.DEFAULT_INITIAL_POLL_DELAY_MINUTE;
import static org.onosproject.diagnosis.impl.OsgiPropertyConstants.DEFAULT_POLL_FREQUENCY_MINUTE;
import static org.onosproject.diagnosis.impl.OsgiPropertyConstants.REBOOT_RETRY_COUNT;
import static org.onosproject.diagnosis.impl.OsgiPropertyConstants.DEFAULT_REBOOT_RETRY_COUNT;
import static org.onosproject.diagnosis.impl.OsgiPropertyConstants.INITIAL_CLUSTER_TIMEOUT_PERIOD;
import static org.onosproject.diagnosis.impl.OsgiPropertyConstants.DEFAULT_CLUSTER_TIMEOUT_PERIOD;
import static org.onosproject.diagnosis.impl.OsgiPropertyConstants.INITIAL_DIAGNOSIS_ACTION;
import static org.onosproject.diagnosis.impl.OsgiPropertyConstants.DEFAULT_DIAGNOSIS_ACTION;


@Component(immediate = true,
        property = {
                INITIAL_POLL_DELAY_MINUTE + ":Integer=" + DEFAULT_INITIAL_POLL_DELAY_MINUTE,
                POLL_FREQUENCY_MINUTE + ":Integer=" + DEFAULT_POLL_FREQUENCY_MINUTE,
                REBOOT_RETRY_COUNT + ":Integer=" + DEFAULT_REBOOT_RETRY_COUNT,
                INITIAL_CLUSTER_TIMEOUT_PERIOD + ":Integer=" + DEFAULT_CLUSTER_TIMEOUT_PERIOD,
                INITIAL_DIAGNOSIS_ACTION + ":Boolean=" + DEFAULT_DIAGNOSIS_ACTION,
        })
public class NodeDiagnosisManager {

    private static final MessageSubject REBOOT_MSG = new MessageSubject("Node-diagnosis");

    private static int initialPollDelayMinute = DEFAULT_INITIAL_POLL_DELAY_MINUTE;
    private static int pollFrequencyMinute = DEFAULT_POLL_FREQUENCY_MINUTE;
    private static final File CFG_FILE = new File("../config/diag-info.json");
    private static final String REBOOT_NU = "rebootNu";
    private static int initialClusterTimeoutPeriod = DEFAULT_CLUSTER_TIMEOUT_PERIOD;
    private static boolean initialDiagnosisAction = true;
    private static int rebootRetryCount = DEFAULT_REBOOT_RETRY_COUNT;
    private int rebootNu;

    private final Logger log = getLogger(getClass());
    private ScheduledExecutorService metricsExecutor;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private ServiceComponentRuntime scrService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private BundleService bundleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private SystemService systemService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private ClusterCommunicationService communicationService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService cfgService;

    private ScheduledFuture<?> clusterNodeDiagnosisFuture;

    private BundleContext bundleContext;
    private ClusterAdminService caService;
    private NodeId localNodeId;
    private Sigar sigar;
    private static Process getTcpProc;
    private static Process getUdpProc;
    private static final String[] CMD_FOR_NETSTAT_PID = {"/bin/sh", "-c",
            "ps -ef | grep netstat | grep -v grep | cut -c10-15 | tr -d \' \'"};
    private static final String[] CMD_FOR_PID = {"/bin/sh", "-c",
            "ps -ef | grep org.apache.karaf.main.Main | grep -v grep | cut -c10-15 | tr -d \' \'"};
    private static final String[] CMD_FOR_TOTAL_MEMORY = {"/bin/sh", "-c",
            "free -b | cut -d \' \' -f 5"};
    private static long memoryThreshold;
    private static final int FD_THRESHOLD = 50000;
    private static final int SOCKETS_THRESHOLD = 50000;
    private static final int DATA_BLOCK_1024 = 1024;
    private static final int MEMORY_START_IDX = 3;
    private static final String EXTRA_SPACE = "\\s+";
    private static long pid;

    private static final long TIMEOUT = 3000;

    @Activate
    public void activate() {
        cfgService.registerProperties(getClass());
        bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
        getNodeId();
        sigar = new Sigar();
        scheduleAppDiagnosisPolling();
        scheduleClusterNodeDiagnosisPolling();
        getPid();
        getMemoryThreshold();
        scheduleSdncMemoryDiagnosisPolling();
        scheduleSdncFileDescriptorDiagnosisPolling();
        communicationService.addSubscriber(REBOOT_MSG, new InternalSampleCollector(),
                getPoolThreadExecutor());
        rebootNu = fetchRebootNu(); //to restrict number of reboots , reboot numbers will be saved in file and used
        rebootRetryCount = fetchRetryRebootCount(); // to set maximum limit for reboot retry.
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        cfgService.unregisterProperties(getClass(), false);
        communicationService.removeSubscriber(REBOOT_MSG);
        metricsExecutor.shutdownNow();
        clusterNodeDiagnosisFuture.cancel(true);
        log.info("Stopped");
    }

    @Modified
    public void modified(ComponentContext context) {
        readComponentConfiguration(context);
        log.info("modified");
    }

    /**
     * Extracts properties from the component configuration context.
     *
     * @param context the component context
     */
    private void readComponentConfiguration(ComponentContext context) {

        Dictionary<?, ?> properties = context.getProperties();
        boolean changed = false;

        int newPollFrequency = getNewPollFrequency(properties);
        if (newPollFrequency != pollFrequencyMinute) {
            pollFrequencyMinute = newPollFrequency;
            changed = true;
        }

        int newPollDelay = getNewPollDelay(properties);
        if (newPollDelay != pollFrequencyMinute) {
            initialPollDelayMinute = newPollDelay;
            changed = true;
        }
        log.info("Node Diagnosis properties are:" +
                        " initialPollDelayMinute: {}, pollFrequencyMinute: {}",
                initialPollDelayMinute, pollFrequencyMinute);
        if (changed) {
            //stops the old scheduled task
            this.clusterNodeDiagnosisFuture.cancel(true);
            //schedules new task at the new polling rate
            log.info("New Scheduler started with,Node Diagnosis properties:" +
                            " initialPollDelayMinute: {}, pollFrequencyMinute: {}",
                    initialPollDelayMinute, pollFrequencyMinute);
            scheduleClusterNodeDiagnosisPolling();
        }

        int newRebootRetryCount = getNewRebootRetryCount(properties);
        updateDiagFile(rebootNu, newRebootRetryCount);
        initialDiagnosisAction = getNewDiagnosisAction(properties);
    }

    private int getNewPollFrequency(Dictionary<?, ?> properties) {
        int newPollFrequency;
        try {
            newPollFrequency = getIntegerProperty(properties, POLL_FREQUENCY_MINUTE);
        } catch (NumberFormatException | ClassCastException e) {
            newPollFrequency = DEFAULT_POLL_FREQUENCY_MINUTE;
        }
        return newPollFrequency;
    }

    private int getNewPollDelay(Dictionary<?, ?> properties) {
        int newPollDelay;
        try {
            newPollDelay = getIntegerProperty(properties, INITIAL_POLL_DELAY_MINUTE);
        } catch (NumberFormatException | ClassCastException e) {
            newPollDelay = DEFAULT_INITIAL_POLL_DELAY_MINUTE;
        }
        return newPollDelay;
    }

    private int getNewRebootRetryCount(Dictionary<?, ?> properties) {
        int newRebootRetryCount;
        try {
            newRebootRetryCount = getIntegerProperty(properties, REBOOT_RETRY_COUNT);
        } catch (NumberFormatException | ClassCastException e) {
            newRebootRetryCount = DEFAULT_REBOOT_RETRY_COUNT;
        }
        return newRebootRetryCount;
    }

    private boolean getNewDiagnosisAction(Dictionary<?, ?> properties) {
        boolean newDiagnosisAction;
        try {
            newDiagnosisAction = isPropertyEnabled(properties, INITIAL_DIAGNOSIS_ACTION);
        } catch (NumberFormatException | ClassCastException e) {
            newDiagnosisAction = DEFAULT_DIAGNOSIS_ACTION;
        }
        return newDiagnosisAction;
    }

    private List<Bundle> getAllBundles() {
        return Arrays.asList(bundleContext.getBundles());
    }

    private void getNodeId() {
        caService = DefaultServiceDirectory.getService(ClusterAdminService.class);
        if (Objects.isNull(caService)) {
            return;
        }

        localNodeId = caService.getLocalNode().id();
    }

    private void scheduleAppDiagnosisPolling() {
        metricsExecutor = newSingleThreadScheduledExecutor(
                groupedThreads("Nodediagnosis/diagnosisThread",
                        "Nodediagnosis-executor-%d", log));
        metricsExecutor.scheduleAtFixedRate(this::appDiagnosis,
                60,
                30, TimeUnit.SECONDS);
    }

    private void scheduleClusterNodeDiagnosisPolling() {
        clusterNodeDiagnosisFuture = metricsExecutor.scheduleAtFixedRate(this::clusterNodeDiagnosis,
                initialPollDelayMinute,
                pollFrequencyMinute, TimeUnit.MINUTES);
    }

    private void scheduleSdncMemoryDiagnosisPolling() {
        metricsExecutor.scheduleAtFixedRate(this::sdncMemoryDiagnosis,
                60,
                30, TimeUnit.SECONDS);

    }

    private void scheduleSdncFileDescriptorDiagnosisPolling() {
        metricsExecutor.scheduleAtFixedRate(this::sdncFileDescriptorDiagnosis,
                60,
                30, TimeUnit.SECONDS);
    }

    private void appDiagnosis() {
        verifyBundles(null);
        verifyApps();
    }

    private void verifyBundles(String bundleName) {

        if (Objects.isNull(bundleContext)) {
            return;
        }
        try {
            FrameworkWiring wiring = bundleContext.getBundle(Constants.SYSTEM_BUNDLE_LOCATION)
                    .adapt(FrameworkWiring.class);
            if (Objects.isNull(wiring)) {
                return;
            }

            boolean result;
            List<Bundle> bundleList;
            if (Objects.nonNull(bundleName)) {
                log.info("bundle to be resolved and refreshed: {}", bundleName);
                bundleList = this.getAllBundles().stream()
                        .filter(bundle -> bundleService.getInfo(bundle).getName().equals(bundleName))
                        .collect(Collectors.toList());
            } else {
                bundleList = this.getAllBundles().stream()
                        .filter(bundle -> bundleService.getDiag(bundle).split("[\n|\r]").length > 1)
                        .collect(Collectors.toList());
            }
            /**
             * Example diags :
             *  BundleName:onos-providers-openflow-flow,
             *  Diag:Declarative Services
             * ,number of lines of diag:1
             * BundleName:onos-apps-faultmanagement-fmgui,
             *  Diag:Declarative Services
             *  org.onosproject.faultmanagement.alarms.gui.AlarmTableComponent (136)
             *   missing references: uiExtensionService
             * org.onosproject.faultmanagement.alarms.gui.AlarmTopovComponent (137)
             *   missing references: uiExtensionService
             *    number of lines of diag:5
             */
            this.getAllBundles().forEach(
                    bundle -> {
                        log.debug("Bundle service - BundleName:{}, Diag:{}, number of lines of diag:{}",
                                bundleService.getInfo(bundle).getName(),
                                bundleService.getDiag(bundle),
                                bundleService.getDiag(bundle).split("[\n|\r]").length);
                    });


            CompletableFuture<Boolean> completableBundles = CompletableFuture.supplyAsync(() -> {
                Boolean isResolved = wiring.resolveBundles(bundleList);

                wiring.refreshBundles(bundleList);
                return isResolved;
            });
            result = completableBundles.get();

            if (Objects.nonNull(bundleName)) {
                log.info("bundle {} is in resolved State ? {}", bundleName, result ? "Yes" : "No");
            } else {
                log.info("All the  bundles are in resolved State ? {}", result ? "Yes" : "No");
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error("exception occurred because of", e);
        } catch (Exception e) {
            log.error("Exception occured in Verifying Bundles", e);
        }
    }

    private void verifyApps() {
        log.debug("verifyApps() method invoked");
        List<ComponentDescriptionDTO> nonActiveComponents = getNonActiveComponents();

        nonActiveComponents.forEach(component -> {
            try {
                scrService.enableComponent(component).timeout(TIMEOUT);
            } catch (Exception e) {
                throw new IllegalStateException("Unable to start component " + component.name, e);
            }
        });
    }

    private List<ComponentDescriptionDTO> getNonActiveComponents() {
        List<ComponentDescriptionDTO> nonActiveComponents = new ArrayList<>();
        for (ComponentDescriptionDTO component : scrService.getComponentDescriptionDTOs()) {
            if (scrService.isComponentEnabled(component)) {
                for (ComponentConfigurationDTO config : scrService.getComponentConfigurationDTOs(component)) {
                    if (config.state != ComponentConfigurationDTO.ACTIVE) {
                        nonActiveComponents.add(component);
                        break;
                    }
                }
            }
        }
        return nonActiveComponents;
    }

    private int fetchRebootNu() {
        int rebootNum = 0;
        if (!CFG_FILE.exists()) {
            log.debug("CFG file not found for reboot number");
            return rebootNum;
        }

        ObjectNode root;
        try {
            root = (ObjectNode) new ObjectMapper().readTree(CFG_FILE);
            if (Objects.nonNull(root.findValue(REBOOT_NU))) {
                rebootNum = root.findValue(REBOOT_NU).asInt();
            }
        } catch (IOException e) {
            log.error("applyConfiguration: Exception occurred: {} for {}", e, CFG_FILE);
        }
        return rebootNum;
    }

    private int fetchRetryRebootCount() {
        int rebootCount = rebootRetryCount;
        if (!CFG_FILE.exists()) {
            log.debug("CFG file not found for reboot number");
            return rebootCount;
        }

        ObjectNode root;
        try {
            root = (ObjectNode) new ObjectMapper().readTree(CFG_FILE);
            if (Objects.nonNull(root.findValue(REBOOT_RETRY_COUNT))) {
                rebootCount = root.findValue(REBOOT_RETRY_COUNT).asInt();
            }
        } catch (IOException e) {
            log.error("applyConfiguration: Exception occurred: {} for {}", e, CFG_FILE);
        }
        return rebootCount;
    }

    private void resetRebootNu() {
        updateRebootNu(0);
    }

    private void updateRebootNu(int rebootnum) {
        updateDiagFile(rebootnum, rebootRetryCount);
    }

    private void updateDiagFile(int rebootnum, int defaultRebootcount) {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Integer> output = new HashMap<>();
        output.put(REBOOT_RETRY_COUNT, defaultRebootcount);
        output.put(REBOOT_NU, rebootnum);
        rebootNu = rebootnum;
        rebootRetryCount = defaultRebootcount;
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(CFG_FILE, output);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void clusterNodeDiagnosis() {
        if (Objects.isNull(caService)) {
            return;
        }
        try {
            if (caService.getState(localNodeId).equals(ControllerNode.State.READY)) {
                if (rebootNu > 0) {
                    resetRebootNu();
                }
                return;
            }
            long lastUpdatedInstant = caService.getLastUpdatedInstant(localNodeId).until(Instant.now(),
                    ChronoUnit.MINUTES);
            if (lastUpdatedInstant <= initialClusterTimeoutPeriod) {
                return;
            }
            /**
             * Diagnosis Action if set to true, onos reboot occurs when required.
             * Diagnosis Action if set to false, leaves logs informing that onos reboot is needed.
             */
            if (!initialDiagnosisAction) {
                log.info("onos Halt is needed as cluster node status is in: {} for Time out period: {}" +
                                " for node {} with lastUpdatedInstant: {}" +
                                " But, not onos is not rebooted as Diagnosis action is set to false",
                        caService.getState(localNodeId), initialClusterTimeoutPeriod, localNodeId, lastUpdatedInstant);
                return;
            }
            log.info("onos Halt is needed as cluster node status is in: {} for Time out period: {}" +
                            " for node {} with lastUpdatedInstant: {}",
                    caService.getState(localNodeId), initialClusterTimeoutPeriod, localNodeId, lastUpdatedInstant);
            if (rebootNu < rebootRetryCount) {
                updateRebootNu(rebootNu + 1);
                log.info("Halting.Number of Halting:{}", rebootNu);
                multicastReboot(true, Collections.singleton(localNodeId));
            } else {
                log.info("Halting is ignored as it is crossed limit of default Halting number");
            }
        } catch (Exception e) {
            log.error("Exception occured in Cluster Node Diagnosis", e);
        }
    }

    /**
     * Gets memory threshold.
     * Obtains total memory of the system where onos is running.
     * 80% of the total memory is taken as threshold.
     */
    private void getMemoryThreshold() {
        String memStr = "";
        try {
            String outputMem;
            Process getMemProc = Runtime.getRuntime().exec(CMD_FOR_TOTAL_MEMORY);
            try (BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(getMemProc.getInputStream()))) {
                while ((outputMem = bufferedReader.readLine()) != null) {
                    memStr += outputMem;
                }
            }

            memStr = memStr.replaceAll("\n", "");
            long totalMem = Long.parseLong(memStr);
            //Taking 80% of total memory as threshold
            memoryThreshold = (long) (totalMem * 0.80);
            log.trace("totalMemory {}", memoryThreshold);
        } catch (Exception e) {
            log.error("Exception occured while getting Pid", e);
        }
    }

    /**
     * Gets pid of the onos service.
     */
    private void getPid() {
        String pidStr = "";
        try {
            String outputPid;
            Process getPidProc = Runtime.getRuntime().exec(CMD_FOR_PID);
            try (BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(getPidProc.getInputStream()))) {
                while ((outputPid = bufferedReader.readLine()) != null) {
                    pidStr += outputPid;
                }
            }

            this.pid = Long.parseLong(pidStr);
            log.trace("pid {}", pid);
        } catch (Exception e) {
            log.error("Exception occured while getting Pid", e);
        }
    }

    /**
     * Restart onos if sdnc memory exceeds memory threshold.
     */
    private void sdncMemoryDiagnosis() {
        if (Objects.isNull(pid)) {
            return;
        }
        if (Objects.isNull(memoryThreshold)) {
            return;
        }
        try {
            String[] getMemCmd = {
                    "/bin/sh",
                    "-c",
                    " ps -p " + pid + " -o rss"
            };

            String outputMem;
            String outputMemFinal = "";
            Process getMemProc = Runtime.getRuntime().exec(getMemCmd);
            try (BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(getMemProc.getInputStream()))) {
                while ((outputMem = bufferedReader.readLine()) != null) {
                    outputMemFinal += outputMem;
                }
            }
            //Ex:outputFinalMem-> "     RSS1031496"
            outputMemFinal = outputMemFinal.replaceAll(EXTRA_SPACE, "");
            String memTotalStr = outputMemFinal.substring(MEMORY_START_IDX);
            if (memTotalStr.isEmpty()) {
                log.error("Total Memory is empty");
                return;
            }
            long memTotal = Long.parseLong(memTotalStr);
            memTotal *= DATA_BLOCK_1024;
            log.trace("memTotal {}", memTotal);

            if (memTotal > memoryThreshold) {
                log.info("onos Halt is needed as memory has exceeded. " +
                                "The threshold is {} and used memory is {} for node {}.",
                        memoryThreshold, memTotal, localNodeId);
                multicastReboot(true, Collections.singleton(localNodeId));
            }
        } catch (Exception e) {
            log.error("exception at Sdnc Memory Diagnosis", e);
        }

    }

    /**
     * To obtain number of tcp socket descriptors.
     */
    private static class CallableTcpexecute implements Callable<Long> {
        public Long call() throws Exception {
            String[] cmdTcpFd = {"/bin/sh", "-c",
                    "netstat -anp 2>/dev/null | grep " + pid + "/java | grep tcp | wc -l"};
            getTcpProc = Runtime.getRuntime().exec(cmdTcpFd);
            if (Objects.isNull(getTcpProc)) {
                return 0L;
            }
            String outputTcp;
            try (BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(getTcpProc.getInputStream()))) {
                outputTcp = bufferedReader.readLine();
            }
            if (Objects.isNull(outputTcp)) {
                return 0L;
            }
            return Long.parseLong(outputTcp);
        }
    }

    /**
     * To obtain number of udp socket descriptors.
     */
    private static class CallableUdpexecute implements Callable<Long> {
        public Long call() throws Exception {
            String[] cmdUdpFd = {"/bin/sh", "-c",
                    "netstat -anp 2>/dev/null | grep " + pid + "/java | grep udp | wc -l"};
            getUdpProc = Runtime.getRuntime().exec(cmdUdpFd);
            if (Objects.isNull(getUdpProc)) {
                return 0L;
            }
            String outputUdp;
            try (BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(getUdpProc.getInputStream()))) {
                outputUdp = bufferedReader.readLine();
            }
            if (Objects.isNull(outputUdp)) {
                return 0L;
            }
            return Long.parseLong(outputUdp);
        }
    }

    /**
     * Restarts onos if total number of socket descriptors exceeds threshold.
     */
    private void socketDescriptorsDiagnosis() {
        ExecutorService executorService = Executors.newCachedThreadPool();
        Future<Long> futureTcp;
        Future<Long> futureUdp;
        futureTcp = executorService.submit(new CallableTcpexecute());
        futureUdp = executorService.submit(new CallableUdpexecute());
        try {
            long tcpSds = futureTcp.get(5, TimeUnit.SECONDS);
            long udpSds = futureUdp.get(5, TimeUnit.SECONDS);

            long totalSockets = tcpSds + udpSds;
            log.trace("total {}, tcp {}, udp {}", totalSockets, tcpSds, udpSds);
            if (totalSockets > SOCKETS_THRESHOLD) {
                log.info("onos Halt is needed as socket descriptors has exceeded " +
                        "threshold limit for node {}", localNodeId);
                multicastReboot(true, Collections.singleton(localNodeId));
            }
        } catch (TimeoutException e) {
            log.error("Timeout exception at Socket Descriptors diagnosis", e);
            try {
                if (Objects.nonNull(getTcpProc)) {
                    getTcpProc.destroy();
                }
                if (Objects.nonNull(getUdpProc)) {
                    getUdpProc.destroy();
                }
            } catch (Exception ex) {
                log.error("Exception at destroying Tcp/Udp process", ex);
            }

            String outputPid;
            try {
                String pidStr = "";
                Process getPidProc = Runtime.getRuntime().exec(CMD_FOR_NETSTAT_PID);
                try (BufferedReader bufferedReader = new BufferedReader(
                        new InputStreamReader(getPidProc.getInputStream()))) {
                    while ((outputPid = bufferedReader.readLine()) != null) {
                        pidStr += outputPid;
                    }
                }
                if (!pidStr.equals("")) {
                    Runtime.getRuntime().exec("kill " + pidStr);
                }
            } catch (Exception ex) {
                log.error("Exception at killing netstat command", ex);
            }

            log.info("onos Halt is needed as timeout occured while finding total number of " +
                    "socket descriptors for node {}", localNodeId);
            multicastReboot(true, Collections.singleton(localNodeId));
        } catch (Exception e) {
            log.error("exception at Socket Descriptors diagnosis", e);
        } finally {
            futureTcp.cancel(true);
            futureUdp.cancel(true);
        }
    }

    /**
     * Restarts onos if total number of threads and file descriptors exceeds threshold.
     */
    private void threadsAndFilesDescriptorDiagnosis() {
        if (Objects.isNull(pid)) {
            return;
        }
        try {
            ProcFd procFd = sigar.getProcFd(pid);
            long totalFd = procFd.getTotal();
            log.trace("total fds{}", totalFd);
            if (totalFd > FD_THRESHOLD) {
                log.info("onos halt is needed as number of threads and file descriptors " +
                        "has exceeded Threshold limit for node {}", localNodeId);
                multicastReboot(true, Collections.singleton(localNodeId));
            }
        } catch (Exception e) {
            log.error("Exception at Sdnc file descriptor diagnosis", e);

        }

    }


    private void sdncFileDescriptorDiagnosis() {
        socketDescriptorsDiagnosis();
        threadsAndFilesDescriptorDiagnosis();
    }

    public void restartNode() {
        try {
            systemService.reboot("now", SystemService.Swipe.CACHE);
        } catch (Exception e) {
            log.error("error occured because of {} ", e.getMessage());
        }
    }

    private void multicastReboot(boolean removeDb, Set<NodeId> nodeIds) {
        String data = "Reboot:" + removeDb;
        communicationService.multicast(data, REBOOT_MSG, String::getBytes, nodeIds);
    }

    private class InternalSampleCollector implements ClusterMessageHandler {
        @Override
        public void handle(ClusterMessage message) {
            String reqMsg = new String(message.payload());
            log.info("Cluster communication message subject{} and message {}",
                    message.subject(), reqMsg);
            boolean flag = Boolean.parseBoolean(reqMsg.split(":")[1].trim());
            if (flag) {
                System.setProperty("apache.karaf.removedb", "true");
            }
            if (message.subject().equals(REBOOT_MSG)) {
                restartNode();
            }
        }
    }
}
