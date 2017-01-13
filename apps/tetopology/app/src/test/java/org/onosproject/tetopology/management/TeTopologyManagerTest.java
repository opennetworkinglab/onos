/**
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.tetopology.management;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.onosproject.net.NetTestTools.injectEventDispatcher;
import static org.onosproject.tetopology.management.api.TeTopologyEvent.Type.NETWORK_ADDED;
import static org.onosproject.tetopology.management.api.TeTopologyEvent.Type.NETWORK_REMOVED;
import static org.onosproject.tetopology.management.api.TeTopologyEvent.Type.TE_TOPOLOGY_ADDED;
import static org.onosproject.tetopology.management.api.TeTopologyEvent.Type.TE_TOPOLOGY_REMOVED;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.common.event.impl.TestEventDispatcher;
import org.onosproject.event.Event;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.tetopology.management.api.Network;
import org.onosproject.tetopology.management.api.TeTopology;
import org.onosproject.tetopology.management.api.TeTopologyEvent;
import org.onosproject.tetopology.management.api.TeTopologyListener;
import org.onosproject.tetopology.management.api.TeTopologyProvider;
import org.onosproject.tetopology.management.api.TeTopologyProviderRegistry;
import org.onosproject.tetopology.management.api.TeTopologyProviderService;
import org.onosproject.tetopology.management.api.TeTopologyService;
import org.onosproject.tetopology.management.api.link.TeLink;
import org.onosproject.tetopology.management.api.link.TeLinkTpGlobalKey;
import org.onosproject.tetopology.management.api.node.TeNode;
import org.onosproject.tetopology.management.api.node.TeNodeKey;
import org.onosproject.tetopology.management.impl.TeMgrUtil;
import org.onosproject.tetopology.management.impl.TeTopologyManager;

import com.google.common.collect.Lists;

/**
 * Test TeTopology service and TeTopologyProvider service.
 */
public class TeTopologyManagerTest {
    private static final ProviderId PID = new ProviderId("test", "TeTopologyManagerTest");

    private TeTopologyManager mgr;
    protected TeTopologyService service;
    protected TeTopologyProviderRegistry registry;
    protected TeTopologyProviderService providerService;
    protected TestProvider provider;
    protected TestListener listener = new TestListener();

    @Before
    public void setUp() {
        mgr = new TeTopologyManager();
        service = mgr;
        registry = mgr;
        mgr.store = new SimpleTeTopologyStore();

        injectEventDispatcher(mgr, new TestEventDispatcher());

        mgr.activateBasics();
        service.addListener(listener);

        provider = new TestProvider();
        providerService = registry.register(provider);
        assertTrue("providerService should not be null", providerService != null);
        assertTrue("Provider should be registered",
                   registry.getProviders().contains(provider.id()));
    }

    @After
    public void tearDown() {
        registry.unregister(provider);
        assertFalse("provider should not be registered",
                    registry.getProviders().contains(provider.id()));
        service.removeListener(listener);
        mgr.deactivateBasics();
    }

    private void createNetwork() {
        Network originNetwork = DefaultBuilder.buildSampleAbstractNetwork();
        providerService.networkUpdated(originNetwork);
        Network network = service
                .network(TeMgrUtil.toNetworkId(DefaultBuilder.teTopologyKey()));
        assertNotNull("Network should be found", network);
    }

    /**
     * Checks the right events are received when a network with TE topology is
     * added.
     */
    @Test
    public void networkAdded() {
        createNetwork();
        validateEvents(TE_TOPOLOGY_ADDED, NETWORK_ADDED);
    }

    /**
     * Checks the TE topology components are set properly in Manager and Store
     * when a network is added.
     */
    @Test
    public void teTopologyVerify() {
        createNetwork();
        TeTopology teTopology = service
                .teTopology(DefaultBuilder.teTopologyKey());
        assertNotNull("TeTopology should be found", teTopology);
        assertTrue("Number of TE nodes should be 1",
                   teTopology.teNodes().size() == 1);
        assertTrue("Number of TE links should be 1",
                   teTopology.teLinks().size() == 1);
        TeNode teNode = service
                .teNode(new TeNodeKey(DefaultBuilder.teTopologyKey(),
                                      DefaultBuilder.teNode().teNodeId()));
        assertNotNull("TeNode should be found", teNode);
        assertTrue("TE node should be identical", teNode.equals(DefaultBuilder.teNode()));
        assertTrue("Number of TTPs should be 1",
                   teNode.tunnelTerminationPoints().size() == 1);
        TeLink teLink = service
                .teLink(new TeLinkTpGlobalKey(DefaultBuilder
                        .teTopologyKey(), DefaultBuilder.teLink().teLinkKey()));
        assertNotNull("TeLink should be found", teLink);
    }

    /**
     * Checks the right events are received when a network with TE topology is
     * added and then removed.
     */
    @Test
    public void networkRemoved() {
        createNetwork();
        providerService.networkRemoved(TeMgrUtil
                .toNetworkId(DefaultBuilder.teTopologyKey()));
        validateEvents(TE_TOPOLOGY_ADDED, NETWORK_ADDED, NETWORK_REMOVED,
                       TE_TOPOLOGY_REMOVED);
    }

    /**
     * Validates whether the manager receives the right events.
     *
     * @param types a set of types of control message event
     */
    protected void validateEvents(Enum... types) {
        int i = 0;
        assertEquals("wrong events received", types.length, listener.events.size());
        for (Event event : listener.events) {
            assertEquals("incorrect event type", types[i], event.type());
            i++;
        }
        listener.events.clear();
    }

    private class TestProvider extends AbstractProvider implements TeTopologyProvider {
        protected TestProvider() {
            super(PID);
        }
    }

    private static class TestListener implements TeTopologyListener {
        final List<TeTopologyEvent> events = Lists.newArrayList();

        @Override
        public void event(TeTopologyEvent event) {
            events.add(event);
        }
    }

}
