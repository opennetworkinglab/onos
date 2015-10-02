/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.incubator.net.mcast.impl;

import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onlab.packet.IpPrefix;
import org.onosproject.common.event.impl.TestEventDispatcher;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.core.IdGenerator;
import org.onosproject.core.Version;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.PortNumber;
import org.onosproject.net.mcast.McastEvent;
import org.onosproject.net.mcast.McastListener;
import org.onosproject.net.mcast.McastRoute;
import org.onosproject.store.service.TestStorageService;

import java.util.List;
import java.util.Set;

import static junit.framework.Assert.fail;
import static junit.framework.TestCase.assertEquals;
import static org.onosproject.net.NetTestTools.did;
import static org.onosproject.net.NetTestTools.injectEventDispatcher;

/**
 * Tests for the multicast RIB.
 */
public class MulticastRouteManagerTest {

    McastRoute r1 = new McastRoute(IpPrefix.valueOf("1.1.1.1/8"),
                                          IpPrefix.valueOf("1.1.1.2/8"),
                                          McastRoute.Type.IGMP);

    McastRoute r11 = new McastRoute(IpPrefix.valueOf("1.1.1.1/8"),
                                          IpPrefix.valueOf("1.1.1.2/8"),
                                          McastRoute.Type.STATIC);

    McastRoute r2 = new McastRoute(IpPrefix.valueOf("2.2.2.1/8"),
                                          IpPrefix.valueOf("2.2.2.2/8"),
                                          McastRoute.Type.PIM);

    ConnectPoint cp1 = new ConnectPoint(did("1"), PortNumber.portNumber(1));

    ConnectPoint cp2 = new ConnectPoint(did("2"), PortNumber.portNumber(2));

    private TestMulticastListener listener = new TestMulticastListener();

    private MulticastRouteManager manager;

    private List<McastEvent> events;

    @Before
    public void setUp() throws Exception {
        manager = new MulticastRouteManager();
        injectEventDispatcher(manager, new TestEventDispatcher());
        TestUtils.setField(manager, "storageService", new TestStorageService());
        TestUtils.setField(manager, "coreService", new TestCoreService());
        events  = Lists.newArrayList();
        manager.activate();
        manager.addListener(listener);
    }

    @After
    public void tearDown() {
        manager.removeListener(listener);
        manager.deactivate();
    }

    @Test
    public void testAdd() {
        manager.add(r1);

        assertEquals("Add failed", manager.mcastRoutes.size(), 1);
        validateEvents(McastEvent.Type.ROUTE_ADDED);
    }

    @Test
    public void testRemove() {
        manager.add(r1);

        manager.remove(r1);

        assertEquals("Remove failed", manager.mcastRoutes.size(), 0);
        validateEvents(McastEvent.Type.ROUTE_ADDED, McastEvent.Type.ROUTE_REMOVED);
    }

    @Test
    public void testAddSource() {
        manager.add(r1);

        manager.addSource(r1, cp1);

        validateEvents(McastEvent.Type.ROUTE_ADDED, McastEvent.Type.SOURCE_ADDED);
        assertEquals("Route is not equal", cp1, manager.fetchSource(r1));
    }

    @Test
    public void testAddSink() {
        manager.add(r1);

        manager.addSource(r1, cp1);
        manager.addSink(r1, cp1);

        validateEvents(McastEvent.Type.ROUTE_ADDED,
                       McastEvent.Type.SOURCE_ADDED,
                       McastEvent.Type.SINK_ADDED);
        assertEquals("Route is not equal", Lists.newArrayList(cp1), manager.fetchSinks(r1));
    }

    @Test
    public void testRemoveSink() {
        manager.add(r1);

        manager.addSource(r1, cp1);
        manager.addSink(r1, cp1);
        manager.addSink(r1, cp2);
        manager.removeSink(r1, cp2);

        validateEvents(McastEvent.Type.ROUTE_ADDED,
                       McastEvent.Type.SOURCE_ADDED,
                       McastEvent.Type.SINK_ADDED,
                       McastEvent.Type.SINK_ADDED,
                       McastEvent.Type.SINK_REMOVED);
        assertEquals("Route is not equal", Lists.newArrayList(cp1), manager.fetchSinks(r1));
    }

    private void validateEvents(McastEvent.Type... evs) {
        if (events.size() != evs.length) {
            fail(String.format("Mismatch number of events# obtained -> %s : expected %s",
                               events, evs));
        }

        for (int i = 0; i < evs.length; i++) {
            if (evs[i] != events.get(i).type()) {
                fail(String.format("Mismtached events# obtained -> %s : expected %s",
                                   events, evs));
            }
        }
    }

    class TestMulticastListener implements McastListener {

        @Override
        public void event(McastEvent event) {
            events.add(event);
        }
    }

    private class TestCoreService implements CoreService {
        @Override
        public Version version() {
            return null;
        }

        @Override
        public Set<ApplicationId> getAppIds() {
            return null;
        }

        @Override
        public ApplicationId getAppId(Short id) {
            return null;
        }

        @Override
        public ApplicationId getAppId(String name) {
            return null;
        }

        @Override
        public ApplicationId registerApplication(String identifier) {
            return new DefaultApplicationId(0, identifier);
        }

        @Override
        public IdGenerator getIdGenerator(String topic) {
            return null;
        }
    }
}
