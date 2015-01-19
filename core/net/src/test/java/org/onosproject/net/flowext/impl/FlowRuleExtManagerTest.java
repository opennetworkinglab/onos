/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.net.flowext.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.event.impl.TestEventDispatcher;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.Device;
import org.onosproject.net.Device.Type;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceServiceAdapter;
import org.onosproject.net.flowext.FlowRuleBatchExtRequest;
import org.onosproject.net.flowext.FlowRuleExtEntry;
import org.onosproject.net.flowext.FlowRuleExtEvent;
import org.onosproject.net.flowext.FlowRuleExtListener;
import org.onosproject.net.flowext.FlowRuleExtProvider;
import org.onosproject.net.flowext.FlowRuleExtService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.store.trivial.impl.SimpleFlowRuleExtStore;

import com.google.common.collect.Sets;

/**
 * Test for the applyBatch of FlowRuleExtManager .
 */
public class FlowRuleExtManagerTest {


    private static final ProviderId PID = new ProviderId("of", "foo");
    private static final DeviceId DID = DeviceId.deviceId("of:123");
    private static final Device DEV = new DefaultDevice(
            PID, DID, Type.SWITCH, "", "", "", "", null);

    private FlowRuleExtManager mgr;

    protected FlowRuleExtService service;
    protected TestListener listener = new TestListener();

    @Before
    public void setUp() {
        mgr = new FlowRuleExtManager();
        mgr.store = new SimpleFlowRuleExtStore();
        mgr.eventDispatcher = new TestEventDispatcher();
        mgr.deviceService = new TestDeviceService();
        service = mgr;

        mgr.activate();
        mgr.addListener(listener);
    }

    @After
    public void tearDown() {
        service.removeListener(listener);
        mgr.deactivate();
        mgr.eventDispatcher = null;
        mgr.deviceService = null;
    }

    @Test
    public void testApplyBatch() {

        FlowRuleExtEntry r1 = new FlowRuleExtEntry(DID, "of:123:01".getBytes());
        FlowRuleExtEntry r2 = new FlowRuleExtEntry(DID, "of:123:02".getBytes());
        FlowRuleExtEntry r3 = new FlowRuleExtEntry(DID, "of:123:03".getBytes());

        Collection<FlowRuleExtEntry> batchOperation = new ArrayList<FlowRuleExtEntry>();;
        assertTrue("store should be empty",
                   Sets.newHashSet(service.getExtMessages(DID)).isEmpty());
        batchOperation.add(r1);
        batchOperation.add(r2);
        batchOperation.add(r3);
        service.applyBatch(batchOperation);
        Collection<FlowRuleExtEntry> store = (Collection<FlowRuleExtEntry>)service.getExtMessages(DID);
        assertEquals("3 rules should exist", 3, store.size());
        assertThat(store.toArray()[0],equalTo(r1));
        assertThat(store.toArray()[1],equalTo(r2));
        assertThat(store.toArray()[2],equalTo(r3));
    }

    private static class TestListener implements FlowRuleExtListener {
        final List<FlowRuleExtEvent> events = new ArrayList<>();

        @Override
        public void event(FlowRuleExtEvent event) {
            events.add(event);
        }
    }

    private static class TestDeviceService extends DeviceServiceAdapter {

        @Override
        public int getDeviceCount() {
            return 1;
        }

        @Override
        public Iterable<Device> getDevices() {
            return Arrays.asList(DEV);
        }

        @Override
        public Device getDevice(DeviceId deviceId) {
            return DEV;
        }

        @Override
        public MastershipRole getRole(DeviceId deviceId) {
            return null;
        }

        @Override
        public List<Port> getPorts(DeviceId deviceId) {
            return null;
        }

        @Override
        public Port getPort(DeviceId deviceId, PortNumber portNumber) {
            return null;
        }

        @Override
        public boolean isAvailable(DeviceId deviceId) {
            return false;
        }

        @Override
        public void addListener(DeviceListener listener) {
        }

        @Override
        public void removeListener(DeviceListener listener) {
        }

    }

    private class TestProvider extends AbstractProvider implements FlowRuleExtProvider {

        protected TestProvider(ProviderId id) {
            super(PID);
        }

		@Override
		public void applyFlowRule(FlowRuleBatchExtRequest flowRules) {
			// TODO Auto-generated method stub
			
		}

    }

    public class TestApplicationId extends DefaultApplicationId {

        public TestApplicationId(short id, String name) {
            super(id, name);
        }
    }

}
