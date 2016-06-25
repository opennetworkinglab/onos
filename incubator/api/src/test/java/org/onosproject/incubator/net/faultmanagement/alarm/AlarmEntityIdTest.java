/*
 * Copyright 2015-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.incubator.net.faultmanagement.alarm;

import com.google.common.testing.EqualsTester;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.onosproject.incubator.net.faultmanagement.alarm.AlarmEntityId.alarmEntityId;

/**
 * Test of the alarm source identifier.
 *
 */
public class AlarmEntityIdTest {

    /**
     * Checks that the class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(AlarmEntityId.class);
    }

    @Test
    public void string() {
        assertEquals("och:foo",
                alarmEntityId("och:foo").toString());
    }

    @Test
    public void basics() {
        new EqualsTester()
                .addEqualityGroup(
                        alarmEntityId("och:foo"),
                        alarmEntityId("och:foo"))
                .addEqualityGroup(alarmEntityId("och:bar"))
                .testEquals();

    }

    @Test
    public void validSchemaPermitted() {
        alarmEntityId("none:foo");
        alarmEntityId("port:foo");
        alarmEntityId("och:foo");
        alarmEntityId("other:foo");

    }

    @Test(expected = IllegalArgumentException.class)
    public void verifyUnexpectedSchemaRejected() {
        alarmEntityId("junk:foo");
    }

    @Test(expected = IllegalArgumentException.class)
    public void verifyCorruptSchemaRejected() {
        alarmEntityId("other:");
    }

}
