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

package org.onosproject.ui.model.topo;


import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.ChassisId;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.cluster.DefaultControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.DefaultEdgeLink;
import org.onosproject.net.DefaultHost;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.EdgeLink;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.region.DefaultRegion;
import org.onosproject.net.region.Region;
import org.onosproject.net.region.RegionId;
import org.onosproject.ui.model.AbstractUiModelTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.PortNumber.portNumber;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for {@link UiTopology}.
 */
public class UiTopologyTest extends AbstractUiModelTest {

    private static final DeviceId DEV_X = deviceId("dev-X");
    private static final DeviceId DEV_Y = deviceId("dev-Y");
    private static final PortNumber P1 = portNumber(1);
    private static final PortNumber P2 = portNumber(2);

    private static final String DEV_X_ID = "dev-x/1";
    private static final String DEV_Y_ID = "dev-y/2";

    private static final ConnectPoint CP_X1 = new ConnectPoint(DEV_X, P1);
    private static final ConnectPoint CP_Y2 = new ConnectPoint(DEV_Y, P2);

    private static final Link LINK_X1_TO_Y2 = DefaultLink.builder()
            .providerId(ProviderId.NONE)
            .src(CP_X1)
            .dst(CP_Y2)
            .type(Link.Type.DIRECT)
            .build();

    private static final UiLinkId DX1_DY2 = UiLinkId.uiLinkId(LINK_X1_TO_Y2);

    private static final RegionId ROOT = UiRegion.NULL_ID;
    private static final RegionId R1 = RegionId.regionId("R1");
    private static final RegionId R2 = RegionId.regionId("R2");
    private static final RegionId R3 = RegionId.regionId("R3");

    private static final String DEV_LINK_CLASS = "UiDeviceLink";
    private static final String REG_LINK_CLASS = "UiRegionLink";
    private static final String REG_DEV_LINK_CLASS = "UiRegionDeviceLink";

    private Host host;
    private UiHost uiHost;
    private UiDeviceLink deviceLink;
    private EdgeLink edgeLink;
    private UiEdgeLink uiEdgeLink;
    private UiClusterMember mem1;
    private UiRegion region1;
    private UiDevice dev1;
    private UiLinkId linkId;

    private static final ProviderId PID = new ProviderId("of", "bar");
    private static final VlanId VID = VlanId.vlanId((short) 20);
    private static final MacAddress MAC_ADDRESS = MacAddress.valueOf("00:11:00:00:00:01");
    private static final HostId HID = HostId.hostId(MAC_ADDRESS, VID);
    private static final DeviceId DID = deviceId("of:foo");
    private static final HostLocation LOC = new HostLocation(DID, PortNumber.portNumber(8), 123L);
    private static final ConnectPoint CP = new ConnectPoint(DEV_X, PortNumber.portNumber(8));
    private static final Set<IpAddress> IPSET = Sets.newHashSet(IpAddress.valueOf("10.0.0.1"),
            IpAddress.valueOf("10.0.0.2"));
    private static final String MID = "id1";

    private UiTopology topo;
    private UiDeviceLink devLink;

    private List<RegionId> xBranch;
    private List<RegionId> yBranch;
    private UiSynthLink synth;

    @Before
    public void setUp() {
        topo = new UiTopology(MOCK_SERVICES);
        devLink = new UiDeviceLink(null, DX1_DY2);
        devLink.attachBackingLink(LINK_X1_TO_Y2);
    }

    @Test
    public void basic() {
        title("basic");
        print(topo);
    }

    private List<RegionId> branch(RegionId... ids) {
        List<RegionId> result = new ArrayList<>(ids.length);
        Collections.addAll(result, ids);
        return result;
    }

    private void verifySynth(RegionId id, String cls, String epA, String epB) {
        synth = topo.makeSynthLink(devLink, xBranch, yBranch);
        UiLink ulink = synth.link();
        print(synth);
        print("EpA{%s}  EpB{%s}", ulink.endPointA(), ulink.endPointB());

        assertEquals("wrong region", id, synth.regionId());
        assertEquals("wrong link class", cls, ulink.type());
        assertEquals("wrong EP A", epA, ulink.endPointA());
        assertEquals("wrong EP B", epB, ulink.endPointB());
    }

    @Test
    public void makeSynthDevToDevRoot() {
        title("makeSynthDevToDevRoot");
        xBranch = branch(ROOT);
        yBranch = branch(ROOT);
        verifySynth(ROOT, DEV_LINK_CLASS, DEV_X_ID, DEV_Y_ID);
    }

    @Test
    public void makeSynthDevToDevR1() {
        title("makeSynthDevToDevR1");
        xBranch = branch(ROOT, R1);
        yBranch = branch(ROOT, R1);
        verifySynth(R1, DEV_LINK_CLASS, DEV_X_ID, DEV_Y_ID);
    }

    @Test
    public void makeSynthDevToDevR2() {
        title("makeSynthDevToDevR2");
        xBranch = branch(ROOT, R1, R2);
        yBranch = branch(ROOT, R1, R2);
        verifySynth(R2, DEV_LINK_CLASS, DEV_X_ID, DEV_Y_ID);
    }

    @Test
    public void makeSynthRegToRegRoot() {
        title("makeSynthRegToRegRoot");
        xBranch = branch(ROOT, R1);
        yBranch = branch(ROOT, R2);
        verifySynth(ROOT, REG_LINK_CLASS, R1.id(), R2.id());
    }

    @Test
    public void makeSynthRegToRegR1() {
        title("makeSynthRegToRegR1");
        xBranch = branch(ROOT, R1, R2);
        yBranch = branch(ROOT, R1, R3);
        verifySynth(R1, REG_LINK_CLASS, R2.id(), R3.id());
    }

    @Test
    public void makeSynthRegToDevRoot() {
        title("makeSynthRegToDevRoot");

        // Note: link is canonicalized to region--device order

        xBranch = branch(ROOT);
        yBranch = branch(ROOT, R1);
        verifySynth(ROOT, REG_DEV_LINK_CLASS, R1.id(), DEV_X_ID);

        xBranch = branch(ROOT, R1);
        yBranch = branch(ROOT);
        verifySynth(ROOT, REG_DEV_LINK_CLASS, R1.id(), DEV_Y_ID);
    }

    @Test
    public void makeSynthRegToDevR3() {
        title("makeSynthRegToDevR3");

        // Note: link is canonicalized to region--device order

        xBranch = branch(ROOT, R3);
        yBranch = branch(ROOT, R3, R1);
        verifySynth(R3, REG_DEV_LINK_CLASS, R1.id(), DEV_X_ID);

        xBranch = branch(ROOT, R3, R1);
        yBranch = branch(ROOT, R3);
        verifySynth(R3, REG_DEV_LINK_CLASS, R1.id(), DEV_Y_ID);
    }

    @Test
    public void mockTopology() {
        host = new DefaultHost(PID, HID, MAC_ADDRESS, VID, LOC, IPSET);
        uiHost = new UiHost(topo, host);
        deviceLink = new UiDeviceLink(topo, DX1_DY2);
        edgeLink = DefaultEdgeLink.createEdgeLink(CP, true);
        linkId = UiLinkId.uiLinkId(edgeLink);
        uiEdgeLink = new UiEdgeLink(topo, linkId);
        mem1 = clusterMember(MID, 001);
        region1 = region();
        dev1 = device();
        Set<DeviceId> deviceIds = new HashSet<>();
        Set<HostId> hostIds = new HashSet<>();

        topo.add(uiHost);
        topo.add(mem1);
        topo.add(region1);
        topo.add(dev1);
        topo.add(deviceLink);
        topo.add(uiEdgeLink);


        assertTrue(topo.allRegions().contains(region1));
        assertTrue(topo.allClusterMembers().contains(mem1));
        assertTrue(topo.findClusterMember(NodeId.nodeId("id1")).equals(mem1));
        assertTrue(topo.findRegion(R1).equals(region1));
        assertTrue(topo.findDevice(DID).equals(dev1));
        assertTrue(topo.findHost(HID).equals(uiHost));
        assertTrue(topo.findDeviceLink(DX1_DY2).equals(deviceLink));

        deviceIds.add(DID);
        assertTrue(topo.deviceSet(deviceIds).contains(dev1));
        hostIds.add(HID);
        assertTrue(topo.hostSet(hostIds).contains(uiHost));

        topo.clear();
        assertThat(topo.allClusterMembers(), is(empty()));
        assertThat(topo.allDeviceLinks(), is(empty()));
        assertThat(topo.allDevices(), is(empty()));
        assertThat(topo.allHosts(), is(empty()));
        assertThat(topo.allRegions(), is(empty()));
    }
    private UiClusterMember clusterMember(String nodeId, int ipAddress) {
        return new UiClusterMember(topo, new DefaultControllerNode(NodeId.nodeId(nodeId),
                IpAddress.valueOf(ipAddress)));
    }
    private UiRegion region() {
        return new UiRegion(topo, (new DefaultRegion(R1, "reg1", Region.Type.METRO, DefaultAnnotations.EMPTY,
                null)));
    }
    private UiDevice device() {
        return new UiDevice(topo, new DefaultDevice(PID, DID, Device.Type.SWITCH, "whitebox",
                "1.1.x", "3.9.1", "43331", new ChassisId()));
    }


}
