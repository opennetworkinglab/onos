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

package org.onosproject.drivers.barefoot;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.net.pi.model.PiMeterId;
import org.onosproject.net.pi.runtime.PiMeterBand;
import org.onosproject.net.pi.runtime.PiMeterBandType;
import org.onosproject.net.pi.runtime.PiMeterCellConfig;
import org.onosproject.net.pi.runtime.PiMeterCellId;

import java.util.Map;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

/**
 * Test for TofinoMeterProgrammable behavior.
 */
public class TofinoMeterProgrammableTest {

    private TofinoMeterProgrammable meterProgrammable;
    private PiMeterCellId meterCellId = PiMeterCellId.ofIndirect(PiMeterId.of("foo"), 0);

    private static final Map<Long, Long> RATES = ImmutableMap.<Long, Long>builder()
            .put(125000L, 124500L)
            .put(244800L, 244375L)
            .put(300000L, 299125L)
            .build();
    private static final Map<Long, Long> WRONG_RATES = ImmutableMap.<Long, Long>builder()
            .put(12500L, 0L)
            .put(60000L, 0L)
            .put(124900L, 0L)
            .put(12501L, 12000L)
            .put(60001L, 58000L)
            .put(124901L, 122400L)
            .put(12502L, 12800L)
            .put(60002L, 62000L)
            .put(124902L, 128000L)
            .build();

    private static final Map<Long, Long> BURSTS = ImmutableMap.<Long, Long>builder()
            .put(1482L, 1375L)
            .put(7410L, 7375L)
            .put(14820L, 14750L)
            .build();

    private static final Map<Long, Long> WRONG_BURSTS = ImmutableMap.<Long, Long>builder()
            .put(1482L, 1374L)
            .put(7410L, 7249L)
            .put(14820L, 14624L)
            .put(1483L, 1376L)
            .put(7411L, 7376L)
            .put(14821L, 14751L)
            .build();

    @Before
    public void setup() {
        meterProgrammable = new TofinoMeterProgrammable();
    }

    /**
     * Test isRateSimilar check of the tofino behavior.
     */
    @Test
    public void testIsRateSimilar() {
        PiMeterBand onosMeterBand;
        PiMeterBand deviceMeterBand;
        PiMeterCellConfig onosMeter;
        PiMeterCellConfig deviceMeter;
        for (Map.Entry<Long, Long> entry : RATES.entrySet()) {
            onosMeterBand = new PiMeterBand(PiMeterBandType.COMMITTED, entry.getKey(), 0);
            deviceMeterBand = new PiMeterBand(PiMeterBandType.COMMITTED, entry.getValue(), 0);
            onosMeter = PiMeterCellConfig.builder()
                    .withMeterCellId(meterCellId)
                    .withMeterBand(onosMeterBand)
                    .withMeterBand(new PiMeterBand(PiMeterBandType.PEAK, 0, 0))
                    .build();
            deviceMeter = PiMeterCellConfig.builder()
                    .withMeterCellId(meterCellId)
                    .withMeterBand(deviceMeterBand)
                    .withMeterBand(new PiMeterBand(PiMeterBandType.PEAK, 0, 0))
                    .build();
            assertTrue(meterProgrammable.isSimilar(onosMeter, deviceMeter));
        }
    }

    /**
     * Test wrong isRateSimilar of the tofino behavior.
     */
    @Test
    public void testWrongIsRateSimilar() {
        PiMeterBand onosMeterBand;
        PiMeterBand deviceMeterBand;
        PiMeterCellConfig onosMeter;
        PiMeterCellConfig deviceMeter;
        for (Map.Entry<Long, Long> entry : WRONG_RATES.entrySet()) {
            onosMeterBand = new PiMeterBand(PiMeterBandType.COMMITTED, entry.getKey(), 0);
            deviceMeterBand = new PiMeterBand(PiMeterBandType.COMMITTED, entry.getValue(), 0);
            onosMeter = PiMeterCellConfig.builder()
                    .withMeterCellId(meterCellId)
                    .withMeterBand(onosMeterBand)
                    .withMeterBand(new PiMeterBand(PiMeterBandType.PEAK, 0, 0))
                    .build();
            deviceMeter = PiMeterCellConfig.builder()
                    .withMeterCellId(meterCellId)
                    .withMeterBand(deviceMeterBand)
                    .withMeterBand(new PiMeterBand(PiMeterBandType.PEAK, 0, 0))
                    .build();
            assertFalse(meterProgrammable.isSimilar(onosMeter, deviceMeter));
        }
    }

    /**
     * Test isBurstSimilar of the tofino behavior.
     */
    @Test
    public void testIsBurstSimilar() {
        PiMeterBand onosMeterBand;
        PiMeterBand deviceMeterBand;
        PiMeterCellConfig onosMeter;
        PiMeterCellConfig deviceMeter;
        for (Map.Entry<Long, Long> entry : BURSTS.entrySet()) {
            onosMeterBand = new PiMeterBand(PiMeterBandType.COMMITTED, 0, entry.getKey());
            deviceMeterBand = new PiMeterBand(PiMeterBandType.COMMITTED, 0, entry.getValue());
            onosMeter = PiMeterCellConfig.builder()
                    .withMeterCellId(meterCellId)
                    .withMeterBand(onosMeterBand)
                    .withMeterBand(new PiMeterBand(PiMeterBandType.PEAK, 0, 0))
                    .build();
            deviceMeter = PiMeterCellConfig.builder()
                    .withMeterCellId(meterCellId)
                    .withMeterBand(deviceMeterBand)
                    .withMeterBand(new PiMeterBand(PiMeterBandType.PEAK, 0, 0))
                    .build();
            assertTrue(meterProgrammable.isSimilar(onosMeter, deviceMeter));
        }
    }

    /**
     * Test wrong isBurstSimilar of the tofino behavior.
     */
    @Test
    public void testWrongIsBurstSimilar() {
        PiMeterBand onosMeterBand;
        PiMeterBand deviceMeterBand;
        PiMeterCellConfig onosMeter;
        PiMeterCellConfig deviceMeter;
        for (Map.Entry<Long, Long> entry : WRONG_BURSTS.entrySet()) {
            onosMeterBand = new PiMeterBand(PiMeterBandType.COMMITTED, 0, entry.getKey());
            deviceMeterBand = new PiMeterBand(PiMeterBandType.COMMITTED, 0, entry.getValue());
            onosMeter = PiMeterCellConfig.builder()
                    .withMeterCellId(meterCellId)
                    .withMeterBand(onosMeterBand)
                    .withMeterBand(new PiMeterBand(PiMeterBandType.PEAK, 0, 0))
                    .build();
            deviceMeter = PiMeterCellConfig.builder()
                    .withMeterCellId(meterCellId)
                    .withMeterBand(deviceMeterBand)
                    .withMeterBand(new PiMeterBand(PiMeterBandType.PEAK, 0, 0))
                    .build();
            assertFalse(meterProgrammable.isSimilar(onosMeter, deviceMeter));
        }
    }

}
