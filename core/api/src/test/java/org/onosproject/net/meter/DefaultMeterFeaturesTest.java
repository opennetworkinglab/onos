/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.net.meter;

import org.junit.Before;
import org.junit.Test;
import org.onosproject.net.DeviceId;

import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * DefaultMeterFeatures Tests.
 */
public class DefaultMeterFeaturesTest {

    private MeterFeatures mf;
    private DeviceId did = DeviceId.deviceId("foo:foo");
    private short band = 2;
    private short color = 3;
    private Set<Band.Type> types;
    private Set<Meter.Unit> units;
    private MeterScope globalScope = MeterScope.globalScope();
    private MeterScope fooScope = MeterScope.of("foo");

    @Before
    public void setup() {
        types = Set.of(Band.Type.DROP);
        units = Set.of(Meter.Unit.KB_PER_SEC);
    }

    @Test
    public void testZeroMaxMeter() {
        mf = DefaultMeterFeatures.builder()
             .forDevice(did)
             .withMaxMeters(0L)
             .withScope(globalScope)
             .withMaxBands(band)
             .withMaxColors(color)
             .withBandTypes(types)
             .withUnits(units)
             .hasBurst(true)
             .hasStats(true).build();

        assertEquals(-1, mf.startIndex());
        assertEquals(-1, mf.endIndex());
        assertEquals(0L, mf.maxMeter());
    }

    @Test
    public void testOfMaxMeter() {
        mf = DefaultMeterFeatures.builder()
             .forDevice(did)
             .withMaxMeters(1024L)
             .withScope(globalScope)
             .withMaxBands(band)
             .withMaxColors(color)
             .withBandTypes(types)
             .withUnits(units)
             .hasBurst(true)
             .hasStats(true).build();

        assertEquals(1L, mf.startIndex());
        assertEquals(1024L, mf.endIndex());
        assertEquals(1024L, mf.maxMeter());
    }

    @Test
    public void testNonOfMaxMeter() {
        mf = DefaultMeterFeatures.builder()
             .forDevice(did)
             .withMaxMeters(1024L)
             .withScope(fooScope)
             .withMaxBands(band)
             .withMaxColors(color)
             .withBandTypes(types)
             .withUnits(units)
             .hasBurst(true)
             .hasStats(true).build();

        assertEquals(0L, mf.startIndex());
        assertEquals(1023L, mf.endIndex());
        assertEquals(1024L, mf.maxMeter());
    }
}
