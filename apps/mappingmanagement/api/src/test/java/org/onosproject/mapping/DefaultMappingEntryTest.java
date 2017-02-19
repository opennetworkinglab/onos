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
import org.onosproject.net.DeviceId;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for the default mapping entry class.
 */
public class DefaultMappingEntryTest {

    private static final MappingKey MAPPING_KEY =
                         new MappingTestMocks.MockMappingKey();
    private static final MappingValue MAPPING_VALUE =
                         new MappingTestMocks.MockMappingValue();

    private final MappingEntry entry1 = makeMappingEntry(1);
    private final MappingEntry sameAsEntry1 = makeMappingEntry(1);
    private final MappingEntry entry2 = makeMappingEntry(2);

    /**
     * Creates a new mapping entry from an unique value.
     *
     * @param value unique value
     * @return a new mapping entry
     */
    private static DefaultMappingEntry makeMappingEntry(int value) {
        Mapping mapping = new DefaultMapping.Builder()
                                .forDevice(DeviceId.deviceId("lisp:10.1.1." +
                                                    Integer.toString(value)))
                                .withKey(MAPPING_KEY)
                                .withValue(MAPPING_VALUE)
                                .withId(value)
                                .build();

        return new DefaultMappingEntry(mapping, MappingEntry.MappingEntryState.ADDED);
    }

    /**
     * Tests the equals, hashCode and toString methods using Guava EqualsTester.
     */
    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(entry1, sameAsEntry1)
                .addEqualityGroup(entry2)
                .testEquals();
    }

    /**
     * Tests creation of a DefaultMappingEntry using a mapping builder.
     */
    @Test
    public void testConstruction() {
        DefaultMappingEntry entry = (DefaultMappingEntry) entry1;

        assertThat(entry.deviceId(), is(DeviceId.deviceId("lisp:10.1.1.1")));
        assertThat(entry.id(), is(MappingId.valueOf(1)));
        assertThat(entry.key(), is(MAPPING_KEY));
        assertThat(entry.value(), is(MAPPING_VALUE));
    }
}
