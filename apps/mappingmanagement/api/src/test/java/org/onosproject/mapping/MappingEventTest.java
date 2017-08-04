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
import org.junit.Before;
import org.junit.Test;
import org.onosproject.event.AbstractEventTest;
import org.onosproject.net.DeviceId;

import java.util.concurrent.TimeUnit;

/**
 * Unit tests for the MappingEvent class.
 */
public class MappingEventTest extends AbstractEventTest {

    private static final String DEVICE_ID_1 = "lisp:10.1.1.1";
    private static final String DEVICE_ID_2 = "lisp:10.1.1.2";

    private MappingEvent event1;
    private MappingEvent sameAsEvent1;
    private MappingEvent event2;

    /**
     * Creates a mock of mapping object from mappingId and deviceId.
     *
     * @param mid mapping identifier
     * @param did device identifier
     * @return mock of mapping object
     */
    private Mapping mockMapping(long mid, String did) {
        Mapping.Builder builder = new DefaultMapping.Builder();
        DeviceId deviceId = DeviceId.deviceId(did);

        return builder
                .withId(mid)
                .withKey(new MappingTestMocks.MockMappingKey())
                .withValue(new MappingTestMocks.MockMappingValue())
                .forDevice(deviceId)
                .build();
    }

    @Before
    public void setup() {
        final Mapping mapping1 = mockMapping(1, DEVICE_ID_1);
        final Mapping mapping2 = mockMapping(2, DEVICE_ID_2);

        event1 = new MappingEvent(MappingEvent.Type.MAPPING_ADDED, mapping1);
        sameAsEvent1 = new MappingEvent(MappingEvent.Type.MAPPING_ADDED, mapping1);
        event2 = new MappingEvent(MappingEvent.Type.MAPPING_ADD_REQUESTED, mapping2);
    }

    /**
     * Tests the equals, hashCode and toString methods using Guava EqualsTester.
     */
    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(event1)
                .addEqualityGroup(sameAsEvent1)
                .addEqualityGroup(event2)
                .testEquals();
    }

    /**
     * Tests the constructor where a time is passed in.
     */
    @Test
    public void testTimeConstructor() {
        final long time = 123L;
        final Mapping mapping = mockMapping(1, DEVICE_ID_1);
        final MappingEvent event =
                new MappingEvent(MappingEvent.Type.MAPPING_REMOVE_REQUESTED, mapping, time);
        validateEvent(event, MappingEvent.Type.MAPPING_REMOVE_REQUESTED, mapping, time);
    }

    /**
     * Tests creation of a MappingEvent.
     */
    @Test
    public void testConstructor() {
        final long time = System.currentTimeMillis();
        final Mapping mapping = mockMapping(1, DEVICE_ID_1);
        final MappingEvent event = new MappingEvent(MappingEvent.Type.MAPPING_UPDATED, mapping);
        validateEvent(event, MappingEvent.Type.MAPPING_UPDATED, mapping, time,
                time + TimeUnit.SECONDS.toMillis(30));
    }
}
