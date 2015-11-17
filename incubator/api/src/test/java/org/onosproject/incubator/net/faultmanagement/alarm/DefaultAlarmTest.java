/*
 * Copyright 2014 Open Networking Laboratory
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;
import org.onosproject.net.DeviceId;

public class DefaultAlarmTest {

    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultAlarm.class);
    }

    /**
     * Checks the construction of a DefaultAlarm object.
     */
    @Test
    public void testConstruction() {
        final String nameValue = "name3";
        final DefaultAlarm a = new DefaultAlarm.Builder(AlarmId.valueOf(4),
                DeviceId.NONE, nameValue, Alarm.SeverityLevel.CLEARED, 3).build();

        assertThat(a, is(notNullValue()));
        final DefaultAlarm b = new DefaultAlarm.Builder(a).build();

        assertEquals(a, b);
    }
}
