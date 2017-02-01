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
package org.onosproject.ovsdb.provider.tunnel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onosproject.core.GroupId;
import org.onosproject.incubator.net.tunnel.DefaultTunnelDescription;
import org.onosproject.incubator.net.tunnel.IpTunnelEndPoint;
import org.onosproject.incubator.net.tunnel.Tunnel;
import org.onosproject.incubator.net.tunnel.TunnelDescription;
import org.onosproject.incubator.net.tunnel.TunnelEndPoint;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.incubator.net.tunnel.TunnelName;
import org.onosproject.incubator.net.tunnel.TunnelProvider;
import org.onosproject.incubator.net.tunnel.TunnelProviderRegistry;
import org.onosproject.incubator.net.tunnel.TunnelProviderService;
import org.onosproject.incubator.net.tunnel.Tunnel.State;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.DefaultPath;
import org.onosproject.net.Link;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.provider.AbstractProviderService;
import org.onosproject.net.provider.ProviderId;

/**
 * Test for ovsdb tunnel provider.
 */
public class OvsdbTunnelProviderTest {
    private final OvsdbTunnelProvider provider = new OvsdbTunnelProvider();
    private final TestTunnelRegistry tunnelRegistry = new TestTunnelRegistry();
    private TestTunnelProviderService providerService;

    Link link = DefaultLink.builder()
            .providerId(this.provider.id())
            .src(ConnectPoint.deviceConnectPoint("192.168.2.3/20"))
            .dst(ConnectPoint.deviceConnectPoint("192.168.2.4/30"))
            .type(Link.Type.DIRECT)
            .build();

    @Before
    public void setUp() {
        provider.providerRegistry = tunnelRegistry;
        provider.activate();
    }

    @Test
    public void basics() {
        assertNotNull("registration expected", providerService);
        assertEquals("incorrect provider", provider, providerService.provider());
    }

    @Test
    public void testTunnelAdded() {
        TunnelEndPoint src = IpTunnelEndPoint.ipTunnelPoint(IpAddress
                .valueOf("192.168.1.1"));
        TunnelEndPoint dst = IpTunnelEndPoint.ipTunnelPoint(IpAddress
                .valueOf("192.168.1.3"));
        SparseAnnotations annotations = DefaultAnnotations.builder()
                .set("bandwidth", "1024").build();

        List<Link> links = new ArrayList<Link>();
        links.add(link);
        TunnelDescription tunnel = new DefaultTunnelDescription(
                                                                TunnelId.valueOf("1234"),
                                                                src,
                                                                dst,
                                                                Tunnel.Type.VXLAN,
                                                                new GroupId(0),
                                                                this.provider.id(),
                                                                TunnelName.tunnelName("tunnel12"),
                                                                new DefaultPath(this.provider.id(), links, 0.3),
                                                                annotations);
        provider.tunnelAdded(tunnel);
        assertEquals(1, providerService.tunnelSet.size());
    }

    @Test
    public void testTunnelRemoved() {
        TunnelEndPoint src = IpTunnelEndPoint.ipTunnelPoint(IpAddress
                .valueOf("192.168.1.1"));
        TunnelEndPoint dst = IpTunnelEndPoint.ipTunnelPoint(IpAddress
                .valueOf("192.168.1.3"));
        SparseAnnotations annotations = DefaultAnnotations.builder()
                .set("bandwidth", "1024").build();

        List<Link> links = new ArrayList<Link>();
        links.add(link);
        TunnelDescription tunnel = new DefaultTunnelDescription(
                                                                TunnelId.valueOf("1234"),
                                                                src,
                                                                dst,
                                                                Tunnel.Type.VXLAN,
                                                                new GroupId(0),
                                                                this.provider.id(),
                                                                TunnelName.tunnelName("tunnel1"),
                                                                new DefaultPath(this.provider.id(), links, 0.3),
                                                                annotations);
        provider.tunnelRemoved(tunnel);
        assertEquals(0, providerService.tunnelSet.size());
    }

    @After
    public void tearDown() {
        provider.deactivate();
        provider.providerRegistry = null;
    }

    private class TestTunnelRegistry implements TunnelProviderRegistry {

        @Override
        public TunnelProviderService register(TunnelProvider provider) {
            providerService = new TestTunnelProviderService(provider);
            return providerService;
        }

        @Override
        public void unregister(TunnelProvider provider) {

        }

        @Override
        public Set<ProviderId> getProviders() {
            return null;
        }

    }

    private class TestTunnelProviderService
            extends AbstractProviderService<TunnelProvider>
            implements TunnelProviderService {
        Set<TunnelDescription> tunnelSet = new HashSet<TunnelDescription>();

        protected TestTunnelProviderService(TunnelProvider provider) {
            super(provider);
        }

        @Override
        public TunnelId tunnelAdded(TunnelDescription tunnel) {
            tunnelSet.add(tunnel);
            return null;
        }

        @Override
        public TunnelId tunnelAdded(TunnelDescription tunnel, State state) {
            return null;
        }

        @Override
        public void tunnelRemoved(TunnelDescription tunnel) {
            tunnelSet.remove(tunnel);
        }

        @Override
        public void tunnelUpdated(TunnelDescription tunnel) {

        }

        @Override
        public void tunnelUpdated(TunnelDescription tunnel, State state) {

        }

        @Override
        public Tunnel tunnelQueryById(TunnelId tunnelId) {
            return null;
        }

    }
}
