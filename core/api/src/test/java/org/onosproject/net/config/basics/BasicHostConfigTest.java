/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.net.config.basics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.onlab.packet.EthType;
import org.onlab.packet.IpAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.NetTestTools;
import org.onosproject.net.config.ConfigApplyDelegate;

import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

public class BasicHostConfigTest {

    /**
     * Tests construction, setters and getters of a BasicHostConfig object.
     */
    @Test
    public void testConstruction() {
        BasicHostConfig config = new BasicHostConfig();
        ConfigApplyDelegate delegate = configApply -> { };
        ObjectMapper mapper = new ObjectMapper();
        HostId hostId = NetTestTools.hid("12:34:56:78:90:ab/1");
        IpAddress ip1 = IpAddress.valueOf("1.1.1.1");
        IpAddress ip2 = IpAddress.valueOf("1.1.1.2");
        IpAddress ip3 = IpAddress.valueOf("1.1.1.3");
        Set<IpAddress> ips = ImmutableSet.of(ip1, ip2, ip3);
        HostLocation loc1 = new HostLocation(
                NetTestTools.connectPoint("d1", 1), System.currentTimeMillis());
        HostLocation loc2 = new HostLocation(
                NetTestTools.connectPoint("d2", 2), System.currentTimeMillis());
        Set<HostLocation> locs = ImmutableSet.of(loc1, loc2);
        HostLocation loc3 = new HostLocation(
                NetTestTools.connectPoint("d3", 1), System.currentTimeMillis());
        HostLocation loc4 = new HostLocation(
                NetTestTools.connectPoint("d4", 2), System.currentTimeMillis());
        Set<HostLocation> auxLocations = ImmutableSet.of(loc3, loc4);
        VlanId vlanId = VlanId.vlanId((short) 10);
        EthType ethType = EthType.EtherType.lookup((short) 0x88a8).ethType();

        config.init(hostId, "KEY", JsonNodeFactory.instance.objectNode(), mapper, delegate);

        config.setIps(ips)
              .setLocations(locs)
              .setAuxLocations(auxLocations)
              .setInnerVlan(vlanId)
              .setOuterTpid(ethType);

        assertThat(config.isValid(), is(true));
        assertThat(config.name(), is("-"));
        assertThat(config.ipAddresses(), hasSize(3));
        assertThat(config.ipAddresses(), hasItems(ip1, ip2, ip3));
        assertThat(config.locations(), hasSize(2));
        assertThat(config.locations(), hasItems(loc1, loc2));
        assertThat(config.auxLocations(), hasSize(2));
        assertThat(config.auxLocations(), hasItems(loc3, loc4));
        assertThat(config.innerVlan(), is(vlanId));
        assertThat(config.outerTpid(), is(ethType));
    }

}