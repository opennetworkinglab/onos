/*
 * Copyright 2015-present Open Networking Laboratory
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

package org.onlab.util;

import static org.junit.Assert.*;
import static org.onlab.util.PositionalParameterStringFormatter.format;

import org.junit.Test;

public class PositionalParameterStringFormatterTest {

    @Test
    public void testFormat0() {
        String fmt = "Some string 1 2 3";
        assertEquals("Some string 1 2 3", format(fmt));
    }

    @Test
    public void testFormat1() {
        String fmt = "Some string {} 2 3";
        assertEquals("Some string 1 2 3", format(fmt, 1));
    }

    @Test
    public void testFormat2() {
        String fmt = "Some string {} 2 {}";
        assertEquals("Some string 1 2 3", format(fmt, 1, "3"));
    }

    @Test
    public void testFormatNull() {
        String fmt = "Some string {} 2 {}";
        assertEquals("Some string 1 2 null", format(fmt, 1, null));
    }

    @Test
    public void testFormatExtraBracket() {
        String fmt = "Some string {} 2 {}";
        assertEquals("Some string 1 2 {}", format(fmt, 1));
    }

    @Test
    public void testFormatMissingBracket() {
        String fmt = "Some string 1 2 3";
        assertEquals("Some string 1 2 3", format(fmt, 7));
    }
}
