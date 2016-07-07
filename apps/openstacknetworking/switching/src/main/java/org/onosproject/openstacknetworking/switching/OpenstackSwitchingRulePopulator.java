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

package org.onosproject.openstacknetworking.switching;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.Ethernet;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.ExtensionTreatmentResolver;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionPropertyException;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.openstacknode.OpenstackNodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;

import static org.onosproject.net.flow.instructions.ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_TUNNEL_DST;
import static org.onosproject.openstacknetworking.Constants.*;

/**
 * Populates switching flow rules.
 */
@Component(immediate = true)
public final class OpenstackSwitchingRulePopulator extends AbstractVmHandler {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowObjectiveService flowObjectiveService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackNodeService nodeService;


    private static final String TUNNEL_DST = "tunnelDst";

    @Activate
    protected void activate() {
        super.activate();
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    private void populateSwitchingRules(Host host) {
        populateFlowRulesForTunnelTag(host);
        populateFlowRulesForTrafficToSameCnode(host);
        populateFlowRulesForTrafficToDifferentCnode(host);

        log.debug("Populated switching rule for {}", host);
    }

    private void populateFlowRulesForTunnelTag(Host host) {
        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

        sBuilder.matchEthType(Ethernet.TYPE_IPV4)
                .matchInPort(host.location().port());

        tBuilder.setTunnelId(Long.valueOf(getVni(host)));

        ForwardingObjective fo = DefaultForwardingObjective.builder()
                .withSelector(sBuilder.build())
                .withTreatment(tBuilder.build())
                .withPriority(TUNNELTAG_RULE_PRIORITY)
                .withFlag(ForwardingObjective.Flag.SPECIFIC)
                .fromApp(appId)
                .add();

        flowObjectiveService.forward(host.location().deviceId(), fo);
    }

    private void populateFlowRulesForTrafficToSameCnode(Host host) {
        //For L2 Switching Case
        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

        sBuilder.matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(getIp(host).toIpPrefix())
                .matchTunnelId(Long.valueOf(getVni(host)));

        tBuilder.setOutput(host.location().port());

        ForwardingObjective fo = DefaultForwardingObjective.builder()
                .withSelector(sBuilder.build())
                .withTreatment(tBuilder.build())
                .withPriority(SWITCHING_RULE_PRIORITY)
                .withFlag(ForwardingObjective.Flag.SPECIFIC)
                .fromApp(appId)
                .add();

        flowObjectiveService.forward(host.location().deviceId(), fo);
    }

    private void populateFlowRulesForTrafficToDifferentCnode(Host host) {
        Ip4Address localVmIp = getIp(host);
        DeviceId localDeviceId = host.location().deviceId();
        Optional<IpAddress> localDataIp = nodeService.dataIp(localDeviceId);

        if (!localDataIp.isPresent()) {
            log.debug("Failed to get data IP for device {}",
                    host.location().deviceId());
            return;
        }

        String vni = getVni(host);
        getVmsInDifferentCnode(host).forEach(remoteVm -> {
            Optional<IpAddress> remoteDataIp = nodeService.dataIp(remoteVm.location().deviceId());
            if (remoteDataIp.isPresent()) {
                setVxLanFlowRule(vni,
                        localDeviceId,
                        remoteDataIp.get().getIp4Address(),
                        getIp(remoteVm));

                setVxLanFlowRule(vni,
                        remoteVm.location().deviceId(),
                        localDataIp.get().getIp4Address(),
                        localVmIp);
            }
        });
    }

    private void setVxLanFlowRule(String vni, DeviceId deviceId, Ip4Address remoteIp,
                                  Ip4Address vmIp) {
        Optional<PortNumber> tunnelPort = nodeService.tunnelPort(deviceId);
        if (!tunnelPort.isPresent()) {
            log.warn("Failed to get tunnel port from {}", deviceId);
            return;
        }

        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

        sBuilder.matchEthType(Ethernet.TYPE_IPV4)
                .matchTunnelId(Long.parseLong(vni))
                .matchIPDst(vmIp.toIpPrefix());
        tBuilder.extension(buildNiciraExtenstion(deviceId, remoteIp), deviceId)
                .setOutput(tunnelPort.get());

        ForwardingObjective fo = DefaultForwardingObjective.builder()
                .withSelector(sBuilder.build())
                .withTreatment(tBuilder.build())
                .withPriority(SWITCHING_RULE_PRIORITY)
                .withFlag(ForwardingObjective.Flag.SPECIFIC)
                .fromApp(appId)
                .add();

        flowObjectiveService.forward(deviceId, fo);
    }

    private void removeSwitchingRules(Host host) {
        removeFlowRuleForTunnelTag(host);
        removeFlowRuleForVMsInSameCnode(host);
        removeFlowRuleForVMsInDiffrentCnode(host);

        log.debug("Removed switching rule for {}", host);
    }

    private void removeFlowRuleForTunnelTag(Host host) {
        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

        sBuilder.matchEthType(Ethernet.TYPE_IPV4)
                .matchInPort(host.location().port());

        ForwardingObjective fo = DefaultForwardingObjective.builder()
                .withSelector(sBuilder.build())
                .withTreatment(tBuilder.build())
                .withPriority(TUNNELTAG_RULE_PRIORITY)
                .withFlag(ForwardingObjective.Flag.SPECIFIC)
                .fromApp(appId)
                .remove();

        flowObjectiveService.forward(host.location().deviceId(), fo);
    }

    private void removeFlowRuleForVMsInSameCnode(Host host) {
        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        sBuilder.matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(getIp(host).toIpPrefix())
                .matchTunnelId(Long.valueOf(getVni(host)));

        ForwardingObjective fo = DefaultForwardingObjective.builder()
                .withSelector(sBuilder.build())
                .withTreatment(DefaultTrafficTreatment.builder().build())
                .withFlag(ForwardingObjective.Flag.SPECIFIC)
                .withPriority(SWITCHING_RULE_PRIORITY)
                .fromApp(appId)
                .remove();

        flowObjectiveService.forward(host.location().deviceId(), fo);
    }

    private void removeFlowRuleForVMsInDiffrentCnode(Host host) {
        DeviceId deviceId = host.location().deviceId();
        final boolean anyPortRemainedInSameCnode = hostService.getConnectedHosts(deviceId)
                .stream()
                .filter(this::isValidHost)
                .anyMatch(h -> Objects.equals(getVni(h), getVni(host)));

        getVmsInDifferentCnode(host).forEach(h -> {
           removeVxLanFlowRule(h.location().deviceId(), getIp(host), getVni(host));
           if (!anyPortRemainedInSameCnode) {
               removeVxLanFlowRule(deviceId, getIp(h), getVni(host));
           }
       });
    }

    private void removeVxLanFlowRule(DeviceId deviceId, Ip4Address vmIp, String vni) {
        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();

        sBuilder.matchEthType(Ethernet.TYPE_IPV4)
                .matchTunnelId(Long.valueOf(vni))
                .matchIPDst(vmIp.toIpPrefix());

        ForwardingObjective fo = DefaultForwardingObjective.builder()
                .withSelector(sBuilder.build())
                .withTreatment(DefaultTrafficTreatment.builder().build())
                .withFlag(ForwardingObjective.Flag.SPECIFIC)
                .withPriority(SWITCHING_RULE_PRIORITY)
                .fromApp(appId)
                .remove();

        flowObjectiveService.forward(deviceId, fo);
    }

    private ExtensionTreatment buildNiciraExtenstion(DeviceId deviceId, Ip4Address remoteIp) {
        Device device = deviceService.getDevice(deviceId);
        if (device != null && !device.is(ExtensionTreatmentResolver.class)) {
            log.error("The extension treatment is not supported");
            return null;
        }

        ExtensionTreatmentResolver resolver = device.as(ExtensionTreatmentResolver.class);
        ExtensionTreatment treatment = resolver.getExtensionInstruction(NICIRA_SET_TUNNEL_DST.type());
        try {
            treatment.setPropertyValue(TUNNEL_DST, remoteIp);
            return treatment;
        } catch (ExtensionPropertyException e) {
            log.warn("Failed to get tunnelDst extension treatment for {}", deviceId);
            return null;
        }
    }

    @Override
    protected void hostDetected(Host host) {
        populateSwitchingRules(host);
        log.info("Added new virtual machine to switching service {}", host);
    }

    @Override
    protected void hostRemoved(Host host) {
        removeSwitchingRules(host);
        log.info("Removed virtual machine from switching service {}", host);
    }
}
