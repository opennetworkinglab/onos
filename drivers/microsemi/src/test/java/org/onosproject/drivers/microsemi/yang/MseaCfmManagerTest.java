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
package org.onosproject.drivers.microsemi.yang;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.UncheckedIOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ip4Address;
import org.onosproject.drivers.microsemi.yang.MseaCfmNetconfService.DmEntryParts;
import org.onosproject.drivers.microsemi.yang.impl.MseaCfmManager;
import org.onosproject.drivers.microsemi.yang.utils.MepIdUtil;
import org.onosproject.drivers.microsemi.yang.utils.MepIdUtil2;
import org.onosproject.drivers.microsemi.yang.utils.MepIdUtil3;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdCharStr;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdIdCharStr;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;
import org.onosproject.incubator.net.l2monitoring.soam.SoamId;
import org.onosproject.netconf.NetconfDeviceInfo;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.onosproject.yang.gen.v1.ietfyangtypes.rev20130715.ietfyangtypes.MacAddress;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.MseaCfm;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.DefaultMefCfm;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.MefCfm;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.abortloopback.AbortLoopbackInput;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.abortloopback.DefaultAbortLoopbackInput;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.DefaultMaintenanceDomain;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.MaintenanceDomain;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.maintenancedomain.maintenanceassociation.MaintenanceAssociationEndPoint;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.maintenancedomain.mdnameandtypecombo.DefaultNameCharacterString;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.maintenancedomain.mdnameandtypecombo.NameCharacterString;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.targetaddressgroup.AddressType;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.targetaddressgroup.addresstype.DefaultMacAddress;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.targetaddressgroup.addresstype.DefaultMepId;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.transmitloopback.DefaultTransmitLoopbackInput;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.transmitloopback.TransmitLoopbackInput;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.transmitloopback.transmitloopbackinput.DefaultTargetAddress;
import org.onosproject.yang.gen.v1.mseasoampm.rev20160229.mseasoampm.mefcfm.maintenancedomain.maintenanceassociation.maintenanceassociationendpoint.augmentedmseacfmmaintenanceassociationendpoint.delaymeasurements.DelayMeasurement;
import org.onosproject.yang.gen.v1.mseasoampm.rev20160229.mseasoampm.mefcfm.maintenancedomain.maintenanceassociation.maintenanceassociationendpoint.augmentedmseacfmmaintenanceassociationendpoint.lossmeasurements.lossmeasurement.MessagePeriodEnum;
import org.onosproject.yang.gen.v1.mseasoampm.rev20160229.mseasoampm.sessionstatustype.SessionStatusTypeEnum;
import org.onosproject.yang.gen.v1.mseatypes.rev20160229.mseatypes.Identifier45;
import org.onosproject.yang.gen.v1.mseatypes.rev20160229.mseatypes.MepIdType;
import org.onosproject.yang.gen.v1.mseatypes.rev20160229.mseatypes.PriorityType;

public class MseaCfmManagerTest {

    MseaCfmManager mseaCfmService;
    NetconfSession session;

    @Before
    public void setUp() throws Exception {
        try {
            mseaCfmService = new MockMseaCfmManager();
            mseaCfmService.activate();
        } catch (UncheckedIOException e) {
            fail(e.getMessage());
        }
        NetconfDeviceInfo deviceInfo = new NetconfDeviceInfo("netconf", "netconf",
                Ip4Address.valueOf("1.2.3.4"), 830);
        session = new MockNetconfSessionEa1000(deviceInfo);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testGetConfigMseaCfmEssentials()
            throws NetconfException, CfmConfigException {
        MseaCfm mseaCfm = mseaCfmService.getMepEssentials(
                MdIdCharStr.asMdId("md-1"),
                MaIdCharStr.asMaId("ma-1-1"),
                MepId.valueOf((short) 1), session);
        assertNotNull(mseaCfm);

        //See SAMPLE_MSEACFM_MD_MA_MEP_ESSENTIALS_REPLY in MockNetconfSessionEa1000
        assertEquals(1, mseaCfm.mefCfm().maintenanceDomain().size());
    }

    @Test
    public void testGetConfigMseaCfmFull()
            throws NetconfException, CfmConfigException {
        MseaCfm mseaCfm = mseaCfmService.getMepFull(
                MdIdCharStr.asMdId("md-1"),
                MaIdCharStr.asMaId("ma-1-1"),
                MepId.valueOf((short) 1), session);
        assertNotNull(mseaCfm);

        //See SAMPLE_MSEACFM_MD_MA_MEP_FULL_REPLY in MockNetconfSessionEa1000
        assertEquals(1, mseaCfm.mefCfm().maintenanceDomain().size());
        MaintenanceAssociationEndPoint mep = mseaCfm.mefCfm()
                .maintenanceDomain().get(0)
                .maintenanceAssociation().get(0)
                .maintenanceAssociationEndPoint().get(0);
        assertTrue(mep.administrativeState());
        assertEquals("00:b0:ae:03:ff:31", mep.macAddress().toString());

        org.onosproject.yang.gen.v1.mseasoamfm.rev20160229.mseasoamfm.mefcfm.maintenancedomain
        .maintenanceassociation.maintenanceassociationendpoint
        .AugmentedMseaCfmMaintenanceAssociationEndPoint augmentedMep =
            MepIdUtil2.convertFmAugmentedMep(mep);

        assertEquals("partially-active", augmentedMep.connectivityStatus().toString());
        assertEquals("up", augmentedMep.interfaceStatus().enumeration().toString());
    }

    /**
     * Driven by SAMPLE_MSEACFM_DELAY_MEASUREMENT_FULL_REPLY.
     * @throws NetconfException If there's a problem
     */
    @Test
    public void testGetSoamDm() throws NetconfException {
        MseaCfm mseaCfmWithDm = mseaCfmService.getSoamDm(
                MdIdCharStr.asMdId("md-1"),
                MaIdCharStr.asMaId("ma-1-1"),
                MepId.valueOf((short) 1),
                SoamId.valueOf(1),
                DmEntryParts.ALL_PARTS, session);

        assertNotNull(mseaCfmWithDm);
        MaintenanceAssociationEndPoint mep = mseaCfmWithDm.mefCfm()
            .maintenanceDomain().get(0)
            .maintenanceAssociation().get(0)
            .maintenanceAssociationEndPoint().get(0);

        //Because of a checkstyle problem with typecasts including really long
        //package names, this has to be handed off to a different class
        org.onosproject.yang.gen.v1.mseasoampm.rev20160229.mseasoampm.mefcfm.maintenancedomain
        .maintenanceassociation.maintenanceassociationendpoint
        .AugmentedMseaCfmMaintenanceAssociationEndPoint augmentedMep =
                MepIdUtil.convertPmAugmentedMep(mep);

        DelayMeasurement dm = augmentedMep.delayMeasurements().delayMeasurement().get(0);
        assertEquals(true, dm.administrativeState());
        assertTrue(dm.measurementEnable().get(3)); //frame-delay-two-way-bins
        assertTrue(dm.measurementEnable().get(1)); //frame-delay-two-way-max

        assertEquals(MessagePeriodEnum.YANGAUTOPREFIX3MS.name(), dm.messagePeriod().name());
        assertEquals(6, dm.priority().uint8());
        assertEquals(1000, dm.frameSize());
        assertEquals(15, dm.measurementInterval());
        assertEquals(32, dm.numberIntervalsStored());
        assertEquals(SessionStatusTypeEnum.ACTIVE.name(),
                dm.sessionStatus().enumeration().name());
        assertEquals(100, dm.frameDelayTwoWay().uint32());
        assertEquals(101, dm.interFrameDelayVariationTwoWay().uint32());

        //The remoteMep of the DM is a choice, which for mepId is a leafref object
        org.onosproject.yang.gen.v1.mseasoampm.rev20160229.mseasoampm.remotemepgroup.remotemep
        .DefaultMepId remoteMepId = MepIdUtil3.convertPmRemoteMepToMepId(dm.remoteMep());
        assertNotNull(remoteMepId);
        assertEquals(10, ((MepIdType) remoteMepId.mepId()).uint16());

    }

    /**
     * Create the Maintenance Domain "md-1".
     * @throws NetconfException
     */
    @Test
    public void testSetMseaCfm() throws NetconfException {
        NameCharacterString mdName = new DefaultNameCharacterString();
        mdName.name(Identifier45.fromString("md-1"));

        MaintenanceDomain yangMd = new DefaultMaintenanceDomain();
        yangMd.id((short) 1);
        yangMd.mdNameAndTypeCombo(mdName);

        MefCfm mefCfm = new DefaultMefCfm();
        mefCfm.addToMaintenanceDomain(yangMd);
      //FIXME implement this
//        MseaCfmOpParam mseaCfmOpParam = (MseaCfmOpParam) MseaCfmOpParam.builder().mefCfm(mefCfm).build();
//        mseaCfmService.setMseaCfm(mseaCfmOpParam, session, NcDsType.running);
    }

    @Test
    /**
     * Using Remote remote MEP ID and all arguments.
     */
    public void testTransmitLoopback1() {
        TransmitLoopbackInput lbTr1 = new DefaultTransmitLoopbackInput();
        lbTr1.maintenanceDomain(Short.valueOf((short) 1));
        lbTr1.maintenanceAssociation(Short.valueOf((short) 2));
        lbTr1.maintenanceAssociationEndPoint(Short.valueOf((short) 3));

        DefaultTargetAddress ta = new DefaultTargetAddress();
        DefaultMepId mepId = new DefaultMepId();
        mepId.mepId(MepIdType.of(4));
        ta.addressType((AddressType) mepId);
        lbTr1.targetAddress(ta);

//        lbTr1.dataTlv(new byte[]{0x01, 0x02, 0x03}); Not supported in onos-yang-tools just yet
        lbTr1.numberOfMessages(10);
        lbTr1.vlanDropEligible(true);
        lbTr1.vlanPriority(PriorityType.of((short) 1));
        try {
            mseaCfmService.transmitLoopback(lbTr1, session);
        } catch (NetconfException e) {
            fail("Calling of TransmitLoopback failed: " + e.getMessage());
        }
    }

    @Test
    /**
     * Using Remote Mac address in place of remote MEP ID and fewer arguments.
     */
    public void testTransmitLoopback2() {
        TransmitLoopbackInput lbTr2 = new DefaultTransmitLoopbackInput();

        lbTr2.maintenanceDomain(Short.valueOf((short) 63));
        lbTr2.maintenanceAssociation(Short.valueOf((short) 62));
        lbTr2.maintenanceAssociationEndPoint(Short.valueOf((short) 61));

        DefaultTargetAddress ta = new DefaultTargetAddress();
        DefaultMacAddress macAddr = new DefaultMacAddress();
        macAddr.macAddress(MacAddress.of("FF:EE:DD:CC:BB:AA"));
        ta.addressType(macAddr);
        lbTr2.targetAddress(ta);
        try {
            mseaCfmService.transmitLoopback(lbTr2, session);
        } catch (NetconfException e) {
            fail("Calling of TransmitLoopback failed: " + e.getMessage());
        }
    }

    @Test
    public void testAbortLoopback() throws NetconfException {
        AbortLoopbackInput lbAbort = new DefaultAbortLoopbackInput();

        lbAbort.maintenanceDomain((short) 70);
        lbAbort.maintenanceAssociation((short) 71);
        lbAbort.maintenanceAssociationEndPoint((short) 72);

        try {
            mseaCfmService.abortLoopback(lbAbort, session);
        } catch (NetconfException e) {
            fail("Calling of AbortLoopback failed: " + e.getMessage());
        }
    }

    @Test
    public void testTransmitLinktrace() throws NetconfException {
        try {
            mseaCfmService.transmitLinktrace(null, session);
        } catch (UnsupportedOperationException e) {
            assertTrue(e.getMessage().contains("Not yet implemented"));
        }
    }

}
