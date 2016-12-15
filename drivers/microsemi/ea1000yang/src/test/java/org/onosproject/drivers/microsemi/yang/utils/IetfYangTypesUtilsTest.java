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
package org.onosproject.drivers.microsemi.yang.utils;

import static org.junit.Assert.assertEquals;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev20130715.ietfyangtypes.DateAndTime;

public class IetfYangTypesUtilsTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testFromYangDateTime() {
        OffsetDateTime odt = IetfYangTypesUtils.fromYangDateTime(DateAndTime.fromString("2015-07-08T12:49:20.9Z"));
        assertEquals(OffsetDateTime.of(2015, 7, 8, 12, 49, 20, 900000000, ZoneOffset.UTC), odt);
    }

    @Test
    public void testFromYangDateTimeZoned() {
        ZonedDateTime zdt = IetfYangTypesUtils.fromYangDateTimeZoned(DateAndTime.fromString("2015-07-08T12:49:20.9Z"),
                ZoneId.systemDefault());

        assertEquals(OffsetDateTime.of(2015, 7, 8, 12, 49, 20, 900000000, ZoneOffset.UTC).toInstant(),
                zdt.toOffsetDateTime().toInstant());
    }

    @Test
    public void testFromYangDateTimeToLocal() {
        LocalDateTime ldt = IetfYangTypesUtils
                .fromYangDateTimeToLocal(DateAndTime.fromString("2015-07-08T12:49:20.9Z"));

        OffsetDateTime odtExpected = OffsetDateTime.of(2015, 7, 8, 12, 49, 20, 900000000, ZoneOffset.UTC);
        ZoneOffset offsetForcurrentZone = ZoneId.systemDefault().getRules().getOffset(ldt);
        assertEquals(odtExpected.toInstant(), ldt.toInstant(offsetForcurrentZone));
    }

}
