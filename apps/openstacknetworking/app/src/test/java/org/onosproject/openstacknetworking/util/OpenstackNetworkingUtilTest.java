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
package org.onosproject.openstacknetworking.util;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.testing.EqualsTester;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.openstacknode.api.NodeState;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeTest;
import org.openstack4j.model.network.NetFloatingIP;
import org.openstack4j.openstack.networking.domain.NeutronFloatingIP;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.getGwByComputeDevId;

public final class OpenstackNetworkingUtilTest {

    private NetFloatingIP floatingIp;

    @Before
    public void setUp() {
        floatingIp = NeutronFloatingIP.builder()
                .floatingNetworkId("floatingNetworkingId")
                .portId("portId")
                .build();
    }

    @After
    public void tearDown() {
    }

    /**
     * Tests the floatingIp translation.
     */
    @Test
    public void testFloatingIp() throws IOException {
        ObjectNode floatingIpNode =
                OpenstackNetworkingUtil.modelEntityToJson(floatingIp, NeutronFloatingIP.class);
        InputStream is = IOUtils.toInputStream(floatingIpNode.toString(), StandardCharsets.UTF_8.name());
        NetFloatingIP floatingIp2 = (NetFloatingIP)
                OpenstackNetworkingUtil.jsonToModelEntity(is, NeutronFloatingIP.class);
        new EqualsTester().addEqualityGroup(floatingIp, floatingIp2).testEquals();
    }

    /**
     * Tests the getGwByComputeDevId method.
     */
    @Test
    public void testGetGwByComputeDevId() {
        Set<OpenstackNode> gws = Sets.newConcurrentHashSet();
        gws.add(genGateway(1));
        gws.add(genGateway(2));
        gws.add(genGateway(3));

        Set<OpenstackNode> cloneOfGws = ImmutableSet.copyOf(gws);

        Map<String, Integer> gwCountMap = Maps.newConcurrentMap();
        int numOfDev = 99;

        for (int i = 1; i < 1 + numOfDev; i++) {
            OpenstackNode gw = getGwByComputeDevId(gws, genDeviceId(i));

            if (gwCountMap.get(gw.hostname()) == null) {
                gwCountMap.put(gw.hostname(), 1);
            } else {
                gwCountMap.compute(gw.hostname(), (k, v) -> v + 1);
            }

            new EqualsTester().addEqualityGroup(
                    getGwByComputeDevId(gws, genDeviceId(i)),
                    getGwByComputeDevId(cloneOfGws, genDeviceId(i)))
                    .testEquals();
        }

        int sum = gwCountMap.values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(numOfDev, sum);
    }

    private OpenstackNode genGateway(int index) {

        Device intgBrg = InternalOpenstackNodeTest.createDevice(index);

        String hostname = "gateway-" + index;
        IpAddress ip = Ip4Address.valueOf("10.10.10." + index);
        NodeState state = NodeState.COMPLETE;
        String uplinkPort = "eth0";
        return InternalOpenstackNodeTest.createNode(hostname,
                OpenstackNode.NodeType.GATEWAY, intgBrg, ip, uplinkPort, state);

    }

    private DeviceId genDeviceId(int index) {
        return DeviceId.deviceId("of:compute-" + index);
    }

    protected class InternalOpenstackNodeTest extends OpenstackNodeTest {
    }
}
