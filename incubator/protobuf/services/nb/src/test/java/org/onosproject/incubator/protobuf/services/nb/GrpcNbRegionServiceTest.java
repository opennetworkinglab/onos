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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.grpc.BindableService;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onosproject.cluster.NodeId;
import org.onosproject.grpc.nb.net.region.RegionServiceGrpc;
import org.onosproject.grpc.nb.net.region.RegionServiceGrpc.RegionServiceBlockingStub;
import org.onosproject.grpc.nb.net.region.RegionServiceNb;
import org.onosproject.grpc.net.models.RegionProtoOuterClass;
import org.onosproject.incubator.protobuf.models.net.RegionProtoTranslator;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.HostId;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.region.DefaultRegion;
import org.onosproject.net.region.Region;
import org.onosproject.net.region.RegionId;
import org.onosproject.net.region.RegionListener;
import org.onosproject.net.region.RegionService;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertTrue;
import static org.onosproject.cluster.NodeId.nodeId;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.HostId.hostId;

public class GrpcNbRegionServiceTest {

    private static InProcessServer<BindableService> inprocessServer;
    private static RegionServiceBlockingStub blockingStub;
    private static ManagedChannel channel;

    private static final String C1 = "C1";
    private static final String C2 = "C2";
    private static final String C3 = "C3";

    private static final NodeId CNID_1 = nodeId(C1);
    private static final NodeId CNID_2 = nodeId(C2);
    private static final NodeId CNID_3 = nodeId(C3);

    private static final String R1 = "R1";
    private static final String R2 = "R2";
    private static final String R3 = "R3";

    private static final Set<NodeId> SET_C1 = ImmutableSet.of(CNID_1);
    private static final Set<NodeId> SET_C2 = ImmutableSet.of(CNID_2);
    private static final Set<NodeId> SET_C3 = ImmutableSet.of(CNID_3);

    private static final Region REGION_1 =
            region(R1, Region.Type.METRO, ImmutableList.of(SET_C1, SET_C2));
    private static final Region REGION_2 =
            region(R2, Region.Type.CAMPUS, ImmutableList.of(SET_C2, SET_C1));
    private static final Region REGION_3 =
            region(R3, Region.Type.CAMPUS, ImmutableList.of(SET_C3, SET_C1));

    private static final Set<Region> REGION_SET =
            ImmutableSet.of(REGION_1, REGION_2, REGION_3);

    private static final String D1 = "d1";
    private static final String D2 = "d2";
    private static final String D3 = "d3";
    private static final String D4 = "d4";
    private static final String D5 = "d5";
    private static final String D6 = "d6";
    private static final String D7 = "d7";
    private static final String D8 = "d8";
    private static final String D9 = "d9";

    private static final String MFR = "Mfr";
    private static final String HW = "h/w";
    private static final String SW = "s/w";
    private static final String SERIAL = "ser123";

    private static final DeviceId DEVID_1 = deviceId(D1);
    private static final DeviceId DEVID_2 = deviceId(D2);
    private static final DeviceId DEVID_3 = deviceId(D3);
    private static final DeviceId DEVID_4 = deviceId(D4);
    private static final DeviceId DEVID_5 = deviceId(D5);
    private static final DeviceId DEVID_6 = deviceId(D6);
    private static final DeviceId DEVID_7 = deviceId(D7);
    private static final DeviceId DEVID_8 = deviceId(D8);
    private static final DeviceId DEVID_9 = deviceId(D9);

    private static final Set<DeviceId> DEVS_TRUNK =
            ImmutableSet.of(DEVID_1, DEVID_2, DEVID_3);

    private static final Set<DeviceId> DEVS_LEFT =
            ImmutableSet.of(DEVID_4, DEVID_5, DEVID_6);

    private static final Set<DeviceId> DEVS_RIGHT =
            ImmutableSet.of(DEVID_7, DEVID_8, DEVID_9);

    private static final String[][] HOST_DATA = {
            {"AA:00:00:00:00:1A/None", R1},
            {"AA:00:00:00:00:1B/None", R1},
            {"AA:00:00:00:00:2A/None", R1},
            {"AA:00:00:00:00:2B/None", R1},
            {"AA:00:00:00:00:3A/None", R1},
            {"AA:00:00:00:00:3B/None", R1},
            {"AA:00:00:00:00:4A/None", R2},
            {"AA:00:00:00:00:4B/None", R2},
            {"AA:00:00:00:00:5A/None", R2},
            {"AA:00:00:00:00:5B/None", R2},
            {"AA:00:00:00:00:6A/None", R2},
            {"AA:00:00:00:00:6B/None", R2},
            {"AA:00:00:00:00:7A/None", R3},
            {"AA:00:00:00:00:7B/None", R3},
            {"AA:00:00:00:00:8A/None", R3},
            {"AA:00:00:00:00:8B/None", R3},
            {"AA:00:00:00:00:9A/None", R3},
            {"AA:00:00:00:00:9B/None", R3},
    };

    private static final Map<HostId, RegionId> HOSTS = new HashMap<>();
    private static final RegionService MOCK_REGION = new MockRegionService();

    /**
     * Returns device with given ID.
     *
     * @param id device ID
     * @return device instance
     */
    protected static Device device(String id) {
        return new DefaultDevice(ProviderId.NONE, deviceId(id),
                                 Device.Type.SWITCH, MFR, HW, SW, SERIAL, null);
    }

    /**
     * Returns a region instance with specified parameters.
     *
     * @param id      region id
     * @param type    region type
     * @param masters ordered list of master sets
     * @return region instance
     */
    private static Region region(String id, Region.Type type,
                                   List<Set<NodeId>> masters) {
        return new DefaultRegion(RegionId.regionId(id), "Region-" + id,
                                 type, DefaultAnnotations.EMPTY, masters);
    }

    /**
     * Creates a map of hostIds corresponding to their regionIds.
     *
     */
    private static void populateHosts() {
        for (String[] row : HOST_DATA) {
            HOSTS.put(hostId(row[0]), RegionId.regionId(row[1]));
        }
    }

    public GrpcNbRegionServiceTest() {
    }

    @Test
    public void testGetRegion() throws InterruptedException {

        RegionServiceNb.getRegionRequest request = RegionServiceNb.getRegionRequest.newBuilder()
                                                                                   .setRegionId(R1).build();
        RegionServiceNb.getRegionReply response;

        try {
            response = blockingStub.getRegion(request);
            Region actualRegion = RegionProtoTranslator.translate(response.getRegion());
            assertTrue(REGION_1.equals(actualRegion));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGetRegions() throws InterruptedException {

        RegionServiceNb.getRegionsRequest request = RegionServiceNb.getRegionsRequest.newBuilder()
                .build();
        RegionServiceNb.getRegionsReply response;

        try {
            response = blockingStub.getRegions(request);
            Set<Region> actualRegions = new HashSet<Region>();
            for (RegionProtoOuterClass.RegionProto region : response.getRegionList()) {
                actualRegions.add(RegionProtoTranslator.translate(region));
            }
            assertTrue(REGION_SET.equals(actualRegions));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGetRegionForDevice() throws InterruptedException {

        RegionServiceNb.getRegionForDeviceRequest request = RegionServiceNb.getRegionForDeviceRequest.newBuilder()
                .setDeviceId(D1).build();
        RegionServiceNb.getRegionForDeviceReply response;

        try {
            response = blockingStub.getRegionForDevice(request);
            Region actualRegion = RegionProtoTranslator.translate(response.getRegion());
            assertTrue(REGION_1.equals(actualRegion));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGetRegionDevices() throws InterruptedException {

        RegionServiceNb.getRegionDevicesRequest request = RegionServiceNb.getRegionDevicesRequest.newBuilder()
                .setRegionId(R1).build();
        RegionServiceNb.getRegionDevicesReply response;

        try {
            response = blockingStub.getRegionDevices(request);
            Set<DeviceId> actualDevices = new HashSet<DeviceId>();
            for (String deviceId : response.getDeviceIdList()) {
                actualDevices.add(DeviceId.deviceId(deviceId));
            }
            assertTrue(DEVS_TRUNK.equals(actualDevices));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGetRegionHosts() throws InterruptedException {
        RegionServiceNb.getRegionHostsRequest request = RegionServiceNb.getRegionHostsRequest.newBuilder()
                .setRegionId(R1).build();
        RegionServiceNb.getRegionHostsReply response;

        Set<HostId> expectedHosts = new HashSet<HostId>();

        expectedHosts.add(HostId.hostId(HOST_DATA[0][0]));
        expectedHosts.add(HostId.hostId(HOST_DATA[1][0]));
        expectedHosts.add(HostId.hostId(HOST_DATA[2][0]));
        expectedHosts.add(HostId.hostId(HOST_DATA[3][0]));
        expectedHosts.add(HostId.hostId(HOST_DATA[4][0]));
        expectedHosts.add(HostId.hostId(HOST_DATA[5][0]));

        Set<HostId> actualHosts = new HashSet<HostId>();

        try {
            response = blockingStub.getRegionHosts(request);
            for (String hostId : response.getHostIdList()) {
                actualHosts.add(HostId.hostId(hostId));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertTrue(expectedHosts.equals(actualHosts));
    }

    @BeforeClass
    public static void beforeClass() throws InstantiationException, IllegalAccessException, IOException {
        GrpcNbRegionService regionService = new GrpcNbRegionService();
        regionService.regionService = MOCK_REGION;
        inprocessServer = regionService.registerInProcessServer();
        inprocessServer.start();

        channel = InProcessChannelBuilder.forName("test").directExecutor()
                // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
                // needing certificates.
                .usePlaintext(true).build();
        blockingStub = RegionServiceGrpc.newBlockingStub(channel);
        populateHosts();
    }

    @AfterClass
    public static void afterClass() {
        channel.shutdownNow();

        inprocessServer.stop();
    }

    private static class MockRegionService implements RegionService {

        private final Map<RegionId, Region> lookup = new HashMap<>();

        MockRegionService() {
            lookup.put(REGION_1.id(), REGION_1);
            lookup.put(REGION_2.id(), REGION_2);
            lookup.put(REGION_3.id(), REGION_3);
        }

        @Override
        public Set<Region> getRegions() {
            return REGION_SET;
        }

        @Override
        public Region getRegion(RegionId regionId) {
            return lookup.get(regionId);
        }

        @Override
        public Region getRegionForDevice(DeviceId deviceId) {
            if (DEVS_TRUNK.contains(deviceId)) {
                return REGION_1;
            }
            if (DEVS_LEFT.contains(deviceId)) {
                return REGION_2;
            }
            if (DEVS_RIGHT.contains(deviceId)) {
                return REGION_3;
            }
            return null;
        }

        @Override
        public Set<DeviceId> getRegionDevices(RegionId regionId) {
            if (REGION_1.id().equals(regionId)) {
                return DEVS_TRUNK;
            }
            if (REGION_2.id().equals(regionId)) {
                return DEVS_LEFT;
            }
            if (REGION_3.id().equals(regionId)) {
                return DEVS_RIGHT;
            }
            return Collections.emptySet();
        }

        @Override
        public Set<HostId> getRegionHosts(RegionId regionId) {
            Set<HostId> hosts = new HashSet<HostId>();
            for (HostId hostId : HOSTS.keySet()) {
                if (HOSTS.get(hostId).equals(regionId)) {
                    hosts.add(hostId);
                }
            }
            return hosts;
        }

        @Override
        public void addListener(RegionListener listener) {

        }

        @Override
        public void removeListener(RegionListener listener) {

        }
    }
}