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
package org.onosproject.incubator.net.l2monitoring.soam;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.junit.Test;
import org.onosproject.incubator.net.l2monitoring.soam.StartTime.StartTimeOption;
import org.onosproject.incubator.net.l2monitoring.soam.StopTime.StopTimeOption;

public class StartTimeTest {

    @Test
    public void testStartImmediate() {
        StartTime st = StartTime.immediate();
        assertEquals(StartTimeOption.IMMEDIATE, st.option());
        assertNull(st.relativeTime());
        assertNull(st.absoluteTime());
    }

    @Test
    public void testStartRelative() {
        StartTime st = StartTime.relative(Duration.ofMinutes(20));
        assertEquals(StartTimeOption.RELATIVE, st.option());
        assertEquals(20 * 60, st.relativeTime().getSeconds());
        assertNull(st.absoluteTime());
    }

    @Test
    public void testStartAbsolute() {
        StartTime st = StartTime.absolute(OffsetDateTime
                .of(2017, 3, 20, 11, 43, 11, 0, ZoneOffset.ofHours(-7))
                .toInstant());
        assertEquals(StartTimeOption.ABSOLUTE, st.option());
        assertNull(st.relativeTime());
        assertEquals("2017-03-20T11:43:11-07:00", st
                .absoluteTime()
                .atOffset(ZoneOffset.ofHours(-7))
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    }

    @Test
    public void testStopImmediate() {
        StopTime st = StopTime.none();
        assertEquals(StopTimeOption.NONE, st.option());
        assertNull(st.relativeTime());
        assertNull(st.absoluteTime());
    }

    @Test
    public void testStopRelative() {
        StopTime st = StopTime.relative(Duration.ofMinutes(20));
        assertEquals(StopTimeOption.RELATIVE, st.option());
        assertEquals(20 * 60, st.relativeTime().getSeconds());
        assertNull(st.absoluteTime());
    }

    @Test
    public void testStopAbsolute() {
        StopTime st = StopTime.absolute(OffsetDateTime
                .of(2017, 3, 20, 11, 43, 11, 0, ZoneOffset.ofHours(-7))
                .toInstant());
        assertEquals(StopTimeOption.ABSOLUTE, st.option());
        assertNull(st.relativeTime());
        assertEquals("2017-03-20T11:43:11-07:00", st
                .absoluteTime()
                .atOffset(ZoneOffset.ofHours(-7))
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    }

}
