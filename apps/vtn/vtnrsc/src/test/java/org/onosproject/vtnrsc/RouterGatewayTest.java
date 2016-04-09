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

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.google.common.testing.EqualsTester;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Unit tests for RouterGateway class.
 */
public class RouterGatewayTest {
    final TenantNetworkId networkId1 = TenantNetworkId.networkId("1");
    final TenantNetworkId networkId2 = TenantNetworkId.networkId("2");
    final Set<FixedIp> fixedIpSet1 = new HashSet<>();
    final Set<FixedIp> fixedIpSet2 = new HashSet<>();

    /**
     * Checks that the RouterGateway class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(RouterGateway.class);
    }

    /**
     * Checks the operation of equals().
     */
    @Test
    public void testEquals() {
        RouterGateway routerGateway1 = RouterGateway.routerGateway(networkId1,
                                                                   true,
                                                                   fixedIpSet1);
        RouterGateway routerGateway2 = RouterGateway.routerGateway(networkId1,
                                                                   true,
                                                                   fixedIpSet1);
        RouterGateway routerGateway3 = RouterGateway.routerGateway(networkId2,
                                                                   true,
                                                                   fixedIpSet2);
        new EqualsTester().addEqualityGroup(routerGateway1, routerGateway2)
                .addEqualityGroup(routerGateway3).testEquals();
    }

    /**
     * Checks the construction of a RouterGateway object.
     */
    @Test
    public void testConstruction() {
        RouterGateway routerGateway = RouterGateway.routerGateway(networkId1,
                                                                  true,
                                                                  fixedIpSet1);
        assertThat(fixedIpSet1, is(notNullValue()));
        assertThat(fixedIpSet1, is(routerGateway.externalFixedIps()));
        assertThat(networkId1, is(notNullValue()));
        assertThat(networkId1, is(routerGateway.networkId()));
        assertThat(routerGateway.enableSnat(), is(true));
    }
}
