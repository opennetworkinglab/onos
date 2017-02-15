/*
 * Copyright 2017-present Open Networking Laboratory
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
import org.onosproject.mapping.actions.MappingAction;
import org.onosproject.mapping.actions.MappingActions;
import org.onosproject.mapping.addresses.MappingAddress;
import org.onosproject.mapping.addresses.MappingAddresses;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Unit tests for the default mapping value class.
 */
public class DefaultMappingValueTest {

    private static final String IP_ADDRESS_1 = "1.2.3.4/24";
    private static final String IP_ADDRESS_2 = "5.6.7.8/24";
    private final IpPrefix ip1 = IpPrefix.valueOf(IP_ADDRESS_1);
    private final IpPrefix ip2 = IpPrefix.valueOf(IP_ADDRESS_2);
    private final MappingAddress ma1 = MappingAddresses.ipv4MappingAddress(ip1);
    private final MappingAddress ma2 = MappingAddresses.ipv4MappingAddress(ip2);

    /**
     * Checks that the DefaultMappingValue class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultMappingValue.class);
    }

    /**
     * Tests methods defined on the Builder.
     */
    @Test
    public void testBuilderMethods() {
        MappingAction action = MappingActions.noAction();
        MappingTreatment treatment = DefaultMappingTreatment.builder()
                                        .withAddress(ma1)
                                        .setUnicastPriority(10)
                                        .setUnicastWeight(10)
                                        .build();

        MappingValue value = DefaultMappingValue.builder()
                                        .withAction(action)
                                        .add(treatment)
                                        .build();

        assertThat(value.action(), is(action));
        assertThat(value.treatments(), hasSize(1));
    }

    /**
     * Tests equals() method.
     */
    @Test
    public void testEquals() {
        MappingTreatment treatment1 = DefaultMappingTreatment.builder()
                                                .withAddress(ma1)
                                                .setUnicastPriority(10)
                                                .setUnicastWeight(10)
                                                .build();

        MappingTreatment treatment2 = DefaultMappingTreatment.builder()
                                                .withAddress(ma2)
                                                .setUnicastPriority(20)
                                                .setUnicastWeight(20)
                                                .build();

        MappingAction noAction = MappingActions.noAction();
        MappingAction forward = MappingActions.forward();

        MappingValue value1 = DefaultMappingValue.builder()
                                        .withAction(noAction)
                                        .add(treatment1)
                                        .build();

        MappingValue sameAsValue1 = DefaultMappingValue.builder()
                                        .withAction(noAction)
                                        .add(treatment1)
                                        .build();

        MappingValue value2 = DefaultMappingValue.builder()
                                        .withAction(forward)
                                        .add(treatment2)
                                        .build();

        new EqualsTester()
                .addEqualityGroup(value1, sameAsValue1)
                .addEqualityGroup(value2)
                .testEquals();
    }
}
