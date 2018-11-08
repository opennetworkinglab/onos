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
package org.onosproject.openstackvtap.impl;

import com.google.common.testing.EqualsTester;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onosproject.openstackvtap.api.OpenstackVtapNetwork;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Unit tests for DefaultOpenstackVtapNetwork class.
 */
public class DefaultOpenstackVtapNetworkTest {

    private static final OpenstackVtapNetwork.Mode VTAP_NETWORK_MODE_1 = OpenstackVtapNetwork.Mode.VXLAN;
    private static final OpenstackVtapNetwork.Mode VTAP_NETWORK_MODE_2 = OpenstackVtapNetwork.Mode.GRE;

    private static final int VTAP_NETWORK_NETWORK_ID_1 = 1;
    private static final int VTAP_NETWORK_NETWORK_ID_2 = 2;

    private static final IpAddress SERVER_IP_1 = IpAddress.valueOf("20.10.10.1");
    private static final IpAddress SERVER_IP_2 = IpAddress.valueOf("20.10.20.1");

    private OpenstackVtapNetwork vtapNetwork1;
    private OpenstackVtapNetwork sameAsVtapNetwork1;
    private OpenstackVtapNetwork vtapNetwork2;

    /**
     * Initial setup for this unit test.
     */
    @Before
    public void setup() {
        OpenstackVtapNetwork.Builder builder1 = DefaultOpenstackVtapNetwork.builder();
        vtapNetwork1 = builder1
                .mode(VTAP_NETWORK_MODE_1)
                .networkId(VTAP_NETWORK_NETWORK_ID_1)
                .serverIp(SERVER_IP_1)
                .build();

        OpenstackVtapNetwork.Builder builder2 = DefaultOpenstackVtapNetwork.builder();
        sameAsVtapNetwork1 = builder2
                .mode(VTAP_NETWORK_MODE_1)
                .networkId(VTAP_NETWORK_NETWORK_ID_1)
                .serverIp(SERVER_IP_1)
                .build();

        OpenstackVtapNetwork.Builder builder3 = DefaultOpenstackVtapNetwork.builder();
        vtapNetwork2 = builder3
                .mode(VTAP_NETWORK_MODE_2)
                .networkId(VTAP_NETWORK_NETWORK_ID_2)
                .serverIp(SERVER_IP_2)
                .build();
    }

    /**
     * Tests class immutability.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultOpenstackVtapNetwork.class);
    }

    /**
     * Tests object equality.
     */
    @Test
    public void testEquality() {
        new EqualsTester()
                .addEqualityGroup(vtapNetwork1, sameAsVtapNetwork1)
                .addEqualityGroup(vtapNetwork2).testEquals();
    }

    /**
     * Tests object construction.
     */
    @Test
    public void testConstruction() {
        OpenstackVtapNetwork vtapNetwork = vtapNetwork1;

        assertThat(vtapNetwork.mode(), is(VTAP_NETWORK_MODE_1));
        assertThat(vtapNetwork.networkId(), is(VTAP_NETWORK_NETWORK_ID_1));
        assertThat(vtapNetwork.serverIp(), is(SERVER_IP_1));
    }
}
