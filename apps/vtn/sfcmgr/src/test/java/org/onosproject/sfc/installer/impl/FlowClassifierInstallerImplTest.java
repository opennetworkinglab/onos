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
package org.onosproject.sfc.installer.impl;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.NshServicePathId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.DeviceServiceAdapter;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flowobjective.FlowObjectiveService;
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
import org.onosproject.vtnrsc.DefaultFlowClassifier;
import org.onosproject.vtnrsc.DefaultPortChain;
import org.onosproject.vtnrsc.DefaultPortPair;
import org.onosproject.vtnrsc.DefaultPortPairGroup;
import org.onosproject.vtnrsc.DefaultVirtualPort;
import org.onosproject.vtnrsc.FixedIp;
import org.onosproject.vtnrsc.FlowClassifier;
import org.onosproject.vtnrsc.FlowClassifierId;
import org.onosproject.vtnrsc.PortChain;
import org.onosproject.vtnrsc.PortChainId;
import org.onosproject.vtnrsc.PortPair;
import org.onosproject.vtnrsc.PortPairGroup;
import org.onosproject.vtnrsc.PortPairGroupId;
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

public class FlowClassifierInstallerImplTest {

    FlowObjectiveService flowObjectiveService = new FlowObjectiveAdapter();
    DeviceService deviceService = new DeviceServiceAdapter();
    HostService hostService = new HostServiceAdapter();
    VirtualPortService virtualPortService = new VirtualPortAdapter();
    VtnRscService vtnRscService = new VtnRscAdapter();
    PortPairService portPairService = new PortPairAdapter();
    PortPairGroupService portPairGroupService = new PortPairGroupAdapter();
    FlowClassifierService flowClassifierService = new FlowClassifierAdapter();

    final DriverService driverService = createMock(DriverService.class);

    final PortChainId portChainId = PortChainId.of("78888888-fc23-aeb6-f44b-56dc5e2fb3ae");
    final TenantId tenantId = TenantId.tenantId("1");
    final String name = "PortChain";
    final String description = "PortChain";
    final List<PortPairGroupId> portPairGroups = new LinkedList<PortPairGroupId>();
    final List<FlowClassifierId> flowClassifiers = new LinkedList<FlowClassifierId>();
    PortPairGroupId portPairGroupId1 = PortPairGroupId.of("73333333-fc23-aeb6-f44b-56dc5e2fb3ae");
    PortPairGroupId portPairGroupId2 = PortPairGroupId.of("73343531-fc23-aeb6-f44b-56dc5e2fb3af");

    PortPairId portPairId1 = PortPairId.of("73333333-fc23-aeb6-f44b-56dc5e2fb3ae");
    PortPairId portPairId2 = PortPairId.of("74444444-fc23-aeb6-f44b-56dc5e2fb3ae");

    FlowClassifierId flowClassifierId1 = FlowClassifierId.of("74444444-fc23-aeb6-f44b-56dc5e2fb3ae");
    FlowClassifierId flowClassifierId2 = FlowClassifierId.of("74444444-fc23-aeb6-f44b-56dc5e2fb3af");

    final String ppName = "PortPair";
    final String ppDescription = "PortPair";
    final String ingress = "d3333333-24fc-4fae-af4b-321c5e2eb3d1";
    final String egress = "a4444444-4a56-2a6e-cd3a-9dee4e2ec345";


    final String ppgName = "PortPairGroup";
    final String ppgDescription = "PortPairGroup";
    final List<PortPairId> portPairList = new LinkedList<PortPairId>();

    VirtualPortId id1 = VirtualPortId.portId(ingress);
    VirtualPortId id2 = VirtualPortId.portId("3414");

    DeviceId deviceId = DeviceId.deviceId("of:000000000000001");

    final DriverHandler driverHandler = new MockDriverHandler();

    private PortPair createPortPair(PortPairId  ppId) {
        DefaultPortPair.Builder portPairBuilder = new DefaultPortPair.Builder();
        PortPair portPair = portPairBuilder.setId(ppId).setName(ppName).setTenantId(tenantId)
                .setDescription(ppDescription).setIngress(ingress).setEgress(egress).build();
        return portPair;
    }

    private PortPairGroup createPortPairGroup(PortPairGroupId ppgId) {

        portPairList.clear();
        // Create same two port-pair-group objects.
        portPairList.add(portPairId1);
        portPairList.add(portPairId2);

        DefaultPortPairGroup.Builder portPairGroupBuilder = new DefaultPortPairGroup.Builder();
        PortPairGroup portPairGroup = portPairGroupBuilder.setId(ppgId).setTenantId(tenantId)
                .setName(ppgName).setDescription(ppgDescription).setPortPairs(portPairList).build();

        return portPairGroup;

    }

    private PortChain createPortChain() {

        portPairGroups.clear();
        flowClassifiers.clear();
        // create list of Port Pair Groups.

        portPairGroups.add(portPairGroupId1);
        portPairGroups.add(portPairGroupId2);
        // create list of Flow classifiers.
        flowClassifiers.add(flowClassifierId1);
        flowClassifiers.add(flowClassifierId2);

        DefaultPortChain.Builder portChainBuilder = new DefaultPortChain.Builder();
        final PortChain portChain = portChainBuilder.setId(portChainId).setTenantId(tenantId).setName(name)
                .setDescription(description).setPortPairGroups(portPairGroups).setFlowClassifiers(flowClassifiers)
                .build();

        return portChain;
    }

    private FlowClassifier createFlowClassifier(FlowClassifierId id) {
        final String name = "FlowClassifier1";
        final String description = "FlowClassifier1";
        final String ethType = "IPv4";
        final String protocol = "tcp";
        final int minSrcPortRange = 5;
        final int maxSrcPortRange = 10;
        final int minDstPortRange = 5;
        final int maxDstPortRange = 10;
        final TenantId tenantId = TenantId.tenantId("1");
        final IpPrefix srcIpPrefix = IpPrefix.valueOf("0.0.0.0/0");
        final IpPrefix dstIpPrefix = IpPrefix.valueOf("10.10.10.10/0");
        final VirtualPortId virtualSrcPort = id1;
        final VirtualPortId virtualDstPort = id2;

        DefaultFlowClassifier.Builder flowClassifierBuilder = new DefaultFlowClassifier.Builder();
        final FlowClassifier flowClassifier = flowClassifierBuilder.setFlowClassifierId(id)
                .setTenantId(tenantId).setName(name).setDescription(description).setEtherType(ethType)
                .setProtocol(protocol).setMinSrcPortRange(minSrcPortRange).setMaxSrcPortRange(maxSrcPortRange)
                .setMinDstPortRange(minDstPortRange).setMaxDstPortRange(maxDstPortRange).setSrcIpPrefix(srcIpPrefix)
                .setDstIpPrefix(dstIpPrefix).setSrcPort(virtualSrcPort).setDstPort(virtualDstPort).build();
        return flowClassifier;
    }

    private VirtualPort createVirtualPort() {
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

        VirtualPort d1 = new DefaultVirtualPort(id1, networkId, true,
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

        ApplicationId appId = new DefaultApplicationId(1, "test");
        FlowClassifierInstallerImpl flowClassifierInstaller = new FlowClassifierInstallerImpl();
        flowClassifierInstaller.virtualPortService = virtualPortService;
        flowClassifierInstaller.vtnRscService = vtnRscService;
        flowClassifierInstaller.portPairService = portPairService;
        flowClassifierInstaller.portPairGroupService = portPairGroupService;
        flowClassifierInstaller.flowClassifierService = flowClassifierService;
        flowClassifierInstaller.driverService = driverService;
        flowClassifierInstaller.deviceService = deviceService;
        flowClassifierInstaller.hostService = hostService;
        flowClassifierInstaller.flowObjectiveService = flowObjectiveService;
        flowClassifierInstaller.appId = appId;

        final PortChain portChain = createPortChain();
        NshServicePathId nshSpiId = NshServicePathId.of(10);

        portPairGroupService.createPortPairGroup(createPortPairGroup(portPairGroupId1));
        portPairGroupService.createPortPairGroup(createPortPairGroup(portPairGroupId2));
        portPairService.createPortPair(createPortPair(portPairId1));
        portPairService.createPortPair(createPortPair(portPairId2));
        FlowClassifier fc1 = createFlowClassifier(flowClassifierId1);
        FlowClassifier fc2 = createFlowClassifier(flowClassifierId2);
        flowClassifierService.createFlowClassifier(fc1);
        flowClassifierService.createFlowClassifier(fc2);

        List<VirtualPort> virtualPortList = Lists.newArrayList();
        virtualPortList.add(createVirtualPort());
        virtualPortService.createPorts(virtualPortList);

        expect(driverService.createHandler(deviceId)).andReturn(driverHandler).anyTimes();
        replay(driverService);

        ConnectPoint connectPoint = flowClassifierInstaller.installFlowClassifier(portChain, nshSpiId);

        assertThat(connectPoint, is(HostLocation.NONE));
    }
}