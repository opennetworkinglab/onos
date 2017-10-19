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

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.onosproject.drivers.microsemi.yang.utils.MaNameUtil;
import org.onosproject.incubator.net.l2monitoring.cfm.DefaultMepLbCreate;
import org.onosproject.incubator.net.l2monitoring.cfm.Mep.Priority;
import org.onosproject.incubator.net.l2monitoring.cfm.MepEntry;
import org.onosproject.incubator.net.l2monitoring.cfm.MepLbCreate;
import org.onosproject.incubator.net.l2monitoring.cfm.RemoteMepEntry.InterfaceStatusTlvType;
import org.onosproject.incubator.net.l2monitoring.cfm.RemoteMepEntry.PortStatusTlvType;
import org.onosproject.incubator.net.l2monitoring.cfm.RemoteMepEntry.RemoteMepState;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdCharStr;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdShort;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdId;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdIdCharStr;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.maintenancedomain.MdNameAndTypeCombo;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.maintenancedomain.maintenanceassociation.MaNameAndTypeCombo;

import java.util.BitSet;

/**
 * Test of the CFM implementation on EA1000 through the incubator/net/l2monitoring interface.
 */
public class EA1000CfmMepProgrammableTest {
    EA1000CfmMepProgrammable cfmProgrammable;
    public static final MdId MD_ID_1 = MdIdCharStr.asMdId("md-1");
    public static final MaIdShort MA_ID_11 = MaIdCharStr.asMaId("ma-1-1");
    public static final MepId MEP_111 = MepId.valueOf((short) 1);
    public static final MepId MEP_112 = MepId.valueOf((short) 2);

    @Before
    public void setUp() throws Exception {
        cfmProgrammable = new EA1000CfmMepProgrammable();
        cfmProgrammable.setHandler(new MockEa1000DriverHandler());
        assertNotNull(cfmProgrammable.handler().data().deviceId());
    }


    @Ignore
    @Test
    public void testCreateMep() {
        fail("Not yet implemented");
    }

    @Ignore
    @Test
    public void testGetAllMeps() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetMep() throws CfmConfigException {
        MepEntry mepEntry = cfmProgrammable.getMep(MD_ID_1, MA_ID_11, MEP_111);

        //Result will come from MockNetconfSessionEa1000.SAMPLE_MSEACFM_MD_MA_MEP_FULL_REPLY
        assertNotNull(mepEntry);
        assertTrue(mepEntry.administrativeState());
        assertTrue(mepEntry.cciEnabled());
        assertEquals(Priority.PRIO5.name(), mepEntry.ccmLtmPriority().name());

        assertTrue("Expecting remote-mac-error", mepEntry.activeMacStatusDefect()); //remote-mac-error
        assertTrue("Expecting remote-rdi", mepEntry.activeRdiCcmDefect()); //remote-rdi

        assertNotNull(mepEntry.activeRemoteMepList());

//FIXME Waiting on patch https://gerrit.onosproject.org/#/c/15778/
//        assertEquals("Expecting 2 Remote Meps", 2, mepEntry.activeRemoteMepList().size());
        mepEntry.activeRemoteMepList().forEach(rmep -> {
            if (rmep.remoteMepId().value() == 1) {
                assertEquals(RemoteMepState.RMEP_FAILED.name(),
                        rmep.state().toString());
                assertEquals(54654654L, rmep.failedOrOkTime().toMillis());
                assertEquals("aa:bb:cc:dd:ee:ff".toUpperCase(), rmep.macAddress().toString());
                assertFalse(rmep.rdi());
                assertEquals(PortStatusTlvType.PS_NO_STATUS_TLV.name(),
                        rmep.portStatusTlvType().toString());
                assertEquals(InterfaceStatusTlvType.IS_DORMANT.name(),
                        rmep.interfaceStatusTlvType().toString());
            }
        });

    }

    @Test
    public void testGetMep2() throws CfmConfigException {
        MepEntry mepEntry = cfmProgrammable.getMep(MD_ID_1, MA_ID_11, MEP_112);

        //Result will come from MockNetconfSessionEa1000.SAMPLE_MSEACFM_MD_MA_MEP_FULL_REPLY
        assertNotNull(mepEntry);
        assertTrue(mepEntry.administrativeState());
        assertTrue(mepEntry.cciEnabled());
        assertEquals(Priority.PRIO4.name(), mepEntry.ccmLtmPriority().name());

        assertNotNull(mepEntry.activeRemoteMepList());
        BitSet bs1 = new BitSet();
        bs1.clear();
//FIXME Waiting on patch https://gerrit.onosproject.org/#/c/15778/
//        assertEquals("Expecting 2 Remote Meps", 2, mepEntry.activeRemoteMepList().size());
        mepEntry.activeRemoteMepList().forEach(rmep -> {
            if (rmep.remoteMepId().value() == 1) {
                assertEquals(RemoteMepState.RMEP_FAILED.name(),
                        rmep.state().toString());
                assertEquals(54654654L, rmep.failedOrOkTime().toMillis());
                assertEquals("aa:bb:cc:dd:ee:ff".toUpperCase(), rmep.macAddress().toString());
                assertFalse(rmep.rdi());
                assertEquals(PortStatusTlvType.PS_NO_STATUS_TLV.name(),
                        rmep.portStatusTlvType().toString());
                assertEquals(InterfaceStatusTlvType.IS_DORMANT.name(),
                        rmep.interfaceStatusTlvType().toString());
            }
        });

    }

    /**
     * For sampleXmlRegexDeleteMseaCfmMep.
     * @throws CfmConfigException If an error occurs
     */
    @Test
    public void testDeleteMep() throws CfmConfigException {
        assertTrue(cfmProgrammable.deleteMep(MD_ID_1, MA_ID_11, MEP_111));
    }

    /**
     * For sampleXmlRegexTransmitLoopback.
     * @throws CfmConfigException If an error occurs
     */
    @Test
    public void testTransmitLoopback() throws CfmConfigException {
        MepLbCreate.MepLbCreateBuilder lbCreate =
                    DefaultMepLbCreate.builder(MepId.valueOf((short) 12));
        lbCreate.numberMessages(5);
//        lbCreate.dataTlvHex("AA:BB:CC:DD:EE");
        lbCreate.vlanPriority(Priority.PRIO3);
        lbCreate.vlanDropEligible(true);

        cfmProgrammable.transmitLoopback(MD_ID_1, MA_ID_11, MEP_111, lbCreate.build());
    }

    @Test
    public void testAbortLoopback() throws CfmConfigException {
        cfmProgrammable.abortLoopback(MD_ID_1, MA_ID_11, MEP_111);
    }

//    @Test
//    public void testTransmitLinktrace() {
//        fail("Not yet implemented");
//    }

    @Test
    public void testGetYangMdNameFromApiMdId() throws CfmConfigException {
        MdNameAndTypeCombo name = EA1000CfmMepProgrammable
                .getYangMdNameFromApiMdId(MdIdCharStr.asMdId("md-1"));

        assertEquals(org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm
                .maintenancedomain.mdnameandtypecombo
                .DefaultNameCharacterString.class, name.getClass());

//There's a problem with checkstyle for typecast on really long paths
//        assertEquals("md-1", ((org.onosproject.yang.gen.v1.http.www.microsemi.com
//                .microsemi.edge.assure.msea.cfm.rev20160229.mseacfm.mefcfm
//                .maintenancedomain.mdnameandtypecombo
//                .DefaultNameCharacterString) name).name().string());
    }

    @Test
    public void testGetYangMaNameFromApiMaId() throws CfmConfigException {
        MaNameAndTypeCombo name = MaNameUtil
                .getYangMaNameFromApiMaId(MaIdCharStr.asMaId("ma-1-1"));
        assertEquals(org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm
                .maintenancedomain.maintenanceassociation.manameandtypecombo
                .DefaultNameCharacterString.class, name.getClass());

//There's a problem with checkstyle for typecast on really long paths
//        assertEquals("ma-1-1", ((org.onosproject.yang.gen.v1.http.www.microsemi.com
//                .microsemi.edge.assure.msea.cfm.rev20160229.mseacfm.mefcfm
//                .maintenancedomain.maintenanceassociation.manameandtypecombo
//                .DefaultNameCharacterString) name).name().string());
    }

}
