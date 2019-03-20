/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.packetthrottle;

import com.google.common.collect.ImmutableList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.cfg.ComponentConfigAdapter;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketServiceAdapter;
import org.onosproject.net.packet.PacketInFilter;
import com.google.common.collect.Sets;
import org.onosproject.net.packet.packetfilter.ArpPacketClassifier;
import org.onosproject.net.packet.packetfilter.DefaultPacketInFilter;
import static org.junit.Assert.*;


import java.util.ArrayList;
import java.util.Set;
import java.util.List;

/**
 * Set of tests of the PacketThrottleManager.
 */
public class PacketThrottleManagerTest {

    private PacketThrottleManager manager;


    @Before
    public void setUp() {
        manager = new PacketThrottleManager();
        manager.configService = new TestComponentConfig();
        manager.packetService = new MockPacketService();
        manager.activate();
    }

    @After
    public void tearDown() {
        manager.deactivate();
    }

    @Test
    public void testFilter() {
        DefaultPacketInFilter filter;
        ArpPacketClassifier arp = new ArpPacketClassifier();
        filter = new DefaultPacketInFilter(100, 500, 10, 10, PacketThrottleManager.ARP_FILTER, arp);
        manager.packetService.addFilter(filter);
        assertEquals(9, manager.packetService.getFilters().size());
        manager.packetService.removeFilter(filter);
        assertEquals(8, manager.packetService.getFilters().size());
    }

    private class MockPacketService extends PacketServiceAdapter {
        Set<PacketProcessor> packetProcessors = Sets.newHashSet();
        OutboundPacket emittedPacket;
        private List<PacketInFilter> filters = new ArrayList<>();

        @Override
        public void addProcessor(PacketProcessor processor, int priority) {

            packetProcessors.add(processor);
        }

        public void processPacket(PacketContext packetContext) {
            packetProcessors.forEach(p -> p.process(packetContext));
        }

        @Override
        public void emit(OutboundPacket packet) {

            this.emittedPacket = packet;
        }

        @Override
        public void addFilter(PacketInFilter filter) {
            filters.add(filter);
        }

        @Override
        public void removeFilter(PacketInFilter filter) {
            filters.remove(filter);
        }

        @Override
        public List<PacketInFilter> getFilters() {
            return ImmutableList.copyOf(filters);
        }
    }

    /**
     * Mocks the ComponentConfigRegistry.
     */
    private class TestComponentConfig extends ComponentConfigAdapter {

    }
}
