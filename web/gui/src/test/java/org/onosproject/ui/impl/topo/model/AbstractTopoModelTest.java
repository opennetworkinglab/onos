/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.ui.impl.topo.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ClusterServiceAdapter;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.DefaultControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.cluster.RoleInfo;
import org.onosproject.mastership.MastershipService;
import org.onosproject.mastership.MastershipServiceAdapter;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.DefaultHost;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.DeviceServiceAdapter;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.host.HostServiceAdapter;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.link.LinkServiceAdapter;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.region.DefaultRegion;
import org.onosproject.net.region.Region;
import org.onosproject.net.region.RegionId;
import org.onosproject.net.region.RegionService;
import org.onosproject.ui.UiTopoLayoutService;
import org.onosproject.ui.impl.AbstractUiImplTest;
import org.onosproject.ui.model.ServiceBundle;
import org.onosproject.ui.model.topo.UiTopoLayout;
import org.onosproject.ui.model.topo.UiTopoLayoutId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.onosproject.cluster.NodeId.nodeId;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.HostId.hostId;
import static org.onosproject.net.PortNumber.portNumber;
import static org.onosproject.ui.model.topo.UiTopoLayoutId.layoutId;

/**
 * Base class for model test classes.
 */
abstract class AbstractTopoModelTest extends AbstractUiImplTest {

    /*
      Our mock environment:

      Three controllers: C1, C2, C3

      Nine devices: D1 .. D9

             D4 ---+              +--- D7
                   |              |
            D5 --- D1 --- D2 --- D3 --- D8
                   |              |
             D6 ---+              +--- D9

      Twelve hosts (two per D4 ... D9)  H4a, H4b, H5a, H5b, ...

      Layouts:
        LROOT : (default)
        +-- L1 : R1
        +-- L2 : R2
        +-- L3 : R3

      Regions:
        R1 : D1, D2, D3
        R2 : D4, D5, D6
        R3 : D7, D8, D9

      Mastership:
        C1 : D1, D2, D3
        C2 : D4, D5, D6
        C3 : D7, D8, D9

      Roles: (backups)
        C1 -> C2, C3
        C2 -> C1, C3
        C3 -> C1, C2
     */

    protected static final String C1 = "C1";
    protected static final String C2 = "C2";
    protected static final String C3 = "C3";

    protected static final NodeId CNID_1 = nodeId(C1);
    protected static final NodeId CNID_2 = nodeId(C2);
    protected static final NodeId CNID_3 = nodeId(C3);

    protected static final ControllerNode CNODE_1 = cnode(CNID_1, "10.0.0.1");
    protected static final ControllerNode CNODE_2 = cnode(CNID_2, "10.0.0.2");
    protected static final ControllerNode CNODE_3 = cnode(CNID_3, "10.0.0.3");

    protected static final String R1 = "R1";
    protected static final String R2 = "R2";
    protected static final String R3 = "R3";

    protected static final Set<NodeId> SET_C1 = ImmutableSet.of(CNID_1);
    protected static final Set<NodeId> SET_C2 = ImmutableSet.of(CNID_2);
    protected static final Set<NodeId> SET_C3 = ImmutableSet.of(CNID_3);

    protected static final Region REGION_1 =
            region(R1, Region.Type.METRO, ImmutableList.of(SET_C1, SET_C2));
    protected static final Region REGION_2 =
            region(R2, Region.Type.CAMPUS, ImmutableList.of(SET_C2, SET_C1));
    protected static final Region REGION_3 =
            region(R3, Region.Type.CAMPUS, ImmutableList.of(SET_C3, SET_C1));

    protected static final Set<Region> REGION_SET =
            ImmutableSet.of(REGION_1, REGION_2, REGION_3);

    protected static final String LROOT = "LROOT";
    protected static final String L1 = "L1";
    protected static final String L2 = "L2";
    protected static final String L3 = "L3";

    protected static final UiTopoLayout LAYOUT_ROOT = layout(LROOT, null, null);
    protected static final UiTopoLayout LAYOUT_1 = layout(L1, REGION_1, LROOT);
    protected static final UiTopoLayout LAYOUT_2 = layout(L2, REGION_2, LROOT);
    protected static final UiTopoLayout LAYOUT_3 = layout(L3, REGION_3, LROOT);

    protected static final Set<UiTopoLayout> LAYOUT_SET =
            ImmutableSet.of(LAYOUT_ROOT, LAYOUT_1, LAYOUT_2, LAYOUT_3);
    protected static final Set<UiTopoLayout> ROOT_KIDS =
            ImmutableSet.of(LAYOUT_1, LAYOUT_2, LAYOUT_3);
    protected static final Set<UiTopoLayout> PEERS_OF_1 =
            ImmutableSet.of(LAYOUT_2, LAYOUT_3);
    protected static final Set<UiTopoLayout> PEERS_OF_2 =
            ImmutableSet.of(LAYOUT_1, LAYOUT_3);
    protected static final Set<UiTopoLayout> PEERS_OF_3 =
            ImmutableSet.of(LAYOUT_1, LAYOUT_2);

    protected static final String D1 = "d1";
    protected static final String D2 = "d2";
    protected static final String D3 = "d3";
    protected static final String D4 = "d4";
    protected static final String D5 = "d5";
    protected static final String D6 = "d6";
    protected static final String D7 = "d7";
    protected static final String D8 = "d8";
    protected static final String D9 = "d9";

    protected static final String MFR = "Mfr";
    protected static final String HW = "h/w";
    protected static final String SW = "s/w";
    protected static final String SERIAL = "ser123";

    protected static final DeviceId DEVID_1 = deviceId(D1);
    protected static final DeviceId DEVID_2 = deviceId(D2);
    protected static final DeviceId DEVID_3 = deviceId(D3);
    protected static final DeviceId DEVID_4 = deviceId(D4);
    protected static final DeviceId DEVID_5 = deviceId(D5);
    protected static final DeviceId DEVID_6 = deviceId(D6);
    protected static final DeviceId DEVID_7 = deviceId(D7);
    protected static final DeviceId DEVID_8 = deviceId(D8);
    protected static final DeviceId DEVID_9 = deviceId(D9);

    protected static final Device DEV_1 = device(D1);
    protected static final Device DEV_2 = device(D2);
    protected static final Device DEV_3 = device(D3);
    protected static final Device DEV_4 = device(D4);
    protected static final Device DEV_5 = device(D5);
    protected static final Device DEV_6 = device(D6);
    protected static final Device DEV_7 = device(D7);
    protected static final Device DEV_8 = device(D8);
    protected static final Device DEV_9 = device(D9);

    protected static final List<Device> ALL_DEVS =
            ImmutableList.of(
                    DEV_1, DEV_2, DEV_3,
                    DEV_4, DEV_5, DEV_6,
                    DEV_7, DEV_8, DEV_9
            );

    private static final Set<DeviceId> DEVS_TRUNK =
            ImmutableSet.of(DEVID_1, DEVID_2, DEVID_3);

    private static final Set<DeviceId> DEVS_LEFT =
            ImmutableSet.of(DEVID_4, DEVID_5, DEVID_6);

    private static final Set<DeviceId> DEVS_RIGHT =
            ImmutableSet.of(DEVID_7, DEVID_8, DEVID_9);

    private static final String[][] LINK_CONNECT_DATA = {
            {D1, "12", D2, "21"},
            {D2, "23", D3, "32"},
            {D4, "41", D1, "14"},
            {D5, "51", D1, "15"},
            {D6, "61", D1, "16"},
            {D7, "73", D3, "37"},
            {D8, "83", D3, "38"},
            {D9, "93", D3, "39"},
    };

    private static final String HOST_MAC_PREFIX = "aa:00:00:00:00:";

    /**
     * Returns IP address instance for given string.
     *
     * @param s string
     * @return IP address
     */
    protected static IpAddress ip(String s) {
        return IpAddress.valueOf(s);
    }

    /**
     * Returns controller node instance for given ID and IP.
     *
     * @param id identifier
     * @param ip IP address
     * @return controller node instance
     */
    protected static ControllerNode cnode(NodeId id, String ip) {
        return new DefaultControllerNode(id, ip(ip));
    }

    /**
     * Returns UI topology layout instance with the specified parameters.
     *
     * @param layoutId the layout ID
     * @param region   the backing region
     * @param parentId the parent layout ID
     * @return layout instance
     */
    protected static UiTopoLayout layout(String layoutId, Region region,
                                         String parentId) {
        UiTopoLayoutId pid = parentId == null
                ? UiTopoLayoutId.DEFAULT_ID : layoutId(parentId);
        return new UiTopoLayout(layoutId(layoutId)).region(region).parent(pid);
    }

    /**
     * Returns a region instance with specified parameters.
     *
     * @param id      region id
     * @param type    region type
     * @param masters ordered list of master sets
     * @return region instance
     */
    protected static Region region(String id, Region.Type type,
                                   List<Set<NodeId>> masters) {
        return new DefaultRegion(RegionId.regionId(id), "Region-" + id,
                type, DefaultAnnotations.EMPTY, masters);
    }

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
     * Returns canned results.
     * <p>
     * At some future point, we may make this "programmable", so that
     * its state can be changed over the course of a unit test.
     */
    protected static final ServiceBundle MOCK_SERVICES =
            new ServiceBundle() {
                @Override
                public UiTopoLayoutService layout() {
                    return MOCK_LAYOUT;
                }

                @Override
                public ClusterService cluster() {
                    return MOCK_CLUSTER;
                }

                @Override
                public MastershipService mastership() {
                    return MOCK_MASTER;
                }

                @Override
                public RegionService region() {
                    return MOCK_REGION;
                }

                @Override
                public DeviceService device() {
                    return MOCK_DEVICE;
                }

                @Override
                public LinkService link() {
                    return MOCK_LINK;
                }

                @Override
                public HostService host() {
                    return MOCK_HOST;
                }

                @Override
                public IntentService intent() {
                    return null;
                }

                @Override
                public FlowRuleService flow() {
                    return null;
                }
            };

    private static final ClusterService MOCK_CLUSTER = new MockClusterService();
    private static final MastershipService MOCK_MASTER = new MockMasterService();
    private static final UiTopoLayoutService MOCK_LAYOUT = new MockLayoutService();
    private static final RegionService MOCK_REGION = new MockRegionService();
    private static final DeviceService MOCK_DEVICE = new MockDeviceService();
    private static final LinkService MOCK_LINK = new MockLinkService();
    private static final HostService MOCK_HOST = new MockHostService();


    private static class MockClusterService extends ClusterServiceAdapter {
        private final Map<NodeId, ControllerNode> nodes = new HashMap<>();
        private final Map<NodeId, ControllerNode.State> states = new HashMap<>();

        MockClusterService() {
            nodes.put(CNODE_1.id(), CNODE_1);
            nodes.put(CNODE_2.id(), CNODE_2);
            nodes.put(CNODE_3.id(), CNODE_3);

            states.put(CNODE_1.id(), ControllerNode.State.READY);
            states.put(CNODE_2.id(), ControllerNode.State.ACTIVE);
            states.put(CNODE_3.id(), ControllerNode.State.ACTIVE);
        }

        @Override
        public Set<ControllerNode> getNodes() {
            return ImmutableSet.copyOf(nodes.values());
        }

        @Override
        public ControllerNode getNode(NodeId nodeId) {
            return nodes.get(nodeId);
        }

        @Override
        public ControllerNode.State getState(NodeId nodeId) {
            return states.get(nodeId);
        }
    }


    private static class MockMasterService extends MastershipServiceAdapter {
        private final Map<NodeId, Set<DeviceId>> masterOf = new HashMap<>();

        MockMasterService() {
            masterOf.put(CNODE_1.id(), DEVS_TRUNK);
            masterOf.put(CNODE_2.id(), DEVS_LEFT);
            masterOf.put(CNODE_3.id(), DEVS_RIGHT);
        }

        @Override
        public NodeId getMasterFor(DeviceId deviceId) {
            if (DEVS_TRUNK.contains(deviceId)) {
                return CNID_1;
            }
            if (DEVS_LEFT.contains(deviceId)) {
                return CNID_2;
            }
            if (DEVS_RIGHT.contains(deviceId)) {
                return CNID_3;
            }
            return null;
        }

        @Override
        public Set<DeviceId> getDevicesOf(NodeId nodeId) {
            return masterOf.get(nodeId);
        }

        @Override
        public RoleInfo getNodesFor(DeviceId deviceId) {
            NodeId master = null;
            List<NodeId> backups = new ArrayList<>();

            if (DEVS_TRUNK.contains(deviceId)) {
                master = CNID_1;
                backups.add(CNID_2);
                backups.add(CNID_3);
            } else if (DEVS_LEFT.contains(deviceId)) {
                master = CNID_2;
                backups.add(CNID_1);
                backups.add(CNID_3);
            } else if (DEVS_RIGHT.contains(deviceId)) {
                master = CNID_3;
                backups.add(CNID_1);
                backups.add(CNID_2);
            }
            return new RoleInfo(master, backups);
        }
    }

    // TODO: consider implementing UiTopoLayoutServiceAdapter and extending that here
    private static class MockLayoutService implements UiTopoLayoutService {
        private final Map<UiTopoLayoutId, UiTopoLayout> map = new HashMap<>();
        private final Map<UiTopoLayoutId, Set<UiTopoLayout>> peers = new HashMap<>();
        private final Map<RegionId, UiTopoLayout> byRegion = new HashMap<>();

        MockLayoutService() {
            map.put(LAYOUT_ROOT.id(), LAYOUT_ROOT);
            map.put(LAYOUT_1.id(), LAYOUT_1);
            map.put(LAYOUT_2.id(), LAYOUT_2);
            map.put(LAYOUT_3.id(), LAYOUT_3);

            peers.put(LAYOUT_ROOT.id(), ImmutableSet.of());
            peers.put(LAYOUT_1.id(), ImmutableSet.of(LAYOUT_2, LAYOUT_3));
            peers.put(LAYOUT_2.id(), ImmutableSet.of(LAYOUT_1, LAYOUT_3));
            peers.put(LAYOUT_3.id(), ImmutableSet.of(LAYOUT_1, LAYOUT_2));

            byRegion.put(REGION_1.id(), LAYOUT_1);
            byRegion.put(REGION_2.id(), LAYOUT_2);
            byRegion.put(REGION_3.id(), LAYOUT_3);
        }

        @Override
        public UiTopoLayout getRootLayout() {
            return LAYOUT_ROOT;
        }

        @Override
        public Set<UiTopoLayout> getLayouts() {
            return LAYOUT_SET;
        }

        @Override
        public boolean addLayout(UiTopoLayout layout) {
            return false;
        }

        @Override
        public UiTopoLayout getLayout(UiTopoLayoutId layoutId) {
            return map.get(layoutId);
        }

        @Override
        public UiTopoLayout getLayout(RegionId regionId) {
            return byRegion.get(regionId);
        }

        @Override
        public Set<UiTopoLayout> getPeerLayouts(UiTopoLayoutId layoutId) {
            return peers.get(layoutId);
        }

        @Override
        public Set<UiTopoLayout> getChildren(UiTopoLayoutId layoutId) {
            return LAYOUT_ROOT.id().equals(layoutId)
                    ? ROOT_KIDS
                    : Collections.emptySet();
        }

        @Override
        public boolean removeLayout(UiTopoLayout layout) {
            return false;
        }
    }

    private static class MockRegionService extends RegionServiceAdapter {

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
    }


    private static class MockDeviceService extends DeviceServiceAdapter {
        private final Map<DeviceId, Device> devices = new HashMap<>();

        MockDeviceService() {
            for (Device dev : ALL_DEVS) {
                devices.put(dev.id(), dev);
            }
        }

        @Override
        public int getDeviceCount() {
            return devices.size();
        }

        @Override
        public Iterable<Device> getDevices() {
            return ImmutableList.copyOf(devices.values());
        }

        @Override
        public Device getDevice(DeviceId deviceId) {
            return devices.get(deviceId);
        }

    }

    /**
     * Synthesizes a pair of unidirectional links between two devices. The
     * string array should be of the form:
     * <pre>
     *     { "device-A-id", "device-A-port", "device-B-id", "device-B-port" }
     * </pre>
     *
     * @param linkPairData device ids and ports
     * @return pair of synthesized links
     */
    protected static List<Link> makeLinkPair(String[] linkPairData) {
        DeviceId devA = deviceId(linkPairData[0]);
        PortNumber portA = portNumber(Long.valueOf(linkPairData[1]));
        DeviceId devB = deviceId(linkPairData[2]);
        PortNumber portB = portNumber(Long.valueOf(linkPairData[3]));

        Link linkA = DefaultLink.builder()
                .providerId(ProviderId.NONE)
                .type(Link.Type.DIRECT)
                .src(new ConnectPoint(devA, portA))
                .dst(new ConnectPoint(devB, portB))
                .build();

        Link linkB = DefaultLink.builder()
                .providerId(ProviderId.NONE)
                .type(Link.Type.DIRECT)
                .src(new ConnectPoint(devB, portB))
                .dst(new ConnectPoint(devA, portA))
                .build();

        return ImmutableList.of(linkA, linkB);
    }

    private static class MockLinkService extends LinkServiceAdapter {
        private final Set<Link> links = new HashSet<>();

        MockLinkService() {
            for (String[] linkPair : LINK_CONNECT_DATA) {
                links.addAll(makeLinkPair(linkPair));
            }
        }

        @Override
        public int getLinkCount() {
            return links.size();
        }

        @Override
        public Iterable<Link> getLinks() {
            return ImmutableSet.copyOf(links);
        }

        // TODO: possibly fill out other methods if we find the model uses them
    }


    /**
     * Creates a default host connected at the given edge device and port. Note
     * that an identifying hex character ("a" - "f") should be supplied. This
     * will be included in the MAC address of the host (and equivalent value
     * as last byte in IP address).
     *
     * @param device  edge device
     * @param port    port number
     * @param hexChar identifying hex character
     * @return host connected at that location
     */
    protected static Host createHost(Device device, int port, String hexChar) {
        DeviceId deviceId = device.id();
        String devNum = deviceId.toString().substring(1);

        MacAddress mac = MacAddress.valueOf(HOST_MAC_PREFIX + devNum + hexChar);
        HostId hostId = hostId(String.format("%s/-1", mac));

        int ipByte = Integer.valueOf(hexChar, 16);
        if (ipByte < 10 || ipByte > 15) {
            throw new IllegalArgumentException("hexChar must be a-f");
        }
        HostLocation loc = new HostLocation(deviceId, portNumber(port), 0);

        IpAddress ip = ip("10." + devNum + ".0." + ipByte);

        return new DefaultHost(ProviderId.NONE, hostId, mac, VlanId.NONE,
                loc, ImmutableSet.of(ip));
    }

    /**
     * Creates a pair of hosts connected to the specified device.
     *
     * @param d edge device
     * @return pair of hosts
     */
    protected static List<Host> createHostPair(Device d) {
        List<Host> hosts = new ArrayList<>();
        hosts.add(createHost(d, 101, "a"));
        hosts.add(createHost(d, 102, "b"));
        return hosts;
    }

    private static class MockHostService extends HostServiceAdapter {
        private final Map<HostId, Host> hosts = new HashMap<>();

        MockHostService() {
            for (Device d : ALL_DEVS) {
                for (Host h : createHostPair(d)) {
                    hosts.put(h.id(), h);
                }
            }
        }

        @Override
        public int getHostCount() {
            return hosts.size();
        }

        @Override
        public Iterable<Host> getHosts() {
            return ImmutableSet.copyOf(hosts.values());
        }

        @Override
        public Host getHost(HostId hostId) {
            return hosts.get(hostId);
        }

        // TODO: possibly fill out other methods, should the model require them
    }

}
