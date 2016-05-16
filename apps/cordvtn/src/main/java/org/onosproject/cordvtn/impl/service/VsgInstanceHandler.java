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
package org.onosproject.cordvtn.impl.service;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.cordvtn.api.Instance;
import org.onosproject.cordvtn.api.InstanceHandler;
import org.onosproject.cordvtn.impl.CordVtnInstanceHandler;
import org.onosproject.cordvtn.impl.CordVtnInstanceManager;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.HostId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.host.DefaultHostDescription;
import org.onosproject.net.host.HostDescription;
import org.onosproject.xosclient.api.VtnPort;
import org.onosproject.xosclient.api.VtnPortApi;
import org.onosproject.xosclient.api.VtnPortId;
import org.onosproject.xosclient.api.VtnService;

import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.cordvtn.api.Instance.*;
import static org.onosproject.cordvtn.impl.CordVtnPipeline.*;
import static org.onosproject.net.flow.criteria.Criterion.Type.IPV4_DST;
import static org.onosproject.net.flow.instructions.L2ModificationInstruction.L2SubType.VLAN_PUSH;

/**
 * Provides network connectivity for vSG instances.
 */
@Component(immediate = true)
@Service(value = VsgInstanceHandler.class)
public final class VsgInstanceHandler extends CordVtnInstanceHandler implements InstanceHandler {

    private static final String STAG = "stag";
    private static final String VSG_VM = "vsgVm";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CordVtnInstanceManager instanceManager;

    @Activate
    protected void activate() {
        serviceType = VtnService.ServiceType.VSG;
        eventExecutor = newSingleThreadScheduledExecutor(groupedThreads("onos/cordvtn-vsg", "event-handler"));
        super.activate();
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public void instanceDetected(Instance instance) {
        if (isVsgContainer(instance)) {
            log.info("vSG container detected {}", instance);

            // find vsg vm for this vsg container
            String vsgVmId = instance.getAnnotation(VSG_VM);
            if (Strings.isNullOrEmpty(vsgVmId)) {
                log.warn("Failed to find VSG VM for {}", instance);
                return;
            }

            Instance vsgVm = Instance.of(hostService.getHost(HostId.hostId(vsgVmId)));
            VtnPort vtnPort = getVtnPort(vsgVm);
            if (vtnPort == null || getStag(vtnPort) == null) {
                return;
            }

            populateVsgRules(vsgVm, getStag(vtnPort),
                             nodeManager.dpPort(vsgVm.deviceId()),
                             vtnPort.addressPairs().keySet(),
                             true);

        } else {
            VtnPort vtnPort = getVtnPort(instance);
            if (vtnPort == null || getStag(vtnPort) == null) {
                return;
            }

            vtnPort.addressPairs().entrySet().stream()
                    .forEach(pair -> addVsgContainer(
                            instance,
                            pair.getKey(),
                            pair.getValue(),
                            getStag(vtnPort).toString()
                    ));
            super.instanceDetected(instance);
        }
    }

    @Override
    public void instanceRemoved(Instance instance) {
        if (isVsgContainer(instance)) {
            log.info("vSG container vanished {}", instance);

            // find vsg vm for this vsg container
            String vsgVmId = instance.getAnnotation(VSG_VM);
            if (Strings.isNullOrEmpty(vsgVmId)) {
                log.warn("Failed to find VSG VM for {}", instance);
                return;
            }

            Instance vsgVm = Instance.of(hostService.getHost(HostId.hostId(vsgVmId)));
            VtnPort vtnPort = getVtnPort(vsgVm);
            if (vtnPort == null || getStag(vtnPort) == null) {
                return;
            }

            populateVsgRules(vsgVm, getStag(vtnPort),
                             nodeManager.dpPort(vsgVm.deviceId()),
                             vtnPort.addressPairs().keySet(),
                             false);

        } else {
            // TODO remove vsg vm related rules
            super.instanceRemoved(instance);
        }
    }

    /**
     * Updates set of vSGs in a given vSG VM.
     *
     * @param vsgVmId vsg vm host id
     * @param stag stag
     * @param vsgInstances full set of vsg wan ip and mac address pairs in this vsg vm
     */
    public void updateVsgInstances(HostId vsgVmId, String stag, Map<IpAddress, MacAddress> vsgInstances) {
        if (hostService.getHost(vsgVmId) == null) {
            log.debug("vSG VM {} is not added yet, ignore this update", vsgVmId);
            return;
        }

        Instance vsgVm = Instance.of(hostService.getHost(vsgVmId));
        if (vsgVm == null) {
            log.warn("Failed to find existing vSG VM for STAG: {}", stag);
            return;
        }

        log.info("Updates vSGs in {} with STAG: {}", vsgVm, stag);

        // adds vSGs in the address pair
        vsgInstances.entrySet().stream()
                .filter(addr -> hostService.getHostsByMac(addr.getValue()).isEmpty())
                .forEach(addr -> addVsgContainer(
                        vsgVm,
                        addr.getKey(),
                        addr.getValue(),
                        stag));

        // removes vSGs not listed in the address pair
        hostService.getConnectedHosts(vsgVm.host().location()).stream()
                .filter(host -> !host.mac().equals(vsgVm.mac()))
                .filter(host -> !vsgInstances.values().contains(host.mac()))
                .forEach(host -> {
                    log.info("Removed vSG {}", host.toString());
                    instanceManager.removeInstance(host.id());
                });
    }

    private boolean isVsgContainer(Instance instance) {
        return !Strings.isNullOrEmpty(instance.host().annotations().value(STAG));
    }

    private void addVsgContainer(Instance vsgVm, IpAddress vsgWanIp, MacAddress vsgMac,
                                 String stag) {
        HostId hostId = HostId.hostId(vsgMac);
        DefaultAnnotations.Builder annotations = DefaultAnnotations.builder()
                .set(SERVICE_TYPE, vsgVm.serviceType().toString())
                .set(SERVICE_ID, vsgVm.serviceId().id())
                .set(PORT_ID, vsgVm.portId().id())
                .set(NESTED_INSTANCE, TRUE)
                .set(STAG, stag)
                .set(VSG_VM, vsgVm.host().id().toString())
                .set(CREATE_TIME, String.valueOf(System.currentTimeMillis()));

        HostDescription hostDesc = new DefaultHostDescription(
                vsgMac,
                VlanId.NONE,
                vsgVm.host().location(),
                Sets.newHashSet(vsgWanIp),
                annotations.build());

        instanceManager.addInstance(hostId, hostDesc);
    }

    private void populateVsgRules(Instance vsgVm, VlanId stag, PortNumber dpPort,
                                  Set<IpAddress> vsgWanIps, boolean install) {
        // for traffics with s-tag, strip the tag and take through the vSG VM
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchInPort(dpPort)
                .matchVlanId(stag)
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(vsgVm.portNumber())
                .build();

        FlowRule flowRule = DefaultFlowRule.builder()
                .fromApp(appId)
                .withSelector(selector)
                .withTreatment(treatment)
                .withPriority(PRIORITY_DEFAULT)
                .forDevice(vsgVm.deviceId())
                .forTable(TABLE_VLAN)
                .makePermanent()
                .build();

        pipeline.processFlowRule(install, flowRule);

        // for traffics with customer vlan, tag with the service vlan based on input port with
        // lower priority to avoid conflict with WAN tag
        selector = DefaultTrafficSelector.builder()
                .matchInPort(vsgVm.portNumber())
                .matchVlanId(stag)
                .build();

        treatment = DefaultTrafficTreatment.builder()
                .setOutput(dpPort)
                .build();

        flowRule = DefaultFlowRule.builder()
                .fromApp(appId)
                .withSelector(selector)
                .withTreatment(treatment)
                .withPriority(PRIORITY_DEFAULT)
                .forDevice(vsgVm.deviceId())
                .forTable(TABLE_VLAN)
                .makePermanent()
                .build();

        pipeline.processFlowRule(install, flowRule);

        // for traffic coming from WAN, tag 500 and take through the vSG VM
        // based on destination ip
        vsgWanIps.stream().forEach(ip -> {
            TrafficSelector downstream = DefaultTrafficSelector.builder()
                    .matchEthType(Ethernet.TYPE_IPV4)
                    .matchIPDst(ip.toIpPrefix())
                    .build();

            TrafficTreatment downstreamTreatment = DefaultTrafficTreatment.builder()
                    .pushVlan()
                    .setVlanId(VLAN_WAN)
                    .setEthDst(vsgVm.mac())
                    .setOutput(vsgVm.portNumber())
                    .build();

            FlowRule downstreamFlowRule = DefaultFlowRule.builder()
                    .fromApp(appId)
                    .withSelector(downstream)
                    .withTreatment(downstreamTreatment)
                    .withPriority(PRIORITY_DEFAULT)
                    .forDevice(vsgVm.deviceId())
                    .forTable(TABLE_DST_IP)
                    .makePermanent()
                    .build();

            pipeline.processFlowRule(install, downstreamFlowRule);
        });

        // remove downstream flow rules for the vSG not shown in vsgWanIps
        for (FlowRule rule : flowRuleService.getFlowRulesById(appId)) {
            if (!rule.deviceId().equals(vsgVm.deviceId())) {
                continue;
            }
            PortNumber output = getOutputFromTreatment(rule);
            if (output == null || !output.equals(vsgVm.portNumber()) ||
                    !isVlanPushFromTreatment(rule)) {
                continue;
            }

            IpPrefix dstIp = getDstIpFromSelector(rule);
            if (dstIp != null && !vsgWanIps.contains(dstIp.address())) {
                pipeline.processFlowRule(false, rule);
            }
        }
    }

    private VtnPort getVtnPort(Instance instance) {
        checkNotNull(osAccess, OPENSTACK_ACCESS_ERROR);
        checkNotNull(xosAccess, XOS_ACCESS_ERROR);

        VtnPortId vtnPortId = instance.portId();
        VtnPortApi portApi = xosClient.getClient(xosAccess).vtnPort();
        VtnPort vtnPort = portApi.vtnPort(vtnPortId, osAccess);
        if (vtnPort == null) {
            log.warn("Failed to get port information of {}", instance);
            return null;
        }
        return vtnPort;
    }

    // TODO get stag from XOS when XOS provides it, extract if from port name for now
    private VlanId getStag(VtnPort vtnPort) {
        checkNotNull(vtnPort);

        String portName = vtnPort.name();
        if (portName != null && portName.startsWith(STAG)) {
            return VlanId.vlanId(portName.split("-")[1]);
        } else {
            return null;
        }
    }

    private PortNumber getOutputFromTreatment(FlowRule flowRule) {
        Instruction instruction = flowRule.treatment().allInstructions().stream()
                .filter(inst -> inst instanceof Instructions.OutputInstruction)
                .findFirst()
                .orElse(null);
        if (instruction == null) {
            return null;
        }
        return ((Instructions.OutputInstruction) instruction).port();
    }

    private IpPrefix getDstIpFromSelector(FlowRule flowRule) {
        Criterion criterion = flowRule.selector().getCriterion(IPV4_DST);
        if (criterion != null && criterion instanceof IPCriterion) {
            IPCriterion ip = (IPCriterion) criterion;
            return ip.ip();
        } else {
            return null;
        }
    }

    private boolean isVlanPushFromTreatment(FlowRule flowRule) {
        Instruction instruction = flowRule.treatment().allInstructions().stream()
                .filter(inst -> inst instanceof L2ModificationInstruction)
                .filter(inst -> ((L2ModificationInstruction) inst).subtype().equals(VLAN_PUSH))
                .findAny()
                .orElse(null);
        return instruction != null;
    }
}
