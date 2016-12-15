/*
 * Copyright 2017-present Open Networking Laboratory
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
import static org.junit.Assert.fail;

import java.io.UncheckedIOException;
import java.text.ParseException;
import java.time.OffsetDateTime;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ip4Address;
import org.onosproject.drivers.microsemi.yang.impl.IetfSystemManager;
import org.onosproject.netconf.NetconfDeviceInfo;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.system.rev20160505.ietfsystemmicrosemi.system.AugmentedSysSystem;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.system.rev20160505.ietfsystemmicrosemi.systemstate.platform.AugmentedSysPlatform;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.system.rev20140806.IetfSystem;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.system.rev20140806.IetfSystemOpParam;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.system.rev20140806.ietfsystem.DefaultSystem;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.system.rev20140806.ietfsystem.System;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.system.rev20140806.ietfsystem.system.Clock;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.system.rev20140806.ietfsystem.system.DefaultClock;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.system.rev20140806.ietfsystem.system.clock.timezone.DefaultTimezoneName;
import org.onosproject.yms.ymsm.YmsService;

public class IetfSystemManagerTest {

    IetfSystemManager sysSvc = null;
    YmsService ymsService;
    NetconfSession session;

    @Before
    public void setUp() throws Exception {
        try {
            sysSvc = new MockIetfSystemManager();
            sysSvc.activate();
        } catch (UncheckedIOException e) {
            fail(e.getMessage());
        }
        NetconfDeviceInfo deviceInfo = new NetconfDeviceInfo("netconf", "netconf", Ip4Address.valueOf("1.2.3.4"), 830);
        session = new MockNetconfSessionEa1000(deviceInfo);
    }

    @After
    public void tearDown() throws Exception {
        sysSvc.deactivate();
    }

    @Test
    public void testGetIetfSystemSession() throws NetconfException {
        Clock.ClockBuilder cBuilder = new DefaultClock.ClockBuilder();
        Clock clock = cBuilder.build();

        System.SystemBuilder sBuilder = new DefaultSystem.SystemBuilder();
        System system = sBuilder.clock(clock).build();

        IetfSystemOpParam.IetfSystemBuilder builder = new IetfSystemOpParam.IetfSystemBuilder();
        IetfSystemOpParam sampleSystem = (IetfSystemOpParam) builder.system(system).build();

        IetfSystem sys = sysSvc.getIetfSystem(sampleSystem, session);
        assertNotNull(sys);

        assertEquals(sys.system().clock().timezone().getClass(), DefaultTimezoneName.class);
        DefaultTimezoneName tzName = (DefaultTimezoneName) sys.system().clock().timezone();
        assertEquals("Etc/UTC", tzName.timezoneName().string());
    }

    @Test
    public void testGetIetfSystemInit() throws NetconfException {

        IetfSystem sys = sysSvc.getIetfSystemInit(session);
        assertNotNull(sys);
        assertNotNull(sys.system());

        AugmentedSysSystem sysSystem = (AugmentedSysSystem) sys.system().yangAugmentedInfo(AugmentedSysSystem.class);

        assertEquals("-8.4683990", sysSystem.longitude().toPlainString());
        assertEquals("51.9036140", sysSystem.latitude().toPlainString());
        assertEquals("4.4.0-53-generic", sys.systemState().platform().osRelease());

        AugmentedSysPlatform sysSystemState =
                (AugmentedSysPlatform) sys.systemState().platform().yangAugmentedInfo(AugmentedSysPlatform.class);

        assertEquals("Eagle Simulator.", sysSystemState.deviceIdentification().serialNumber());
    }

    @Test
    public void testSetCurrentDatetime() throws NetconfException, ParseException {
        sysSvc.setCurrentDatetime(OffsetDateTime.now(), session);
        //Look at MockNetconfSessionEa1000::sampleXmlRegexSetCurrentDatetime() for catching an error
    }
}
