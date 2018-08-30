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
package org.onosproject.incubator.net.l2monitoring.soam.impl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onlab.packet.ChassisId;
import org.onosproject.common.event.impl.TestEventDispatcher;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.core.IdGenerator;
import org.onosproject.incubator.net.l2monitoring.cfm.DefaultMepEntry;
import org.onosproject.incubator.net.l2monitoring.cfm.MaintenanceAssociation;
import org.onosproject.incubator.net.l2monitoring.cfm.Mep;
import org.onosproject.incubator.net.l2monitoring.cfm.MepEntry;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdCharStr;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdShort;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdId;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdIdCharStr;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;
import org.onosproject.incubator.net.l2monitoring.cfm.impl.TestCfmMepProgrammable;
import org.onosproject.incubator.net.l2monitoring.cfm.impl.TestDeviceDiscoveryBehavior;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmMdService;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmMepProgrammable;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmMepService;
import org.onosproject.incubator.net.l2monitoring.soam.SoamConfigException;
import org.onosproject.incubator.net.l2monitoring.soam.SoamDmProgrammable;
import org.onosproject.incubator.net.l2monitoring.soam.SoamId;
import org.onosproject.incubator.net.l2monitoring.soam.SoamService;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DefaultDelayMeasurementCreate;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DefaultDelayMeasurementEntry;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementCreate;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementEntry;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementStatCurrent;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementStatHistory;
import org.onosproject.net.AbstractProjectableModel;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.Behaviour;
import org.onosproject.net.driver.DefaultDriver;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.provider.ProviderId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.fail;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertTrue;
import static org.onosproject.net.NetTestTools.injectEventDispatcher;

/**
 * Services OAM Manager test.
 */
public class SoamManagerTest {
    private static final String TEST_MFR = "testMfr";
    private static final String TEST_HW_VERSION = "testHwVersion";
    private static final String TEST_SW_VERSION = "testSwVersion";
    private static final String TEST_SN = "testSn";
    private static final String TEST_DRIVER = "testDriver";
    public static final String TEST_DRIVER_3 = "testDriver3";
    public static final String TEST_SW_3 = "testSw3";


    protected static final MdId MDNAME1 = MdIdCharStr.asMdId("md-1");
    protected static final MaIdShort MANAME1 = MaIdCharStr.asMaId("ma-1-1");
    protected static final MepId MEPID1 = MepId.valueOf((short) 10);
    protected static final DeviceId DEVICE_ID1 = DeviceId.deviceId("netconf:1.2.3.4:830");
    protected static final SoamId DMID101 = SoamId.valueOf(101);
    protected static final SoamId DMID102 = SoamId.valueOf(102);
    protected static final SoamId LMID101 = SoamId.valueOf(201);

    private MaintenanceAssociation ma1;
    private MepEntry mep1;

    private SoamManager soamManager;
    private SoamService soamService;

    private final CfmMdService mdService = createMock(CfmMdService.class);
    private CfmMepService mepService = createMock(CfmMepService.class);
    private final DeviceService deviceService = createMock(DeviceService.class);
    private final DriverService driverService = createMock(DriverService.class);
    private Device device1;
    private Driver testDriver;

    @Before
    public void setup() throws CfmConfigException, SoamConfigException {
        soamManager = new SoamManager();
        TestUtils.setField(soamManager, "coreService", new TestCoreService());
        TestUtils.setField(soamManager, "cfmMepService", mepService);
        TestUtils.setField(soamManager, "deviceService", deviceService);

        injectEventDispatcher(soamManager, new TestEventDispatcher());
        soamService = soamManager;
        soamManager.activate();

        DelayMeasurementEntry dmEntry1 = DefaultDelayMeasurementEntry
                .builder(DMID101, DelayMeasurementCreate.DmType.DM1DMTX,
                        DelayMeasurementCreate.Version.Y17312011,
                        MepId.valueOf((short) 11), Mep.Priority.PRIO5).build();
        DelayMeasurementEntry dmEntry2 = DefaultDelayMeasurementEntry
                .builder(DMID102, DelayMeasurementCreate.DmType.DM1DMTX,
                        DelayMeasurementCreate.Version.Y17312011,
                        MepId.valueOf((short) 11), Mep.Priority.PRIO6).build();

        mep1 = DefaultMepEntry.builder(MEPID1, DEVICE_ID1, PortNumber.P0,
                Mep.MepDirection.UP_MEP, MDNAME1, MANAME1)
                .addToDelayMeasurementList(dmEntry1)
                .addToDelayMeasurementList(dmEntry2)
                .buildEntry();

        device1 = new DefaultDevice(
                ProviderId.NONE, DEVICE_ID1, Device.Type.SWITCH,
                TEST_MFR, TEST_HW_VERSION, TEST_SW_VERSION, TEST_SN,
                new ChassisId(1),
                DefaultAnnotations.builder().set(AnnotationKeys.DRIVER, TEST_DRIVER).build());

        AbstractProjectableModel.setDriverService(null, driverService);

        Map<Class<? extends Behaviour>, Class<? extends Behaviour>> behaviours = new HashMap<>();
        behaviours.put(DeviceDescriptionDiscovery.class, TestDeviceDiscoveryBehavior.class);
        behaviours.put(CfmMepProgrammable.class, TestCfmMepProgrammable.class);
        behaviours.put(SoamDmProgrammable.class, TestSoamDmProgrammable.class);

        testDriver = new DefaultDriver(
                TEST_DRIVER, new ArrayList<Driver>(),
                TEST_MFR, TEST_HW_VERSION, TEST_SW_VERSION,
                behaviours, new HashMap<>());

    }

    @After
    public void tearDown() {
//        soamManager.deactivate();
    }

    @Test
    public void testGetAllDms() throws CfmConfigException, SoamConfigException {
        expect(deviceService.getDevice(DEVICE_ID1)).andReturn(device1).anyTimes();
        replay(deviceService);

        expect(mepService.getMep(MDNAME1, MANAME1, MEPID1)).andReturn(mep1).anyTimes();
        replay(mepService);

        expect(driverService.getDriver(DEVICE_ID1)).andReturn(testDriver).anyTimes();
        replay(driverService);

        Collection<DelayMeasurementEntry> dmEntries =
                soamManager.getAllDms(MDNAME1, MANAME1, MEPID1);
        assertNotNull(dmEntries);
        assertEquals(1, dmEntries.size());
    }

    @Test
    public void testGetDm() throws CfmConfigException, SoamConfigException {
        expect(deviceService.getDevice(DEVICE_ID1)).andReturn(device1).anyTimes();
        replay(deviceService);

        expect(mepService.getMep(MDNAME1, MANAME1, MEPID1)).andReturn(mep1).anyTimes();
        replay(mepService);

        expect(driverService.getDriver(DEVICE_ID1)).andReturn(testDriver).anyTimes();
        replay(driverService);

        DelayMeasurementEntry dmEntry =
                soamManager.getDm(MDNAME1, MANAME1, MEPID1, DMID101);

        assertNotNull(dmEntry);
        assertEquals(DMID101, dmEntry.dmId());
    }

    @Test
    public void testGetDmCurrentStat() throws CfmConfigException, SoamConfigException {
        expect(deviceService.getDevice(DEVICE_ID1)).andReturn(device1).anyTimes();
        replay(deviceService);

        expect(mepService.getMep(MDNAME1, MANAME1, MEPID1)).andReturn(mep1).anyTimes();
        replay(mepService);

        expect(driverService.getDriver(DEVICE_ID1)).andReturn(testDriver).anyTimes();
        replay(driverService);

        DelayMeasurementStatCurrent dmCurrentStat =
                soamManager.getDmCurrentStat(MDNAME1, MANAME1, MEPID1, DMID101);

        assertNotNull(dmCurrentStat);
        assertTrue(dmCurrentStat.startTime().isBefore(Instant.now()));
    }

    @Test
    public void testGetDmHistoryStats() throws CfmConfigException, SoamConfigException {
        expect(deviceService.getDevice(DEVICE_ID1)).andReturn(device1).anyTimes();
        replay(deviceService);

        expect(mepService.getMep(MDNAME1, MANAME1, MEPID1)).andReturn(mep1).anyTimes();
        replay(mepService);

        expect(driverService.getDriver(DEVICE_ID1)).andReturn(testDriver).anyTimes();
        replay(driverService);

        Collection<DelayMeasurementStatHistory> dmHistoricalStats =
                soamManager.getDmHistoricalStats(MDNAME1, MANAME1, MEPID1, DMID101);

        assertNotNull(dmHistoricalStats);
        assertEquals(2, dmHistoricalStats.size());
    }

    @Test
    public void testCreateDm() throws CfmConfigException, SoamConfigException {
        expect(deviceService.getDevice(DEVICE_ID1)).andReturn(device1).anyTimes();
        replay(deviceService);

        expect(mepService.getMep(MDNAME1, MANAME1, MEPID1)).andReturn(mep1).anyTimes();
        replay(mepService);

        expect(driverService.getDriver(DEVICE_ID1)).andReturn(testDriver).anyTimes();
        replay(driverService);

        DelayMeasurementCreate dmCreate1 = DefaultDelayMeasurementCreate
                .builder(DelayMeasurementCreate.DmType.DM1DMTX,
                    DelayMeasurementCreate.Version.Y17312011,
                    MepId.valueOf((short) 11), Mep.Priority.PRIO3)
                .binsPerFdInterval((short) 4)
                .binsPerFdrInterval((short) 5)
                .binsPerIfdvInterval((short) 6)
                .build();

        assertEquals(1000, soamManager.createDm(
                    MDNAME1, MANAME1, MEPID1, dmCreate1).get().value());
    }

    @Test
    public void testCreateDmNoBehavior() throws CfmConfigException, SoamConfigException {
        final DeviceId deviceId3 = DeviceId.deviceId("netconf:3.2.3.4:830");
        final MepId mepId3 = MepId.valueOf((short) 3);

        Map<Class<? extends Behaviour>, Class<? extends Behaviour>> behaviours = new HashMap<>();
        behaviours.put(DeviceDescriptionDiscovery.class, TestDeviceDiscoveryBehavior.class);

        Driver testDriver3 = new DefaultDriver(
                TEST_DRIVER_3, new ArrayList<Driver>(),
                TEST_MFR, TEST_HW_VERSION, TEST_SW_3,
                behaviours, new HashMap<>());

        Device device3 = new DefaultDevice(
                ProviderId.NONE, deviceId3, Device.Type.SWITCH,
                TEST_MFR, TEST_HW_VERSION, TEST_SW_3, TEST_SN,
                new ChassisId(2),
                DefaultAnnotations.builder().set(AnnotationKeys.DRIVER, TEST_DRIVER_3).build());

        expect(deviceService.getDevice(deviceId3)).andReturn(device3).anyTimes();
        replay(deviceService);

        MepEntry mep3 = DefaultMepEntry.builder(mepId3, deviceId3, PortNumber.P0,
                Mep.MepDirection.UP_MEP, MDNAME1, MANAME1)
                .buildEntry();

        expect(mepService.getMep(MDNAME1, MANAME1, mepId3)).andReturn(mep3).anyTimes();
        replay(mepService);

        expect(driverService.getDriver(deviceId3)).andReturn(testDriver3).anyTimes();
        replay(driverService);

        DelayMeasurementCreate dmCreate1 = DefaultDelayMeasurementCreate
                .builder(DelayMeasurementCreate.DmType.DM1DMTX,
                        DelayMeasurementCreate.Version.Y17312011,
                        MepId.valueOf((short) 11), Mep.Priority.PRIO3)
                .binsPerFdInterval((short) 4)
                .binsPerFdrInterval((short) 5)
                .binsPerIfdvInterval((short) 6)
                .build();

        try {
            soamManager.createDm(MDNAME1, MANAME1, mepId3, dmCreate1);
            fail("Expecting exception since device does not support behavior");
        } catch (CfmConfigException e) {
            assertEquals("Device netconf:3.2.3.4:830 from MEP :md-1/" +
                    "ma-1-1/3 does not implement SoamDmProgrammable", e.getMessage());
        }
    }

    @Test
    public void testAbortAllDmOnMep() throws CfmConfigException {
        //TODO: Implement underlying method
        try {
            soamManager.abortDm(MDNAME1, MANAME1, MEPID1);
            fail("Expecting UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
    }

    @Test
    public void testAbortOneDm() throws CfmConfigException {
        //TODO: Implement underlying method
        try {
            soamManager.abortDm(MDNAME1, MANAME1, MEPID1, DMID101);
            fail("Expecting UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
    }

    @Test
    public void testClearAllDmHistoriesOnMep() throws CfmConfigException {
        //TODO: Implement underlying method
        try {
            soamManager.clearDelayHistoryStats(MDNAME1, MANAME1, MEPID1);
            fail("Expecting UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
    }

    @Test
    public void testClearOneDmHistories() throws CfmConfigException {
        //TODO: Implement underlying method
        try {
            soamManager.clearDelayHistoryStats(MDNAME1, MANAME1, MEPID1, DMID101);
            fail("Expecting UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
    }

    @Test
    public void testGetAllLmsOnMep() throws CfmConfigException {
        //TODO: Implement underlying method
        try {
            soamManager.getAllLms(MDNAME1, MANAME1, MEPID1);
            fail("Expecting UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
    }

    @Test
    public void testGetLm() throws CfmConfigException {
        //TODO: Implement underlying method
        try {
            soamManager.getLm(MDNAME1, MANAME1, MEPID1, LMID101);
            fail("Expecting UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
    }

    @Test
    public void testGetLmCurrentStat() {
        //TODO: Implement underlying method
        try {
            soamManager.getLmCurrentStat(MDNAME1, MANAME1, MEPID1, LMID101);
            fail("Expecting UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
    }

    @Test
    public void testGetLmhistoricalStats() {
        //TODO: Implement underlying method
        try {
            soamManager.getLmHistoricalStats(MDNAME1, MANAME1, MEPID1, LMID101);
            fail("Expecting UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
    }

    @Test
    public void testCreateLm() throws CfmConfigException {
        //TODO: Implement underlying method
        try {
            soamManager.createLm(MDNAME1, MANAME1, MEPID1, null);
            fail("Expecting UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
    }

    @Test
    public void testAbortAllLmOnMep() throws CfmConfigException {
        //TODO: Implement underlying method
        try {
            soamManager.abortLm(MDNAME1, MANAME1, MEPID1);
            fail("Expecting UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
    }

    @Test
    public void testAbortOneLm() throws CfmConfigException {
        //TODO: Implement underlying method
        try {
            soamManager.abortLm(MDNAME1, MANAME1, MEPID1, LMID101);
            fail("Expecting UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
    }

    @Test
    public void testClearAllLossHistoryStatsOnMep() throws CfmConfigException {
        //TODO: Implement underlying method
        try {
            soamManager.clearLossHistoryStats(MDNAME1, MANAME1, MEPID1);
            fail("Expecting UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
    }

    @Test
    public void testClearLossHistoryStatsOnLm() throws CfmConfigException {
        //TODO: Implement underlying method
        try {
            soamManager.clearLossHistoryStats(MDNAME1, MANAME1, MEPID1, LMID101);
            fail("Expecting UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
    }

    @Test
    public void testCreateTestSignal() throws CfmConfigException {
        //TODO: Implement underlying method
        try {
            soamManager.createTestSignal(MDNAME1, MANAME1, MEPID1, null);
            fail("Expecting UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
    }

    @Test
    public void testAbortTestSignal() throws CfmConfigException {
        //TODO: Implement underlying method
        try {
            soamManager.abortTestSignal(MDNAME1, MANAME1, MEPID1);
            fail("Expecting UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
    }

    protected class TestCoreService extends CoreServiceAdapter {

        @Override
        public IdGenerator getIdGenerator(String topic) {
            return new IdGenerator() {
                private AtomicLong counter = new AtomicLong(0);

                @Override
                public long getNewId() {
                    return counter.getAndIncrement();
                }
            };
        }
    }
}
