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

package org.onosproject.segmentrouting;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.VlanId;
import org.onosproject.TestApplicationId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultHost;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.ConfigApplyDelegate;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.host.InterfaceIpAddress;
import org.onosproject.net.intf.Interface;
import org.onosproject.net.intf.InterfaceService;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.segmentrouting.config.PwaasConfig;
import org.onosproject.segmentrouting.pwaas.DefaultL2Tunnel;
import org.onosproject.segmentrouting.pwaas.DefaultL2TunnelDescription;
import org.onosproject.segmentrouting.pwaas.DefaultL2TunnelPolicy;
import org.onosproject.segmentrouting.pwaas.L2Mode;

import java.io.InputStream;
import java.util.Set;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

/**
 * Unit tests for class {@link PwaasConfig}.
 */
public class PwaasConfigTest {

    private static final String TUNNEL_ID_1 = "1";
    private static final String TUNNEL_ID_2 = "20";
    private static final String NOT_PRESENT_TUNNEL_ID = "2";
    private static final ConnectPoint INGRESS_1 = ConnectPoint.deviceConnectPoint("of:0000000000000001/1");
    private static final ConnectPoint INGRESS_2 = ConnectPoint.deviceConnectPoint("of:0000000000000001/1");
    private static final ConnectPoint EGRESS_1 = ConnectPoint.deviceConnectPoint("of:0000000000000002/1");
    private static final ConnectPoint EGRESS_2 = ConnectPoint.deviceConnectPoint("of:0000000000000002/1");
    private static final VlanId INGRESS_INNER_TAG_1 = VlanId.vlanId("10");
    private static final VlanId INGRESS_INNER_TAG_2 = VlanId.vlanId("100");
    private static final VlanId INGRESS_OUTER_TAG_1 = VlanId.vlanId("20");
    private static final VlanId INGRESS_OUTER_TAG_2 = VlanId.vlanId("200");
    private static final VlanId EGRESS_INNER_TAG_1 = VlanId.vlanId("10");
    private static final VlanId EGRESS_INNER_TAG_2 = VlanId.vlanId("100");
    private static final VlanId EGRESS_OUTER_TAG_1 = VlanId.vlanId("21");
    private static final VlanId EGRESS_OUTER_TAG_2 = VlanId.vlanId("210");
    private static final String MODE_1 = "RAW";
    private static final String MODE_2 = "RAW";
    private static final VlanId SD_TAG_1 = VlanId.NONE;
    private static final VlanId SD_TAG_2 = VlanId.NONE;
    private static final MplsLabel PW_LABEL_1 = MplsLabel.mplsLabel("255");
    private static final MplsLabel PW_LABEL_2 = MplsLabel.mplsLabel("1255");

    /*
     * Configuration below copied from host handler test.
     */

    // Host Mac, VLAN
    private static final ProviderId PROVIDER_ID = ProviderId.NONE;
    private static final MacAddress HOST_MAC = MacAddress.valueOf("00:00:00:00:00:01");
    private static final VlanId HOST_VLAN_UNTAGGED = VlanId.NONE;
    private static final HostId HOST_ID_UNTAGGED = HostId.hostId(HOST_MAC, HOST_VLAN_UNTAGGED);
    private static final VlanId HOST_VLAN_TAGGED = VlanId.vlanId((short) 20);
    private static final HostId HOST_ID_TAGGED = HostId.hostId(HOST_MAC, HOST_VLAN_TAGGED);
    // Host IP
    private static final IpAddress HOST_IP11 = IpAddress.valueOf("10.0.1.1");
    private static final IpAddress HOST_IP21 = IpAddress.valueOf("10.0.2.1");
    private static final IpAddress HOST_IP12 = IpAddress.valueOf("10.0.1.2");
    private static final IpAddress HOST_IP13 = IpAddress.valueOf("10.0.1.3");
    private static final IpAddress HOST_IP14 = IpAddress.valueOf("10.0.1.4");
    private static final IpAddress HOST_IP32 = IpAddress.valueOf("10.0.3.2");
    // Device
    private static final DeviceId DEV1 = DeviceId.deviceId("of:0000000000000001");
    private static final DeviceId DEV2 = DeviceId.deviceId("of:0000000000000002");
    private static final DeviceId DEV3 = DeviceId.deviceId("of:0000000000000003");
    private static final DeviceId DEV4 = DeviceId.deviceId("of:0000000000000004");
    private static final DeviceId DEV5 = DeviceId.deviceId("of:0000000000000005");
    private static final DeviceId DEV6 = DeviceId.deviceId("of:0000000000000006");
    // Port
    private static final PortNumber P1 = PortNumber.portNumber(1);
    private static final PortNumber P2 = PortNumber.portNumber(2);
    private static final PortNumber P3 = PortNumber.portNumber(3);
    private static final PortNumber P9 = PortNumber.portNumber(9);
    // Connect Point
    private static final ConnectPoint CP11 = new ConnectPoint(DEV1, P1);
    private static final HostLocation HOST_LOC11 = new HostLocation(CP11, 0);
    private static final ConnectPoint CP12 = new ConnectPoint(DEV1, P2);
    private static final HostLocation HOST_LOC12 = new HostLocation(CP12, 0);
    private static final ConnectPoint CP13 = new ConnectPoint(DEV1, P3);
    private static final HostLocation HOST_LOC13 = new HostLocation(CP13, 0);
    private static final ConnectPoint CP21 = new ConnectPoint(DEV2, P1);
    private static final HostLocation HOST_LOC21 = new HostLocation(CP21, 0);
    private static final ConnectPoint CP22 = new ConnectPoint(DEV2, P2);
    private static final HostLocation HOST_LOC22 = new HostLocation(CP22, 0);
    // Connect Point for dual-homed host failover
    private static final ConnectPoint CP31 = new ConnectPoint(DEV3, P1);
    private static final HostLocation HOST_LOC31 = new HostLocation(CP31, 0);
    private static final ConnectPoint CP32 = new ConnectPoint(DEV3, P2);
    private static final HostLocation HOST_LOC32 = new HostLocation(CP32, 0);
    private static final ConnectPoint CP41 = new ConnectPoint(DEV4, P1);
    private static final HostLocation HOST_LOC41 = new HostLocation(CP41, 0);
    private static final ConnectPoint CP39 = new ConnectPoint(DEV3, P9);
    private static final ConnectPoint CP49 = new ConnectPoint(DEV4, P9);
    // Conenct Point for mastership test
    private static final ConnectPoint CP51 = new ConnectPoint(DEV5, P1);
    private static final HostLocation HOST_LOC51 = new HostLocation(CP51, 0);
    private static final ConnectPoint CP61 = new ConnectPoint(DEV6, P1);
    private static final HostLocation HOST_LOC61 = new HostLocation(CP61, 0);
    // Interface VLAN
    private static final VlanId INTF_VLAN_UNTAGGED = VlanId.vlanId((short) 10);
    private static final Set<VlanId> INTF_VLAN_TAGGED = Sets.newHashSet(VlanId.vlanId((short) 20));
    private static final VlanId INTF_VLAN_NATIVE = VlanId.vlanId((short) 30);
    private static final Set<VlanId> INTF_VLAN_PAIR = Sets.newHashSet(VlanId.vlanId((short) 10),
                                     VlanId.vlanId((short) 20), VlanId.vlanId((short) 30));
    private static final VlanId INTF_VLAN_OTHER = VlanId.vlanId((short) 40);
    // Interface subnet
    private static final IpPrefix INTF_PREFIX1 = IpPrefix.valueOf("10.0.1.254/24");
    private static final IpPrefix INTF_PREFIX2 = IpPrefix.valueOf("10.0.2.254/24");
    private static final IpPrefix INTF_PREFIX3 = IpPrefix.valueOf("10.0.3.254/24");
    private static final InterfaceIpAddress INTF_IP1 =
            new InterfaceIpAddress(INTF_PREFIX1.address(), INTF_PREFIX1);
    private static final InterfaceIpAddress INTF_IP2 =
            new InterfaceIpAddress(INTF_PREFIX2.address(), INTF_PREFIX2);
    private static final InterfaceIpAddress INTF_IP3 =
            new InterfaceIpAddress(INTF_PREFIX3.address(), INTF_PREFIX3);
    // Interfaces
    private static final Interface INTF11 =
            new Interface(null, CP11, Lists.newArrayList(INTF_IP1), MacAddress.NONE, null,
                          INTF_VLAN_UNTAGGED, null, null);
    private static final Interface INTF12 =
            new Interface(null, CP12, Lists.newArrayList(INTF_IP1), MacAddress.NONE, null,
                          INTF_VLAN_UNTAGGED, null, null);
    private static final Interface INTF13 =
            new Interface(null, CP13, Lists.newArrayList(INTF_IP2), MacAddress.NONE, null,
                          null, INTF_VLAN_TAGGED, INTF_VLAN_NATIVE);
    private static final Interface INTF21 =
            new Interface(null, CP21, Lists.newArrayList(INTF_IP1), MacAddress.NONE, null,
                          INTF_VLAN_UNTAGGED, null, null);
    private static final Interface INTF22 =
            new Interface(null, CP22, Lists.newArrayList(INTF_IP1), MacAddress.NONE, null,
                          INTF_VLAN_UNTAGGED, null, null);
    private static final Interface INTF31 =
            new Interface(null, CP31, Lists.newArrayList(INTF_IP1), MacAddress.NONE, null,
                          INTF_VLAN_UNTAGGED, null, null);
    private static final Interface INTF32 =
            new Interface(null, CP32, Lists.newArrayList(INTF_IP3), MacAddress.NONE, null,
                          INTF_VLAN_OTHER, null, null);
    private static final Interface INTF39 =
            new Interface(null, CP39, Lists.newArrayList(INTF_IP1), MacAddress.NONE, null,
                          null, INTF_VLAN_PAIR, null);
    private static final Interface INTF41 =
            new Interface(null, CP41, Lists.newArrayList(INTF_IP1), MacAddress.NONE, null,
                          INTF_VLAN_UNTAGGED, null, null);
    private static final Interface INTF49 =
            new Interface(null, CP49, Lists.newArrayList(INTF_IP1), MacAddress.NONE, null,
                          null, INTF_VLAN_PAIR, null);
    // Host
    private static final Host HOST1 = new DefaultHost(PROVIDER_ID, HOST_ID_UNTAGGED, HOST_MAC,
                                                      HOST_VLAN_UNTAGGED,
                                                      Sets.newHashSet(HOST_LOC11, HOST_LOC21),
                                                      Sets.newHashSet(HOST_IP11),
                                                      false);

    // A set of hosts
    private static final Set<Host> HOSTS = Sets.newHashSet(HOST1);
    // A set of devices of which we have mastership
    private static final Set<DeviceId> LOCAL_DEVICES = Sets.newHashSet(DEV1, DEV2, DEV3, DEV4);
    // A set of interfaces
    private static final Set<Interface> INTERFACES = Sets.newHashSet(INTF11, INTF12, INTF13, INTF21,
                                                                     INTF22, INTF31, INTF32, INTF39, INTF41, INTF49);

    private PwaasConfig config;
    private PwaasConfig invalidConfigVlan;
    private PwaasConfig invalidConfigMode;
    private PwaasConfig invalidConfigLabel;
    private PwaasConfig invalidConfigConflictingVlan;

    @Before
    public void setUp() throws Exception {
        InputStream jsonStream = PwaasConfig.class
                .getResourceAsStream("/pwaas.json");
        InputStream jsonStreamInvalid1 = PwaasConfig.class
                .getResourceAsStream("/pwaas-invalid-mode.json");
        InputStream jsonStreamInvalid2 = PwaasConfig.class
                .getResourceAsStream("/pwaas-invalid-pwlabel.json");
        InputStream jsonStreamInvalid3 = PwaasConfig.class
                .getResourceAsStream("/pwaas-invalid-vlan.json");
        InputStream jsonStreamInvalid4 = PwaasConfig.class
                .getResourceAsStream("/pwaas-conflicting-vlan.json");

        String key = SegmentRoutingManager.APP_NAME;
        ApplicationId subject = new TestApplicationId(key);
        ObjectMapper mapper = new ObjectMapper();

        JsonNode jsonNode = mapper.readTree(jsonStream);
        JsonNode jsonNodeInvalid1 = mapper.readTree(jsonStreamInvalid1);
        JsonNode jsonNodeInvalid2 = mapper.readTree(jsonStreamInvalid2);
        JsonNode jsonNodeInvalid3 = mapper.readTree(jsonStreamInvalid3);
        JsonNode jsonNodeInvalid4 = mapper.readTree(jsonStreamInvalid4);

        ConfigApplyDelegate delegate = new MockDelegate();

        DeviceService devService = new MockDeviceService();
        InterfaceService infService = new MockInterfaceService(INTERFACES);

        // create two devices and add them
        DefaultAnnotations.Builder builderDev1 = DefaultAnnotations.builder();
        DefaultAnnotations.Builder builderDev2 = DefaultAnnotations.builder();

        Device dev1 = new MockDevice(DEV1, builderDev1.build());
        Device dev2 = new MockDevice(DEV2, builderDev2.build());
        ((MockDeviceService) devService).addDevice(dev1);
        ((MockDeviceService) devService).addDevice(dev2);

        config = new PwaasConfig(devService, infService);
        invalidConfigVlan = new PwaasConfig(devService, infService);
        invalidConfigMode = new PwaasConfig(devService, infService);
        invalidConfigLabel = new PwaasConfig(devService, infService);
        invalidConfigConflictingVlan = new PwaasConfig(devService, infService);

        config.init(subject, key, jsonNode, mapper, delegate);
        invalidConfigVlan.init(subject, key, jsonNodeInvalid1, mapper, delegate);
        invalidConfigMode.init(subject, key, jsonNodeInvalid2, mapper, delegate);
        invalidConfigLabel.init(subject, key, jsonNodeInvalid3, mapper, delegate);
        invalidConfigConflictingVlan.init(subject, key, jsonNodeInvalid4, mapper, delegate);

        config.deviceService = devService;
        config.intfService = infService;

        invalidConfigVlan.deviceService = devService;
        invalidConfigVlan.intfService = infService;

        invalidConfigLabel.deviceService = devService;
        invalidConfigLabel.intfService = infService;

        invalidConfigMode.deviceService = devService;
        invalidConfigMode.intfService = infService;

        invalidConfigConflictingVlan.deviceService = devService;
        invalidConfigConflictingVlan.intfService = infService;
    }

    /**
     * Tests config validity.
     */
    @Test
    public void testIsValid() {
        try {
            assertTrue(config.isValid());
        } catch (IllegalArgumentException e) {
            assertTrue(false);
        }
    }

    /**
     * Tests config in-validity.
     */
    @Test
    public void testValid1() {
        assertFalse(invalidConfigVlan.isValid());
    }

    @Test
    public void testValid2() {
        assertFalse(invalidConfigMode.isValid());
    }

    @Test
    public void testValid3() {
        assertFalse(invalidConfigLabel.isValid());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValid4() {
        invalidConfigConflictingVlan.isValid();
    }

    /**
     * Tests getPwIds.
     */
    @Test
    public void testGetPwIds() {
        Set<Long> pwIds = config.getPwIds();

        assertThat(pwIds.size(), is(2));
        assertTrue(pwIds.contains(Long.parseLong(TUNNEL_ID_1)));
        assertTrue(pwIds.contains(Long.parseLong(TUNNEL_ID_2)));
        assertFalse(pwIds.contains(Long.parseLong(NOT_PRESENT_TUNNEL_ID)));
    }

    /**
     * Tests getPwDescription.
     */
    @Test
    public void testGetPwDescription() {
        DefaultL2TunnelDescription l2TunnelDescription = null;

        DefaultL2Tunnel l2Tunnel = new DefaultL2Tunnel(
            L2Mode.valueOf(MODE_1),
            SD_TAG_1,
            Long.parseLong(TUNNEL_ID_1),
            PW_LABEL_1
        );
        DefaultL2TunnelPolicy l2TunnelPolicy = new DefaultL2TunnelPolicy(
                Long.parseLong(TUNNEL_ID_1),
                INGRESS_1,
                INGRESS_INNER_TAG_1,
                INGRESS_OUTER_TAG_1,
                EGRESS_1,
                EGRESS_INNER_TAG_1,
                EGRESS_OUTER_TAG_1
        );

        l2TunnelDescription = config.getPwDescription(Long.parseLong(TUNNEL_ID_1));
        assertThat(l2TunnelDescription.l2Tunnel().pwMode(), is(l2Tunnel.pwMode()));
        assertThat(l2TunnelDescription.l2Tunnel().sdTag(), is(l2Tunnel.sdTag()));
        assertThat(l2TunnelDescription.l2Tunnel().tunnelId(), is(l2Tunnel.tunnelId()));
        assertThat(l2TunnelDescription.l2Tunnel().pwLabel(), is(l2Tunnel.pwLabel()));
        assertThat(l2TunnelDescription.l2TunnelPolicy().tunnelId(), is(l2TunnelPolicy.tunnelId()));
        assertThat(l2TunnelDescription.l2TunnelPolicy().cP1InnerTag(), is(l2TunnelPolicy.cP1InnerTag()));
        assertThat(l2TunnelDescription.l2TunnelPolicy().cP1OuterTag(), is(l2TunnelPolicy.cP1OuterTag()));
        assertThat(l2TunnelDescription.l2TunnelPolicy().cP2InnerTag(), is(l2TunnelPolicy.cP2InnerTag()));
        assertThat(l2TunnelDescription.l2TunnelPolicy().cP2OuterTag(), is(l2TunnelPolicy.cP2OuterTag()));
        assertThat(l2TunnelDescription.l2TunnelPolicy().cP1(), is(l2TunnelPolicy.cP1()));
        assertThat(l2TunnelDescription.l2TunnelPolicy().cP2(), is(l2TunnelPolicy.cP2()));

        l2Tunnel = new DefaultL2Tunnel(
                L2Mode.valueOf(MODE_2),
                SD_TAG_2,
                Long.parseLong(TUNNEL_ID_2),
                PW_LABEL_2
        );
        l2TunnelPolicy = new DefaultL2TunnelPolicy(
                Long.parseLong(TUNNEL_ID_2),
                INGRESS_2,
                INGRESS_INNER_TAG_2,
                INGRESS_OUTER_TAG_2,
                EGRESS_2,
                EGRESS_INNER_TAG_2,
                EGRESS_OUTER_TAG_2
        );

        l2TunnelDescription = config.getPwDescription(Long.parseLong(TUNNEL_ID_2));
        assertThat(l2TunnelDescription.l2Tunnel().pwMode(), is(l2Tunnel.pwMode()));
        assertThat(l2TunnelDescription.l2Tunnel().sdTag(), is(l2Tunnel.sdTag()));
        assertThat(l2TunnelDescription.l2Tunnel().tunnelId(), is(l2Tunnel.tunnelId()));
        assertThat(l2TunnelDescription.l2Tunnel().pwLabel(), is(l2Tunnel.pwLabel()));
        assertThat(l2TunnelDescription.l2TunnelPolicy().tunnelId(), is(l2TunnelPolicy.tunnelId()));
        assertThat(l2TunnelDescription.l2TunnelPolicy().cP1InnerTag(), is(l2TunnelPolicy.cP1InnerTag()));
        assertThat(l2TunnelDescription.l2TunnelPolicy().cP1OuterTag(), is(l2TunnelPolicy.cP1OuterTag()));
        assertThat(l2TunnelDescription.l2TunnelPolicy().cP2OuterTag(), is(l2TunnelPolicy.cP2OuterTag()));
        assertThat(l2TunnelDescription.l2TunnelPolicy().cP2OuterTag(), is(l2TunnelPolicy.cP2OuterTag()));
        assertThat(l2TunnelDescription.l2TunnelPolicy().cP1(), is(l2TunnelPolicy.cP1()));
        assertThat(l2TunnelDescription.l2TunnelPolicy().cP2(), is(l2TunnelPolicy.cP2()));
    }

    private class MockDelegate implements ConfigApplyDelegate {
        @Override
        public void onApply(Config config) {
        }
    }

}
