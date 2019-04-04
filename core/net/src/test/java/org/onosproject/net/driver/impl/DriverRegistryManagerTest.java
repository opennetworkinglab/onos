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

package org.onosproject.net.driver.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.cfg.ComponentConfigAdapter;
import org.onosproject.common.event.impl.TestEventDispatcher;
import org.onosproject.component.ComponentService;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.device.DeviceServiceAdapter;
import org.onosproject.net.driver.DefaultDriver;
import org.onosproject.net.driver.DefaultDriverProvider;
import org.onosproject.net.driver.DriverEvent;
import org.onosproject.net.driver.DriverListener;
import org.onosproject.net.driver.TestBehaviour;
import org.onosproject.net.driver.TestBehaviourImpl;

import static org.junit.Assert.*;
import static org.onosproject.net.driver.DefaultDriverTest.*;
import static org.onosproject.net.driver.DriverEvent.Type.DRIVER_ENHANCED;
import static org.onosproject.net.driver.DriverEvent.Type.DRIVER_REDUCED;

/**
 * Suite of tests for the driver registry mechanism.
 */
public class DriverRegistryManagerTest {

    private DriverRegistryManager mgr;
    private TestEventListener testListener = new TestEventListener();
    private TestComponentService componentService = new TestComponentService();

    @Before
    public void setUp() {
        mgr = new DriverRegistryManager();
        mgr.deviceService = new DeviceServiceAdapter();
        mgr.componentConfigService = new ComponentConfigAdapter();
        mgr.eventDispatcher = new TestEventDispatcher();
        mgr.componentService = componentService;
        mgr.activate(null);
    }

    @After
    public void tearDown() {
        mgr.deactivate();
    }

    @Test
    public void basicEvents() {
        mgr.addListener(testListener);
        DefaultDriverProvider mockProvider = new DefaultDriverProvider();
        DefaultDriver driver = new DefaultDriver("foo", Lists.newArrayList(),
                                                 MFR, HW, SW,
                                                 ImmutableMap.of(TestBehaviour.class,
                                                                 TestBehaviourImpl.class),
                                                 ImmutableMap.of("foo", "bar"));
        mockProvider.addDriver(driver);
        mgr.registerProvider(mockProvider);
        assertEquals("wrong driver event type", DRIVER_ENHANCED, testListener.event.type());
        assertSame("wrong driver event subject", driver, testListener.event.subject());

        mgr.unregisterProvider(mockProvider);
        assertEquals("wrong driver event type", DRIVER_REDUCED, testListener.event.type());
        assertSame("wrong driver event subject", driver, testListener.event.subject());

        mgr.removeListener(testListener);
    }

    @Test
    public void managerStart() {
        DefaultDriverProvider mockProvider = new DefaultDriverProvider();
        DefaultDriver driver = new DefaultDriver("default", Lists.newArrayList(),
                                                 MFR, HW, SW,
                                                 ImmutableMap.of(TestBehaviour.class,
                                                                 TestBehaviourImpl.class),
                                                 ImmutableMap.of("foo", "bar"));
        mockProvider.addDriver(driver);
        mgr.registerProvider(mockProvider);
        assertTrue("should be activated", componentService.activated);

        mgr.unregisterProvider(mockProvider);
        assertFalse("should not be dactivated", componentService.activated);
    }

    @Test
    public void basicQueries() {
        DefaultDriverProvider mockProvider = new DefaultDriverProvider();
        DefaultDriver driver = new DefaultDriver("default", Lists.newArrayList(),
                                                 MFR, HW, SW,
                                                 ImmutableMap.of(TestBehaviour.class,
                                                                 TestBehaviourImpl.class),
                                                 ImmutableMap.of("foo", "bar"));
        mockProvider.addDriver(driver);
        mgr.registerProvider(mockProvider);
        assertSame("driver is missing", driver, mgr.getDriver("default"));
        assertSame("driver is missing", driver, mgr.getDriver(MFR, HW, SW));
        assertArrayEquals("driver list is wrong",
                          ImmutableList.of(driver).toArray(),
                          mgr.getDrivers().toArray());
        assertArrayEquals("provider list is wrong",
                          ImmutableList.of(mockProvider).toArray(),
                          mgr.getProviders().toArray());
        assertEquals("wrong behaviour class", TestBehaviourImpl.class,
                     mgr.getBehaviourClass("org.onosproject.net.driver.TestBehaviourImpl"));
    }

    // TODO: add tests for REGEX matching and for driver inheritance

    private class TestEventListener implements DriverListener {
        private DriverEvent event;

        @Override
        public void event(DriverEvent event) {
            this.event = event;
        }
    }

    private class TestComponentService implements ComponentService {
        private boolean activated;

        @Override
        public void activate(ApplicationId appId, String name) {
            activated = true;
        }

        @Override
        public void deactivate(ApplicationId appId, String name) {
            activated = false;
        }
    }
}