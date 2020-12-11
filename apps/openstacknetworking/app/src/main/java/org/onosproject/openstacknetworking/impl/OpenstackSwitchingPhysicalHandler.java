/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.openstacknetworking.impl;

import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
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
import org.onosproject.openstacknetworking.api.OpenstackFlowRuleService;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.AnnotationKeys.PORT_NAME;
import static org.onosproject.openstacknetworking.api.Constants.OPENSTACK_NETWORKING_APP_ID;
import static org.onosproject.openstacknetworking.api.Constants.PRE_FLAT_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_SWITCHING_RULE;
import static org.onosproject.openstacknetworking.api.Constants.VTAG_TABLE;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.structurePortName;
import static org.onosproject.openstacknode.api.Constants.INTEGRATION_TO_PHYSICAL_PREFIX;
import static org.slf4j.LoggerFactory.getLogger;
/**
 * Populates switching flow rules on OVS for the physical interfaces.
 */
@Component(immediate = true)
public class OpenstackSwitchingPhysicalHandler {
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
    protected OpenstackFlowRuleService osFlowRuleService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackNodeService osNodeService;
    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler"));
    private final InternalDeviceListener internalDeviceListener = new InternalDeviceListener();
    private ApplicationId appId;
    @Activate
    protected void activate() {
        appId = coreService.registerApplication(OPENSTACK_NETWORKING_APP_ID);
        deviceService.addListener(internalDeviceListener);
        log.info("Started");
    }
    @Deactivate
    protected void deactivate() {
        eventExecutor.shutdown();
        deviceService.removeListener(internalDeviceListener);
        log.info("Stopped");
    }
    /**
     * An internal device listener which listens the port events generated from
     * OVS integration bridge.
     */
    private class InternalDeviceListener implements DeviceListener {
        @Override
        public boolean isRelevant(DeviceEvent event) {
            Port port = event.port();
            if (port == null) {
                return false;
            }
            OpenstackNode osNode = osNodeService.node(event.subject().id());
            if (osNode == null) {
                return false;
            }
            Set<String> intPatchPorts = osNode.phyIntfs().stream()
                    .map(pi -> structurePortName(INTEGRATION_TO_PHYSICAL_PREFIX
                            + pi.network())).collect(Collectors.toSet());
            String portName = port.annotations().value(PORT_NAME);
            return intPatchPorts.contains(portName);
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
            setFlatJumpRuleForPatchPort(event.subject().id(),
                    event.port().number(), true);
        }
        private void processPortRemoval(DeviceEvent event) {
            if (!isRelevantHelper(event)) {
                return;
            }
            setFlatJumpRuleForPatchPort(event.subject().id(),
                    event.port().number(), false);
        }
        private void setFlatJumpRuleForPatchPort(DeviceId deviceId,
                                                 PortNumber portNumber,
                                                 boolean install) {
            TrafficSelector.Builder selector = DefaultTrafficSelector.builder()
                    .matchInPort(portNumber);

            TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
                    .transition(PRE_FLAT_TABLE);

            osFlowRuleService.setRule(
                    appId,
                    deviceId,
                    selector.build(),
                    treatment.build(),
                    PRIORITY_SWITCHING_RULE,
                    VTAG_TABLE,
                    install);
        }
    }
}
