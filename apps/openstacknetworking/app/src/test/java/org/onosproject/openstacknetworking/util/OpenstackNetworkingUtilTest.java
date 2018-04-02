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
import com.google.common.testing.EqualsTester;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openstack4j.model.network.NetFloatingIP;
import org.openstack4j.openstack.networking.domain.NeutronFloatingIP;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

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
}
