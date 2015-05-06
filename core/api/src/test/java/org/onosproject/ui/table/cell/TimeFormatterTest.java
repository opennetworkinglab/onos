/*
 * Copyright 2015 Open Networking Laboratory
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
 *
 */

package org.onosproject.ui.table.cell;

import org.joda.time.DateTime;
import org.junit.Test;
import org.onosproject.ui.table.CellFormatter;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link TimeFormatter}.
 */
public class TimeFormatterTest {

    private static final DateTime TIME = DateTime.parse("2010-06-30T01:20");
    private static final String EXP_OUTPUT = "1:20:00 AM PDT";

    private CellFormatter fmt = TimeFormatter.INSTANCE;

    @Test
    public void basic() {
        assertEquals("wrong format", EXP_OUTPUT, fmt.format(TIME));
    }
}
