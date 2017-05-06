/*
* Copyright 2017-present Open Networking Foundation
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

package org.onosproject.incubator.protobuf.services.nb;

import com.google.common.collect.ImmutableSet;
import io.grpc.BindableService;
import io.grpc.inprocess.InProcessChannelBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import io.grpc.ManagedChannel;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.grpc.net.models.HostProtoOuterClass;
import org.onosproject.incubator.protobuf.models.net.HostProtoTranslator;
import org.onosproject.incubator.protobuf.models.net.HostIdProtoTranslator;
import org.onosproject.incubator.protobuf.models.net.ConnectPointProtoTranslator;
import org.onosproject.grpc.nb.net.host.HostServiceGrpc.HostServiceBlockingStub;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultHost;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.PortNumber;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;

import org.onosproject.grpc.nb.net.host.HostServiceGrpc;
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;
import static org.onosproject.grpc.nb.net.host.HostServiceNb.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Unit tests of gRPC northbound host service.
 */
public class GrpcNbHostServiceTest {
    private final Logger log = getLogger(getClass());

    private static InProcessServer<BindableService> inprocessServer;
    private static HostServiceBlockingStub blockingStub;
    private static ManagedChannel channel;
    private static final HostService MOCK_HOST = new MockHostService();
    private static List<Host> allHosts = new ArrayList<>();
    private static Host h1;
    private static HostId id1;
    private static IpAddress ip1;
    private static MacAddress mac1;
    private static DeviceId deviceId;
    private static ConnectPoint c1;
    private static boolean started = false;
    private static boolean stopped = false;
    private static boolean requestMac = false;

    public GrpcNbHostServiceTest() {}

    private static void populateHosts() {
        ip1 = IpAddress.valueOf("10.1.1.1");
        IpAddress ip2 = IpAddress.valueOf("10.1.1.2");
        IpAddress ip3 = IpAddress.valueOf("10.1.1.3");
        mac1 = MacAddress.valueOf("67:11:23:45:87:11");
        MacAddress mac2 = MacAddress.valueOf("67:11:23:45:87:12");
        MacAddress mac3 = MacAddress.valueOf("67:11:23:45:87:13");
        id1 = HostId.hostId(mac1);
        HostId id2 = HostId.hostId(mac2);
        HostId id3 = HostId.hostId(mac3);
        deviceId = DeviceId.deviceId("test");

        c1 = new ConnectPoint(deviceId, PortNumber.portNumber(101));
        HostLocation hostLocation1 = new HostLocation(deviceId, PortNumber.portNumber(101), 0);
        HostLocation hostLocation2 = new HostLocation(deviceId, PortNumber.portNumber(102), 0);
        HostLocation hostLocation3 = new HostLocation(deviceId, PortNumber.portNumber(103), 0);

        h1 = new DefaultHost(ProviderId.NONE, id1, mac1, VlanId.NONE,
                             hostLocation1, ImmutableSet.of(ip1));
        allHosts.add(h1);
        allHosts.add(new DefaultHost(ProviderId.NONE, id2, mac2, VlanId.NONE,
                                     hostLocation2, ImmutableSet.of(ip2)));
        allHosts.add(new DefaultHost(ProviderId.NONE, id3, mac3, VlanId.NONE,
                                     hostLocation3, ImmutableSet.of(ip3)));
    }

    /**
     * Tests gRPC getComponentNames interface.
     */
    @Test
    public void testGetHostCount() throws InterruptedException {
        getHostCountRequest request = getHostCountRequest.getDefaultInstance();
        getHostCountReply reply;

        try {
            reply = blockingStub.getHostCount(request);
            assertTrue(allHosts.size() == reply.getHostCount());
        } catch (Exception e) {
            log.error("Get host count error! Exception={}", e.toString());
        }
    }

    /**
     * Tests gRPC getComponentNames interface.
     */
    @Test
    public void testGetHosts() throws InterruptedException {
        getHostsRequest request = getHostsRequest.getDefaultInstance();
        getHostsReply reply;

        try {
            reply = blockingStub.getHosts(request);
            Set<Host> actualHosts = new HashSet<>();
            for (HostProtoOuterClass.HostProto host : reply.getHostList()) {
                actualHosts.add(HostProtoTranslator.translate(host));
            }

            Set<Host> expectedHosts = new HashSet<>();
            for (Host h : allHosts) {
                expectedHosts.add(h);
            }
            assertTrue(actualHosts.equals(expectedHosts));
        } catch (Exception e) {
            log.error("Get all hosts error! Exception={}", e.toString());
        }
    }

    /**
     * Tests gRPC getHost interface.
     */
    @Test
    public void testGetHost() throws InterruptedException {
        getHostRequest request = getHostRequest.newBuilder().setHostId(HostIdProtoTranslator.translate(id1)).build();
        getHostReply reply;

        try {
            reply = blockingStub.getHost(request);
            assertTrue(HostProtoTranslator.translate(reply.getHost()).equals(h1));
        } catch (Exception e) {
            log.error("Get host with hostId error! Exception={}", e.toString());
        }
    }

    /**
     * Tests gRPC getHostsByVlan interface.
     */
    @Test
    public void testGetHostsByVlan() throws InterruptedException {
        getHostsByVlanRequest request = getHostsByVlanRequest.newBuilder().setVlanId(VlanId.NONE.toString()).build();
        getHostsByVlanReply reply;

        try {
            reply = blockingStub.getHostsByVlan(request);

            Set<Host> actualHosts = new HashSet<>();
            for (HostProtoOuterClass.HostProto host : reply.getHostList()) {
                actualHosts.add(HostProtoTranslator.translate(host));
            }

            Set<Host> expectedHosts = new HashSet<>();
            for (Host h : allHosts) {
                expectedHosts.add(h);
            }
            assertTrue(actualHosts.equals(expectedHosts));
        } catch (Exception e) {
            log.error("Get hosts that belong to the specified VLAN error! Exception={}", e.toString());
        }
    }

    /**
     * Tests gRPC getHostsByMac interface.
     */
    @Test
    public void testGetHostsByMac() throws InterruptedException {
        getHostsByMacRequest request = getHostsByMacRequest.newBuilder().setMac(mac1.toString()).build();
        getHostsByMacReply reply;

        try {
            reply = blockingStub.getHostsByMac(request);

            Set<Host> actualHosts = new HashSet<>();
            for (HostProtoOuterClass.HostProto host : reply.getHostList()) {
                actualHosts.add(HostProtoTranslator.translate(host));
            }
            Set<Host> expectedHosts = new HashSet<>();
            expectedHosts.add(allHosts.get(0));
            assertTrue(actualHosts.equals(expectedHosts));
        } catch (Exception e) {
            log.error("Get hosts that have the specified MAC address error! Exception={}", e.toString());
        }
    }

    /**
     * Tests gRPC getHostsByIp interface.
     */
    @Test
    public void testGetHostsByIp() throws InterruptedException {
        getHostsByIpRequest request = getHostsByIpRequest.newBuilder().setIpAddress(ip1.toString()).build();
        getHostsByIpReply reply;

        try {
            reply = blockingStub.getHostsByIp(request);

            Set<Host> actualHosts = new HashSet<>();
            for (HostProtoOuterClass.HostProto host : reply.getHostList()) {
                actualHosts.add(HostProtoTranslator.translate(host));
            }

            Set<Host> expectedHosts = new HashSet<>();
            expectedHosts.add(allHosts.get(0));
            assertTrue(actualHosts.equals(expectedHosts));
        } catch (Exception e) {
            log.error("Get hosts that have the specified IP address error! Exception={}", e.toString());
        }
    }

    /**
     * Tests gRPC getConnectedHosts interface.
     */
    @Test
    public void testGetConnectedHosts() throws InterruptedException {
        getConnectedHostsRequest request = getConnectedHostsRequest.newBuilder()
                .setConnectPoint(ConnectPointProtoTranslator.translate(c1))
                .build();
        getConnectedHostsReply reply;

        try {
            reply = blockingStub.getConnectedHosts(request);

            Set<Host> actualHosts = new HashSet<>();
            for (HostProtoOuterClass.HostProto host : reply.getHostList()) {
                actualHosts.add(HostProtoTranslator.translate(host));
            }

            Set<Host> expectedHosts = new HashSet<>();
            expectedHosts.add(allHosts.get(0));
            assertTrue(actualHosts.equals(expectedHosts));
        } catch (Exception e) {
            log.error("Get connected hosts with connect point error! Exception={}", e.toString());
        }
    }

    /**
     * Tests gRPC getConnectedHostsByDeviceId interface.
     */
    @Test
    public void testGetConnectedHostsByDeviceId() throws InterruptedException {
        getConnectedHostsRequest request = getConnectedHostsRequest.newBuilder()
                .setDeviceId(deviceId.toString())
                .build();
        getConnectedHostsReply reply;

        try {
            reply = blockingStub.getConnectedHosts(request);

            Set<Host> actualHosts = new HashSet<>();
            for (HostProtoOuterClass.HostProto host : reply.getHostList()) {
                actualHosts.add(HostProtoTranslator.translate(host));
            }

            Set<Host> expectedHosts = new HashSet<>();
            for (Host h : allHosts) {
                expectedHosts.add(h);
            }
            assertTrue(actualHosts.equals(expectedHosts));
        } catch (Exception e) {
            log.error("Get connected hosts with deviceId error! Exception={}", e.toString());
        }
    }

    /**
     * Tests gRPC startMonitoringIp interface.
     */
    @Test
    public void testStartMonitoringIp() throws InterruptedException {
        startMonitoringIpRequest request = startMonitoringIpRequest.newBuilder().setIpAddress(ip1.toString()).build();

        try {
            blockingStub.startMonitoringIp(request);
            assertTrue(started);
        } catch (Exception e) {
            log.error("Start monitoring hosts with the given IP address error! Exception={}", e.toString());
        }
    }

    /**
     * Tests gRPC stopMonitoringIp interface.
     */
    @Test
    public void testStopMonitoringIp() throws InterruptedException {
        stopMonitoringIpRequest request = stopMonitoringIpRequest.newBuilder().setIpAddress(ip1.toString()).build();

        try {
            blockingStub.stopMonitoringIp(request);
            assertTrue(stopped);
        } catch (Exception e) {
            log.error("Stop monitoring hosts with the given IP address error! Exception={}", e.toString());
        }
    }

    /**
     * Tests gRPC requestMac interface.
     */
    @Test
    public void testRequestMac() throws InterruptedException {
        requestMacRequest request = requestMacRequest.newBuilder().setIpAddress(ip1.toString()).build();

        try {
            blockingStub.requestMac(request);
            assertTrue(requestMac);
        } catch (Exception e) {
            log.error("Resolve the MAC address for the given IP address error! Exception={}", e.toString());
        }
    }

    /**
     * Initialization before start testing gRPC northbound host service.
     */
    @BeforeClass
    public static void beforeClass() throws InstantiationException, IllegalAccessException, IOException {
        GrpcNbHostService hostService = new GrpcNbHostService();
        hostService.hostService = MOCK_HOST;
        inprocessServer = hostService.registerInProcessServer();

        inprocessServer.start();
        channel = InProcessChannelBuilder.forName("test").directExecutor()
                // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
                // needing certificates.
                .usePlaintext(true).build();
        blockingStub = HostServiceGrpc.newBlockingStub(channel);
        populateHosts();
    }

    /**
     * Finalization after test gRPC northbound host service.
     */
    @AfterClass
    public static void afterClass() {

        channel.shutdownNow();
        inprocessServer.stop();
    }

    private static class MockHostService implements HostService {

        MockHostService() {
        }

        @Override
        public int getHostCount() {
            return allHosts.size();
        }

        @Override
        public Iterable<Host> getHosts() {
            return allHosts;
        }

        @Override
        public Host getHost(HostId hostId) {
            return allHosts.stream().filter(h -> h.id().equals(hostId)).findFirst().get();
        }

        @Override
        public Set<Host> getHostsByVlan(VlanId vlanId) {
            return allHosts.stream().filter(h -> h.vlan().equals(vlanId)).collect(Collectors.toSet());
        }

        @Override
        public Set<Host> getHostsByMac(MacAddress mac) {
            return allHosts.stream().filter(h -> h.mac().equals(mac)).collect(Collectors.toSet());
        }

        @Override
        public Set<Host> getHostsByIp(IpAddress ip) {
            return allHosts.stream().filter(h -> h.ipAddresses().contains(ip)).collect(Collectors.toSet());
        }

        @Override
        public Set<Host> getConnectedHosts(ConnectPoint connectPoint) {
            return allHosts.stream().filter(h -> h.location().deviceId().equals(connectPoint.deviceId())
                    && h.location().port().equals(connectPoint.port()))
                    .collect(Collectors.toSet());
        }

        @Override
        public Set<Host> getConnectedHosts(DeviceId deviceId) {
            return allHosts.stream().filter(h -> h.location().deviceId().equals(deviceId)).collect(Collectors.toSet());
        }

        @Override
        public void startMonitoringIp(IpAddress ip) {
            started = true;
        }

        @Override
        public void stopMonitoringIp(IpAddress ip) {
            stopped = true;
        }

        @Override
        public void requestMac(IpAddress ip) {
            requestMac = true;
        }

        @Override
        public void addListener(HostListener listener) {
        }

        @Override
        public void removeListener(HostListener listener) {
        }
    }
}
