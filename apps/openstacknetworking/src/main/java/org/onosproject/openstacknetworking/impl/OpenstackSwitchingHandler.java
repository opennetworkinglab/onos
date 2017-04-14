/*
* Copyright 2016-present Open Networking Laboratory
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

import com.google.common.base.Strings;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.Ethernet;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.openstacknetworking.api.InstancePort;
import org.onosproject.openstacknetworking.api.InstancePortEvent;
import org.onosproject.openstacknetworking.api.InstancePortListener;
import org.onosproject.openstacknetworking.api.InstancePortService;
import org.onosproject.openstacknetworking.api.OpenstackNetworkService;
import org.onosproject.openstacknode.OpenstackNodeService;
import org.openstack4j.model.network.Network;
import org.slf4j.Logger;

import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.openstacknetworking.api.Constants.*;
import static org.onosproject.openstacknetworking.impl.RulePopulatorUtil.buildExtension;
import static org.onosproject.openstacknode.OpenstackNodeService.NodeType.COMPUTE;
import static org.slf4j.LoggerFactory.getLogger;


/**
 * Populates switching flow rules on OVS for the basic connectivity among the
 * virtual instances in the same network.
 */
@Component(immediate = true)
public final class OpenstackSwitchingHandler {

    private final Logger log = getLogger(getClass());

    private static final String ERR_SET_FLOWS = "Failed to set flows for %s: ";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowObjectiveService flowObjectiveService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected InstancePortService instancePortService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackNetworkService osNetworkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackNodeService osNodeService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler"));
    private final InstancePortListener instancePortListener = new InternalInstancePortListener();
    private ApplicationId appId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(OPENSTACK_NETWORKING_APP_ID);
        instancePortService.addListener(instancePortListener);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        instancePortService.removeListener(instancePortListener);
        eventExecutor.shutdown();

        log.info("Stopped");
    }

    private void setNetworkRules(InstancePort instPort, boolean install) {
        switch (osNetworkService.network(instPort.networkId()).getNetworkType()) {
            case VXLAN:
                setTunnelTagFlowRules(instPort, install);
                setForwardingRules(instPort, install);
                break;
            case VLAN:
                setVlanTagFlowRules(instPort, install);
                setForwardingRulesForVlan(instPort, install);
                break;
            default:
                break;
        }
    }

    private void setForwardingRules(InstancePort instPort, boolean install) {
        // switching rules for the instPorts in the same node
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(instPort.ipAddress().toIpPrefix())
                .matchTunnelId(getVni(instPort))
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setEthDst(instPort.macAddress())
                .setOutput(instPort.portNumber())
                .build();

        RulePopulatorUtil.setRule(
                flowObjectiveService,
                appId,
                instPort.deviceId(),
                selector,
                treatment,
                ForwardingObjective.Flag.SPECIFIC,
                PRIORITY_SWITCHING_RULE,
                install);

        // switching rules for the instPorts in the remote node
        osNodeService.completeNodes().stream()
                .filter(osNode -> osNode.type() == COMPUTE)
                .filter(osNode -> !osNode.intBridge().equals(instPort.deviceId()))
                .forEach(osNode -> {
                    TrafficTreatment treatmentToRemote = DefaultTrafficTreatment.builder()
                            .extension(buildExtension(
                                    deviceService,
                                    osNode.intBridge(),
                                    osNodeService.dataIp(instPort.deviceId()).get().getIp4Address()),
                                    osNode.intBridge())
                            .setOutput(osNodeService.tunnelPort(osNode.intBridge()).get())
                            .build();

                    RulePopulatorUtil.setRule(
                            flowObjectiveService,
                            appId,
                            osNode.intBridge(),
                            selector,
                            treatmentToRemote,
                            ForwardingObjective.Flag.SPECIFIC,
                            PRIORITY_SWITCHING_RULE,
                            install);
                });
    }

    private void setForwardingRulesForVlan(InstancePort instPort, boolean install) {
        // switching rules for the instPorts in the same node
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(instPort.ipAddress().toIpPrefix())
                .matchVlanId(getVlanId(instPort))
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .popVlan()
                .setEthDst(instPort.macAddress())
                .setOutput(instPort.portNumber())
                .build();

        RulePopulatorUtil.setRule(
                flowObjectiveService,
                appId,
                instPort.deviceId(),
                selector,
                treatment,
                ForwardingObjective.Flag.SPECIFIC,
                PRIORITY_SWITCHING_RULE,
                install);

        // switching rules for the instPorts in the remote node
        osNodeService.completeNodes().stream()
                .filter(osNode -> osNode.type() == COMPUTE)
                .filter(osNode -> !osNode.intBridge().equals(instPort.deviceId()))
                .filter(osNode -> osNode.vlanPort().isPresent())
                .forEach(osNode -> {
                    TrafficTreatment treatmentToRemote = DefaultTrafficTreatment.builder()
                                    .setOutput(osNodeService.vlanPort(osNode.intBridge()).get())
                                    .build();

                    RulePopulatorUtil.setRule(
                            flowObjectiveService,
                            appId,
                            osNode.intBridge(),
                            selector,
                            treatmentToRemote,
                            ForwardingObjective.Flag.SPECIFIC,
                            PRIORITY_SWITCHING_RULE,
                            install);
                });

    }

    private void setTunnelTagFlowRules(InstancePort instPort, boolean install) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchInPort(instPort.portNumber())
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setTunnelId(getVni(instPort))
                .build();

        RulePopulatorUtil.setRule(
                flowObjectiveService,
                appId,
                instPort.deviceId(),
                selector,
                treatment,
                ForwardingObjective.Flag.SPECIFIC,
                PRIORITY_TUNNEL_TAG_RULE,
                install);
    }

    private void setVlanTagFlowRules(InstancePort instPort, boolean install) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchInPort(instPort.portNumber())
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .pushVlan()
                .setVlanId(getVlanId(instPort))
                .build();

        RulePopulatorUtil.setRule(
                flowObjectiveService,
                appId,
                instPort.deviceId(),
                selector,
                treatment,
                ForwardingObjective.Flag.SPECIFIC,
                PRIORITY_TUNNEL_TAG_RULE,
                install);

    }

    private VlanId getVlanId(InstancePort instPort) {
        Network osNet = osNetworkService.network(instPort.networkId());

        if (osNet == null || Strings.isNullOrEmpty(osNet.getProviderSegID())) {
            final String error = String.format(
                    ERR_SET_FLOWS + "Failed to get VNI for %s",
                    instPort, osNet == null ? "<none>" : osNet.getName());
            throw new IllegalStateException(error);
        }

        return VlanId.vlanId(osNet.getProviderSegID());
    }


    private Long getVni(InstancePort instPort) {
        Network osNet = osNetworkService.network(instPort.networkId());
        if (osNet == null || Strings.isNullOrEmpty(osNet.getProviderSegID())) {
            final String error = String.format(
                    ERR_SET_FLOWS + "Failed to get VNI for %s",
                    instPort, osNet == null ? "<none>" : osNet.getName());
            throw new IllegalStateException(error);
        }
        return Long.valueOf(osNet.getProviderSegID());
    }

    private class InternalInstancePortListener implements InstancePortListener {

        @Override
        public boolean isRelevant(InstancePortEvent event) {
            InstancePort instPort = event.subject();
            return mastershipService.isLocalMaster(instPort.deviceId());
        }

        @Override
        public void event(InstancePortEvent event) {
            InstancePort instPort = event.subject();
            switch (event.type()) {
                case OPENSTACK_INSTANCE_PORT_UPDATED:
                case OPENSTACK_INSTANCE_PORT_DETECTED:
                    eventExecutor.execute(() -> {
                        log.info("Instance port detected MAC:{} IP:{}",
                                instPort.macAddress(),
                                instPort.ipAddress());
                        instPortDetected(event.subject());
                    });
                    break;
                case OPENSTACK_INSTANCE_PORT_VANISHED:
                    eventExecutor.execute(() -> {
                        log.info("Instance port vanished MAC:{} IP:{}",
                                instPort.macAddress(),
                                instPort.ipAddress());
                        instPortRemoved(event.subject());
                    });
                    break;
                default:
                    break;
            }
        }

        private void instPortDetected(InstancePort instPort) {
            setNetworkRules(instPort, true);
            // TODO add something else if needed
        }

        private void instPortRemoved(InstancePort instPort) {
            setNetworkRules(instPort, false);
            // TODO add something else if needed
        }
    }
}
