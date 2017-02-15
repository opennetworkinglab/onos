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
import org.onosproject.mapping.addresses.MappingAddress;
import org.onosproject.mapping.addresses.MappingAddresses;

import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Unit tests for the default mapping key class.
 */
public class DefaultMappingKeyTest {

    private static final String IP_ADDRESS_1 = "1.2.3.4/24";
    private static final String IP_ADDRESS_2 = "5.6.7.8/24";

    /**
     * Checks that the DefaultMappingKey class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultMappingKey.class);
    }

    /**
     * Tests equals() method.
     */
    @Test
    public void testEquals() {
        IpPrefix ip1 = IpPrefix.valueOf(IP_ADDRESS_1);
        MappingAddress address1 = MappingAddresses.ipv4MappingAddress(ip1);

        MappingKey key1 = DefaultMappingKey.builder()
                                .withAddress(address1)
                                .build();
        MappingKey sameAsKey1 = DefaultMappingKey.builder()
                                .withAddress(address1)
                                .build();

        IpPrefix ip2 = IpPrefix.valueOf(IP_ADDRESS_2);
        MappingAddress address2 = MappingAddresses.ipv4MappingAddress(ip2);

        MappingKey key2 = DefaultMappingKey.builder()
                                .withAddress(address2)
                                .build();

        new EqualsTester()
                .addEqualityGroup(key1, sameAsKey1)
                .addEqualityGroup(key2)
                .testEquals();
    }
}
