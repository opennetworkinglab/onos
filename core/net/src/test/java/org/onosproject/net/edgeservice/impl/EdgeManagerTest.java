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
package org.onosproject.net.edgeservice.impl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.common.event.impl.TestEventDispatcher;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceServiceAdapter;
import org.onosproject.net.edge.EdgePortEvent;
import org.onosproject.net.edge.EdgePortListener;
import org.onosproject.net.packet.PacketServiceAdapter;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyListener;
import org.onosproject.net.topology.TopologyServiceAdapter;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertFalse;

/**
 * Test of the edge port manager.
 */
public class EdgeManagerTest {

    private EdgeManager mgr;
    private final EdgePortListener testListener = new TestListener();


    @Before
    public void setUp() {
        mgr = new EdgeManager();
        mgr.eventDispatcher = new TestEventDispatcher();
        mgr.topologyService = new TestTopologyManager();
        mgr.deviceService = new TestDeviceManager();
        mgr.packetService = new TestPacketManager();
        mgr.activate();
        mgr.addListener(testListener);
    }

    @After
    public void tearDown() {
        mgr.removeListener(testListener);
        mgr.deactivate();
    }

    @Test
    public void basics() {
        assertFalse("no ports expected", mgr.getEdgePoints().iterator().hasNext());
    }

    private class TestTopologyManager extends TopologyServiceAdapter {
        private TopologyListener listener;
        private Set<ConnectPoint> infrastructurePorts;

        @Override
        public boolean isInfrastructure(Topology topology, ConnectPoint connectPoint) {
            return infrastructurePorts.contains(connectPoint);
        }

        @Override
        public void addListener(TopologyListener listener) {
            this.listener = listener;
        }

        @Override
        public void removeListener(TopologyListener listener) {
            this.listener = null;
        }
    }

    private class TestDeviceManager extends DeviceServiceAdapter {

        private Set<Device> devices;

        @Override
        public boolean isAvailable(DeviceId deviceId) {
            for (Device device : devices) {
                if (device.id().equals(deviceId)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public List<Port> getPorts(DeviceId deviceId) {
            return super.getPorts(deviceId);
        }

        @Override
        public Iterable<Device> getAvailableDevices() {
            return devices;
        }
    }

    private class TestPacketManager extends PacketServiceAdapter {
    }


    private class TestListener implements EdgePortListener {
        private List<EdgePortEvent> events;

        @Override
        public void event(EdgePortEvent event) {
            events.add(event);
        }
    }
}