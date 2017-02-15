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
import org.junit.Before;
import org.junit.Test;
import org.onosproject.net.DeviceId;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutableBaseClass;

/**
 * Unit tests for the default mapping class.
 */
public class DefaultMappingTest {

    private Mapping mapping1;
    private Mapping sameAsMapping1;
    private Mapping mapping2;

    @Before
    public void setup() {

        Mapping.Builder builder1 = new DefaultMapping.Builder();
        DeviceId deviceId1 = DeviceId.deviceId("lisp:10.1.1.1");
        MappingKey mappingKey1 = new MappingTestMocks.MockMappingKey();
        MappingValue mappingValue1 = new MappingTestMocks.MockMappingValue();

        mapping1 = builder1
                        .withId(1)
                        .withKey(mappingKey1)
                        .withValue(mappingValue1)
                        .forDevice(deviceId1)
                        .build();

        Mapping.Builder builder2 = new DefaultMapping.Builder();

        sameAsMapping1 = builder2
                        .withId(1)
                        .withKey(mappingKey1)
                        .withValue(mappingValue1)
                        .forDevice(deviceId1)
                        .build();

        Mapping.Builder builder3 = new DefaultMapping.Builder();
        DeviceId deviceId2 = DeviceId.deviceId("lisp:10.1.1.2");

        mapping2 = builder3
                        .withId(2)
                        .withKey(new MappingTestMocks.MockMappingKey())
                        .withValue(new MappingTestMocks.MockMappingValue())
                        .forDevice(deviceId2)
                        .build();
    }

    /**
     * Checks that the DefaultMapping class is immutable but can be inherited from.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutableBaseClass(DefaultMapping.class);
    }

    /**
     * Tests the equals, hashCode and toString methods using Guava EqualsTester.
     */
    @Test
    public void testEquality() {
        new EqualsTester()
                .addEqualityGroup(mapping1, sameAsMapping1)
                .addEqualityGroup(mapping2).testEquals();
    }

    /**
     * Tests creation of a DefaultMapping using a mapping builder.
     */
    @Test
    public void testConstruction() {
        DefaultMapping mapping = (DefaultMapping) mapping1;

        assertThat(mapping.deviceId(), is(DeviceId.deviceId("lisp:10.1.1.1")));
        assertThat(mapping.id(), is(MappingId.valueOf(1)));
    }
}
