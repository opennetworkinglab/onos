/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.net.intent.impl;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onlab.junit.TestUtils.TestUtilsException;
import org.onosproject.core.IdGenerator;
import org.onosproject.event.Event;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.NetworkResource;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.MockIdGenerator;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.resource.ResourceEvent;
import org.onosproject.net.resource.ResourceListener;
import org.onosproject.net.resource.Resources;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyEvent;
import org.onosproject.net.topology.TopologyListener;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import static org.easymock.EasyMock.createMock;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.onosproject.net.resource.ResourceEvent.Type.*;
import static org.onosproject.net.NetTestTools.APP_ID;
import static org.onosproject.net.NetTestTools.device;
import static org.onosproject.net.NetTestTools.link;

/**
 * Tests for the objective tracker.
 */
public class ObjectiveTrackerTest {
    private static final int WAIT_TIMEOUT_SECONDS = 2;
    private Topology topology;
    private ObjectiveTracker tracker;
    private TestTopologyChangeDelegate delegate;
    private List<Event> reasons;
    private TopologyListener listener;
    private DeviceListener deviceListener;
    private ResourceListener resourceListener;
    private IdGenerator mockGenerator;

    /**
     * Initialization shared by all test cases.
     *
     * @throws TestUtilsException if any filed look ups fail
     */
    @Before
    public void setUp() throws TestUtilsException {
        topology = createMock(Topology.class);
        tracker = new ObjectiveTracker();
        delegate = new TestTopologyChangeDelegate();
        tracker.setDelegate(delegate);
        reasons = new LinkedList<>();
        listener = TestUtils.getField(tracker, "listener");
        deviceListener = TestUtils.getField(tracker, "deviceListener");
        resourceListener = TestUtils.getField(tracker, "resourceListener");
        mockGenerator = new MockIdGenerator();
        Intent.bindIdGenerator(mockGenerator);
    }

    /**
     * Code to clean up shared by all test case.
     */
    @After
    public void tearDown() {
        tracker.unsetDelegate(delegate);
        Intent.unbindIdGenerator(mockGenerator);
    }

    /**
     * Topology change delegate mock that tracks the events coming into it
     * and saves them.  It provides a latch so that tests can wait for events
     * to be generated.
     */
    static class TestTopologyChangeDelegate implements TopologyChangeDelegate {

        CountDownLatch latch = new CountDownLatch(1);
        List<Key> intentIdsFromEvent;
        boolean compileAllFailedFromEvent;

        @Override
        public void triggerCompile(Iterable<Key> intentKeys,
                                   boolean compileAllFailed) {
            intentIdsFromEvent = Lists.newArrayList(intentKeys);
            compileAllFailedFromEvent = compileAllFailed;
            latch.countDown();
        }
    }

    /**
     * Tests an event with no associated reasons.
     *
     * @throws InterruptedException if the latch wait fails.
     */
    @Test
    public void testEventNoReasons() throws InterruptedException {
        final TopologyEvent event = new TopologyEvent(
                TopologyEvent.Type.TOPOLOGY_CHANGED,
                topology,
                null);

        listener.event(event);
        assertThat(
                delegate.latch.await(WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                is(true));

        assertThat(delegate.intentIdsFromEvent, hasSize(0));
        assertThat(delegate.compileAllFailedFromEvent, is(true));
    }

    /**
     * Tests an event for a link down where none of the reasons match
     * currently installed intents.
     *
     * @throws InterruptedException if the latch wait fails.
     */
    @Test
    public void testEventLinkDownNoMatches() throws InterruptedException {
        final Link link = link("src", 1, "dst", 2);
        final LinkEvent linkEvent = new LinkEvent(LinkEvent.Type.LINK_REMOVED, link);
        reasons.add(linkEvent);

        final TopologyEvent event = new TopologyEvent(
                TopologyEvent.Type.TOPOLOGY_CHANGED,
                topology,
                reasons);

        listener.event(event);
        assertThat(
                delegate.latch.await(WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                is(true));

        assertThat(delegate.intentIdsFromEvent, hasSize(0));
        assertThat(delegate.compileAllFailedFromEvent, is(false));
    }

    /**
     * Tests an event for a link being added.
     *
     * @throws InterruptedException if the latch wait fails.
     */
    @Test
    public void testEventLinkAdded() throws InterruptedException {
        final Link link = link("src", 1, "dst", 2);
        final LinkEvent linkEvent = new LinkEvent(LinkEvent.Type.LINK_ADDED, link);
        reasons.add(linkEvent);

        final TopologyEvent event = new TopologyEvent(
                TopologyEvent.Type.TOPOLOGY_CHANGED,
                topology,
                reasons);

        listener.event(event);
        assertThat(
                delegate.latch.await(WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                is(true));

        assertThat(delegate.intentIdsFromEvent, hasSize(0));
        assertThat(delegate.compileAllFailedFromEvent, is(true));
    }

    /**
     * Tests an event for a link down where the link matches existing intents.
     *
     * @throws InterruptedException if the latch wait fails.
     */
    @Test
    public void testEventLinkDownMatch() throws Exception {
        final Link link = link("src", 1, "dst", 2);
        final LinkEvent linkEvent = new LinkEvent(LinkEvent.Type.LINK_REMOVED, link);
        reasons.add(linkEvent);

        final TopologyEvent event = new TopologyEvent(
                TopologyEvent.Type.TOPOLOGY_CHANGED,
                topology,
                reasons);

        final Key key = Key.of(0x333L, APP_ID);
        Collection<NetworkResource> resources = ImmutableSet.of(link);
        tracker.addTrackedResources(key, resources);

        listener.event(event);
        assertThat(
                delegate.latch.await(WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                is(true));

        assertThat(delegate.intentIdsFromEvent, hasSize(1));
        assertThat(delegate.compileAllFailedFromEvent, is(false));
        assertThat(delegate.intentIdsFromEvent.get(0).toString(),
                   equalTo("0x333"));
    }

    /**
     * Tests a resource available event.
     *
     * @throws InterruptedException if the latch wait fails.
     */
    @Test
    public void testResourceEvent() throws Exception {
        ResourceEvent event = new ResourceEvent(RESOURCE_ADDED,
                Resources.discrete(DeviceId.deviceId("a"), PortNumber.portNumber(1)).resource());
        resourceListener.event(event);

        assertThat(
                delegate.latch.await(WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                is(true));

        assertThat(delegate.intentIdsFromEvent, hasSize(0));
        assertThat(delegate.compileAllFailedFromEvent, is(true));
    }

    /**
     * Tests an event for a host becoming available that matches an intent.
     *
     * @throws InterruptedException if the latch wait fails.
     */

    @Test
    public void testEventHostAvailableMatch() throws Exception {
        final Device host = device("host1");

        final DeviceEvent deviceEvent =
                new DeviceEvent(DeviceEvent.Type.DEVICE_ADDED, host);
        reasons.add(deviceEvent);

        final Key key = Key.of(0x333L, APP_ID);
        Collection<NetworkResource> resources = ImmutableSet.of(host.id());
        tracker.addTrackedResources(key, resources);

        deviceListener.event(deviceEvent);
        assertThat(
                delegate.latch.await(WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                is(true));

        assertThat(delegate.intentIdsFromEvent, hasSize(1));
        assertThat(delegate.compileAllFailedFromEvent, is(true));
        assertThat(delegate.intentIdsFromEvent.get(0).toString(),
                equalTo("0x333"));
    }

    /**
     * Tests an event for a host becoming unavailable that matches an intent.
     *
     * @throws InterruptedException if the latch wait fails.
     */

    @Test
    public void testEventHostUnavailableMatch() throws Exception {
        final Device host = device("host1");

        final DeviceEvent deviceEvent =
                new DeviceEvent(DeviceEvent.Type.DEVICE_REMOVED, host);
        reasons.add(deviceEvent);

        final Key key = Key.of(0x333L, APP_ID);
        Collection<NetworkResource> resources = ImmutableSet.of(host.id());
        tracker.addTrackedResources(key, resources);

        deviceListener.event(deviceEvent);
        assertThat(
                delegate.latch.await(WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                is(true));

        assertThat(delegate.intentIdsFromEvent, hasSize(1));
        assertThat(delegate.compileAllFailedFromEvent, is(false));
        assertThat(delegate.intentIdsFromEvent.get(0).toString(),
                equalTo("0x333"));
    }

    /**
     * Tests an event for a host becoming available that matches an intent.
     *
     * @throws InterruptedException if the latch wait fails.
     */

    @Test
    public void testEventHostAvailableNoMatch() throws Exception {
        final Device host = device("host1");

        final DeviceEvent deviceEvent =
                new DeviceEvent(DeviceEvent.Type.DEVICE_ADDED, host);
        reasons.add(deviceEvent);

        deviceListener.event(deviceEvent);
        assertThat(
                delegate.latch.await(WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                is(true));

        assertThat(delegate.intentIdsFromEvent, hasSize(0));
        assertThat(delegate.compileAllFailedFromEvent, is(true));
    }


}
