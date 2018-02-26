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
package org.onosproject.incubator.net.l2monitoring.soam.loss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;
import org.onosproject.incubator.net.l2monitoring.cfm.Mep.Priority;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;
import org.onosproject.incubator.net.l2monitoring.soam.MilliPct;
import org.onosproject.incubator.net.l2monitoring.soam.SoamConfigException;
import org.onosproject.incubator.net.l2monitoring.soam.SoamId;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementCreate.Version;
import org.onosproject.incubator.net.l2monitoring.soam.loss.LossMeasurementCreate.CounterOption;
import org.onosproject.incubator.net.l2monitoring.soam.loss.LossMeasurementCreate.LmType;
import org.onosproject.incubator.net.l2monitoring.soam.loss.LossMeasurementEntry.AvailabilityType;
import org.onosproject.incubator.net.l2monitoring.soam.loss.LossMeasurementEntry.LmEntryBuilder;

public class LossMeasurementEntryTest {

    LossMeasurementEntry lme1;

    @Before
    public void setUp() throws SoamConfigException {
        LmEntryBuilder builder = DefaultLmEntry.builder(
                Version.Y17312008, MepId.valueOf((short) 10),
                Priority.PRIO3, LmType.LMLMM, SoamId.valueOf(1))
                .measuredAvailabilityBackwardStatus(AvailabilityType.UNAVAILABLE)
                .measuredAvailabilityForwardStatus(AvailabilityType.UNKNOWN)
                .measuredBackwardFlr(MilliPct.ofMilliPct(1600))
                .measuredBackwardLastTransitionTime(Instant.ofEpochSecond(123456L))
                .measuredForwardFlr(MilliPct.ofMilliPct(1601))
                .measuredForwardLastTransitionTime(Instant.ofEpochSecond(123457L))

                .availabilityCurrent(DefaultLaStatCurrent.builder(
                        Duration.ofMinutes(9), false, Instant.ofEpochSecond(9876543))
                        .build())
                .measurementCurrent(DefaultLmStatCurrent.builder(
                        Duration.ofMinutes(10), true, Instant.ofEpochSecond(9876544))
                        .build())

                .addToAvailabilityHistories(DefaultLaStatHistory.builder(
                        Duration.ofMinutes(11), true, SoamId.valueOf(10),
                        Instant.ofEpochSecond(9876545))
                        .build())
                .addToAvailabilityHistories(DefaultLaStatHistory.builder(
                        Duration.ofMinutes(12), true, SoamId.valueOf(11),
                        Instant.ofEpochSecond(9876546))
                        .build())

                .addToMeasurementHistories(DefaultLmStatHistory.builder(
                        Duration.ofMinutes(13), true, SoamId.valueOf(12),
                        Instant.ofEpochSecond(9876547))
                        .build())
                .addToMeasurementHistories(DefaultLmStatHistory.builder(
                        Duration.ofMinutes(14), true, SoamId.valueOf(13),
                        Instant.ofEpochSecond(9876548))
                        .build());

        builder = (LmEntryBuilder) builder
                .addToCountersEnabled(CounterOption.AVAILABILITY_BACKWARD_CONSECUTIVE_HIGH_LOSS)
                .addToCountersEnabled(CounterOption.AVAILABILITY_FORWARD_MAX_FLR)
                .alignMeasurementIntervals(true)
                .frameSize((short) 100);

        lme1 = builder.build();
    }

    @Test
    public void testLmId() {
        assertEquals(1, lme1.lmId().id().intValue());
    }

    @Test
    public void testMeasuredForwardFlr() {
        assertEquals(1601, lme1.measuredForwardFlr().intValue());
    }

    @Test
    public void testMeasuredBackwardFlr() {
        assertEquals(1600, lme1.measuredBackwardFlr().intValue());
    }

    @Test
    public void testMeasuredAvailabilityForwardStatus() {
        assertEquals(AvailabilityType.UNKNOWN, lme1.measuredAvailabilityForwardStatus());
    }

    @Test
    public void testMeasuredAvailabilityBackwardStatus() {
        assertEquals(AvailabilityType.UNAVAILABLE, lme1.measuredAvailabilityBackwardStatus());
    }

    @Test
    public void testMeasuredForwardLastTransitionTime() {
        assertEquals(123457L, lme1.measuredForwardLastTransitionTime().getEpochSecond());
    }

    @Test
    public void testMeasuredBackwardLastTransitionTime() {
        assertEquals(123456L, lme1.measuredBackwardLastTransitionTime().getEpochSecond());
    }

    @Test
    public void testMeasurementCurrent() {
        assertEquals(10, lme1.measurementCurrent().elapsedTime().toMinutes());
    }

    @Test
    public void testMeasurementHistories() {
        assertEquals(2, lme1.measurementHistories().size());
        ArrayList<LossMeasurementStatHistory> histories = new ArrayList<>();
        lme1.measurementHistories().forEach(histories::add);
        assertEquals(14, histories.get(1).elapsedTime().toMinutes());
    }

    @Test
    public void testAvailabilityCurrent() {
        assertEquals(9, lme1.availabilityCurrent().elapsedTime().toMinutes());
    }

    @Test
    public void testAvailabilityHistories() {
        assertEquals(2, lme1.measurementHistories().size());
        ArrayList<LossAvailabilityStatHistory> histories = new ArrayList<>();
        lme1.availabilityHistories().forEach(histories::add);
        assertEquals(11, histories.get(0).elapsedTime().toMinutes());
    }

    @Test
    public void testLmCfgType() {
        assertEquals(LmType.LMLMM, lme1.lmCfgType());
    }

    @Test
    public void testAlignMeasurementIntervals() {
        assertTrue(lme1.alignMeasurementIntervals());
    }
}
