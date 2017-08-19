/*
 * Copyright 2015-present Open Networking Foundation
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

package org.onosproject.ui.table.cell;

import org.junit.Test;
import org.onosproject.ui.table.CellFormatter;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

import static org.hamcrest.Matchers.isOneOf;
import static org.junit.Assert.assertThat;

/**
 * Unit tests for {@link TimeFormatter}.
 */
public class TimeFormatterTest {

    private static final Locale LOCALE = Locale.ENGLISH;
    private static final ZoneOffset ZONE = ZoneOffset.UTC;

    private static final OffsetDateTime TIME = OffsetDateTime.of(2015, 5, 4, 15, 30, 0, 0, ZONE);
    private static final String EXP_ZONE_NAME = "3:30:00 PM UTC";
    private static final String EXP_ZONE_OFFSET = "3:30:00 PM +00:00";

    // Have to use explicit Locale and TimeZone for the unit test, so that
    //  irrespective of which locale and time zone the test is run in, it
    //  always produces the same result...
    private CellFormatter fmt =
            new TimeFormatter().withLocale(LOCALE).withZone(ZONE);

    @Test
    public void basic() {
        assertThat(fmt.format(TIME), isOneOf(EXP_ZONE_OFFSET, EXP_ZONE_NAME));
    }
}
