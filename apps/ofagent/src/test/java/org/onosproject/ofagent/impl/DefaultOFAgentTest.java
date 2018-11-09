/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.ofagent.impl;

import com.google.common.collect.Sets;
import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onlab.packet.TpPort;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.net.TenantId;
import org.onosproject.ofagent.api.OFAgent;
import org.onosproject.ofagent.api.OFController;

import java.util.Set;

import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.onosproject.ofagent.api.OFAgent.State.STARTED;
import static org.onosproject.ofagent.api.OFAgent.State.STOPPED;

/**
 * Unit test of DefaultOFAgent model entity.
 */
public class DefaultOFAgentTest {

    private static final Set<OFController> CONTROLLER_1 = Sets.newHashSet(
            DefaultOFController.of(
                    IpAddress.valueOf("192.168.0.3"),
                    TpPort.tpPort(6653)));

    private static final Set<OFController> CONTROLLER_2 = Sets.newHashSet(
            DefaultOFController.of(
                    IpAddress.valueOf("192.168.0.3"),
                    TpPort.tpPort(6653)),
            DefaultOFController.of(
                    IpAddress.valueOf("192.168.0.4"),
                    TpPort.tpPort(6653)));

    private static final NetworkId NETWORK_1 = NetworkId.networkId(1);
    private static final NetworkId NETWORK_2 = NetworkId.networkId(2);

    private static final TenantId TENANT_1 = TenantId.tenantId("Tenant_1");
    private static final TenantId TENANT_2 = TenantId.tenantId("Tenant_2");

    private static final OFAgent OFAGENT = DefaultOFAgent.builder()
            .networkId(NETWORK_1)
            .tenantId(TENANT_1)
            .controllers(CONTROLLER_1)
            .state(STOPPED)
            .build();

    private static final OFAgent SAME_AS_OFAGENT_1 = DefaultOFAgent.builder()
            .networkId(NETWORK_1)
            .tenantId(TENANT_1)
            .controllers(CONTROLLER_2)
            .state(STOPPED)
            .build();

    private static final OFAgent SAME_AS_OFAGENT_2 = DefaultOFAgent.builder()
            .networkId(NETWORK_1)
            .tenantId(TENANT_1)
            .controllers(CONTROLLER_1)
            .state(STARTED)
            .build();

    private static final OFAgent ANOTHER_OFAGENT = DefaultOFAgent.builder()
            .networkId(NETWORK_2)
            .tenantId(TENANT_2)
            .controllers(CONTROLLER_1)
            .state(STOPPED)
            .build();

    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultOFAgent.class);
    }

    @Test
    public void testEquality() {
        new EqualsTester()
                .addEqualityGroup(OFAGENT, SAME_AS_OFAGENT_1, SAME_AS_OFAGENT_2)
                .addEqualityGroup(ANOTHER_OFAGENT)
                .testEquals();
    }
}
