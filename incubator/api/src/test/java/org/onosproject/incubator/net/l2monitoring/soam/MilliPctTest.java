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

import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.junit.Test;

public class MilliPctTest {

    @Test
    public void testIntValue() {
        assertEquals(100, MilliPct.ofMilliPct(100).intValue());
        assertEquals(-100, MilliPct.ofMilliPct(-100).intValue());
    }

    @Test
    public void testLongValue() {
        assertEquals(100, MilliPct.ofMilliPct(100).longValue());
        assertEquals(-100, MilliPct.ofMilliPct(-100).longValue());
    }

    @Test
    public void testFloatValue() {
        assertEquals(100f, MilliPct.ofMilliPct(100).floatValue(), 0.0001f);
        assertEquals(-100f, MilliPct.ofMilliPct(-100).floatValue(), 0.0001f);
    }

    @Test
    public void testDoubleValue() {
        assertEquals(100f, MilliPct.ofMilliPct(100).doubleValue(), 0.0001f);
        assertEquals(-100f, MilliPct.ofMilliPct(-100).doubleValue(), 0.0001f);
    }

    @Test
    public void testOfPercent() {
        assertEquals(63563, MilliPct.ofPercent(63.563f).intValue());
        assertEquals(-63563, MilliPct.ofPercent(-63.563f).intValue());
    }

    @Test
    public void testOfRatio() {
        assertEquals(43211, MilliPct.ofRatio(0.43211f).intValue());
        assertEquals(-43211, MilliPct.ofRatio(-0.43211f).intValue());
    }

    @Test
    public void testPercentValue() {
        assertEquals(0.1f, MilliPct.ofMilliPct(100).percentValue(), 0.0001f);
        assertEquals(-0.1f, MilliPct.ofMilliPct(-100).percentValue(), 0.0001f);
    }

    @Test
    public void testRatioValue() {
        assertEquals(0.002f, MilliPct.ofMilliPct(200).ratioValue(), 0.0001f);
        assertEquals(-0.002f, MilliPct.ofMilliPct(-200).ratioValue(), 0.0001f);
    }

    @Test
    public void testToString() {
        NumberFormat pf = DecimalFormat.getPercentInstance(); //Varies by machine
        pf.setMaximumFractionDigits(3);
        assertEquals(pf.format(0.12345f), MilliPct.ofMilliPct(12345).toString());
        assertEquals(pf.format(-0.12345f), MilliPct.ofMilliPct(-12345).toString());
    }

}
