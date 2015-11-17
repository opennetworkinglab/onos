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
import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.ExtensionTreatmentResolver;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DefaultDriverData;
import org.onosproject.net.driver.DefaultDriverHandler;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionPropertyException;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Populates switching flow rules.
 */
public class OpenstackSwitchingRulePopulator {

    private static Logger log = LoggerFactory
            .getLogger(OpenstackSwitchingRulePopulator.class);
    private static final int SWITCHING_RULE_PRIORITY = 30000;
    private static final int EAST_WEST_ROUTING_RULE_PRIORITY = 29000;
    private static final int TUNNELTAG_RULE_PRIORITY = 30000;

    private FlowObjectiveService flowObjectiveService;
    private DriverService driverService;
    private DeviceService deviceService;
    private OpenstackRestHandler restHandler;
    private ApplicationId appId;

    private Collection<OpenstackNetwork> openstackNetworkList;
    private Collection<OpenstackPort> openstackPortList;

    /**
     * Creates OpenstackSwitchingRulPopulator.
     *
     * @param appId application id
     * @param flowObjectiveService FlowObjectiveService reference
     * @param deviceService DeviceService reference
     * @param driverService DriverService reference
     */
    public OpenstackSwitchingRulePopulator(ApplicationId appId,
                                           FlowObjectiveService flowObjectiveService,
                                           DeviceService deviceService,
                                           OpenstackRestHandler restHandler,
                                           DriverService driverService) {
        this.flowObjectiveService = flowObjectiveService;
        this.deviceService = deviceService;
        this.driverService = driverService;
        this.restHandler = restHandler;
        this.appId = appId;

        openstackNetworkList = restHandler.getNetworks();
        openstackPortList = restHandler.getPorts();
    }


    /**
     * Populates flow rules for the VM created.
     *
     * @param device device to populate rules to
     * @param port port for the VM created
     */
    public void populateSwitchingRules(Device device, Port port) {
        populateFlowRulesForTunnelTag(device, port);
        populateFlowRulesForTrafficToSameCnode(device, port);
        populateFlowRulesForTrafficToDifferentCnode(device, port);
    }

    /**
     * Populate the flow rules for tagging tunnelId according to which inport is came from.
     *
     * @param device device to put the rules
     * @param port port info of the VM
     */
    private void populateFlowRulesForTunnelTag(Device device, Port port) {
        Ip4Address vmIp = getFixedIpAddressForPort(port.annotations().value("portName"));
        String portName = port.annotations().value("portName");
        String vni = getVniForPort(portName);

        if (vmIp != null) {
            setFlowRuleForTunnelTag(device.id(), port, vni);
        }
    }

    /**
     * Returns OpenstackPort object for the Port reference given.
     *
     * @param port Port object
     * @return OpenstackPort reference, or null when not found
     */
    public OpenstackPort openstackPort(Port port) {
        String uuid = port.annotations().value("portName").substring(3);
        return openstackPortList.stream().filter(p -> p.id().startsWith(uuid))
                .findAny().orElse(null);
    }

    /**
     * Remove flows rules for the VM removed.
     *
     * @param deviceId device to remove rules
     * @param vmIp IP address of the VM removed
     */
    public void removeSwitchingRules(DeviceId deviceId, Ip4Address vmIp) {
        removeFlowRuleForVMsInSameCnode(deviceId, vmIp);
        deviceService.getAvailableDevices().forEach(device -> {
            if (!device.id().equals(deviceId)) {
                removeVxLanFlowRule(device.id(), vmIp);
            }
        });
    }

    /**
     * Populates the flow rules for traffic to VMs in the same Cnode as the sender.
     *
     * @param device device to put the rules
     * @param port port info of the VM
     */
    private void populateFlowRulesForTrafficToSameCnode(Device device, Port port) {
        Ip4Address vmIp = getFixedIpAddressForPort(port.annotations().value("portName"));
        String portName = port.annotations().value("portName");
        String vni = getVniForPort(portName);
        MacAddress vmMacAddress = getVmMacAddressForPort(portName);

        if (vmIp != null) {
            setFlowRuleForVMsInSameCnode(vmIp, device.id(), port, vni, vmMacAddress);
        }
    }

    /**
     * Populates the flow rules for traffic to VMs in different Cnode using
     * Nicira extention.
     *
     * @param device device to put rules
     * @param port port information of the VM
     */
    private void populateFlowRulesForTrafficToDifferentCnode(Device device, Port port) {
        String portName = port.annotations().value("portName");
        String channelId = device.annotations().value("channelId");
        Ip4Address hostIpAddress = Ip4Address.valueOf(channelId.split(":")[0]);
        Ip4Address fixedIp = getFixedIpAddressForPort(portName);
        MacAddress vmMac = getVmMacAddressForPort(portName);
        String vni = getVniForPort(portName);
        deviceService.getAvailableDevices().forEach(d -> {
            if (!d.equals(device)) {
                deviceService.getPorts(d.id()).forEach(p -> {
                    String pName = p.annotations().value("portName");
                    if (!p.equals(port) && vni.equals(getVniForPort(pName))) {
                        String cidx = d.annotations().value("channelId");
                        Ip4Address hostIpx = Ip4Address.valueOf(cidx.split(":")[0]);
                        MacAddress vmMacx = getVmMacAddressForPort(pName);
                        Ip4Address fixedIpx = getFixedIpAddressForPort(pName);
                        if (port.isEnabled()) {
                            setVxLanFlowRule(vni, device.id(), hostIpx, fixedIpx, vmMacx);
                            setVxLanFlowRule(vni, d.id(), hostIpAddress, fixedIp, vmMac);
                        }
                    }
                });
            }
        });
    }

    /**
     * Returns the VNI of the VM of the port.
     *
     * @param portName VM port
     * @return VNI
     */
    private String getVniForPort(String portName) {
        String uuid = portName.substring(3);
        OpenstackPort port = openstackPortList.stream()
                .filter(p -> p.id().startsWith(uuid))
                .findAny().orElse(null);
        if (port == null) {
            log.debug("No port information for port {}", portName);
            return null;
        }

        OpenstackNetwork network = openstackNetworkList.stream()
                .filter(n -> n.id().equals(port.networkId()))
                .findAny().orElse(null);
        if (network == null) {
            log.warn("No VNI information for network {}", port.networkId());
            return null;
        }

        return network.segmentId();
    }

    /**
     * Returns the Fixed IP address of the VM.
     *
     * @param portName VM port info
     * @return IP address of the VM
     */
    private Ip4Address getFixedIpAddressForPort(String portName) {

        String uuid = portName.substring(3);
        OpenstackPort port = openstackPortList.stream()
                .filter(p -> p.id().startsWith(uuid))
                .findFirst().orElse(null);

        if (port == null) {
            log.error("There is no port information for port name {}", portName);
            return null;
        }

        if (port.fixedIps().isEmpty()) {
            log.error("There is no fixed IP info in the port information");
            return null;
        }

        return (Ip4Address) port.fixedIps().values().toArray()[0];
    }

    /**
     * Returns the MAC address of the VM of the port.
     *
     * @param portName VM port
     * @return MAC address of the VM
     */
    private MacAddress getVmMacAddressForPort(String portName) {

        String uuid = portName.substring(3);
        OpenstackPort port = openstackPortList.stream()
                .filter(p -> p.id().startsWith(uuid))
                .findFirst().orElse(null);

        if (port == null) {
            log.error("There is no port information for port name {}", portName);
            return null;
        }

        return port.macAddress();
    }

    private void setFlowRuleForTunnelTag(DeviceId deviceId, Port port, String vni) {

        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

        sBuilder.matchEthType(Ethernet.TYPE_IPV4)
                .matchInPort(port.number());

        tBuilder.setTunnelId(Long.parseLong(vni));

        ForwardingObjective fo = DefaultForwardingObjective.builder()
                .withSelector(sBuilder.build())
                .withTreatment(tBuilder.build())
                .withPriority(TUNNELTAG_RULE_PRIORITY)
                .withFlag(ForwardingObjective.Flag.SPECIFIC)
                .fromApp(appId)
                .add();

        flowObjectiveService.forward(deviceId, fo);
    }


    /**
     * Sets the flow rules for traffic between VMs in the same Cnode.
     *
     * @param ip4Address VM IP address
     * @param id device ID to put rules
     * @param port VM port
     * @param vni VM VNI
     * @param vmMacAddress VM MAC address
     */
    private void setFlowRuleForVMsInSameCnode(Ip4Address ip4Address, DeviceId id,
                                              Port port, String vni, MacAddress vmMacAddress) {

        //For L2 Switching Case
        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

        sBuilder.matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(ip4Address.toIpPrefix())
                .matchTunnelId(Long.parseLong(vni));

        tBuilder.setOutput(port.number());

        ForwardingObjective fo = DefaultForwardingObjective.builder()
                .withSelector(sBuilder.build())
                .withTreatment(tBuilder.build())
                .withPriority(SWITCHING_RULE_PRIORITY)
                .withFlag(ForwardingObjective.Flag.SPECIFIC)
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
     */
    private void setVxLanFlowRule(String vni, DeviceId id, Ip4Address hostIp,
                                  Ip4Address vmIp, MacAddress vmMac) {
        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

        sBuilder.matchEthType(Ethernet.TYPE_IPV4)
                .matchTunnelId(Long.parseLong(vni))
                .matchIPDst(vmIp.toIpPrefix());

        tBuilder.extension(buildNiciraExtenstion(id, hostIp), id)
                .setOutput(getTunnelPort(id));

        ForwardingObjective fo = DefaultForwardingObjective.builder()
                .withSelector(sBuilder.build())
                .withTreatment(tBuilder.build())
                .withPriority(SWITCHING_RULE_PRIORITY)
                .withFlag(ForwardingObjective.Flag.SPECIFIC)
                .fromApp(appId)
                .add();

        flowObjectiveService.forward(id, fo);
    }

    private void removeFlowRuleForVMsInSameCnode(DeviceId id, Ip4Address vmIp) {
        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();

        sBuilder.matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(vmIp.toIpPrefix());

        ForwardingObjective fo = DefaultForwardingObjective.builder()
                .withSelector(sBuilder.build())
                .withTreatment(DefaultTrafficTreatment.builder().build())
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .withPriority(SWITCHING_RULE_PRIORITY)
                .fromApp(appId)
                .remove();

        flowObjectiveService.forward(id, fo);
    }

    private void removeVxLanFlowRule(DeviceId id, Ip4Address vmIp) {
        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        // XXX: Later, more matches will be added when multiple table is implemented.
        sBuilder.matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(vmIp.toIpPrefix());

        ForwardingObjective fo = DefaultForwardingObjective.builder()
                .withSelector(sBuilder.build())
                .withTreatment(DefaultTrafficTreatment.builder().build())
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .withPriority(SWITCHING_RULE_PRIORITY)
                .fromApp(appId)
                .remove();

        flowObjectiveService.forward(id, fo);
    }

    private ExtensionTreatment buildNiciraExtenstion(DeviceId id, Ip4Address hostIp) {
        Driver driver = driverService.getDriver(id);
        DriverHandler driverHandler = new DefaultDriverHandler(new DefaultDriverData(driver, id));
        ExtensionTreatmentResolver resolver = driverHandler.behaviour(ExtensionTreatmentResolver.class);

        ExtensionTreatment extensionInstruction =
                resolver.getExtensionInstruction(
                        ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_TUNNEL_DST.type());

        try {
            extensionInstruction.setPropertyValue("tunnelDst", hostIp);
        } catch (ExtensionPropertyException e) {
            log.error("Error setting Nicira extension setting {}", e);
        }

        return  extensionInstruction;
    }

    private PortNumber getTunnelPort(DeviceId id) {
        Port port = deviceService.getPorts(id).stream()
                .filter(p -> p.annotations().value("portName").equals("vxlan"))
                .findAny().orElse(null);

        if (port == null) {
            log.error("No TunnelPort was created.");
            return null;
        }
        return port.number();
    }
}
