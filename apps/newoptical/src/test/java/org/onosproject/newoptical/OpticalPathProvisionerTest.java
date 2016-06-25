/*
 * Copyright 2016 Open Networking Laboratory
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

package org.onosproject.newoptical;

import javafx.util.Pair;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.ChassisId;
import org.onlab.util.Bandwidth;
import org.onlab.util.Frequency;
import org.onosproject.cluster.ClusterServiceAdapter;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.core.IdGenerator;
import org.onosproject.event.DefaultEventSinkRegistry;
import org.onosproject.event.Event;
import org.onosproject.event.EventDeliveryService;
import org.onosproject.event.EventSink;
import org.onosproject.mastership.MastershipServiceAdapter;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.CltSignalType;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.DefaultPath;
import org.onosproject.net.DefaultPort;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.ElementId;
import org.onosproject.net.Link;
import org.onosproject.net.OchSignal;
import org.onosproject.net.OduSignalType;
import org.onosproject.net.Path;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.NetworkConfigServiceAdapter;
import org.onosproject.net.device.DeviceServiceAdapter;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentEvent;
import org.onosproject.net.intent.IntentListener;
import org.onosproject.net.intent.IntentServiceAdapter;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.OpticalConnectivityIntent;
import org.onosproject.net.link.LinkServiceAdapter;
import org.onosproject.net.optical.impl.DefaultOchPort;
import org.onosproject.net.optical.impl.DefaultOduCltPort;
import org.onosproject.net.optical.impl.DefaultOmsPort;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.resource.DiscreteResourceId;
import org.onosproject.net.resource.Resource;
import org.onosproject.net.resource.ResourceAllocation;
import org.onosproject.net.resource.ResourceConsumer;
import org.onosproject.net.resource.ResourceId;
import org.onosproject.net.resource.ResourceListener;
import org.onosproject.net.resource.ResourceService;
import org.onosproject.net.topology.LinkWeight;
import org.onosproject.net.topology.PathServiceAdapter;
import org.onosproject.newoptical.api.OpticalConnectivityId;
import org.onosproject.newoptical.api.OpticalPathEvent;
import org.onosproject.newoptical.api.OpticalPathListener;
import org.onosproject.store.service.AtomicCounter;
import org.onosproject.store.service.StorageServiceAdapter;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkState;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.onosproject.net.NetTestTools.injectEventDispatcher;

/**
 * Tests for OpticalPathProvisioner class.
 */
public class OpticalPathProvisionerTest {

    private static final ProviderId PROVIDER_ID = new ProviderId("of", "foo");

    // 7-nodes linear topology containing packet/cross-connect/optical links
    private static final ConnectPoint CP11 = createConnectPoint(1, 1);
    private static final ConnectPoint CP12 = createConnectPoint(1, 2);
    private static final ConnectPoint CP21 = createConnectPoint(2, 1);
    private static final ConnectPoint CP22 = createConnectPoint(2, 2); // cross connect port (packet)
    private static final ConnectPoint CP31 = createConnectPoint(3, 1); // cross connect port (oductl)
    private static final ConnectPoint CP32 = createConnectPoint(3, 2);
    private static final ConnectPoint CP41 = createConnectPoint(4, 1);
    private static final ConnectPoint CP42 = createConnectPoint(4, 2);
    private static final ConnectPoint CP51 = createConnectPoint(5, 1);
    private static final ConnectPoint CP52 = createConnectPoint(5, 2); // cross connect port (oductl)
    private static final ConnectPoint CP61 = createConnectPoint(6, 1); // cross connect port (packet)
    private static final ConnectPoint CP62 = createConnectPoint(6, 2);
    private static final ConnectPoint CP71 = createConnectPoint(7, 1);
    private static final ConnectPoint CP72 = createConnectPoint(7, 2);

    private static final Link LINK1 = createLink(CP12, CP21, Link.Type.DIRECT);
    private static final Link LINK2 = createLink(CP22, CP31, Link.Type.OPTICAL); // cross connect link
    private static final Link LINK3 = createLink(CP32, CP41, Link.Type.OPTICAL);
    private static final Link LINK4 = createLink(CP42, CP51, Link.Type.OPTICAL);
    private static final Link LINK5 = createLink(CP52, CP61, Link.Type.OPTICAL); // cross connect link
    private static final Link LINK6 = createLink(CP62, CP71, Link.Type.DIRECT);

    private static final Device DEVICE1 = createDevice(1, Device.Type.SWITCH);
    private static final Device DEVICE2 = createDevice(2, Device.Type.SWITCH);
    private static final Device DEVICE3 = createDevice(3, Device.Type.ROADM);
    private static final Device DEVICE4 = createDevice(4, Device.Type.ROADM);
    private static final Device DEVICE5 = createDevice(5, Device.Type.ROADM);
    private static final Device DEVICE6 = createDevice(6, Device.Type.SWITCH);
    private static final Device DEVICE7 = createDevice(7, Device.Type.SWITCH);

    private static final Port PORT11 = createPacketPort(DEVICE1, CP11);
    private static final Port PORT12 = createPacketPort(DEVICE1, CP12);
    private static final Port PORT21 = createPacketPort(DEVICE2, CP21);
    private static final Port PORT22 = createOduCltPort(DEVICE2, CP22);
    private static final Port PORT31 = createOchPort(DEVICE3, CP31);
    private static final Port PORT32 = createOmsPort(DEVICE3, CP32);
    private static final Port PORT41 = createOmsPort(DEVICE4, CP41);
    private static final Port PORT42 = createOmsPort(DEVICE4, CP42);
    private static final Port PORT51 = createOmsPort(DEVICE5, CP51);
    private static final Port PORT52 = createOchPort(DEVICE5, CP52);
    private static final Port PORT61 = createOduCltPort(DEVICE6, CP61);
    private static final Port PORT62 = createPacketPort(DEVICE6, CP62);
    private static final Port PORT71 = createPacketPort(DEVICE7, CP71);
    private static final Port PORT72 = createPacketPort(DEVICE7, CP72);

    protected OpticalPathProvisioner target;
    protected TestListener listener = new TestListener();
    protected TestDeviceService deviceService;
    protected TestLinkService linkService;
    protected TestPathService pathService;
    protected TestIntentService intentService;
    protected IdGenerator idGenerator;

    @Before
    public void setUp() {
        this.deviceService = new TestDeviceService();
        deviceService.devMap.put(deviceIdOf(1), DEVICE1);
        deviceService.devMap.put(deviceIdOf(2), DEVICE2);
        deviceService.devMap.put(deviceIdOf(3), DEVICE3);
        deviceService.devMap.put(deviceIdOf(4), DEVICE4);
        deviceService.devMap.put(deviceIdOf(5), DEVICE5);
        deviceService.devMap.put(deviceIdOf(6), DEVICE6);
        deviceService.devMap.put(deviceIdOf(7), DEVICE7);
        deviceService.portMap.put(CP11, PORT11);
        deviceService.portMap.put(CP12, PORT12);
        deviceService.portMap.put(CP21, PORT21);
        deviceService.portMap.put(CP22, PORT22);
        deviceService.portMap.put(CP31, PORT31);
        deviceService.portMap.put(CP32, PORT32);
        deviceService.portMap.put(CP41, PORT41);
        deviceService.portMap.put(CP42, PORT42);
        deviceService.portMap.put(CP51, PORT51);
        deviceService.portMap.put(CP52, PORT52);
        deviceService.portMap.put(CP61, PORT61);
        deviceService.portMap.put(CP62, PORT62);
        deviceService.portMap.put(CP71, PORT71);
        deviceService.portMap.put(CP72, PORT72);

        this.linkService = new TestLinkService();
        linkService.links.addAll(Stream.of(LINK1, LINK2, LINK3, LINK4, LINK5, LINK6)
            .collect(Collectors.toList()));

        this.pathService = new TestPathService();
        this.intentService = new TestIntentService();

        this.target = new OpticalPathProvisioner();
        target.coreService = new TestCoreService();
        target.intentService = this.intentService;
        target.pathService = this.pathService;
        target.linkService = this.linkService;
        target.mastershipService = new TestMastershipService();
        target.clusterService = new TestClusterService();
        target.storageService = new TestStorageService();
        target.deviceService = this.deviceService;
        target.networkConfigService = new TestNetworkConfigService();
        target.resourceService = new TestResourceService();
        injectEventDispatcher(target, new TestEventDispatcher());
        target.addListener(listener);

        target.activate();

        // To overwrite opticalView-ed deviceService
        target.deviceService = this.deviceService;

        idGenerator = new IdGenerator() {
            int counter = 1;

            @Override
            public long getNewId() {
                return counter++;
            }
        };
        Intent.bindIdGenerator(idGenerator);
    }

    @After
    public void tearDown() {
        Intent.unbindIdGenerator(idGenerator);
        target.removeListener(listener);
        target = null;
    }

    /**
     * Checks setupConnectivity method works.
     */
    @Test
    public void testSetupConnectivity() {
        Bandwidth bandwidth = Bandwidth.bps(100);
        Duration latency = Duration.ofMillis(10);

        OpticalConnectivityId cid = target.setupConnectivity(CP12, CP71, bandwidth, latency);
        assertNotNull(cid);

        // Checks path computation is called as expected
        assertEquals(1, pathService.edges.size());
        assertEquals(CP12.deviceId(), pathService.edges.get(0).getKey());
        assertEquals(CP71.deviceId(), pathService.edges.get(0).getValue());

        // Checks intents are installed as expected
        assertEquals(1, intentService.submitted.size());
        assertEquals(OpticalConnectivityIntent.class, intentService.submitted.get(0).getClass());
        OpticalConnectivityIntent connIntent = (OpticalConnectivityIntent) intentService.submitted.get(0);
        assertEquals(CP31, connIntent.getSrc());
        assertEquals(CP52, connIntent.getDst());
    }

    /**
     * Checks setupPath method works.
     */
    @Test
    public void testSetupPath() {
        Bandwidth bandwidth = Bandwidth.bps(100);
        Duration latency = Duration.ofMillis(10);
        List<Link> links = Stream.of(LINK1, LINK2, LINK3, LINK4, LINK5, LINK6)
                .collect(Collectors.toList());
        Path path = new DefaultPath(PROVIDER_ID, links, 0);

        OpticalConnectivityId cid = target.setupPath(path, bandwidth, latency);
        assertNotNull(cid);

        // Checks intents are installed as expected
        assertEquals(1, intentService.submitted.size());
        assertEquals(OpticalConnectivityIntent.class, intentService.submitted.get(0).getClass());
        OpticalConnectivityIntent connIntent = (OpticalConnectivityIntent) intentService.submitted.get(0);
        assertEquals(CP31, connIntent.getSrc());
        assertEquals(CP52, connIntent.getDst());
    }

    /**
     * Checks removeConnectivity method works.
     */
    @Test
    public void testRemoveConnectivity() {
        Bandwidth bandwidth = Bandwidth.bps(100);
        Duration latency = Duration.ofMillis(10);

        OpticalConnectivityId cid = target.setupConnectivity(CP12, CP71, bandwidth, latency);

        // Checks intents are withdrawn
        assertTrue(target.removeConnectivity(cid));
        assertEquals(1, intentService.withdrawn.size());
        assertEquals(OpticalConnectivityIntent.class, intentService.withdrawn.get(0).getClass());
        OpticalConnectivityIntent connIntent = (OpticalConnectivityIntent) intentService.withdrawn.get(0);
        assertEquals(CP31, connIntent.getSrc());
        assertEquals(CP52, connIntent.getDst());
    }

    /**
     * Checks getPath method works.
     */
    @Test
    public void testGetPath() {
        Bandwidth bandwidth = Bandwidth.bps(100);
        Duration latency = Duration.ofMillis(10);
        List<Link> links = Stream.of(LINK1, LINK2, LINK3, LINK4, LINK5, LINK6)
                .collect(Collectors.toList());

        OpticalConnectivityId cid = target.setupConnectivity(CP12, CP71, bandwidth, latency);
        Optional<List<Link>> path = target.getPath(cid);

        // Checks returned path is as expected
        assertTrue(path.isPresent());
        assertEquals(links, path.get());
    }

    /**
     * Checks if PATH_INSTALLED event comes up after intent is installed.
     */
    @Test
    public void testInstalledEvent() {
        Bandwidth bandwidth = Bandwidth.bps(100);
        Duration latency = Duration.ofMillis(10);

        OpticalConnectivityId cid = target.setupConnectivity(CP12, CP71, bandwidth, latency);

        intentService.notifyInstalled();

        assertEquals(1, listener.events.size());
        assertEquals(OpticalPathEvent.Type.PATH_INSTALLED, listener.events.get(0).type());
        assertEquals(cid, listener.events.get(0).subject());
    }

    /**
     * Checks if PATH_REMOVED event comes up after packet link is removed.
     */
    @Test
    public void testRemovedEvent() {
        Bandwidth bandwidth = Bandwidth.bps(100);
        Duration latency = Duration.ofMillis(10);

        OpticalConnectivityId cid = target.setupConnectivity(CP12, CP71, bandwidth, latency);

        intentService.notifyInstalled();

        target.removeConnectivity(cid);

        intentService.notifyWithdrawn();

        assertEquals(2, listener.events.size());
        assertEquals(OpticalPathEvent.Type.PATH_REMOVED, listener.events.get(1).type());
        assertEquals(cid, listener.events.get(1).subject());
    }

    private static ConnectPoint createConnectPoint(long devIdNum, long portIdNum) {
        return new ConnectPoint(
                deviceIdOf(devIdNum),
                PortNumber.portNumber(portIdNum));
    }

    private static Link createLink(ConnectPoint src, ConnectPoint dst, Link.Type type) {
        return DefaultLink.builder()
                .providerId(PROVIDER_ID)
                .src(src)
                .dst(dst)
                .state(Link.State.ACTIVE)
                .type(type).build();
    }

    private static Device createDevice(long devIdNum, Device.Type type) {
        return new DefaultDevice(PROVIDER_ID,
                deviceIdOf(devIdNum),
                type,
                "manufacturer",
                "hwVersion",
                "swVersion",
                "serialNumber",
                new ChassisId(1));
    }

    private static Port createPacketPort(Device device, ConnectPoint cp) {
        return new DefaultPort(device, cp.port(), true);
    }

    private static Port createOchPort(Device device, ConnectPoint cp) {
        return new DefaultOchPort(new DefaultPort(device, cp.port(), true),
                OduSignalType.ODU4,
                true,
                OchSignal.newDwdmSlot(ChannelSpacing.CHL_50GHZ, 1));
    }

    private static Port createOduCltPort(Device device, ConnectPoint cp) {
        return new DefaultOduCltPort(new DefaultPort(device, cp.port(), true),
                CltSignalType.CLT_100GBE);
    }

    private static Port createOmsPort(Device device, ConnectPoint cp) {
        return new DefaultOmsPort(new DefaultPort(device, cp.port(), true),
                Frequency.ofKHz(3),
                Frequency.ofKHz(33),
                Frequency.ofKHz(2));
    }

    private static DeviceId deviceIdOf(long devIdNum) {
        return DeviceId.deviceId(String.format("of:%016d", devIdNum));
    }

    private static class TestListener implements OpticalPathListener {
        final List<OpticalPathEvent> events = new ArrayList<>();

        @Override
        public void event(OpticalPathEvent event) {
            events.add(event);
        }
    }

    private static class TestPathService extends PathServiceAdapter {
        List<Pair<DeviceId, DeviceId>> edges = new ArrayList<>();

        @Override
        public Set<Path> getPaths(ElementId src, ElementId dst, LinkWeight weight) {
            if (!(src instanceof DeviceId && dst instanceof DeviceId)) {
                return Collections.emptySet();
            }

            edges.add(new Pair<>((DeviceId) src, (DeviceId) dst));

            Set<Path> paths = new HashSet<>();
            List<Link> links = Stream.of(LINK1, LINK2, LINK3, LINK4, LINK5, LINK6)
                    .collect(Collectors.toList());
            paths.add(new DefaultPath(PROVIDER_ID, links, 0));

            // returns paths containing single path
            return paths;
        }

    }

    private static class TestIntentService extends IntentServiceAdapter {
        List<Intent> submitted = new ArrayList<>();
        List<Intent> withdrawn = new ArrayList<>();
        List<IntentListener> listeners = new ArrayList<>();

        @Override
        public void submit(Intent intent) {
            submitted.add(intent);
        }

        @Override
        public void withdraw(Intent intent) {
            withdrawn.add(intent);
        }

        @Override
        public void addListener(IntentListener listener) {
            listeners.add(listener);
        }

        @Override
        public Intent getIntent(Key intentKey) {
            Intent intent = submitted.stream().filter(i -> i.key().equals(intentKey))
                    .findAny()
                    .get();
            return intent;
        }

        void notifyInstalled() {
            submitted.forEach(i -> {
                IntentEvent event = new IntentEvent(IntentEvent.Type.INSTALLED, i);
                listeners.forEach(l -> l.event(event));
            });
        }

        void notifyWithdrawn() {
            withdrawn.forEach(i -> {
                IntentEvent event = new IntentEvent(IntentEvent.Type.WITHDRAWN, i);
                listeners.forEach(l -> l.event(event));
            });
        }

    }

    private static class TestLinkService extends LinkServiceAdapter {
        List<Link> links = new ArrayList<>();

        @Override
        public Set<Link> getLinks(ConnectPoint connectPoint) {
            return links.stream()
                    .filter(l -> l.src().equals(connectPoint) || l.dst().equals(connectPoint))
                    .collect(Collectors.toSet());
        }

    }

    private static class TestCoreService extends CoreServiceAdapter {
        @Override
        public ApplicationId registerApplication(String name) {
            return new DefaultApplicationId(0, name);
        }
    }

    private static class TestMastershipService extends MastershipServiceAdapter {

    }

    private static class TestClusterService extends ClusterServiceAdapter {

    }

    private static class TestStorageService extends StorageServiceAdapter {
        @Override
        public AtomicCounter getAtomicCounter(String name) {
            return new MockAtomicCounter();
        }
    }

    private static class TestDeviceService extends DeviceServiceAdapter {
        Map<DeviceId, Device> devMap = new HashMap<>();
        Map<ConnectPoint, Port> portMap = new HashMap<>();

        @Override
        public Device getDevice(DeviceId deviceId) {
            return devMap.get(deviceId);
        }

        @Override
        public Port getPort(DeviceId deviceId, PortNumber portNumber) {
            return portMap.get(new ConnectPoint(deviceId, portNumber));
        }
    }

    private static class TestNetworkConfigService extends NetworkConfigServiceAdapter {

    }

    private static class TestResourceService implements ResourceService {

        @Override
        public List<ResourceAllocation> allocate(ResourceConsumer consumer, List<Resource> resources) {
            List<ResourceAllocation> allocations = new ArrayList<>();

            resources.forEach(r -> allocations.add(new ResourceAllocation(r, consumer.consumerId())));

            return allocations;
        }

        @Override
        public boolean release(List<ResourceAllocation> allocations) {
            return false;
        }

        @Override
        public boolean release(ResourceConsumer consumer) {

            return true;
        }

        @Override
        public void addListener(ResourceListener listener) {

        }

        @Override
        public void removeListener(ResourceListener listener) {

        }

        @Override
        public List<ResourceAllocation> getResourceAllocations(ResourceId id) {
            return null;
        }

        @Override
        public <T> Collection<ResourceAllocation> getResourceAllocations(DiscreteResourceId parent, Class<T> cls) {
            return null;
        }

        @Override
        public Collection<ResourceAllocation> getResourceAllocations(ResourceConsumer consumer) {
            return null;
        }

        @Override
        public Set<Resource> getAvailableResources(DiscreteResourceId parent) {
            return null;
        }

        @Override
        public <T> Set<Resource> getAvailableResources(DiscreteResourceId parent, Class<T> cls) {
            return null;
        }

        @Override
        public <T> Set<T> getAvailableResourceValues(DiscreteResourceId parent, Class<T> cls) {
            return null;
        }

        @Override
        public Set<Resource> getRegisteredResources(DiscreteResourceId parent) {
            return null;
        }

        @Override
        public boolean isAvailable(Resource resource) {
            return true;
        }
    }

    private static class MockAtomicCounter implements AtomicCounter {
        long id = 0;

        @Override
        public long incrementAndGet() {
            return ++id;
        }

        @Override
        public long getAndIncrement() {
            return id++;
        }

        @Override
        public long getAndAdd(long delta) {
            long oldId = id;
            id += delta;
            return oldId;
        }

        @Override
        public long addAndGet(long delta) {
            id += delta;
            return id;
        }

        @Override
        public void set(long value) {
            id = value;
        }

        @Override
        public boolean compareAndSet(long expectedValue, long updateValue) {
            if (id == expectedValue) {
                id = updateValue;
                return true;
            } else {
                return false;
            }
        }

        @Override
        public long get() {
            return id;
        }

        @Override
        public String name() {
            return "MockAtomicCounter";
        }
    }

    // copied from org.onosproject.common.event.impl.TestEventDispatcher
    /**
     * Implements event delivery system that delivers events synchronously, or
     * in-line with the post method invocation.
     */
    public class TestEventDispatcher extends DefaultEventSinkRegistry
            implements EventDeliveryService {

        @Override
        @SuppressWarnings("unchecked")
        public synchronized void post(Event event) {
            EventSink sink = getSink(event.getClass());
            checkState(sink != null, "No sink for event %s", event);
            sink.process(event);
        }

        @Override
        public void setDispatchTimeLimit(long millis) {
        }

        @Override
        public long getDispatchTimeLimit() {
            return 0;
        }
    }
}