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

import java.time.Duration;

import org.junit.Before;
import org.junit.Test;
import org.onosproject.incubator.net.l2monitoring.cfm.Mep.Priority;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;
import org.onosproject.incubator.net.l2monitoring.soam.SoamConfigException;
import org.onosproject.incubator.net.l2monitoring.soam.SoamId;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementCreate.DmType;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementCreate.Version;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementEntry.SessionStatus;

import static org.junit.Assert.assertEquals;

public class DelayMeasurementEntryTest {

    DelayMeasurementEntry dmE1;

    @Before
    public void setUp() throws Exception, SoamConfigException, CfmConfigException {

        DelayMeasurementStatCurrent dmE1c = (DelayMeasurementStatCurrent)
                DefaultDelayMeasurementStatCurrent.builder(
                Duration.ofMinutes(6), false)
        .build();

        DelayMeasurementStatHistory dmE1h1 = (DelayMeasurementStatHistory)
                DefaultDelayMeasurementStatHistory.builder(
                        SoamId.valueOf(1), Duration.ofMinutes(15), false)
                .build();

        DelayMeasurementStatHistory dmE1h2 = (DelayMeasurementStatHistory)
                DefaultDelayMeasurementStatHistory.builder(
                        SoamId.valueOf(2), Duration.ofMinutes(15), false)
                .build();

        dmE1 = DefaultDelayMeasurementEntry.builder(
                SoamId.valueOf(1),
                DmType.DMDMM,
                Version.Y17312011,
                MepId.valueOf((short) 10),
                Priority.PRIO3)
                .sessionStatus(SessionStatus.NOT_ACTIVE)
                .frameDelayTwoWay(Duration.ofMillis(1))
                .frameDelayForward(Duration.ofMillis(2))
                .frameDelayBackward(Duration.ofMillis(3))
                .interFrameDelayVariationTwoWay(Duration.ofMillis(4))
                .interFrameDelayVariationForward(Duration.ofMillis(5))
                .interFrameDelayVariationBackward(Duration.ofMillis(6))
                .currentResult(dmE1c)
                .addToHistoricalResults(dmE1h1)
                .addToHistoricalResults(dmE1h2)
                .build();
    }

    @Test
    public void testDmId() {
        assertEquals(1, dmE1.dmId().id().shortValue());
    }

    @Test
    public void testSessionStatus() {
        assertEquals(SessionStatus.NOT_ACTIVE.name(),
                dmE1.sessionStatus().name());
    }

    @Test
    public void testFrameDelayTwoWay() {
        assertEquals(1, dmE1.frameDelayTwoWay().toMillis());
    }

    @Test
    public void testFrameDelayForward() {
        assertEquals(2, dmE1.frameDelayForward().toMillis());
    }

    @Test
    public void testFrameDelayBackward() {
        assertEquals(3, dmE1.frameDelayBackward().toMillis());
    }

    @Test
    public void testInterFrameDelayVariationTwoWay() {
        assertEquals(4, dmE1.interFrameDelayVariationTwoWay().toMillis());
    }

    @Test
    public void testInterFrameDelayVariationForward() {
        assertEquals(5, dmE1.interFrameDelayVariationForward().toMillis());
    }

    @Test
    public void testInterFrameDelayVariationBackward() {
        assertEquals(6, dmE1.interFrameDelayVariationBackward().toMillis());
    }

    @Test
    public void testCurrentResult() {
        assertEquals(360, dmE1.currentResult().elapsedTime().getSeconds());
    }

    @Test
    public void testHistoricalResults() {
        assertEquals(2, dmE1.historicalResults().size());
    }

}
