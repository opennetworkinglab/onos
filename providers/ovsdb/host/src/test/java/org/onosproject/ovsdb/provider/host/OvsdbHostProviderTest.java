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
package org.onosproject.ovsdb.provider.host;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.TpPort;
import org.onosproject.net.DeviceId;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.host.HostDescription;
import org.onosproject.net.host.HostProvider;
import org.onosproject.net.host.HostProviderRegistry;
import org.onosproject.net.host.HostProviderService;
import org.onosproject.net.provider.AbstractProviderService;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.ovsdb.controller.DefaultEventSubject;
import org.onosproject.ovsdb.controller.EventSubject;
import org.onosproject.ovsdb.controller.OvsdbClientService;
import org.onosproject.ovsdb.controller.OvsdbController;
import org.onosproject.ovsdb.controller.OvsdbDatapathId;
import org.onosproject.ovsdb.controller.OvsdbEvent;
import org.onosproject.ovsdb.controller.OvsdbEventListener;
import org.onosproject.ovsdb.controller.OvsdbIfaceId;
import org.onosproject.ovsdb.controller.OvsdbNodeId;
import org.onosproject.ovsdb.controller.OvsdbNodeListener;
import org.onosproject.ovsdb.controller.OvsdbPortName;
import org.onosproject.ovsdb.controller.OvsdbPortNumber;
import org.onosproject.ovsdb.controller.OvsdbPortType;

/**
 * Test for ovsdb host provider.
 */
public class OvsdbHostProviderTest {
    private static final MacAddress MAC = MacAddress
            .valueOf("00:00:11:00:00:01");
    private final OvsdbHostProvider provider = new OvsdbHostProvider();
    private final TestHostRegistry hostRegistry = new TestHostRegistry();
    protected OvsdbControllerTest controller = new OvsdbControllerTest();
    private TestHostProviderService providerService;

    @Before
    public void setUp() {
        provider.providerRegistry = hostRegistry;
        provider.controller = controller;
        provider.activate();
    }

    @Test
    public void basics() {
        assertNotNull("registration expected", providerService);
        assertEquals("incorrect provider", provider, providerService.provider());
    }

    @Test
    public void portAdded() {
        DefaultEventSubject eventSubject = new DefaultEventSubject(MAC, null,
                                                                   new OvsdbPortName("portName"),
                                                                   new OvsdbPortNumber(0L),
                                                                   new OvsdbDatapathId("10002"),
                                                                   new OvsdbPortType("vxlan"),
                                                                   new OvsdbIfaceId("102345"));
        controller.ovsdbEventListener
                .handle(new OvsdbEvent<EventSubject>(
                                                     OvsdbEvent.Type.PORT_ADDED,
                                                     eventSubject));
        assertNotNull("never went throught the provider service",
                      providerService.added);

    }

    @Test
    public void portRemoved() {
        DefaultEventSubject eventSubject = new DefaultEventSubject(MAC, null,
                                                                   new OvsdbPortName("portName"),
                                                                   new OvsdbPortNumber(0L),
                                                                   new OvsdbDatapathId("10002"),
                                                                   new OvsdbPortType("vxlan"),
                                                                   new OvsdbIfaceId("102345"));
        controller.ovsdbEventListener
                .handle(new OvsdbEvent<EventSubject>(
                                                     OvsdbEvent.Type.PORT_REMOVED,
                                                     eventSubject));
        assertEquals("port status unhandled", 1, providerService.removeCount);
    }

    @After
    public void tearDown() {
        provider.deactivate();
        provider.coreService = null;
        provider.providerRegistry = null;
    }

    private class TestHostRegistry implements HostProviderRegistry {

        @Override
        public HostProviderService register(HostProvider provider) {
            providerService = new TestHostProviderService(provider);
            return providerService;
        }

        @Override
        public void unregister(HostProvider provider) {
        }

        @Override
        public Set<ProviderId> getProviders() {
            return null;
        }

    }

    private class TestHostProviderService
            extends AbstractProviderService<HostProvider>
            implements HostProviderService {

        DeviceId added = null;
        DeviceId moved = null;
        DeviceId spine = null;
        public int removeCount;

        protected TestHostProviderService(HostProvider provider) {
            super(provider);
        }

        @Override
        public void hostDetected(HostId hostId, HostDescription hostDescription, boolean replaceIps) {
            DeviceId descr = hostDescription.location().deviceId();
            if (added == null) {
                added = descr;
            } else if ((moved == null) && !descr.equals(added)) {
                moved = descr;
            } else {
                spine = descr;
            }
        }

        @Override
        public void hostVanished(HostId hostId) {
            removeCount++;
        }

        @Override
        public void removeIpFromHost(HostId hostId, IpAddress ipAddress) {

        }

        @Override
        public void removeLocationFromHost(HostId hostId, HostLocation location) {

        }
    }

    private class OvsdbControllerTest implements OvsdbController {
        private OvsdbEventListener ovsdbEventListener = null;

        @Override
        public void addNodeListener(OvsdbNodeListener listener) {

        }

        @Override
        public void removeNodeListener(OvsdbNodeListener listener) {

        }

        @Override
        public void addOvsdbEventListener(OvsdbEventListener listener) {
            ovsdbEventListener = listener;

        }

        @Override
        public void removeOvsdbEventListener(OvsdbEventListener listener) {
            ovsdbEventListener = null;

        }

        @Override
        public List<OvsdbNodeId> getNodeIds() {
            return null;
        }

        @Override
        public OvsdbClientService getOvsdbClient(OvsdbNodeId nodeId) {
            return null;
        }

        @Override
        public void connect(IpAddress ip, TpPort port) {

        }

        @Override
        public void connect(IpAddress ip, TpPort port, Consumer<Exception> failhandler) {

        }
    }
}
