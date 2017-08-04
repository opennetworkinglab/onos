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
package org.onosproject.mapping;

import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.onlab.packet.IpPrefix;
import org.onosproject.mapping.addresses.MappingAddress;
import org.onosproject.mapping.addresses.MappingAddresses;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Unit tests for the default mapping treatment class.
 */
public class DefaultMappingTreatmentTest {

    private static final String IP_ADDRESS_1 = "1.2.3.4/24";
    private static final String IP_ADDRESS_2 = "5.6.7.8/24";

    /**
     * Checks that the DefaultMappingTreatment class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultMappingTreatment.class);
    }

    /**
     * Tests method defined on the Builder.
     */
    @Test
    public void testBuilderMethods() {
        IpPrefix ip = IpPrefix.valueOf(IP_ADDRESS_1);
        MappingAddress address = MappingAddresses.ipv4MappingAddress(ip);

        MappingTreatment.Builder builder =
                DefaultMappingTreatment.builder()
                        .withAddress(address)
                        .setUnicastPriority(10)
                        .setUnicastWeight(10);
        MappingTreatment treatment = builder.build();
        assertThat(treatment.instructions(), hasSize(2));
    }

    /**
     * Tests illegal unicast type instruction construction.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testIllegalUnicastTypeConstruction() {
        IpPrefix ip = IpPrefix.valueOf(IP_ADDRESS_1);
        MappingAddress address = MappingAddresses.ipv4MappingAddress(ip);

        DefaultMappingTreatment.builder()
                .withAddress(address)
                .setUnicastPriority(10)
                .setUnicastWeight(10)
                .setUnicastPriority(20)
                .build();
    }

    /**
     * Tests illegal multicast type instruction construction.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testIllegalMulticastTypeConstruction() {
        IpPrefix ip = IpPrefix.valueOf(IP_ADDRESS_1);
        MappingAddress address = MappingAddresses.ipv4MappingAddress(ip);

        DefaultMappingTreatment.builder()
                .withAddress(address)
                .setMulticastPriority(10)
                .setMulticastWeight(10)
                .setMulticastPriority(20)
                .build();
    }

    /**
     * Tests equals() method.
     */
    @Test
    public void testEquals() {
        IpPrefix ip1 = IpPrefix.valueOf(IP_ADDRESS_1);
        MappingAddress address1 = MappingAddresses.ipv4MappingAddress(ip1);

        MappingTreatment treatment1 = DefaultMappingTreatment.builder()
                                    .withAddress(address1)
                                    .setUnicastPriority(10)
                                    .setUnicastWeight(10)
                                    .build();

        MappingTreatment sameAsTreatment1 = DefaultMappingTreatment.builder()
                                    .withAddress(address1)
                                    .setUnicastPriority(10)
                                    .setUnicastWeight(10)
                                    .build();

        IpPrefix ip2 = IpPrefix.valueOf(IP_ADDRESS_2);
        MappingAddress address2 = MappingAddresses.ipv4MappingAddress(ip2);

        MappingTreatment treatment2 = DefaultMappingTreatment.builder()
                                    .withAddress(address2)
                                    .setMulticastPriority(20)
                                    .setMulticastWeight(20)
                                    .build();

        new EqualsTester()
                .addEqualityGroup(treatment1, sameAsTreatment1)
                .addEqualityGroup(treatment2)
                .testEquals();
    }
}
