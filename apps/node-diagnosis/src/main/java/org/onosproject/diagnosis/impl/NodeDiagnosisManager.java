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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.SharedExecutors.getPoolThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;

import static org.onlab.util.Tools.getIntegerProperty;
import static org.onosproject.diagnosis.impl.OsgiPropertyConstants.INITIAL_POLL_DELAY_MINUTE;
import static org.onosproject.diagnosis.impl.OsgiPropertyConstants.POLL_FREQUENCY_MINUTE;
import static org.onosproject.diagnosis.impl.OsgiPropertyConstants.DEFAULT_INITIAL_POLL_DELAY_MINUTE;
import static org.onosproject.diagnosis.impl.OsgiPropertyConstants.DEFAULT_POLL_FREQUENCY_MINUTE;


@Component(immediate = true,
        property = {
                INITIAL_POLL_DELAY_MINUTE + ":Integer=" + DEFAULT_INITIAL_POLL_DELAY_MINUTE,
                POLL_FREQUENCY_MINUTE + ":Integer=" + DEFAULT_POLL_FREQUENCY_MINUTE,
        })
public class NodeDiagnosisManager {

    private static final MessageSubject REBOOT_MSG = new MessageSubject("Node-diagnosis");

    private static int initialPollDelayMinute = DEFAULT_INITIAL_POLL_DELAY_MINUTE;
    private static int pollFrequencyMinute = DEFAULT_POLL_FREQUENCY_MINUTE;

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
    private Set<NodeId> nodeIds;

    private static final long TIMEOUT = 3000;

    @Activate
    public void activate() {
        cfgService.registerProperties(getClass());
        bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
        getNodeId();
        scheduleAppDiagnosisPolling();
        scheduleClusterNodeDiagnosisPolling();
        communicationService.addSubscriber(REBOOT_MSG, new InternalSampleCollector(),
                                           getPoolThreadExecutor());
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

    private List<Bundle> getAllBundles() {
        return Arrays.asList(bundleContext.getBundles());
    }

    private void getNodeId() {
        caService = DefaultServiceDirectory.getService(ClusterAdminService.class);
        if (Objects.isNull(caService)) {
            return;
        }
        List<ControllerNode> controllerNodes = newArrayList(caService.getNodes());
        nodeIds = controllerNodes
                .stream()
                .map(ControllerNode::id)
                .collect(Collectors.toSet());

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

    private void clusterNodeDiagnosis() {
        if (Objects.isNull(caService)) {
            return;
        }

        List<ControllerNode> nodes = newArrayList(caService.getNodes());
        Set<NodeId> activeNodes = nodes
                .stream()
                .filter(node -> caService.getState(node.id()) == ControllerNode.State.ACTIVE)
                .filter(node -> caService.getLastUpdatedInstant(node.id()).until(Instant.now(), ChronoUnit.MINUTES) > 4)
                .map(ControllerNode::id)
                .collect(Collectors.toSet());
        boolean isNodesActive = nodes
                .stream().filter(node -> !(caService.getState(node.id()) == ControllerNode.State.INACTIVE))
                .allMatch(node -> caService.getState(node.id()) == ControllerNode.State.ACTIVE);
        if (Objects.nonNull(activeNodes) && !activeNodes.isEmpty()) {
            multicastReboot(isNodesActive, activeNodes);
        }
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
