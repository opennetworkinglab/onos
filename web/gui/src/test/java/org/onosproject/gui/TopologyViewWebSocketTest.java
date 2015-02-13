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

package org.onosproject.gui;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onlab.packet.ChassisId;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ClusterServiceAdapter;
import org.onosproject.core.CoreService;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.core.Version;
import org.onosproject.mastership.MastershipService;
import org.onosproject.mastership.MastershipServiceAdapter;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.DefaultHost;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.DeviceServiceAdapter;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.FlowRuleServiceAdapter;
import org.onosproject.net.host.HostService;
import org.onosproject.net.host.HostServiceAdapter;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.IntentServiceAdapter;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.link.LinkServiceAdapter;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.statistic.StatisticService;
import org.onosproject.net.statistic.StatisticServiceAdapter;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.net.topology.TopologyServiceAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class TopologyViewWebSocketTest {

    private static final ProviderId PID = new ProviderId("of", "foo.bar");
    private static final ChassisId CHID = new ChassisId(123L);
    private static final MacAddress MAC = MacAddress.valueOf("00:00:00:00:00:19");
    private static final DeviceId DID = DeviceId.deviceId("of:foo");
    private static final Set<IpAddress> IPS = ImmutableSet.of(IpAddress.valueOf("1.2.3.4"));

    private TestWebSocket ws;
    private TestServiceDirectory sd;

    @Before
    public void setUp() {
        sd = new TestServiceDirectory();
        sd.add(DeviceService.class, new TestDeviceService());
        sd.add(ClusterService.class, new ClusterServiceAdapter());
        sd.add(LinkService.class, new LinkServiceAdapter());
        sd.add(HostService.class, new TestHostService());
        sd.add(MastershipService.class, new MastershipServiceAdapter());
        sd.add(IntentService.class, new IntentServiceAdapter());
        sd.add(FlowRuleService.class, new TestFlowService());
        sd.add(StatisticService.class, new StatisticServiceAdapter());
        sd.add(TopologyService.class, new TopologyServiceAdapter());
        sd.add(CoreService.class, new TestCoreService());
        ws = new TestWebSocket(sd);
    }

    @Test
    public void requestDetailsDevice() {
        // build the request
        String request = "{\"event\":\"requestDetails\", \"sid\":0, "
                + "\"payload\":{\"id\":\"of:000001\",\"class\":\"device\"}}";
        ws.onMessage(request);
        // look at the ws reply, and verify that it is correct
        assertEquals("incorrect id", "of:000001", ws.reply.path("payload").path("id").asText());
        assertEquals("incorrect mfr", "foo", ws.reply.path("payload").path("props").path("Vendor").asText());
    }

    @Test
    public void requestDetailsHost() {
        // build the request
        String request = "{\"event\":\"requestDetails\", \"sid\":0, "
                + "\"payload\":{\"id\":\"00:00:00:00:00:19/-1\",\"class\":\"host\"}}";
        ws.onMessage(request);
        // look at the ws reply, and verify that it is correct
        assertEquals("incorrect id", "00:00:00:00:00:19/-1", ws.reply.path("payload").path("id").asText());
        assertEquals("incorrect ip address", "1.2.3.4", ws.reply.path("payload").path("props").path("IP").asText());
    }

    private class TestWebSocket extends TopologyViewWebSocket {

        private ObjectNode reply;

        /**
         * Creates a new web-socket for serving data to GUI topology view.
         *
         * @param directory service directory
         */
        public TestWebSocket(ServiceDirectory directory) {
            super(directory);
        }

        @Override
        protected synchronized void sendMessage(ObjectNode data) {
            reply = data;
        }
    }

    private class TestCoreService extends CoreServiceAdapter {
        @Override
        public Version version() {
            return Version.version("1.2.3");
        }
    }

    private class TestDeviceService extends DeviceServiceAdapter {

        @Override
        public Device getDevice(DeviceId deviceId) {
            return new DefaultDevice(PID, deviceId, Device.Type.SWITCH,
                                     "foo", "hw", "sw", "sn", CHID);
        }

        @Override
        public List<Port> getPorts(DeviceId deviceId) {
            return new ArrayList<>();
        }
    }

    private class TestFlowService extends FlowRuleServiceAdapter {
        @Override
        public Iterable<FlowEntry> getFlowEntries(DeviceId deviceId) {
            return new ArrayList<>();
        }
    }

    private class TestHostService extends HostServiceAdapter {
        @Override
        public Host getHost(HostId hostId) {
            return new DefaultHost(PID, hostId, MAC, VlanId.NONE,
                                   new HostLocation(DID, PortNumber.P0, 123L), IPS);
        }
    }
}