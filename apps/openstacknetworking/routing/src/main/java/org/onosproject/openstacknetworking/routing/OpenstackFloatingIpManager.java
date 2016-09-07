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
package org.onosproject.openstacknetworking.routing;

import com.google.common.base.Strings;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpAddress;
import org.onlab.util.KryoNamespace;
import org.onlab.util.Tools;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.host.HostService;
import org.onosproject.openstackinterface.OpenstackFloatingIP;
import org.onosproject.openstackinterface.OpenstackInterfaceService;
import org.onosproject.openstacknetworking.AbstractVmHandler;
import org.onosproject.openstacknetworking.Constants;
import org.onosproject.openstacknetworking.OpenstackFloatingIpService;
import org.onosproject.openstacknetworking.RulePopulatorUtil;
import org.onosproject.openstacknode.OpenstackNode;
import org.onosproject.openstacknode.OpenstackNodeEvent;
import org.onosproject.openstacknode.OpenstackNodeListener;
import org.onosproject.openstacknode.OpenstackNodeService;
import org.onosproject.scalablegateway.api.GatewayNode;
import org.onosproject.scalablegateway.api.ScalableGatewayService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.openstacknetworking.Constants.*;
import static org.onosproject.openstacknetworking.RulePopulatorUtil.buildExtension;
import static org.onosproject.openstacknode.OpenstackNodeService.NodeType.GATEWAY;


@Service
@Component(immediate = true)
public class OpenstackFloatingIpManager extends AbstractVmHandler implements OpenstackFloatingIpService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowObjectiveService flowObjectiveService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackNodeService nodeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ScalableGatewayService gatewayService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackInterfaceService openstackService;

    private static final String NOT_ASSOCIATED = "null";
    private static final KryoNamespace.Builder FLOATING_IP_SERIALIZER =
            KryoNamespace.newBuilder().register(KryoNamespaces.API);

    private final ExecutorService eventExecutor = newSingleThreadScheduledExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));
    private final InternalNodeListener nodeListener = new InternalNodeListener();
    private ConsistentMap<IpAddress, Host> floatingIpMap;

    private ApplicationId appId;

    @Activate
    protected void activate() {
        super.activate();
        appId = coreService.registerApplication(ROUTING_APP_ID);
        nodeService.addListener(nodeListener);
        floatingIpMap = storageService.<IpAddress, Host>consistentMapBuilder()
                .withSerializer(Serializer.using(FLOATING_IP_SERIALIZER.build()))
                .withName("openstackrouting-floatingip")
                .withApplicationId(appId)
                .build();

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
        nodeService.removeListener(nodeListener);
        log.info("Stopped");
    }

    @Override
    protected void hostDetected(Host host) {
        IpAddress hostIp = host.ipAddresses().stream().findFirst().get();
        Optional<OpenstackFloatingIP> floatingIp = openstackService.floatingIps().stream()
                .filter(fip -> fip.fixedIpAddress() != null && fip.fixedIpAddress().equals(hostIp))
                .findFirst();
        if (floatingIp.isPresent()) {
            eventExecutor.execute(() -> associateFloatingIp(floatingIp.get()));
        }
    }

    @Override
    protected void hostRemoved(Host host) {
        IpAddress hostIp = host.ipAddresses().stream().findFirst().get();
        Optional<OpenstackFloatingIP> floatingIp = openstackService.floatingIps().stream()
                .filter(fip -> fip.fixedIpAddress() != null && fip.fixedIpAddress().equals(hostIp))
                .findFirst();
        if (floatingIp.isPresent()) {
            eventExecutor.execute(() -> disassociateFloatingIp(floatingIp.get()));
        }
    }

    @Override
    public void createFloatingIp(OpenstackFloatingIP floatingIp) {
    }

    @Override
    public void updateFloatingIp(OpenstackFloatingIP floatingIp) {
        if (Strings.isNullOrEmpty(floatingIp.portId()) ||
                floatingIp.portId().equals(NOT_ASSOCIATED)) {
            eventExecutor.execute(() -> disassociateFloatingIp(floatingIp));
        } else {
            eventExecutor.execute(() -> associateFloatingIp(floatingIp));
        }
    }

    @Override
    public void deleteFloatingIp(String floatingIpId) {
    }

    private void associateFloatingIp(OpenstackFloatingIP floatingIp) {
        Optional<Host> associatedVm = Tools.stream(hostService.getHosts())
                .filter(host -> Objects.equals(
                        host.annotations().value(PORT_ID),
                        floatingIp.portId()))
                .findAny();
        if (!associatedVm.isPresent()) {
            log.warn("Failed to associate floating IP({}) to port:{}",
                     floatingIp.floatingIpAddress(),
                     floatingIp.portId());
            return;
        }

        floatingIpMap.put(floatingIp.floatingIpAddress(), associatedVm.get());
        populateFloatingIpRules(floatingIp.floatingIpAddress(), associatedVm.get());

        log.info("Associated floating IP {} to fixed IP {}",
                 floatingIp.floatingIpAddress(), floatingIp.fixedIpAddress());
    }

    private void disassociateFloatingIp(OpenstackFloatingIP floatingIp) {
        Versioned<Host> associatedVm = floatingIpMap.remove(floatingIp.floatingIpAddress());
        if (associatedVm == null) {
            log.warn("Failed to disassociate floating IP({})",
                     floatingIp.floatingIpAddress());
            // No VM is actually associated with the floating IP, do nothing
            return;
        }

        removeFloatingIpRules(floatingIp.floatingIpAddress(), associatedVm.value());
        log.info("Disassociated floating IP {} from fixed IP {}",
                 floatingIp.floatingIpAddress(),
                 associatedVm.value().ipAddresses());
    }

    private void populateFloatingIpRules(IpAddress floatingIp, Host associatedVm) {
        populateFloatingIpIncomingRules(floatingIp, associatedVm);
        populateFloatingIpOutgoingRules(floatingIp, associatedVm);
    }

    private void removeFloatingIpRules(IpAddress floatingIp, Host associatedVm) {
        Optional<IpAddress> fixedIp = associatedVm.ipAddresses().stream().findFirst();
        if (!fixedIp.isPresent()) {
            log.warn("Failed to remove floating IP({}) from {}",
                     floatingIp, associatedVm);
            return;
        }

        TrafficSelector.Builder sOutgoingBuilder = DefaultTrafficSelector.builder();
        TrafficSelector.Builder sIncomingBuilder = DefaultTrafficSelector.builder();

        sOutgoingBuilder.matchEthType(Ethernet.TYPE_IPV4)
                .matchTunnelId(Long.valueOf(associatedVm.annotations().value(VXLAN_ID)))
                .matchIPSrc(fixedIp.get().toIpPrefix());

        sIncomingBuilder.matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(floatingIp.toIpPrefix());

        gatewayService.getGatewayDeviceIds().stream().forEach(deviceId -> {
            TrafficSelector.Builder sForTrafficFromVmBuilder = DefaultTrafficSelector.builder()
                    .matchEthType(Ethernet.TYPE_IPV4)
                    .matchIPDst(floatingIp.toIpPrefix())
                    .matchInPort(nodeService.tunnelPort(deviceId).get());

            RulePopulatorUtil.removeRule(
                    flowObjectiveService,
                    appId,
                    deviceId,
                    sOutgoingBuilder.build(),
                    ForwardingObjective.Flag.VERSATILE,
                    FLOATING_RULE_PRIORITY);

            RulePopulatorUtil.removeRule(
                    flowObjectiveService,
                    appId,
                    deviceId,
                    sIncomingBuilder.build(),
                    ForwardingObjective.Flag.VERSATILE,
                    FLOATING_RULE_PRIORITY);

            RulePopulatorUtil.removeRule(
                    flowObjectiveService,
                    appId,
                    deviceId,
                    sForTrafficFromVmBuilder.build(),
                    ForwardingObjective.Flag.VERSATILE,
                    FLOATING_RULE_FOR_TRAFFIC_FROM_VM_PRIORITY);
        });
    }

    private void populateFloatingIpIncomingRules(IpAddress floatingIp, Host associatedVm) {
        DeviceId cnodeId = associatedVm.location().deviceId();
        Optional<IpAddress> dataIp = nodeService.dataIp(cnodeId);
        Optional<IpAddress> fixedIp = associatedVm.ipAddresses().stream().findFirst();

        if (!fixedIp.isPresent() || !dataIp.isPresent()) {
            log.warn("Failed to associate floating IP({})", floatingIp);
            return;
        }

        TrafficSelector selectorForTrafficFromExternal = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(floatingIp.toIpPrefix())
                .build();

        gatewayService.getGatewayDeviceIds().stream().forEach(gnodeId -> {
            TrafficTreatment treatmentForTrafficFromExternal =  DefaultTrafficTreatment.builder()
                    .setEthSrc(Constants.DEFAULT_GATEWAY_MAC)
                    .setEthDst(associatedVm.mac())
                    .setIpDst(associatedVm.ipAddresses().stream().findFirst().get())
                    .setTunnelId(Long.valueOf(associatedVm.annotations().value(VXLAN_ID)))
                    .extension(buildExtension(deviceService, gnodeId, dataIp.get().getIp4Address()),
                               gnodeId)
                    .setOutput(nodeService.tunnelPort(gnodeId).get())
                    .build();

            ForwardingObjective forwardingObjectiveForTrafficFromExternal = DefaultForwardingObjective.builder()
                    .withSelector(selectorForTrafficFromExternal)
                    .withTreatment(treatmentForTrafficFromExternal)
                    .withFlag(ForwardingObjective.Flag.VERSATILE)
                    .withPriority(FLOATING_RULE_PRIORITY)
                    .fromApp(appId)
                    .add();

            flowObjectiveService.forward(gnodeId, forwardingObjectiveForTrafficFromExternal);


            TrafficSelector selectorForTrafficFromVm = DefaultTrafficSelector.builder()
                    .matchEthType(Ethernet.TYPE_IPV4)
                    .matchIPDst(floatingIp.toIpPrefix())
                    .matchInPort(nodeService.tunnelPort(gnodeId).get())
                    .build();

            TrafficTreatment treatmentForTrafficFromVm = DefaultTrafficTreatment.builder()
                    .setEthSrc(Constants.DEFAULT_GATEWAY_MAC)
                    .setEthDst(associatedVm.mac())
                    .setIpDst(associatedVm.ipAddresses().stream().findFirst().get())
                    .setTunnelId(Long.valueOf(associatedVm.annotations().value(VXLAN_ID)))
                    .extension(buildExtension(deviceService, gnodeId, dataIp.get().getIp4Address()),
                            gnodeId)
                    .setOutput(PortNumber.IN_PORT)
                    .build();

            ForwardingObjective forwardingObjectiveForTrafficFromVm = DefaultForwardingObjective.builder()
                    .withSelector(selectorForTrafficFromVm)
                    .withTreatment(treatmentForTrafficFromVm)
                    .withFlag(ForwardingObjective.Flag.VERSATILE)
                    .withPriority(FLOATING_RULE_FOR_TRAFFIC_FROM_VM_PRIORITY)
                    .fromApp(appId)
                    .add();

            flowObjectiveService.forward(gnodeId, forwardingObjectiveForTrafficFromVm);

        });
    }

    private void populateFloatingIpOutgoingRules(IpAddress floatingIp, Host associatedVm) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchTunnelId(Long.valueOf(associatedVm.annotations().value(VXLAN_ID)))
                .matchIPSrc(associatedVm.ipAddresses().stream().findFirst().get().toIpPrefix())
                .build();

        gatewayService.getGatewayDeviceIds().stream().forEach(gnodeId -> {
            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .setIpSrc(floatingIp)
                    .setEthSrc(Constants.DEFAULT_GATEWAY_MAC)
                    .setEthDst(Constants.DEFAULT_EXTERNAL_ROUTER_MAC)
                    .setOutput(gatewayService.getUplinkPort(gnodeId))
                    .build();

            ForwardingObjective fo = DefaultForwardingObjective.builder()
                    .withSelector(selector)
                    .withTreatment(treatment)
                    .withFlag(ForwardingObjective.Flag.VERSATILE)
                    .withPriority(FLOATING_RULE_PRIORITY)
                    .fromApp(appId)
                    .add();

            flowObjectiveService.forward(gnodeId, fo);
        });
    }

    // TODO consider the case that port with associated floating IP is attached to a VM

    private class InternalNodeListener implements OpenstackNodeListener {

        @Override
        public void event(OpenstackNodeEvent event) {
            OpenstackNode node = event.node();

            switch (event.type()) {
                case COMPLETE:
                    if (node.type() == GATEWAY) {
                        log.info("GATEWAY node {} detected", node.hostname());
                        eventExecutor.execute(() -> {
                            GatewayNode gnode = GatewayNode.builder()
                                    .gatewayDeviceId(node.intBridge())
                                    .dataIpAddress(node.dataIp().getIp4Address())
                                    .uplinkIntf(node.externalPortName().get())
                                    .build();
                            gatewayService.addGatewayNode(gnode);
                        });
                    }
                    break;
                case INIT:
                case DEVICE_CREATED:
                case INCOMPLETE:
                default:
                    break;
            }
        }
    }
}
