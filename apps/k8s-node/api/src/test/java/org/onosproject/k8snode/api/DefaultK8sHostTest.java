/*
 * Copyright 2020-present Open Networking Foundation
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
package org.onosproject.k8snode.api;

import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;

import java.util.Set;

import static junit.framework.TestCase.assertEquals;
import static org.onosproject.k8snode.api.K8sHostState.COMPLETE;
import static org.onosproject.k8snode.api.K8sHostState.INIT;

/**
 * Unit test for DefaultK8sHost.
 */
public final class DefaultK8sHostTest {

    private static final IpAddress HOST_IP_1 = IpAddress.valueOf("192.168.200.3");
    private static final IpAddress HOST_IP_2 = IpAddress.valueOf("192.168.200.4");

    private static final Set<String> NODE_NAMES_1 = ImmutableSet.of("1", "2");
    private static final Set<String> NODE_NAMES_2 = ImmutableSet.of("3", "4");

    private K8sHost refHost;

    private static final K8sHost K8S_HOST_1 = createHost(
            HOST_IP_1,
            NODE_NAMES_1,
            INIT
    );

    private static final K8sHost K8S_HOST_2 = createHost(
            HOST_IP_1,
            NODE_NAMES_1,
            INIT
    );

    private static final K8sHost K8S_HOST_3 = createHost(
            HOST_IP_2,
            NODE_NAMES_2,
            COMPLETE
    );

    /**
     * Initial setup for this unit test.
     */
    @Before
    public void setUp() {
        refHost = DefaultK8sHost.builder()
                .hostIp(HOST_IP_1)
                .nodeNames(NODE_NAMES_1)
                .state(INIT)
                .build();
    }

    /**
     * Checks equals method works as expected.
     */
    @Test
    public void testEquality() {
        new EqualsTester().addEqualityGroup(K8S_HOST_1, K8S_HOST_2)
                .addEqualityGroup(K8S_HOST_3)
                .testEquals();
    }

    /**
     * Test object construction.
     */
    @Test
    public void testConstruction() {
        assertEquals(refHost.hostIp(), HOST_IP_1);
        assertEquals(refHost.nodeNames(), NODE_NAMES_1);
        assertEquals(refHost.state(), INIT);
    }

    /**
     * Checks the functionality of update state method.
     */
    @Test
    public void testUpdateState() {
        K8sHost updatedHost = refHost.updateState(COMPLETE);

        assertEquals(updatedHost.hostIp(), HOST_IP_1);
        assertEquals(updatedHost.nodeNames(), NODE_NAMES_1);
        assertEquals(updatedHost.state(), COMPLETE);
    }

    private static K8sHost createHost(IpAddress hostIp, Set<String> nodeNames, K8sHostState state) {
        return DefaultK8sHost.builder()
                .hostIp(hostIp)
                .nodeNames(nodeNames)
                .state(state)
                .build();
    }
}
