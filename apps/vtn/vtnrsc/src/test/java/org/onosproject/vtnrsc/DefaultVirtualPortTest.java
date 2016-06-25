/*
 * Copyright 2015-present Open Networking Laboratory
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

package org.onosproject.vtnrsc;

import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.net.DeviceId;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.testing.EqualsTester;

import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Unit tests for DefaultVirtualPort class.
 */
public class DefaultVirtualPortTest {

    private Set<FixedIp> fixedIps;
    private Map<String, String> propertyMap;
    private Set<AllowedAddressPair> allowedAddressPairs;
    private Set<SecurityGroup> securityGroups;
    private VirtualPortId id1;
    private VirtualPortId id2;
    private String macAddressStr = "fa:12:3e:56:ee:a2";
    private String ipAddress = "10.1.1.1";
    private String deviceStr = "of:000000000000001";
    private String tenantIdStr = "123";
    private String portId1 = "1241";
    private String portId2 = "1242";
    private String tenantNetworkId = "1234567";
    private String subnet = "1212";
    private String hostIdStr = "fa:e2:3e:56:ee:a2";

    private void initVirtualPortId() {
        id1 = VirtualPortId.portId(portId1);
        id2 = VirtualPortId.portId(portId2);
    }

    private void initFixedIpSet() {
        FixedIp fixedIp = FixedIp.fixedIp(SubnetId.subnetId(subnet),
                                          IpAddress.valueOf(ipAddress));
        fixedIps = Sets.newHashSet();
        fixedIps.add(fixedIp);
    }

    private void initPropertyMap() {
        String deviceOwner = "james";
        propertyMap = Maps.newHashMap();
        propertyMap.putIfAbsent("deviceOwner", deviceOwner);
    }

    private void initAddressPairSet() {
        allowedAddressPairs = Sets.newHashSet();
        AllowedAddressPair allowedAddressPair = AllowedAddressPair
                .allowedAddressPair(IpAddress.valueOf(ipAddress),
                                    MacAddress.valueOf(macAddressStr));
        allowedAddressPairs.add(allowedAddressPair);
    }

    private void initSecurityGroupSet() {
        securityGroups = Sets.newHashSet();
    }

    /**
     * Checks that the DefaultVirtualPort class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(SecurityGroup.class);
    }

    /**
     * Checks the operation of equals().
     */
    @Test
    public void testEquals() {
        initVirtualPortId();
        initFixedIpSet();
        initPropertyMap();
        initAddressPairSet();
        initSecurityGroupSet();
        TenantNetworkId networkId = TenantNetworkId.networkId(tenantNetworkId);
        MacAddress macAddress = MacAddress.valueOf(macAddressStr);
        TenantId tenantId = TenantId.tenantId(tenantIdStr);
        DeviceId deviceId = DeviceId.deviceId(deviceStr);
        BindingHostId bindingHostId = BindingHostId.bindingHostId(hostIdStr);

        VirtualPort d1 = new DefaultVirtualPort(id1, networkId, true,
                                                propertyMap,
                                                VirtualPort.State.ACTIVE,
                                                macAddress, tenantId, deviceId,
                                                fixedIps, bindingHostId,
                                                allowedAddressPairs,
                                                securityGroups);
        VirtualPort d2 = new DefaultVirtualPort(id1, networkId, true,
                                                propertyMap,
                                                VirtualPort.State.ACTIVE,
                                                macAddress, tenantId, deviceId,
                                                fixedIps, bindingHostId,
                                                allowedAddressPairs,
                                                securityGroups);
        VirtualPort d3 = new DefaultVirtualPort(id2, networkId, true,
                                                propertyMap,
                                                VirtualPort.State.ACTIVE,
                                                macAddress, tenantId, deviceId,
                                                fixedIps, bindingHostId,
                                                allowedAddressPairs,
                                                securityGroups);
        new EqualsTester().addEqualityGroup(d1, d2).addEqualityGroup(d3)
                .testEquals();
    }
}
