/*
* Copyright 2015 Open Networking Laboratory
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

package org.onosproject.openstackswitching;

import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.TpPort;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * It populates switching flow rules.
 *
 */
public class OpenstackSwitchingRulePopulator {

    private static Logger log = LoggerFactory
            .getLogger(OpenstackSwitchingRulePopulator.class);

    private FlowObjectiveService flowObjectiveService;
    private ApplicationId appId;

    /**
     * Returns OpenstackSwitchingRule reference.
     * @param flowObjectiveService FlowObjectiveService reference
     */
    public OpenstackSwitchingRulePopulator(ApplicationId appId,
                                           FlowObjectiveService flowObjectiveService) {
        this.flowObjectiveService = flowObjectiveService;
        this.appId = appId;
    }

    /**
     * Populates flows rules for forwarding packets to and from VMs.
     *
     * @return true if it succeeds to populate rules, false otherwise.
     */
    public boolean populateForwardingRule(Ip4Address ip, DeviceId id, Port port, Ip4Prefix cidr) {


        setFlowRuleForVMsInSameCnode(ip, id, port, cidr);

        return true;
    }

    /**
     * Populates the common flows rules for all VMs.
     *
     * - Send ARP packets to the controller
     * - Send DHCP packets to the controller
     *
     * @param id Device ID to populates rules to
     */
    public void populateDefaultRules(DeviceId id) {

        //setFlowRuleForDHCP(id);
        setFlowRuleForArp(id);

        log.warn("Default rule has been set");
    }

    /**
     * Populates the forwarding rules for VMs with the same VNI but in other Code.
     *
     * @param vni VNI for the networks
     * @param id device ID to populates the flow rules
     * @param hostIp host IP address of the VM
     * @param vmIp fixed IP address for the VM
     * @param idx device ID for OVS of the other VM
     * @param hostIpx host IP address of the other VM
     * @param vmIpx fixed IP address of the other VM
     */
    public void populateForwardingRuleForOtherCnode(String vni, DeviceId id, Ip4Address hostIp,
                                                    Ip4Address vmIp, MacAddress vmMac, PortNumber tunnelPort,
                                                    DeviceId idx, Ip4Address hostIpx,
                                                    Ip4Address vmIpx, MacAddress vmMacx, PortNumber tunnelPortx) {
        setVxLanFlowRule(vni, id, hostIp, vmIp, vmMac, tunnelPort);
        setVxLanFlowRule(vni, idx, hostIpx, vmIpx, vmMacx, tunnelPortx);
    }

    /**
     * Populates the flow rules for DHCP packets from VMs.
     *
     * @param id device ID to set the rules
     */
    private void setFlowRuleForDHCP(DeviceId id) {
        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

        sBuilder.matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(IPv4.PROTOCOL_UDP)
                .matchUdpDst(TpPort.tpPort(OpenstackSwitchingManager.DHCP_PORT));
        tBuilder.setOutput(PortNumber.CONTROLLER);

        ForwardingObjective fo = DefaultForwardingObjective.builder()
                .withSelector(sBuilder.build())
                .withTreatment(tBuilder.build())
                .withPriority(5000)
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .fromApp(appId)
                .add();

        flowObjectiveService.forward(id, fo);
    }

    /**
     * Populates the flow rules for ARP packets from VMs.
     *
     * @param id device ID to put rules.
     */
    private void setFlowRuleForArp(DeviceId id) {
        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

        sBuilder.matchEthType(Ethernet.TYPE_ARP);
        tBuilder.setOutput(PortNumber.CONTROLLER);

        ForwardingObjective fo = DefaultForwardingObjective.builder()
                .withSelector(sBuilder.build())
                .withTreatment(tBuilder.build())
                .withPriority(5000)
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .fromApp(appId)
                .add();

        flowObjectiveService.forward(id, fo);
    }

    /**
     * Sets the flow rules for traffic between VMs in the same Cnode.
     *
     * @param ip4Address VM IP address
     * @param id device ID to put rules
     * @param port VM port
     * @param cidr subnet info of the VMs
     */
    private void setFlowRuleForVMsInSameCnode(Ip4Address ip4Address, DeviceId id,
                                              Port port, Ip4Prefix cidr) {
        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

        sBuilder.matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(ip4Address.toIpPrefix())
                .matchIPSrc(cidr);
        tBuilder.setOutput(port.number());

        ForwardingObjective fo = DefaultForwardingObjective.builder()
                .withSelector(sBuilder.build())
                .withTreatment(tBuilder.build())
                .withPriority(5000)
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .fromApp(appId)
                .add();

        flowObjectiveService.forward(id, fo);
    }

    /**
     * Sets the flow rules between traffic from VMs in different Cnode.
     *
     * @param vni  VNI
     * @param id device ID
     * @param hostIp host IP of the VM
     * @param vmIp fixed IP of the VM
     * @param vmMac MAC address of the VM
     * @param tunnelPort tunnel port to forward traffic to
     */
    private void setVxLanFlowRule(String vni, DeviceId id, Ip4Address hostIp,
                                  Ip4Address vmIp, MacAddress vmMac, PortNumber tunnelPort) {
        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

        sBuilder.matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(vmIp.toIpPrefix());
        tBuilder.setTunnelId(Long.parseLong(vni))
                //.setTunnelDst() <- for Nicira ext
                //.setEthDst(vmMac)
                .setOutput(tunnelPort);

        ForwardingObjective fo = DefaultForwardingObjective.builder()
                .withSelector(sBuilder.build())
                .withTreatment(tBuilder.build())
                .withPriority(5000)
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .fromApp(appId)
                .add();

        flowObjectiveService.forward(id, fo);
    }
}
