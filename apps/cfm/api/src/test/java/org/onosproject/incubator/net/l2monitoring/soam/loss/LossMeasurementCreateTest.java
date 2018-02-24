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

import java.time.Duration;

import org.junit.Before;
import org.junit.Test;
import org.onosproject.incubator.net.l2monitoring.cfm.Mep.Priority;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;
import org.onosproject.incubator.net.l2monitoring.soam.MeasurementCreateBase.SessionType;
import org.onosproject.incubator.net.l2monitoring.soam.MilliPct;
import org.onosproject.incubator.net.l2monitoring.soam.SoamConfigException;
import org.onosproject.incubator.net.l2monitoring.soam.SoamId;
import org.onosproject.incubator.net.l2monitoring.soam.StartTime;
import org.onosproject.incubator.net.l2monitoring.soam.StopTime;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementCreate.DataPattern;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementCreate.TestTlvPattern;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementCreate.Version;
import org.onosproject.incubator.net.l2monitoring.soam.loss.LossMeasurementCreate.CounterOption;
import org.onosproject.incubator.net.l2monitoring.soam.loss.LossMeasurementCreate.LmCreateBuilder;
import org.onosproject.incubator.net.l2monitoring.soam.loss.LossMeasurementCreate.LmType;

import static org.junit.Assert.assertEquals;

public class LossMeasurementCreateTest {

    LossMeasurementCreate lmc1;

    @Before
    public void setUp() throws SoamConfigException {

        LossMeasurementThreshold lmt1 = DefaultLmThreshold
                .builder(SoamId.valueOf(4)).build();


        LmCreateBuilder builder = (LmCreateBuilder) DefaultLmCreate
                .builder(Version.Y17312008, MepId.valueOf((short) 10),
                Priority.PRIO3, LmType.LMLMM)
                .addToCountersEnabled(CounterOption.AVAILABILITY_FORWARD_AVERAGE_FLR)
                .addToCountersEnabled(CounterOption.AVAILABILITY_FORWARD_CONSECUTIVE_HIGH_LOSS)
                .availabilityFlrThreshold(MilliPct.ofRatio(0.201f))
                .availabilityMeasurementInterval(Duration.ofSeconds(5))
                .availabilityNumberConsecutiveFlrMeasurements(6)
                .availabilityNumberConsecutiveHighFlr((short) 7)
                .availabilityNumberConsecutiveIntervals((short) 8)
                .addToLossMeasurementThreshold(lmt1)
                .frameSize((short) 100)
                .dataPattern(DataPattern.ZEROES)
                .testTlvIncluded(true)
                .testTlvPattern(TestTlvPattern.NULL_SIGNAL_WITHOUT_CRC_32)
                .messagePeriod(Duration.ofMinutes(9))
                .measurementInterval(Duration.ofMinutes(10))
                .numberIntervalsStored((short) 11)
                .alignMeasurementIntervals(true)
                .alignMeasurementOffset(Duration.ofSeconds(12))
                .startTime(StartTime.immediate())
                .stopTime(StopTime.none())
                .sessionType(SessionType.PROACTIVE);

        lmc1 = builder.build();
    }

    @Test
    public void testLmCfgType() {
        assertEquals(LmType.LMLMM, lmc1.lmCfgType());
    }

    @Test
    public void testCountersEnabled() {
        assertEquals(2, lmc1.countersEnabled().size());
    }

    @Test
    public void testAvailabilityMeasurementInterval() {
        assertEquals(5, lmc1.availabilityMeasurementInterval().getSeconds());
    }

    @Test
    public void testAvailabilityNumberConsecutiveFlrMeasurements() {
        assertEquals(6, lmc1.availabilityNumberConsecutiveFlrMeasurements().intValue());
    }

    @Test
    public void testAvailabilityFlrThreshold() {
        assertEquals(0.201f, lmc1.availabilityFlrThreshold().ratioValue(), 0.0001f);
    }

    @Test
    public void testAvailabilityNumberConsecutiveIntervals() {
        assertEquals(8, lmc1.availabilityNumberConsecutiveIntervals().shortValue());
    }

    @Test
    public void testAvailabilityNumberConsecutiveHighFlr() {
        assertEquals(7, lmc1.availabilityNumberConsecutiveHighFlr().shortValue());
    }

    @Test
    public void testLossMeasurementThreshold() {
        assertEquals(1, lmc1.lossMeasurementThreshold().size());
    }

    @Test
    public void testVersion() {
        assertEquals(Version.Y17312008, lmc1.version());
    }

    @Test
    public void testRemoteMepId() {
        assertEquals(10, lmc1.remoteMepId().id().shortValue());
    }

    @Test
    public void testMessagePeriod() {
        assertEquals(9, lmc1.messagePeriod().toMinutes());
    }

    @Test
    public void testPriority() {
        assertEquals(Priority.PRIO3, lmc1.priority());
    }

    @Test
    public void testFrameSize() {
        assertEquals(100, lmc1.frameSize().shortValue());
    }

    @Test
    public void testDataPattern() {
        assertEquals(DataPattern.ZEROES, lmc1.dataPattern());
    }

    @Test
    public void testTestTlvIncluded() {
        assertEquals(true, lmc1.testTlvIncluded());
    }

    @Test
    public void testTestTlvPattern() {
        assertEquals(TestTlvPattern.NULL_SIGNAL_WITHOUT_CRC_32, lmc1.testTlvPattern());
    }

    @Test
    public void testMeasurementInterval() {
        assertEquals(10, lmc1.measurementInterval().toMinutes());
    }

    @Test
    public void testNumberIntervalsStored() {
        assertEquals(11, lmc1.numberIntervalsStored().shortValue());
    }

    @Test
    public void testAlignMeasurementIntervals() {
        assertEquals(true, lmc1.alignMeasurementIntervals());
    }

    @Test
    public void testAlignMeasurementOffset() {
        assertEquals(12, lmc1.alignMeasurementOffset().getSeconds());
    }

    @Test
    public void testStartTime() {
        assertEquals(StartTime.immediate().option(),
                lmc1.startTime().option());
    }

    @Test
    public void testStopTime() {
        assertEquals(StopTime.none().option(),
                lmc1.stopTime().option());
    }

    @Test
    public void testSessionType() {
        assertEquals(SessionType.PROACTIVE, lmc1.sessionType());
    }

}
