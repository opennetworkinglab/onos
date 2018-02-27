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

import org.onlab.junit.TestUtils;
import org.onlab.packet.ChassisId;
import org.onosproject.common.event.impl.TestEventDispatcher;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.core.IdGenerator;
import org.onosproject.incubator.net.l2monitoring.cfm.DefaultMep;
import org.onosproject.incubator.net.l2monitoring.cfm.Mep;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdCharStr;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdIdCharStr;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepKeyId;
import org.onosproject.incubator.net.l2monitoring.cfm.impl.CfmMdManager;
import org.onosproject.incubator.net.l2monitoring.cfm.impl.CfmMepManager;
import org.onosproject.incubator.net.l2monitoring.cfm.impl.TestCfmMepProgrammable;
import org.onosproject.incubator.net.l2monitoring.cfm.impl.TestDeviceDiscoveryBehavior;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmMepProgrammable;
import org.onosproject.incubator.net.l2monitoring.cfm.service.MepStore;
import org.onosproject.incubator.net.l2monitoring.soam.SoamDmProgrammable;
import org.onosproject.incubator.net.l2monitoring.soam.impl.TestSoamDmProgrammable;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.onosproject.net.NetTestTools.injectEventDispatcher;

/**
 * Supports testing of services that reply on the CfmMepService.
 */
public class MockCfmMepService extends CfmMepManager {
    private static final String TEST_MFR = "testMfr";
    private static final String TEST_HW_VERSION = "testHwVersion";
    private static final String TEST_SW_VERSION = "testSwVersion";
    private static final String TEST_SN = "testSn";
    private static final String TEST_DRIVER = "testDriver";
    protected static final DeviceId DEVICE_ID1 = DeviceId.deviceId("netconf:1.2.3.4:830");


    private final DriverService driverService = createMock(DriverService.class);

    private Device device1;
    private Driver testDriver;


    @Override
    public void activate() {
        mepStore = createMock(MepStore.class);
        cfmMdService = new MockCfmMdService();
        deviceService = createMock(DeviceService.class);
        ((CfmMdManager) cfmMdService).activate();

        device1 = new DefaultDevice(
                ProviderId.NONE, DEVICE_ID1, Device.Type.SWITCH,
                TEST_MFR, TEST_HW_VERSION, TEST_SW_VERSION, TEST_SN,
                new ChassisId(1),
                DefaultAnnotations.builder().set(AnnotationKeys.DRIVER, TEST_DRIVER).build());

        Map<Class<? extends Behaviour>, Class<? extends Behaviour>> behaviours = new HashMap<>();
        behaviours.put(DeviceDescriptionDiscovery.class, TestDeviceDiscoveryBehavior.class);
        behaviours.put(CfmMepProgrammable.class, TestCfmMepProgrammable.class);
        behaviours.put(SoamDmProgrammable.class, TestSoamDmProgrammable.class);

        TestUtils.setField(this, "coreService", new TestCoreService());
        TestUtils.setField(this, "deviceService", deviceService);
        injectEventDispatcher(this, new TestEventDispatcher());

        testDriver = new DefaultDriver(
                TEST_DRIVER, new ArrayList<Driver>(),
                TEST_MFR, TEST_HW_VERSION, TEST_SW_VERSION,
                behaviours, new HashMap<>());

        try {
            Mep mep1 = DefaultMep.builder(
                        MepId.valueOf((short) 10),
                        DEVICE_ID1,
                        PortNumber.P0,
                        Mep.MepDirection.UP_MEP,
                        MdIdCharStr.asMdId("md-1"),
                        MaIdCharStr.asMaId("ma-1-1"))
                    .build();

            expect(mepStore.getMep(new MepKeyId(mep1))).andReturn(Optional.of(mep1)).anyTimes();
        } catch (CfmConfigException e) {
            throw new IllegalArgumentException("Error creating MEPs for test", e);
        }
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
}
