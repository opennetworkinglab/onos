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
package org.onosproject.drivers.microsemi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.Duration;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.onosproject.incubator.net.l2monitoring.cfm.Mep.Priority;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdCharStr;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdShort;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdId;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdIdCharStr;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;
import org.onosproject.incubator.net.l2monitoring.soam.SoamConfigException;
import org.onosproject.incubator.net.l2monitoring.soam.SoamId;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DefaultDelayMeasurementCreate;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementCreate.DmCreateBuilder;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementCreate.DmType;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementCreate.MeasurementOption;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementCreate.Version;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementEntry;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementEntry.SessionStatus;

public class EA1000SoamDmProgrammableTest {

    EA1000SoamDmProgrammable dmProgrammable;
    MdId mdId1 = MdIdCharStr.asMdId("md-1");
    MaIdShort maId11 = MaIdCharStr.asMaId("ma-1-1");
    MepId mep111 = MepId.valueOf((short) 1);

    @Before
    public void setUp() throws Exception {
        dmProgrammable = new EA1000SoamDmProgrammable();
        dmProgrammable.setHandler(new MockEa1000DriverHandler());
        assertNotNull(dmProgrammable.handler().data().deviceId());
    }

    //TODO Implement all these tests
//    @Test
//    public void testEA1000SoamDmProgrammable() {
//        fail("Not yet implemented");
//    }
//
//    @Test
//    public void testGetAllDms() {
//        fail("Not yet implemented");
//    }

    /**
     * From SAMPLE_MSEACFM_DELAY_MEASUREMENT_FULL_REPLY.
     * @throws CfmConfigException
     * @throws SoamConfigException
     */
    @Test
    public void testGetDm() throws CfmConfigException, SoamConfigException {
        DelayMeasurementEntry dmEntry =
                dmProgrammable.getDm(mdId1, maId11, mep111, SoamId.valueOf(1));
        assertEquals(1, dmEntry.dmId().id().intValue());
        assertEquals(2, dmEntry.measurementsEnabled().size());
        assertEquals(SessionStatus.ACTIVE.name(), dmEntry.sessionStatus().name());
        assertEquals(100, dmEntry.frameDelayTwoWay().toNanos() / 1000);
        assertEquals(101, dmEntry.interFrameDelayVariationTwoWay().toNanos() / 1000);
    }

    @Test
    public void testCreateDm() throws CfmConfigException, SoamConfigException {
        DmCreateBuilder dmBuilder = (DmCreateBuilder) DefaultDelayMeasurementCreate
            .builder(DmType.DMDMM, Version.Y17312011,
                MepId.valueOf((short) 10), Priority.PRIO3)
            .frameSize((short) 1200);

        dmProgrammable.createDm(mdId1, maId11, mep111, dmBuilder.build());
    }

    @Test
    public void testCreateDmWrongMsgPeriod()
            throws CfmConfigException, SoamConfigException {
        DmCreateBuilder dmBuilder = (DmCreateBuilder) DefaultDelayMeasurementCreate
                .builder(DmType.DMDMM, Version.Y17312011,
                        MepId.valueOf((short) 10), Priority.PRIO3)
                .messagePeriod(Duration.ofMillis(1234));

        try {
            dmProgrammable.createDm(mdId1, maId11, mep111, dmBuilder.build());
            fail("Expecting to get an exception");
        } catch (SoamConfigException e) {
            assertTrue(e.getMessage()
                    .contains("EA1000 supports only Message Periods"));
        }

    }

//    @Test
//    public void testGetDmCurrentStat() {
//        fail("Not yet implemented");
//    }
//
//    @Test
//    public void testGetDmHistoricalStats() {
//        fail("Not yet implemented");
//    }
//
//    @Test
//    public void testAbortDmMdIdMaIdShortMepIdSoamId() {
//        fail("Not yet implemented");
//    }
//
//    @Test
//    public void testBuildApiDmFromYangDm() {
//        fail("Not yet implemented");
//    }
//
//    @Test
//    public void testAbortDmMdIdMaIdShortMepId() {
//        fail("Not yet implemented");
//    }
//
//    @Test
//    public void testClearDelayHistoryStatsMdIdMaIdShortMepId() {
//        fail("Not yet implemented");
//    }
//
//    @Test
//    public void testClearDelayHistoryStatsMdIdMaIdShortMepIdSoamId() {
//        fail("Not yet implemented");
//    }
//
//    @Test
//    public void testGetAllLms() {
//        fail("Not yet implemented");
//    }
//
//    @Test
//    public void testGetLm() {
//        fail("Not yet implemented");
//    }
//
//    @Test
//    public void testGetLmCurrentStat() {
//        fail("Not yet implemented");
//    }
//
//    @Test
//    public void testGetLmHistoricalStats() {
//        fail("Not yet implemented");
//    }
//
//    @Test
//    public void testCreateLm() {
//        fail("Not yet implemented");
//    }
//
//    @Test
//    public void testAbortLmMdIdMaIdShortMepId() {
//        fail("Not yet implemented");
//    }
//
//    @Test
//    public void testAbortLmMdIdMaIdShortMepIdSoamId() {
//        fail("Not yet implemented");
//    }
//
//    @Test
//    public void testClearLossHistoryStatsMdIdMaIdShortMepId() {
//        fail("Not yet implemented");
//    }
//
//    @Test
//    public void testClearLossHistoryStatsMdIdMaIdShortMepIdSoamId() {
//        fail("Not yet implemented");
//    }

    @Test
    public void testCreateTestSignal() {
        try {
            dmProgrammable.createTestSignal(mdId1, maId11, mep111, null);
            fail("Expected an exception");
        } catch (UnsupportedOperationException e) {
            assertEquals("Not supported by EA1000", e.getMessage());
        } catch (CfmConfigException e) {
            fail("CfmConfigException was not expected");
        }
    }

    @Test
    public void testAbortTestSignal() {
        try {
            dmProgrammable.abortTestSignal(mdId1, maId11, mep111);
            fail("Expected an exception");
        } catch (UnsupportedOperationException e) {
            assertEquals("Not supported by EA1000", e.getMessage());
        } catch (CfmConfigException e) {
            fail("CfmConfigException was not expected");
        }
    }

    @Test
    public void testMeasurementEnableCollectionOfMeasurementOption() {
        BitSet meBs = BitSet.valueOf(new byte[]{0x05});
        Collection<MeasurementOption> moSet =
                EA1000SoamDmProgrammable.getMeasurementOptions(meBs);
        assertTrue(moSet.contains(MeasurementOption.SOAM_PDUS_RECEIVED));
        assertTrue(moSet.contains(MeasurementOption.FRAME_DELAY_TWO_WAY_MIN));
    }

    @Test
    public void testMeasurementEnableBitSetEmpty() {
        Collection<MeasurementOption> moSet = new ArrayList<>();
        try {
            BitSet bitSet = EA1000SoamDmProgrammable.getMeasurementEnabledSet(moSet);
            assertEquals("{}", bitSet.toString());
        } catch (SoamConfigException e) {
            fail("Was not expecting exception here");
        }
    }

    @Test
    public void testMeasurementEnableBitSetInvalid() {
        Collection<MeasurementOption> moSet = new ArrayList<>();
        moSet.add(MeasurementOption.FRAME_DELAY_BACKWARD_BINS);
        moSet.add(MeasurementOption.FRAME_DELAY_RANGE_BACKWARD_AVERAGE);
        try {
            EA1000SoamDmProgrammable.getMeasurementEnabledSet(moSet);
            fail("Was expecting an exception");
        } catch (SoamConfigException e) {
            assertTrue(e.getMessage()
                    .contains("Measurement Option is not supported on EA1000"));
        }
    }

    @Test
    public void testMeasurementEnableBitSet2Good() {
        Collection<MeasurementOption> moSet = new ArrayList<>();
        moSet.add(MeasurementOption.FRAME_DELAY_TWO_WAY_BINS);
        moSet.add(MeasurementOption.FRAME_DELAY_TWO_WAY_AVERAGE);
        try {
            BitSet bitSet = EA1000SoamDmProgrammable.getMeasurementEnabledSet(moSet);
            assertEquals("{1, 4}", bitSet.toString());
        } catch (SoamConfigException e) {
            fail("Was not expecting exception here");
        }
    }

}
