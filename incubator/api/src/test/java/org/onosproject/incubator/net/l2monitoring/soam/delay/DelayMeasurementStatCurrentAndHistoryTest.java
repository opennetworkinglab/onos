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

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.onosproject.incubator.net.l2monitoring.soam.SoamConfigException;
import org.onosproject.incubator.net.l2monitoring.soam.SoamId;

public class DelayMeasurementStatCurrentAndHistoryTest {

    private DelayMeasurementStatCurrent dmStCurr1;
    private DelayMeasurementStatHistory dmStHist1;

    @Before
    public void setUp() throws Exception, SoamConfigException {
        dmStCurr1 = (DelayMeasurementStatCurrent)
                DefaultDelayMeasurementStatCurrent.builder(
                        Duration.ofMinutes(9), false)
                .startTime(OffsetDateTime.of(2017, 3, 20, 23, 22, 7, 0,
                        ZoneOffset.ofHours(-7)).toInstant())
                .frameDelayTwoWayMin(Duration.ofMillis(101))
                .frameDelayTwoWayMax(Duration.ofMillis(102))
                .frameDelayTwoWayAvg(Duration.ofMillis(103))
                .frameDelayForwardMin(Duration.ofMillis(104))
                .frameDelayForwardMax(Duration.ofMillis(105))
                .frameDelayForwardAvg(Duration.ofMillis(106))
                .frameDelayBackwardMin(Duration.ofMillis(107))
                .frameDelayBackwardMax(Duration.ofMillis(108))
                .frameDelayBackwardAvg(Duration.ofMillis(109))
                .interFrameDelayVariationTwoWayMin(Duration.ofMillis(110))
                .interFrameDelayVariationTwoWayMax(Duration.ofMillis(111))
                .interFrameDelayVariationTwoWayAvg(Duration.ofMillis(112))
                .interFrameDelayVariationForwardMin(Duration.ofMillis(113))
                .interFrameDelayVariationForwardMax(Duration.ofMillis(114))
                .interFrameDelayVariationForwardAvg(Duration.ofMillis(115))
                .interFrameDelayVariationBackwardMin(Duration.ofMillis(116))
                .interFrameDelayVariationBackwardMax(Duration.ofMillis(117))
                .interFrameDelayVariationBackwardAvg(Duration.ofMillis(118))
                .frameDelayRangeTwoWayMax(Duration.ofMillis(119))
                .frameDelayRangeTwoWayAvg(Duration.ofMillis(120))
                .frameDelayRangeForwardMax(Duration.ofMillis(121))
                .frameDelayRangeForwardAvg(Duration.ofMillis(122))
                .frameDelayRangeBackwardMax(Duration.ofMillis(123))
                .frameDelayRangeBackwardAvg(Duration.ofMillis(124))
                .soamPdusSent(125)
                .soamPdusReceived(126)
                .build();

        dmStHist1 = (DelayMeasurementStatHistory)
                DefaultDelayMeasurementStatHistory.builder(
                        SoamId.valueOf(11), Duration.ofMinutes(15), true)
                .endTime(OffsetDateTime.of(2017, 3, 20, 23, 22, 8, 0,
                        ZoneOffset.ofHours(-7)).toInstant())
                .frameDelayTwoWayMin(Duration.ofMillis(201))
                .frameDelayTwoWayMax(Duration.ofMillis(202))
                .frameDelayTwoWayAvg(Duration.ofMillis(203))
                .frameDelayForwardMin(Duration.ofMillis(204))
                .frameDelayForwardMax(Duration.ofMillis(205))
                .frameDelayForwardAvg(Duration.ofMillis(206))
                .frameDelayBackwardMin(Duration.ofMillis(207))
                .frameDelayBackwardMax(Duration.ofMillis(208))
                .frameDelayBackwardAvg(Duration.ofMillis(209))
                .interFrameDelayVariationTwoWayMin(Duration.ofMillis(210))
                .interFrameDelayVariationTwoWayMax(Duration.ofMillis(211))
                .interFrameDelayVariationTwoWayAvg(Duration.ofMillis(212))
                .interFrameDelayVariationForwardMin(Duration.ofMillis(213))
                .interFrameDelayVariationForwardMax(Duration.ofMillis(214))
                .interFrameDelayVariationForwardAvg(Duration.ofMillis(215))
                .interFrameDelayVariationBackwardMin(Duration.ofMillis(216))
                .interFrameDelayVariationBackwardMax(Duration.ofMillis(217))
                .interFrameDelayVariationBackwardAvg(Duration.ofMillis(218))
                .frameDelayRangeTwoWayMax(Duration.ofMillis(219))
                .frameDelayRangeTwoWayAvg(Duration.ofMillis(220))
                .frameDelayRangeForwardMax(Duration.ofMillis(221))
                .frameDelayRangeForwardAvg(Duration.ofMillis(222))
                .frameDelayRangeBackwardMax(Duration.ofMillis(223))
                .frameDelayRangeBackwardAvg(Duration.ofMillis(224))
                .soamPdusSent(225)
                .soamPdusReceived(226)
                .build();
    }

    @Test
    public void testStartTime() {
        assertEquals("2017-03-20T23:22:07-07:00",
                dmStCurr1.startTime().atOffset(ZoneOffset.ofHours(-7))
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    }

    @Test
    public void testEndTime() {
        assertEquals("2017-03-20T23:22:08-07:00",
                dmStHist1.endTime().atOffset(ZoneOffset.ofHours(-7))
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    }

    @Test
    public void testHistoryStatsId() {
        assertEquals(11, dmStHist1.historyStatsId().id().intValue());
    }

    @Test
    public void testElapsedTime() {
        assertEquals(9, dmStCurr1.elapsedTime().toMinutes());

        assertEquals(15, dmStHist1.elapsedTime().toMinutes());
    }

    @Test
    public void testSuspectStatus() {
        assertFalse(dmStCurr1.suspectStatus());

        assertTrue(dmStHist1.suspectStatus());
    }

    @Test
    public void testFrameDelayTwoWayMin() {
        assertEquals(101, dmStCurr1.frameDelayTwoWayMin().toMillis());

        assertEquals(201, dmStHist1.frameDelayTwoWayMin().toMillis());
    }

    @Test
    public void testFrameDelayTwoWayMax() {
        assertEquals(102, dmStCurr1.frameDelayTwoWayMax().toMillis());

        assertEquals(202, dmStHist1.frameDelayTwoWayMax().toMillis());
    }

    @Test
    public void testFrameDelayTwoWayAvg() {
        assertEquals(103, dmStCurr1.frameDelayTwoWayAvg().toMillis());

        assertEquals(203, dmStHist1.frameDelayTwoWayAvg().toMillis());
    }

    @Test
    public void testFrameDelayForwardMin() {
        assertEquals(104, dmStCurr1.frameDelayForwardMin().toMillis());

        assertEquals(204, dmStHist1.frameDelayForwardMin().toMillis());
    }

    @Test
    public void testFrameDelayForwardMax() {
        assertEquals(105, dmStCurr1.frameDelayForwardMax().toMillis());

        assertEquals(205, dmStHist1.frameDelayForwardMax().toMillis());
    }

    @Test
    public void testFrameDelayForwardAvg() {
        assertEquals(106, dmStCurr1.frameDelayForwardAvg().toMillis());

        assertEquals(206, dmStHist1.frameDelayForwardAvg().toMillis());
    }

    @Test
    public void testFrameDelayBackwardMin() {
        assertEquals(107, dmStCurr1.frameDelayBackwardMin().toMillis());

        assertEquals(207, dmStHist1.frameDelayBackwardMin().toMillis());
    }

    @Test
    public void testFrameDelayBackwardMax() {
        assertEquals(108, dmStCurr1.frameDelayBackwardMax().toMillis());

        assertEquals(208, dmStHist1.frameDelayBackwardMax().toMillis());
    }

    @Test
    public void testFrameDelayBackwardAvg() {
        assertEquals(109, dmStCurr1.frameDelayBackwardAvg().toMillis());

        assertEquals(209, dmStHist1.frameDelayBackwardAvg().toMillis());
    }

    @Test
    public void testInterFrameDelayVariationTwoWayMin() {
        assertEquals(110, dmStCurr1.interFrameDelayVariationTwoWayMin().toMillis());

        assertEquals(210, dmStHist1.interFrameDelayVariationTwoWayMin().toMillis());
    }

    @Test
    public void testInterFrameDelayVariationTwoWayMax() {
        assertEquals(111, dmStCurr1.interFrameDelayVariationTwoWayMax().toMillis());

        assertEquals(211, dmStHist1.interFrameDelayVariationTwoWayMax().toMillis());
    }

    @Test
    public void testInterFrameDelayVariationTwoWayAvg() {
        assertEquals(112, dmStCurr1.interFrameDelayVariationTwoWayAvg().toMillis());

        assertEquals(212, dmStHist1.interFrameDelayVariationTwoWayAvg().toMillis());
    }

    @Test
    public void testInterFrameDelayVariationForwardMin() {
        assertEquals(113, dmStCurr1.interFrameDelayVariationForwardMin().toMillis());

        assertEquals(213, dmStHist1.interFrameDelayVariationForwardMin().toMillis());
    }

    @Test
    public void testInterFrameDelayVariationForwardMax() {
        assertEquals(114, dmStCurr1.interFrameDelayVariationForwardMax().toMillis());

        assertEquals(214, dmStHist1.interFrameDelayVariationForwardMax().toMillis());
    }

    @Test
    public void testInterFrameDelayVariationForwardAvg() {
        assertEquals(115, dmStCurr1.interFrameDelayVariationForwardAvg().toMillis());

        assertEquals(215, dmStHist1.interFrameDelayVariationForwardAvg().toMillis());
    }

    @Test
    public void testInterFrameDelayVariationBackwardMin() {
        assertEquals(116, dmStCurr1.interFrameDelayVariationBackwardMin().toMillis());

        assertEquals(216, dmStHist1.interFrameDelayVariationBackwardMin().toMillis());
    }

    @Test
    public void testInterFrameDelayVariationBackwardMax() {
        assertEquals(117, dmStCurr1.interFrameDelayVariationBackwardMax().toMillis());

        assertEquals(217, dmStHist1.interFrameDelayVariationBackwardMax().toMillis());
    }

    @Test
    public void testInterFrameDelayVariationBackwardAvg() {
        assertEquals(118, dmStCurr1.interFrameDelayVariationBackwardAvg().toMillis());

        assertEquals(218, dmStHist1.interFrameDelayVariationBackwardAvg().toMillis());
    }

    @Test
    public void testFrameDelayRangeTwoWayMax() {
        assertEquals(119, dmStCurr1.frameDelayRangeTwoWayMax().toMillis());

        assertEquals(219, dmStHist1.frameDelayRangeTwoWayMax().toMillis());
    }

    @Test
    public void testFrameDelayRangeTwoWayAvg() {
        assertEquals(120, dmStCurr1.frameDelayRangeTwoWayAvg().toMillis());

        assertEquals(220, dmStHist1.frameDelayRangeTwoWayAvg().toMillis());
    }

    @Test
    public void testFrameDelayRangeForwardMax() {
        assertEquals(121, dmStCurr1.frameDelayRangeForwardMax().toMillis());

        assertEquals(221, dmStHist1.frameDelayRangeForwardMax().toMillis());
    }

    @Test
    public void testFrameDelayRangeForwardAvg() {
        assertEquals(122, dmStCurr1.frameDelayRangeForwardAvg().toMillis());

        assertEquals(222, dmStHist1.frameDelayRangeForwardAvg().toMillis());
    }

    @Test
    public void testFrameDelayRangeBackwardMax() {
        assertEquals(123, dmStCurr1.frameDelayRangeBackwardMax().toMillis());

        assertEquals(223, dmStHist1.frameDelayRangeBackwardMax().toMillis());
    }

    @Test
    public void testFrameDelayRangeBackwardAvg() {
        assertEquals(124, dmStCurr1.frameDelayRangeBackwardAvg().toMillis());

        assertEquals(224, dmStHist1.frameDelayRangeBackwardAvg().toMillis());
    }

    @Test
    public void testSoamPdusSent() {
        assertEquals(125, dmStCurr1.soamPdusSent().intValue());

        assertEquals(225, dmStHist1.soamPdusSent().intValue());
    }

    @Test
    public void testSoamPdusReceived() {
        assertEquals(126, dmStCurr1.soamPdusReceived().intValue());

        assertEquals(226, dmStHist1.soamPdusReceived().intValue());
    }

    @Ignore
    @Test
    public void testFrameDelayTwoWayBins() {
        //TODO Add in test
        fail("Not yet implemented");
    }

    @Ignore
    @Test
    public void testFrameDelayForwardBins() {
        //TODO Add in test
        fail("Not yet implemented");
    }

    @Ignore
    @Test
    public void testFrameDelayBackwardBins() {
        //TODO Add in test
        fail("Not yet implemented");
    }

    @Ignore
    @Test
    public void testInterFrameDelayVariationTwoWayBins() {
        //TODO Add in test
        fail("Not yet implemented");
    }

    @Ignore
    @Test
    public void testInterFrameDelayVariationForwardBins() {
        //TODO Add in test
        fail("Not yet implemented");
    }

    @Ignore
    @Test
    public void testInterFrameDelayVariationBackwardBins() {
        //TODO Add in test
        fail("Not yet implemented");
    }

    @Ignore
    @Test
    public void testFrameDelayRangeTwoWayBins() {
        //TODO Add in test
        fail("Not yet implemented");
    }

    @Ignore
    @Test
    public void testFrameDelayRangeForwardBins() {
        //TODO Add in test
        fail("Not yet implemented");
    }

    @Ignore
    @Test
    public void testFrameDelayRangeBackwardBins() {
        //TODO Add in test
        fail("Not yet implemented");
    }
}
