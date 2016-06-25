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
package org.onosproject.sfc.forwarder.impl;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.NshServicePathId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.DeviceServiceAdapter;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions.OutputInstruction;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.host.HostService;
import org.onosproject.net.host.HostServiceAdapter;
import org.onosproject.sfc.util.FlowClassifierAdapter;
import org.onosproject.sfc.util.FlowObjectiveAdapter;
import org.onosproject.sfc.util.MockDriverHandler;
import org.onosproject.sfc.util.PortPairAdapter;
import org.onosproject.sfc.util.PortPairGroupAdapter;
import org.onosproject.sfc.util.VirtualPortAdapter;
import org.onosproject.sfc.util.VtnRscAdapter;
import org.onosproject.vtnrsc.AllowedAddressPair;
import org.onosproject.vtnrsc.BindingHostId;
import org.onosproject.vtnrsc.DefaultPortPair;
import org.onosproject.vtnrsc.DefaultVirtualPort;
import org.onosproject.vtnrsc.FixedIp;
import org.onosproject.vtnrsc.PortPair;
import org.onosproject.vtnrsc.PortPairId;
import org.onosproject.vtnrsc.SecurityGroup;
import org.onosproject.vtnrsc.SubnetId;
import org.onosproject.vtnrsc.TenantId;
import org.onosproject.vtnrsc.TenantNetworkId;
import org.onosproject.vtnrsc.VirtualPort;
import org.onosproject.vtnrsc.VirtualPortId;
import org.onosproject.vtnrsc.flowclassifier.FlowClassifierService;
import org.onosproject.vtnrsc.portpair.PortPairService;
import org.onosproject.vtnrsc.portpairgroup.PortPairGroupService;
import org.onosproject.vtnrsc.service.VtnRscService;
import org.onosproject.vtnrsc.virtualport.VirtualPortService;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class ServiceFunctionForwarderImplTest {

    FlowObjectiveService flowObjectiveService = new FlowObjectiveAdapter();
    DeviceService deviceService = new DeviceServiceAdapter();
    HostService hostService = new HostServiceAdapter();
    VirtualPortService virtualPortService = new VirtualPortAdapter();
    VtnRscService vtnRscService = new VtnRscAdapter();
    PortPairService portPairService = new PortPairAdapter();
    PortPairGroupService portPairGroupService = new PortPairGroupAdapter();
    FlowClassifierService flowClassifierService = new FlowClassifierAdapter();

    final DriverService driverService = createMock(DriverService.class);

    DeviceId deviceId = DeviceId.deviceId("of:000000000000001");
    final TenantId tenantId = TenantId.tenantId("1");

    final DriverHandler driverHandler = new MockDriverHandler();

    private VirtualPort createVirtualPort(VirtualPortId id) {
        Set<FixedIp> fixedIps;
        Map<String, String> propertyMap;
        Set<AllowedAddressPair> allowedAddressPairs;
        Set<SecurityGroup> securityGroups = Sets.newHashSet();

        String macAddressStr = "fa:12:3e:56:ee:a2";
        String ipAddress = "10.1.1.1";
        String tenantNetworkId = "1234567";
        String subnet = "1212";
        String hostIdStr = "fa:e2:3e:56:ee:a2";
        String deviceOwner = "james";
        propertyMap = Maps.newHashMap();
        propertyMap.putIfAbsent("deviceOwner", deviceOwner);

        TenantNetworkId networkId = TenantNetworkId.networkId(tenantNetworkId);
        MacAddress macAddress = MacAddress.valueOf(macAddressStr);
        BindingHostId bindingHostId = BindingHostId.bindingHostId(hostIdStr);
        FixedIp fixedIp = FixedIp.fixedIp(SubnetId.subnetId(subnet),
                                          IpAddress.valueOf(ipAddress));
        fixedIps = Sets.newHashSet();
        fixedIps.add(fixedIp);

        allowedAddressPairs = Sets.newHashSet();
        AllowedAddressPair allowedAddressPair = AllowedAddressPair
                .allowedAddressPair(IpAddress.valueOf(ipAddress),
                                    MacAddress.valueOf(macAddressStr));
        allowedAddressPairs.add(allowedAddressPair);

        VirtualPort d1 = new DefaultVirtualPort(id, networkId, true,
                                                propertyMap,
                                                VirtualPort.State.ACTIVE,
                                                macAddress, tenantId, deviceId,
                                                fixedIps, bindingHostId,
                                                allowedAddressPairs,
                                                securityGroups);
        return d1;
    }

    @Test
    public void testInstallFlowClassifier() {

        PortPairId portPairId1 = PortPairId.of("73333333-fc23-aeb6-f44b-56dc5e2fb3ae");
        PortPairId portPairId2 = PortPairId.of("74444444-fc23-aeb6-f44b-56dc5e2fb3ae");

        final String ppName1 = "PortPair1";
        final String ppDescription1 = "PortPair1";
        final String ingress1 = "d3333333-24fc-4fae-af4b-321c5e2eb3d1";
        final String egress1 = "a4444444-4a56-2a6e-cd3a-9dee4e2ec345";
        DefaultPortPair.Builder portPairBuilder = new DefaultPortPair.Builder();
        PortPair portPair1 = portPairBuilder.setId(portPairId1).setName(ppName1).setTenantId(tenantId)
                .setDescription(ppDescription1).setIngress(ingress1).setEgress(egress1).build();

        final String ppName2 = "PortPair2";
        final String ppDescription2 = "PortPair2";
        final String ingress2 = "d5555555-24fc-4fae-af4b-321c5e2eb3d1";
        final String egress2 = "a6666666-4a56-2a6e-cd3a-9dee4e2ec345";
        PortPair portPair2 = portPairBuilder.setId(portPairId2).setName(ppName2).setTenantId(tenantId)
                .setDescription(ppDescription2).setIngress(ingress2).setEgress(egress2).build();

        ApplicationId appId = new DefaultApplicationId(1, "test");
        ServiceFunctionForwarderImpl serviceFunctionForwarder = new ServiceFunctionForwarderImpl();
        serviceFunctionForwarder.virtualPortService = virtualPortService;
        serviceFunctionForwarder.vtnRscService = vtnRscService;
        serviceFunctionForwarder.portPairService = portPairService;
        serviceFunctionForwarder.portPairGroupService = portPairGroupService;
        serviceFunctionForwarder.flowClassifierService = flowClassifierService;
        serviceFunctionForwarder.driverService = driverService;
        serviceFunctionForwarder.hostService = hostService;
        serviceFunctionForwarder.flowObjectiveService = flowObjectiveService;
        serviceFunctionForwarder.appId = appId;

        NshServicePathId nshSpiId = NshServicePathId.of(10);

        portPairService.createPortPair(portPair1);
        portPairService.createPortPair(portPair2);

        List<PortPairId> path = Lists.newArrayList();
        path.add(portPairId1);
        path.add(portPairId2);

        List<VirtualPort> virtualPortList = Lists.newArrayList();
        virtualPortList.add(createVirtualPort(VirtualPortId.portId(egress1)));
        virtualPortList.add(createVirtualPort(VirtualPortId.portId(ingress2)));
        virtualPortService.createPorts(virtualPortList);

        expect(driverService.createHandler(deviceId)).andReturn(driverHandler).anyTimes();
        replay(driverService);

        serviceFunctionForwarder.installLoadBalancedForwardingRule(path, nshSpiId);

        ForwardingObjective forObj = ((FlowObjectiveAdapter) flowObjectiveService).forwardingObjective();

        // Check for Selector
        assertThat(forObj.selector().getCriterion(Criterion.Type.IN_PORT), instanceOf(PortCriterion.class));

        // Check for treatment
        List<Instruction> instructions = forObj.treatment().allInstructions();
        for (Instruction instruction : instructions) {
            if (instruction.type() == Instruction.Type.OUTPUT) {
                assertThat(((OutputInstruction) instruction).port(), is(PortNumber.P0));
            }
        }
    }
}