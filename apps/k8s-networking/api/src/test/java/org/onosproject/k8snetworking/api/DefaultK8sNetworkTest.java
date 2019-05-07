/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.k8snetworking.api;

import com.google.common.testing.EqualsTester;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Unit tests for the default kubernetes network class.
 */
public class DefaultK8sNetworkTest {

    private static final String NETWORK_ID_1 = "network-1";
    private static final String NETWORK_ID_2 = "network-2";
    private static final String NAME_1 = "network-1";
    private static final String NAME_2 = "network-2";
    private static final K8sNetwork.Type TYPE_1 = K8sNetwork.Type.VXLAN;
    private static final K8sNetwork.Type TYPE_2 = K8sNetwork.Type.GENEVE;
    private static final Integer MTU_1 = 1500;
    private static final Integer MTU_2 = 1600;
    private static final String SEGMENT_ID_1 = "1";
    private static final String SEGMENT_ID_2 = "2";
    private static final String CIDR_1 = "10.10.0.0/24";
    private static final String CIDR_2 = "10.10.1.0/24";

    private K8sNetwork k8sNetwork1;
    private K8sNetwork sameAsK8sNetwork1;
    private K8sNetwork k8sNetwork2;

    /**
     * Initial setup for this unit test.
     */
    @Before
    public void setUp() {
        k8sNetwork1 = DefaultK8sNetwork.builder()
                .networkId(NETWORK_ID_1)
                .name(NAME_1)
                .type(TYPE_1)
                .mtu(MTU_1)
                .segmentId(SEGMENT_ID_1)
                .cidr(CIDR_1)
                .build();

        sameAsK8sNetwork1 = DefaultK8sNetwork.builder()
                .networkId(NETWORK_ID_1)
                .name(NAME_1)
                .type(TYPE_1)
                .mtu(MTU_1)
                .segmentId(SEGMENT_ID_1)
                .cidr(CIDR_1)
                .build();

        k8sNetwork2 = DefaultK8sNetwork.builder()
                .networkId(NETWORK_ID_2)
                .name(NAME_2)
                .type(TYPE_2)
                .mtu(MTU_2)
                .segmentId(SEGMENT_ID_2)
                .cidr(CIDR_2)
                .build();
    }

    /**
     * Tests class immutability.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultK8sNetwork.class);
    }

    /**
     * Tests object equality.
     */
    @Test
    public void testEquality() {
        new EqualsTester().addEqualityGroup(k8sNetwork1, sameAsK8sNetwork1)
                .addEqualityGroup(k8sNetwork2)
                .testEquals();
    }

    /**
     * Test object construction.
     */
    @Test
    public void testConstruction() {
        K8sNetwork k8sNetwork = k8sNetwork1;

        assertEquals(NETWORK_ID_1, k8sNetwork.networkId());
        assertEquals(NAME_1, k8sNetwork.name());
        assertEquals(TYPE_1, k8sNetwork.type());
        assertEquals(MTU_1, k8sNetwork.mtu());
        assertEquals(SEGMENT_ID_1, k8sNetwork.segmentId());
        assertEquals(CIDR_1, k8sNetwork.cidr());
    }
}
