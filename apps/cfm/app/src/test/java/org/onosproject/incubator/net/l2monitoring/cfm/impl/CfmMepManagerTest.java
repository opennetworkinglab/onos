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
package org.onosproject.incubator.net.l2monitoring.cfm.impl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onlab.packet.ChassisId;
import org.onosproject.common.event.impl.TestEventDispatcher;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.core.IdGenerator;
import org.onosproject.incubator.net.l2monitoring.cfm.DefaultMaintenanceAssociation;
import org.onosproject.incubator.net.l2monitoring.cfm.DefaultMep;
import org.onosproject.incubator.net.l2monitoring.cfm.DefaultMepLbCreate;
import org.onosproject.incubator.net.l2monitoring.cfm.DefaultMepLtCreate;
import org.onosproject.incubator.net.l2monitoring.cfm.MaintenanceAssociation;
import org.onosproject.incubator.net.l2monitoring.cfm.Mep;
import org.onosproject.incubator.net.l2monitoring.cfm.MepEntry;
import org.onosproject.incubator.net.l2monitoring.cfm.MepLbCreate;
import org.onosproject.incubator.net.l2monitoring.cfm.MepLtCreate;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdCharStr;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdShort;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdId;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdIdCharStr;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepKeyId;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmMdService;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmMepProgrammable;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmMepService;
import org.onosproject.incubator.net.l2monitoring.cfm.service.MepStore;
import org.onosproject.incubator.net.l2monitoring.soam.SoamDmProgrammable;
import org.onosproject.incubator.net.l2monitoring.soam.impl.TestSoamDmProgrammable;
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
import org.onosproject.store.service.AsyncDocumentTree;
import org.onosproject.store.service.DocumentTreeBuilder;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.TestAsyncDocumentTree;
import org.onosproject.store.service.TestStorageService;
import org.onosproject.store.service.TestTopic;
import org.onosproject.store.service.Topic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.onosproject.net.NetTestTools.injectEventDispatcher;

/**
 * CFM MEP Manager test.
 */
public class CfmMepManagerTest {
    private static final String TEST_MFR = "testMfr";
    private static final String TEST_HW_VERSION = "testHwVersion";
    private static final String TEST_SW_VERSION = "testSwVersion";
    private static final String TEST_SN = "testSn";
    private static final String TEST_DRIVER = "testDriver";
    public static final String TEST_DRIVER_3 = "testDriver3";
    public static final String TEST_SW_3 = "testSw3";
    private final CfmMdService mdService = createMock(CfmMdService.class);
    private final DeviceService deviceService = createMock(DeviceService.class);
    private final DriverService driverService = createMock(DriverService.class);

    private CfmMepService mepService;
    private CfmMepManager mepManager;
    private MepStore mepStore;
    private StorageService storageService;

    protected static final MdId MDNAME1 = MdIdCharStr.asMdId("md-1");
    protected static final MdId MDNAME2 = MdIdCharStr.asMdId("md-2");
    protected static final MaIdShort MANAME1 = MaIdCharStr.asMaId("ma-1-1");
    protected static final MaIdShort MANAME2 = MaIdCharStr.asMaId("ma-2-2");

    private MaintenanceAssociation ma1;
    private MaintenanceAssociation ma2;
    protected static final MepId MEPID1 = MepId.valueOf((short) 10);
    protected static final MepId MEPID11 = MepId.valueOf((short) 11);
    protected static final MepId MEPID12 = MepId.valueOf((short) 12);
    protected static final MepId MEPID2 = MepId.valueOf((short) 20);
    protected static final MepId MEPID21 = MepId.valueOf((short) 21);
    protected static final MepId MEPID22 = MepId.valueOf((short) 22);

    protected static final DeviceId DEVICE_ID1 = DeviceId.deviceId("netconf:1.2.3.4:830");
    protected static final DeviceId DEVICE_ID2 = DeviceId.deviceId("netconf:2.2.3.4:830");

    private Mep mep1;
    private Mep mep11;
    private Mep mep12;
    private Mep mep2;
    private Mep mep21;
    private Mep mep22;

    private Device device1;
    private Device device2;

    private Driver testDriver;

    @Before
    public void setup() throws CfmConfigException {
        mepManager = new CfmMepManager();
        mepStore = new DistributedMepStore();
        storageService = new MockStorageService();

        ma1 = DefaultMaintenanceAssociation.builder(MANAME1, MDNAME1.getNameLength()).build();
        ma2 = DefaultMaintenanceAssociation.builder(MANAME2, MDNAME2.getNameLength()).build();

        TestUtils.setField(mepStore, "storageService", storageService);
        ((DistributedMepStore) mepStore).activate();

        TestUtils.setField(mepManager, "coreService", new TestCoreService());
        TestUtils.setField(mepManager, "deviceService", deviceService);
        TestUtils.setField(mepManager, "cfmMdService", mdService);
        TestUtils.setField(mepManager, "mepStore", mepStore);
        injectEventDispatcher(mepManager, new TestEventDispatcher());

        mepService = mepManager;
        mepManager.activate();

        mep1 = DefaultMep.builder(MEPID1, DEVICE_ID1, PortNumber.P0,
                Mep.MepDirection.UP_MEP, MDNAME1, MANAME1).build();
        mepStore.createUpdateMep(new MepKeyId(MDNAME1, MANAME1, MEPID1), mep1);

        mep11 = DefaultMep.builder(MEPID11, DEVICE_ID1, PortNumber.P0,
                Mep.MepDirection.UP_MEP, MDNAME1, MANAME1).build();
        mepStore.createUpdateMep(new MepKeyId(MDNAME1, MANAME1, MEPID11), mep11);

        mep12 = DefaultMep.builder(MEPID12, DEVICE_ID1, PortNumber.P0,
                Mep.MepDirection.UP_MEP, MDNAME2, MANAME2).build();
        mepStore.createUpdateMep(new MepKeyId(MDNAME2, MANAME2, MEPID12), mep12);

        mep2 = DefaultMep.builder(MEPID2, DEVICE_ID2, PortNumber.portNumber(2),
                Mep.MepDirection.UP_MEP, MDNAME1, MANAME1).build();
        mepStore.createUpdateMep(new MepKeyId(MDNAME1, MANAME1, MEPID2), mep2);

        mep21 = DefaultMep.builder(MEPID21, DEVICE_ID2, PortNumber.portNumber(2),
                Mep.MepDirection.UP_MEP, MDNAME1, MANAME1).build();
        mepStore.createUpdateMep(new MepKeyId(MDNAME1, MANAME1, MEPID21), mep21);

        mep22 = DefaultMep.builder(MEPID22, DEVICE_ID2, PortNumber.portNumber(2),
                Mep.MepDirection.UP_MEP, MDNAME2, MANAME2).build();
        mepStore.createUpdateMep(new MepKeyId(MDNAME2, MANAME2, MEPID22), mep22);

        device1 = new DefaultDevice(
                ProviderId.NONE, DEVICE_ID1, Device.Type.SWITCH,
                TEST_MFR, TEST_HW_VERSION, TEST_SW_VERSION, TEST_SN,
                new ChassisId(1),
                DefaultAnnotations.builder().set(AnnotationKeys.DRIVER, TEST_DRIVER).build());

        device2 = new DefaultDevice(
                ProviderId.NONE, DEVICE_ID2, Device.Type.SWITCH,
                TEST_MFR, TEST_HW_VERSION, TEST_SW_VERSION, TEST_SN,
                new ChassisId(2),
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
//        mepManager.deactivate();
    }

    @Test
    public void testGetAllMeps() throws CfmConfigException {

        expect(mdService.getMaintenanceAssociation(MDNAME1, MANAME1))
                .andReturn(Optional.ofNullable(ma1))
                .anyTimes();
        replay(mdService);

        expect(deviceService.getDevice(DEVICE_ID1)).andReturn(device1).anyTimes();
        expect(deviceService.getDevice(DEVICE_ID2)).andReturn(device2).anyTimes();
        replay(deviceService);

        expect(driverService.getDriver(DEVICE_ID1)).andReturn(testDriver).anyTimes();
        expect(driverService.getDriver(DEVICE_ID2)).andReturn(testDriver).anyTimes();
        replay(driverService);

        Collection<MepEntry> mepEntries = mepManager.getAllMeps(MDNAME1, MANAME1);

        assertEquals(4, mepEntries.size());
    }

    @Test
    public void testGetMep() throws CfmConfigException {

        expect(mdService.getMaintenanceAssociation(MDNAME1, MANAME1))
                .andReturn(Optional.ofNullable(ma1))
                .anyTimes();
        replay(mdService);

        expect(deviceService.getDevice(DEVICE_ID1)).andReturn(device1).anyTimes();
        replay(deviceService);

        expect(driverService.getDriver(DEVICE_ID1)).andReturn(testDriver).anyTimes();
        replay(driverService);

        MepEntry mepEntry = mepManager.getMep(MDNAME1, MANAME1, MEPID1);

        assertEquals(MEPID1.value(), mepEntry.mepId().value());
    }

    @Test
    public void testGetMepMissing() {

        expect(mdService.getMaintenanceAssociation(MDNAME1, MANAME1))
                .andReturn(Optional.ofNullable(ma1))
                .anyTimes();
        replay(mdService);

        expect(deviceService.getDevice(DEVICE_ID1)).andReturn(null).anyTimes();
        replay(deviceService);

        expect(driverService.getDriver(DEVICE_ID1)).andReturn(testDriver).anyTimes();
        replay(driverService);

        try {
            mepManager.getMep(MDNAME1, MANAME1, MEPID1);
            fail("Expecting CfmConfigException because device does not exist");
        } catch (CfmConfigException e) {
            assertEquals("Device not found netconf:1.2.3.4:830", e.getMessage());
        }
    }

    @Test
    public void testDeleteMep() throws CfmConfigException {
        expect(mdService.getMaintenanceAssociation(MDNAME1, MANAME1))
                .andReturn(Optional.ofNullable(ma1))
                .anyTimes();
        replay(mdService);

        expect(deviceService.getDevice(DEVICE_ID1)).andReturn(device1).anyTimes();
        expect(deviceService.getDevice(DEVICE_ID2)).andReturn(device2).anyTimes();
        replay(deviceService);

        expect(driverService.getDriver(DEVICE_ID1)).andReturn(testDriver).anyTimes();
        expect(driverService.getDriver(DEVICE_ID2)).andReturn(testDriver).anyTimes();
        replay(driverService);

        assertTrue(mepManager.deleteMep(MDNAME1, MANAME1, MEPID1, Optional.empty()));
    }

    @Test
    public void testCreateMep() throws CfmConfigException {
        expect(mdService.getMaintenanceAssociation(MDNAME1, MANAME1))
                .andReturn(Optional.ofNullable(ma1))
                .anyTimes();
        replay(mdService);

        expect(deviceService.getDevice(DEVICE_ID1)).andReturn(device1).anyTimes();
        expect(deviceService.getDevice(DEVICE_ID2)).andReturn(device2).anyTimes();
        replay(deviceService);

        expect(driverService.getDriver(DEVICE_ID1)).andReturn(testDriver).anyTimes();
        expect(driverService.getDriver(DEVICE_ID2)).andReturn(testDriver).anyTimes();
        replay(driverService);

        MepId mepId3 = MepId.valueOf((short) 3);
        Mep mep3 = DefaultMep.builder(mepId3, DEVICE_ID1, PortNumber.portNumber(1),
                Mep.MepDirection.UP_MEP, MDNAME1, MANAME1).build();

        //Expecting false - since it was not found
        assertTrue(mepManager.createMep(MDNAME1, MANAME1, mep3));
    }

    @Test
    public void testCreateMepBehaviorNotSupported() throws CfmConfigException {
        final DeviceId deviceId3 = DeviceId.deviceId("netconf:3.2.3.4:830");

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

        expect(mdService.getMaintenanceAssociation(MDNAME1, MANAME1))
                .andReturn(Optional.ofNullable(ma1))
                .anyTimes();
        replay(mdService);

        expect(deviceService.getDevice(deviceId3)).andReturn(device3).anyTimes();
        replay(deviceService);

        expect(driverService.getDriver(deviceId3)).andReturn(testDriver3).anyTimes();
        replay(driverService);

        MepId mepId3 = MepId.valueOf((short) 3);
        Mep mep3 = DefaultMep.builder(mepId3, deviceId3, PortNumber.portNumber(1),
                Mep.MepDirection.UP_MEP, MDNAME1, MANAME1).build();

        try {
            mepManager.createMep(MDNAME1, MANAME1, mep3);
            fail("Expecting CfmConfigException because driver does not support behavior");
        } catch (CfmConfigException e) {
            assertEquals("Device netconf:3.2.3.4:830 does not support " +
                    "CfmMepProgrammable behaviour.", e.getMessage());
        }
    }

    @Test
    public void testTransmitLoopback() {
        expect(mdService.getMaintenanceAssociation(MDNAME1, MANAME1))
                .andReturn(Optional.ofNullable(ma1))
                .anyTimes();
        replay(mdService);

        expect(deviceService.getDevice(DEVICE_ID1)).andReturn(device1).anyTimes();
        replay(deviceService);

        expect(driverService.getDriver(DEVICE_ID1)).andReturn(testDriver).anyTimes();
        replay(driverService);

        MepLbCreate lbCreate = DefaultMepLbCreate.builder(MepId.valueOf((short) 11)).build();
        try {
            mepService.transmitLoopback(MDNAME1, MANAME1, MEPID1, lbCreate);
        } catch (CfmConfigException e) {
            fail("Not expecting an exception");
        }
    }

    @Test
    public void testAbortLoopback() {
        expect(mdService.getMaintenanceAssociation(MDNAME1, MANAME1))
                .andReturn(Optional.ofNullable(ma1))
                .anyTimes();
        replay(mdService);

        expect(deviceService.getDevice(DEVICE_ID1)).andReturn(device1).anyTimes();
        replay(deviceService);

        expect(driverService.getDriver(DEVICE_ID1)).andReturn(testDriver).anyTimes();
        replay(driverService);

        try {
            mepService.abortLoopback(MDNAME1, MANAME1, MEPID1);
        } catch (CfmConfigException e) {
            fail("Not expecting an exception");
        }
    }

    @Test
    public void testTransmitLinktrace() throws CfmConfigException {
        expect(mdService.getMaintenanceAssociation(MDNAME1, MANAME1))
                .andReturn(Optional.ofNullable(ma1))
                .anyTimes();
        replay(mdService);

        expect(deviceService.getDevice(DEVICE_ID1)).andReturn(device1).anyTimes();
        replay(deviceService);

        expect(driverService.getDriver(DEVICE_ID1)).andReturn(testDriver).anyTimes();
        replay(driverService);

        MepLtCreate ltCreate = DefaultMepLtCreate.builder(MepId.valueOf((short) 11)).build();
        try {
            mepService.transmitLinktrace(MDNAME1, MANAME1, MEPID1, ltCreate);
        } catch (UnsupportedOperationException e) {
            assertEquals("Not yet implemented", e.getMessage());
        }
    }

    @Test
    public void testDeviceRemoved() throws CfmConfigException {
        expect(mdService.getMaintenanceAssociation(MDNAME1, MANAME1))
                .andReturn(Optional.ofNullable(ma1))
                .anyTimes();
        expect(mdService.getMaintenanceAssociation(MDNAME2, MANAME2))
                .andReturn(Optional.ofNullable(ma2))
                .anyTimes();
        replay(mdService);

        expect(deviceService.getDevice(DEVICE_ID1)).andReturn(device1).anyTimes();
        expect(deviceService.getDevice(DEVICE_ID2)).andReturn(device2).anyTimes();
        replay(deviceService);

        expect(driverService.getDriver(DEVICE_ID1)).andReturn(testDriver).anyTimes();
        expect(driverService.getDriver(DEVICE_ID2)).andReturn(testDriver).anyTimes();
        replay(driverService);

//        This is arranged like
//        device1                             device2
//        /       \                           /       \
//    md-1        md-2                     md-1      md-2
//      |           |                         |        |
//    ma-1-1      ma-2-2                   ma-1-1    ma-2-2
//    /    \         |                      /   \        \
//  mep1  mep11   mep12                  mep2 mep21     mep22
        assertNotNull(mepService.getMep(MDNAME1, MANAME1, MEPID1));
        assertNotNull(mepService.getMep(MDNAME1, MANAME1, MEPID11));
        assertNotNull(mepService.getMep(MDNAME2, MANAME2, MEPID12));
        assertNotNull(mepService.getMep(MDNAME1, MANAME1, MEPID2));
        assertNotNull(mepService.getMep(MDNAME1, MANAME1, MEPID21));
        assertNotNull(mepService.getMep(MDNAME2, MANAME2, MEPID22));

        //By deleting Device2 we expect Mep2,21,22 to have been deleted but Mep1,11,12 to remain
        ((CfmMepManager) mepService).processDeviceRemoved(device2);

        assertNotNull(mepService.getMep(MDNAME1, MANAME1, MEPID1));
        assertNotNull(mepService.getMep(MDNAME1, MANAME1, MEPID11));
        assertNotNull(mepService.getMep(MDNAME2, MANAME2, MEPID12));
        //The device 2 related ones are gone
        assertNull(mepService.getMep(MDNAME1, MANAME1, MEPID2));
        assertNull(mepService.getMep(MDNAME1, MANAME1, MEPID21));
        assertNull(mepService.getMep(MDNAME2, MANAME2, MEPID22));

        //Now delete device1
        ((CfmMepManager) mepService).processDeviceRemoved(device1);
        assertNull(mepService.getMep(MDNAME1, MANAME1, MEPID1));
        assertNull(mepService.getMep(MDNAME1, MANAME1, MEPID11));
        assertNull(mepService.getMep(MDNAME2, MANAME2, MEPID12));
    }

    private class TestCoreService extends CoreServiceAdapter {

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

    private static class MockStorageService extends TestStorageService {
        @Override
        public <V> DocumentTreeBuilder<V> documentTreeBuilder() {
            return new DocumentTreeBuilder<V>() {
                @Override
                public AsyncDocumentTree<V> buildDocumentTree() {
                    return build();
                }

                @Override
                @SuppressWarnings("unchecked")
                public AsyncDocumentTree<V> build() {
                    return new TestAsyncDocumentTree<>(name());
                }
            };
        }

        @Override
        public <T> Topic<T> getTopic(String name, Serializer serializer) {
            return new TestTopic<>(name);
        }
    }
}
