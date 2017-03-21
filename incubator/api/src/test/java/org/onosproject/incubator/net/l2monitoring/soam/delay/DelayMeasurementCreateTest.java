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
package org.onosproject.incubator.net.l2monitoring.soam.delay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.Duration;
import java.time.OffsetDateTime;

import org.junit.Before;
import org.junit.Test;
import org.onosproject.incubator.net.l2monitoring.cfm.Mep.Priority;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;
import org.onosproject.incubator.net.l2monitoring.soam.MeasurementCreateBase.SessionType;
import org.onosproject.incubator.net.l2monitoring.soam.SoamConfigException;
import org.onosproject.incubator.net.l2monitoring.soam.SoamId;
import org.onosproject.incubator.net.l2monitoring.soam.StartTime;
import org.onosproject.incubator.net.l2monitoring.soam.StartTime.StartTimeOption;
import org.onosproject.incubator.net.l2monitoring.soam.StopTime;
import org.onosproject.incubator.net.l2monitoring.soam.StopTime.StopTimeOption;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DefaultDelayMeasurementCreate.DefaultDmCreateBuilder;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementCreate.DataPattern;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementCreate.DmType;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementCreate.MeasurementOption;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementCreate.TestTlvPattern;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementCreate.Version;

public class DelayMeasurementCreateTest {

    DelayMeasurementCreate dm1;

    @Before
    public void setUp() throws Exception, CfmConfigException, SoamConfigException {

        DelayMeasurementThreshold dmT1 = DefaultDelayMeasurementThreshold
                .builder(SoamId.valueOf(1))
                .averageFrameDelayBackward(Duration.ofMillis(123))
                .averageInterFrameDelayVariationForward(Duration.ofMillis(321))
                .build();

        DelayMeasurementThreshold dmT2 = DefaultDelayMeasurementThreshold
                .builder(SoamId.valueOf(2))
                .averageFrameDelayBackward(Duration.ofMillis(456))
                .averageInterFrameDelayVariationForward(Duration.ofMillis(654))
                .build();

        try {
            DefaultDmCreateBuilder builder = (DefaultDmCreateBuilder)
                    DefaultDelayMeasurementCreate.builder(
            DmType.DMDMM, Version.Y17312011, MepId.valueOf((short) 12), Priority.PRIO6)
            .addToMeasurementsEnabled(MeasurementOption.FRAME_DELAY_FORWARD_MIN)
            .addToMeasurementsEnabled(MeasurementOption.FRAME_DELAY_FORWARD_AVERAGE)
            .addToMeasurementsEnabled(MeasurementOption.FRAME_DELAY_FORWARD_MAX)
            .addToMeasurementsEnabled(MeasurementOption.FRAME_DELAY_FORWARD_BINS)
            .binsPerFdInterval((short) 8)
            .binsPerIfdvInterval((short) 9)
            .ifdvSelectionOffset((short) 10)
            .binsPerFdrInterval((short) 12)
            .addToThresholds(dmT1)
            .addToThresholds(dmT2)
            .messagePeriod(Duration.ofMillis(100L))
            .frameSize((short) 64)
            .dataPattern(DataPattern.ONES)
            .testTlvIncluded(true)
            .testTlvPattern(TestTlvPattern.NULL_SIGNAL_WITHOUT_CRC_32)
            .measurementInterval(Duration.ofMinutes(15))
            .numberIntervalsStored((short) 32)
            .alignMeasurementIntervals(true)
            .alignMeasurementOffset(Duration.ofMinutes(4))
            .sessionType(SessionType.ONDEMAND)
            .startTime(StartTime.relative(Duration.ofMinutes(7)))
            .stopTime(StopTime.relative(Duration.ofMinutes(8)));
            dm1 = builder.build();
        } catch (SoamConfigException e) {
            throw new Exception(e);
        }
    }

    @Test
    public void testInvalidMessagePeriod() throws CfmConfigException {
        try {
            DefaultDelayMeasurementCreate.builder(
                    DmType.DMDMM, Version.Y17312011, MepId.valueOf((short) 20),
                    Priority.PRIO6)
                    .messagePeriod(Duration.ofMinutes(61));
            fail("Expected an exception to be thrown for invalid messagePeriod: " + 3660000);
        } catch (SoamConfigException e) {
            assertEquals(SoamConfigException.class, e.getClass());
        }
    }

    @Test
    public void testInvalidFrameSize() throws CfmConfigException {
        try {
            DefaultDelayMeasurementCreate.builder(
                    DmType.DMDMM, Version.Y17312011, MepId.valueOf((short) 20),
                    Priority.PRIO6)
                    .frameSize((short) 11111);
            fail("Expected an exception to be thrown for frame size: " + 11111);
        } catch (SoamConfigException e) {
            assertEquals(SoamConfigException.class, e.getClass());
        }
    }

    @Test
    public void testInvalidMeasurementInterval() throws CfmConfigException {
        try {
            DefaultDelayMeasurementCreate.builder(
                    DmType.DMDMM, Version.Y17312011, MepId.valueOf((short) 20),
                    Priority.PRIO6)
                    .measurementInterval(Duration.ofMinutes(0));
            fail("Expected an exception to be thrown for invalid measurementInterval: " + 0);
        } catch (SoamConfigException e) {
            assertEquals(SoamConfigException.class, e.getClass());
        }
    }

    @Test
    public void testInvalidNumberIntervalsStored() throws CfmConfigException {
        try {
            DefaultDelayMeasurementCreate.builder(
                    DmType.DMDMM, Version.Y17312011, MepId.valueOf((short) 20),
                    Priority.PRIO6)
                    .numberIntervalsStored((short) 1001);
            fail("Expected an exception to be thrown for number intervals stored: " + 1001);
        } catch (SoamConfigException e) {
            assertEquals(SoamConfigException.class, e.getClass());
        }
    }

    @Test
    public void testInvalidAlignMeasurementOffset() throws CfmConfigException {
        try {
            DefaultDelayMeasurementCreate.builder(
                    DmType.DMDMM, Version.Y17312011, MepId.valueOf((short) 20),
                    Priority.PRIO6)
                    .alignMeasurementOffset(Duration.ofMinutes(525601));
            fail("Expected an exception to be thrown for align Measurement Offset: " + 525601);
        } catch (SoamConfigException e) {
            assertEquals(SoamConfigException.class, e.getClass());
        }
    }

    @Test
    public void testInvalidStartTime() throws CfmConfigException {
        OffsetDateTime oneMinuteAgo = OffsetDateTime.now().minusMinutes(1);
        try {
            DefaultDelayMeasurementCreate.builder(
                    DmType.DMDMM, Version.Y17312011, MepId.valueOf((short) 20),
                    Priority.PRIO6)
                    .startTime(StartTime.absolute(oneMinuteAgo.toInstant()));
            fail("Expected an exception to be thrown for align Start Time: " + oneMinuteAgo);
        } catch (SoamConfigException e) {
            assertEquals(SoamConfigException.class, e.getClass());
        }
    }

    @Test
    public void testInvalidStopTime() throws CfmConfigException {
        OffsetDateTime oneMinuteAgo = OffsetDateTime.now().minusMinutes(1);
        try {
            DefaultDelayMeasurementCreate.builder(
                    DmType.DMDMM, Version.Y17312011, MepId.valueOf((short) 20),
                    Priority.PRIO6)
                    .stopTime(StopTime.absolute(oneMinuteAgo.toInstant()));
            fail("Expected an exception to be thrown for align Stop Time: " + oneMinuteAgo);
        } catch (SoamConfigException e) {
            assertEquals(SoamConfigException.class, e.getClass());
        }
    }

    @Test
    public void testDmCfgType() {
        assertEquals(DmType.DMDMM, dm1.dmCfgType());
    }

    @Test
    public void testVersion() {
        assertEquals(Version.Y17312011, dm1.version());
    }

    @Test
    public void testRemoteMepId() {
        assertEquals(12, dm1.remoteMepId().value());
    }

    @Test
    public void testMeasurementsEnabled() {
        assertEquals(4, dm1.measurementsEnabled().size());
    }

    @Test
    public void testMessagePeriod() {
        assertEquals(100, dm1.messagePeriod().toMillis());
    }

    @Test
    public void testPriority() {
        assertEquals(Priority.PRIO6.name(), dm1.priority().name());
    }

    @Test
    public void testFrameSize() {
        assertEquals(64, dm1.frameSize().shortValue());
    }

    @Test
    public void testDataPattern() {
        assertEquals(DataPattern.ONES, dm1.dataPattern());
    }

    @Test
    public void testTestTlvIncluded() {
        assertTrue(dm1.testTlvIncluded());
    }

    @Test
    public void testTestTlvPattern() {
        assertEquals(TestTlvPattern.NULL_SIGNAL_WITHOUT_CRC_32, dm1.testTlvPattern());
    }

    @Test
    public void testMeasurementInterval() {
        assertEquals(15, dm1.measurementInterval().toMinutes());
    }

    @Test
    public void testNumberIntervalsStored() {
        assertEquals(32, dm1.numberIntervalsStored().shortValue());
    }

    @Test
    public void testAlignMeasurementIntervals() {
        assertTrue(dm1.alignMeasurementIntervals());
    }

    @Test
    public void testAlignMeasurementOffset() {
        assertEquals(4, dm1.alignMeasurementOffset().toMinutes());
    }

    @Test
    public void testBinsPerFdInterval() {
        assertEquals(8, dm1.binsPerFdInterval().shortValue());
    }

    @Test
    public void testBinsPerIfdvInterval() {
        assertEquals(9, dm1.binsPerIfdvInterval().shortValue());
    }

    @Test
    public void testIfdvSelectionOffset() {
        assertEquals(10, dm1.ifdvSelectionOffset().shortValue());
    }

    @Test
    public void testBinsPerFdrInterval() {
        assertEquals(12, dm1.binsPerFdrInterval().shortValue());
    }

    @Test
    public void testSessionType() {
        assertEquals(SessionType.ONDEMAND, dm1.sessionType());
    }

    @Test
    public void testStartTime() {
        assertEquals(StartTimeOption.RELATIVE, dm1.startTime().option());
        assertEquals(7, dm1.startTime().relativeTime().toMinutes());
    }

    @Test
    public void testStopTime() {
        assertEquals(StopTimeOption.RELATIVE, dm1.stopTime().option());
        assertEquals(8, dm1.stopTime().relativeTime().toMinutes());
    }

    @Test
    public void testThresholds() {
        DelayMeasurementThreshold[] thresholds =
                dm1.thresholds().toArray(
                        new DelayMeasurementThreshold[dm1.thresholds().size()]);

        assertEquals(1, thresholds[0].threshId().id().intValue());
        assertEquals(123, thresholds[0].averageFrameDelayBackward().toMillis());
    }
}
