/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.kubevirtnetworking.impl;

import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.kubevirtnetworking.api.KubevirtFlowRuleService;
import org.onosproject.kubevirtnode.api.KubevirtNode;
import org.onosproject.kubevirtnode.api.KubevirtNodeEvent;
import org.onosproject.kubevirtnode.api.KubevirtNodeListener;
import org.onosproject.kubevirtnode.api.KubevirtNodeService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.kubevirtnetworking.api.Constants.ACL_INGRESS_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.ARP_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.KUBEVIRT_NETWORKING_APP_ID;
import static org.onosproject.kubevirtnetworking.api.Constants.PRIORITY_FORWARDING_RULE;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.structurePortName;
import static org.onosproject.kubevirtnode.api.Constants.INTEGRATION_TO_PHYSICAL_PREFIX;
import static org.onosproject.net.AnnotationKeys.PORT_NAME;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Populates switching flow rules on OVS for the provider network (underlay).
 */
@Component(immediate = true)
public class KubevirtSwitchingPhysicalHandler {
    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MastershipService mastershipService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DriverService driverService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LeadershipService leadershipService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtFlowRuleService flowRuleService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtNodeService kubevirtNodeService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler"));
    private final InternalDeviceListener internalDeviceListener = new InternalDeviceListener();
    private final InternalKubevirtNodeListener kubevirtNodeListener = new InternalKubevirtNodeListener();
    private ApplicationId appId;
    private NodeId localNodeId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(KUBEVIRT_NETWORKING_APP_ID);
        localNodeId = clusterService.getLocalNode().id();
        deviceService.addListener(internalDeviceListener);
        kubevirtNodeService.addListener(kubevirtNodeListener);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        eventExecutor.shutdown();
        deviceService.removeListener(internalDeviceListener);
        kubevirtNodeService.removeListener(kubevirtNodeListener);
        log.info("Stopped");
    }

    private boolean containsPhyPatchPort(KubevirtNode node, Port port) {
        Set<String> intPatchPorts = node.phyIntfs().stream()
                .map(pi -> structurePortName(INTEGRATION_TO_PHYSICAL_PREFIX
                        + pi.network())).collect(Collectors.toSet());
        String portName = port.annotations().value(PORT_NAME);
        return intPatchPorts.contains(portName);
    }

    private void setIngressRuleForPatchPort(DeviceId deviceId,
                                            PortNumber portNumber,
                                            boolean install) {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder()
                .matchInPort(portNumber);

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
                .transition(ACL_INGRESS_TABLE);

        flowRuleService.setRule(
                appId,
                deviceId,
                selector.build(),
                treatment.build(),
                PRIORITY_FORWARDING_RULE,
                ARP_TABLE,
                install);
    }

    private class InternalDeviceListener implements DeviceListener {

        @Override
        public boolean isRelevant(DeviceEvent event) {
            Port port = event.port();
            if (port == null) {
                return false;
            }

            KubevirtNode node = kubevirtNodeService.node(event.subject().id());
            if (node == null) {
                return false;
            }

            return containsPhyPatchPort(node, port);
        }

        private boolean isRelevantHelper(DeviceEvent event) {
            return mastershipService.isLocalMaster(event.subject().id());
        }

        @Override
        public void event(DeviceEvent event) {
            log.info("Device event occurred with type {}", event.type());

            switch (event.type()) {
                case PORT_ADDED:
                case PORT_UPDATED:
                    eventExecutor.execute(() -> processPortAddition(event));
                    break;
                case PORT_REMOVED:
                    eventExecutor.execute(() -> processPortRemoval(event));
                    break;
                default:
                    break;
            }
        }

        private void processPortAddition(DeviceEvent event) {
            if (!isRelevantHelper(event)) {
                return;
            }
            setIngressRuleForPatchPort(event.subject().id(),
                    event.port().number(), true);
        }
        private void processPortRemoval(DeviceEvent event) {
            if (!isRelevantHelper(event)) {
                return;
            }
            setIngressRuleForPatchPort(event.subject().id(),
                    event.port().number(), false);
        }
    }

    private class InternalKubevirtNodeListener implements KubevirtNodeListener {
        private boolean isRelevantHelper() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(KubevirtNodeEvent event) {
            switch (event.type()) {
                case KUBEVIRT_NODE_COMPLETE:
                    eventExecutor.execute(() -> processNodeCompletion(event.subject()));
                    break;
                case KUBEVIRT_NODE_INCOMPLETE:
                default:
                    // do nothing
                    break;
            }
        }

        private void processNodeCompletion(KubevirtNode node) {
            if (!isRelevantHelper()) {
                return;
            }

            deviceService.getPorts(node.intgBridge()).forEach(p -> {
                if (containsPhyPatchPort(node, p)) {
                    setIngressRuleForPatchPort(node.intgBridge(), p.number(), true);
                }
            });
        }
    }
}
