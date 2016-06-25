/*
 * Copyright 2015-present Open Networking Laboratory
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
import com.google.common.collect.Sets;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onlab.packet.IpAddress;
import org.onosproject.common.event.impl.TestEventDispatcher;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.incubator.store.mcast.impl.DistributedMcastStore;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.PortNumber;
import org.onosproject.net.mcast.McastEvent;
import org.onosproject.net.mcast.McastListener;
import org.onosproject.net.mcast.McastRoute;
import org.onosproject.store.service.TestStorageService;

import java.util.List;

import static junit.framework.Assert.fail;
import static junit.framework.TestCase.assertEquals;
import static org.onosproject.net.NetTestTools.did;
import static org.onosproject.net.NetTestTools.injectEventDispatcher;

/**
 * Tests for the multicast RIB.
 */
public class MulticastRouteManagerTest {

    McastRoute r1 = new McastRoute(IpAddress.valueOf("1.1.1.1"),
                                   IpAddress.valueOf("1.1.1.2"),
                                   McastRoute.Type.IGMP);

    McastRoute r11 = new McastRoute(IpAddress.valueOf("1.1.1.1"),
                                    IpAddress.valueOf("1.1.1.2"),
                                    McastRoute.Type.STATIC);

    McastRoute r2 = new McastRoute(IpAddress.valueOf("2.2.2.1"),
                                   IpAddress.valueOf("2.2.2.2"),
                                   McastRoute.Type.PIM);

    ConnectPoint cp1 = new ConnectPoint(did("1"), PortNumber.portNumber(1));

    ConnectPoint cp2 = new ConnectPoint(did("2"), PortNumber.portNumber(2));

    private TestMulticastListener listener = new TestMulticastListener();

    private MulticastRouteManager manager;

    private List<McastEvent> events;

    private DistributedMcastStore mcastStore;

    @Before
    public void setUp() throws Exception {
        manager = new MulticastRouteManager();
        mcastStore = new DistributedMcastStore();
        TestUtils.setField(mcastStore, "storageService", new TestStorageService());
        injectEventDispatcher(manager, new TestEventDispatcher());
        events = Lists.newArrayList();
        manager.store = mcastStore;
        mcastStore.activate();
        manager.activate();
        manager.addListener(listener);
    }

    @After
    public void tearDown() {
        manager.removeListener(listener);
        manager.deactivate();
        mcastStore.deactivate();
    }

    @Test
    public void testAdd() {
        manager.add(r1);

        validateEvents(McastEvent.Type.ROUTE_ADDED);
    }

    @Test
    public void testRemove() {
        manager.add(r1);

        manager.remove(r1);


        validateEvents(McastEvent.Type.ROUTE_ADDED, McastEvent.Type.ROUTE_REMOVED);
    }

    @Test
    public void testAddSource() {
        manager.addSource(r1, cp1);

        validateEvents(McastEvent.Type.SOURCE_ADDED);
        assertEquals("Route is not equal", cp1, manager.fetchSource(r1));
    }

    @Test
    public void testAddSink() {
        manager.addSink(r1, cp1);

        validateEvents(McastEvent.Type.SINK_ADDED);
        assertEquals("Route is not equal", Sets.newHashSet(cp1), manager.fetchSinks(r1));
    }

    @Test
    public void testRemoveSink() {

        manager.addSource(r1, cp1);
        manager.addSink(r1, cp1);
        manager.addSink(r1, cp2);
        manager.removeSink(r1, cp2);

        validateEvents(McastEvent.Type.SOURCE_ADDED,
                       McastEvent.Type.SINK_ADDED,
                       McastEvent.Type.SINK_ADDED,
                       McastEvent.Type.SINK_REMOVED);
        assertEquals("Route is not equal", Sets.newHashSet(cp1), manager.fetchSinks(r1));
    }

    private void validateEvents(McastEvent.Type... evs) {
        if (events.size() != evs.length) {
            fail(String.format("Mismatch number of events# obtained -> %s : expected %s",
                               events, evs));
        }

        for (int i = 0; i < evs.length; i++) {
            if (evs[i] != events.get(i).type()) {
                fail(String.format("Mismatched events# obtained -> %s : expected %s",
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

    private class TestCoreService extends CoreServiceAdapter {
        @Override
        public ApplicationId registerApplication(String name) {
            return new DefaultApplicationId(0, name);
        }
    }
}
