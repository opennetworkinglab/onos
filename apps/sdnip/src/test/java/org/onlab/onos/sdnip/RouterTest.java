package org.onlab.onos.sdnip;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.onlab.onos.ApplicationId;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DefaultHost;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.Host;
import org.onlab.onos.net.HostId;
import org.onlab.onos.net.HostLocation;
import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.flow.DefaultTrafficSelector;
import org.onlab.onos.net.flow.DefaultTrafficTreatment;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.TrafficTreatment;
import org.onlab.onos.net.host.HostService;
import org.onlab.onos.net.intent.IntentService;
import org.onlab.onos.net.intent.MultiPointToSinglePointIntent;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.onos.sdnip.config.BgpPeer;
import org.onlab.onos.sdnip.config.Interface;
import org.onlab.onos.sdnip.config.SdnIpConfigService;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onlab.util.TestUtils;
import org.onlab.util.TestUtils.TestUtilsException;

import com.google.common.collect.Sets;

/**
 * This class tests adding a route, updating a route, deleting a route,
 * and adding a route whose next hop is the local BGP speaker.
 */
public class RouterTest {

    private SdnIpConfigService sdnIpConfigService;
    private InterfaceService interfaceService;
    private IntentService intentService;
    private HostService hostService;

    private Map<IpAddress, BgpPeer> bgpPeers;
    private Map<IpAddress, BgpPeer> configuredPeers;
    private Set<Interface> interfaces;
    private Set<Interface> configuredInterfaces;

    private static final ApplicationId APPID = new ApplicationId() {
        @Override
        public short id() {
            return 1;
        }

        @Override
        public String name() {
            return "SDNIP";
        }
    };

    private Router router;

    @Before
    public void setUp() throws Exception {
        bgpPeers = setUpBgpPeers();
        interfaces = setUpInterfaces();
        initRouter();
    }

    /**
     * Initializes Router class.
     */
    private void initRouter() {

        intentService = createMock(IntentService.class);
        hostService = createMock(HostService.class);

        interfaceService = createMock(InterfaceService.class);
        expect(interfaceService.getInterfaces()).andReturn(
                interfaces).anyTimes();

        Set<IpPrefix> ipAddressesOnSw1Eth1 = new HashSet<IpPrefix>();
        ipAddressesOnSw1Eth1.add(IpPrefix.valueOf("192.168.10.0/24"));
        Interface expectedInterface =
                new Interface(new ConnectPoint(
                        DeviceId.deviceId("of:0000000000000001"),
                        PortNumber.portNumber("1")),
                        ipAddressesOnSw1Eth1,
                        MacAddress.valueOf("00:00:00:00:00:01"));
        ConnectPoint egressPoint = new ConnectPoint(
                DeviceId.deviceId("of:0000000000000001"),
                PortNumber.portNumber(1));
        expect(interfaceService.getInterface(egressPoint)).andReturn(
                expectedInterface).anyTimes();

        Set<IpPrefix> ipAddressesOnSw2Eth1 = new HashSet<IpPrefix>();
        ipAddressesOnSw2Eth1.add(IpPrefix.valueOf("192.168.20.0/24"));
        Interface expectedInterfaceNew =
                new Interface(new ConnectPoint(
                        DeviceId.deviceId("of:0000000000000002"),
                        PortNumber.portNumber("1")),
                        ipAddressesOnSw2Eth1,
                        MacAddress.valueOf("00:00:00:00:00:02"));
        ConnectPoint egressPointNew = new ConnectPoint(
                DeviceId.deviceId("of:0000000000000002"),
                PortNumber.portNumber(1));
        expect(interfaceService.getInterface(egressPointNew)).andReturn(
                expectedInterfaceNew).anyTimes();
        replay(interfaceService);

        sdnIpConfigService = createMock(SdnIpConfigService.class);
        expect(sdnIpConfigService.getBgpPeers()).andReturn(bgpPeers).anyTimes();
        replay(sdnIpConfigService);

        router = new Router(APPID, intentService,
                hostService, sdnIpConfigService, interfaceService);
    }

    /**
     * Sets up BGP peers in external networks.
     *
     * @return configured BGP peers as a Map from peer IP address to BgpPeer
     */
    private Map<IpAddress, BgpPeer> setUpBgpPeers() {

        configuredPeers = new HashMap<>();

        String peerSw1Eth1 = "192.168.10.1";
        configuredPeers.put(IpAddress.valueOf(peerSw1Eth1),
                new BgpPeer("00:00:00:00:00:00:00:01", 1, peerSw1Eth1));

        // Two BGP peers are connected to switch 2 port 1.
        String peer1Sw2Eth1 = "192.168.20.1";
        configuredPeers.put(IpAddress.valueOf(peer1Sw2Eth1),
                new BgpPeer("00:00:00:00:00:00:00:02", 1, peer1Sw2Eth1));

        String peer2Sw2Eth1 = "192.168.20.2";
        configuredPeers.put(IpAddress.valueOf(peer2Sw2Eth1),
                new BgpPeer("00:00:00:00:00:00:00:02", 1, peer2Sw2Eth1));

        return configuredPeers;
    }

    /**
     * Sets up logical interfaces, which emulate the configured interfaces
     * in SDN-IP application.
     *
     * @return configured interfaces as a Set
     */
    private Set<Interface> setUpInterfaces() {

        configuredInterfaces = Sets.newHashSet();

        Set<IpPrefix> ipAddressesOnSw1Eth1 = new HashSet<IpPrefix>();
        ipAddressesOnSw1Eth1.add(IpPrefix.valueOf("192.168.10.0/24"));
        configuredInterfaces.add(
                new Interface(new ConnectPoint(
                        DeviceId.deviceId("of:0000000000000001"),
                        PortNumber.portNumber(1)),
                        ipAddressesOnSw1Eth1,
                        MacAddress.valueOf("00:00:00:00:00:01")));

        Set<IpPrefix> ipAddressesOnSw2Eth1 = new HashSet<IpPrefix>();
        ipAddressesOnSw2Eth1.add(IpPrefix.valueOf("192.168.20.0/24"));
        configuredInterfaces.add(
                new Interface(new ConnectPoint(
                        DeviceId.deviceId("of:0000000000000002"),
                        PortNumber.portNumber(1)),
                        ipAddressesOnSw2Eth1,
                        MacAddress.valueOf("00:00:00:00:00:02")));

        Set<IpPrefix> ipAddressesOnSw3Eth1 = new HashSet<IpPrefix>();
        ipAddressesOnSw3Eth1.add(IpPrefix.valueOf("192.168.30.0/24"));
        configuredInterfaces.add(
                new Interface(new ConnectPoint(
                        DeviceId.deviceId("of:0000000000000003"),
                        PortNumber.portNumber(1)),
                        ipAddressesOnSw3Eth1,
                        MacAddress.valueOf("00:00:00:00:00:03")));

        return configuredInterfaces;
    }

    /**
     * This method tests adding a route entry.
     */
    @Test
    public void testProcessRouteAdd() throws TestUtilsException {

        // Construct a route entry
        RouteEntry routeEntry = new RouteEntry(
                IpPrefix.valueOf("1.1.1.0/24"),
                IpAddress.valueOf("192.168.10.1"));

        // Construct a MultiPointToSinglePointIntent intent
        TrafficSelector.Builder selectorBuilder =
                DefaultTrafficSelector.builder();
        selectorBuilder.matchEthType(Ethernet.TYPE_IPV4).matchIPDst(
                routeEntry.prefix());

        TrafficTreatment.Builder treatmentBuilder =
                DefaultTrafficTreatment.builder();
        treatmentBuilder.setEthDst(MacAddress.valueOf("00:00:00:00:00:01"));

        Set<ConnectPoint> ingressPoints = new HashSet<ConnectPoint>();
        ingressPoints.add(new ConnectPoint(
                DeviceId.deviceId("of:0000000000000002"),
                PortNumber.portNumber("1")));
        ingressPoints.add(new ConnectPoint(
                DeviceId.deviceId("of:0000000000000003"),
                PortNumber.portNumber("1")));

        ConnectPoint egressPoint = new ConnectPoint(
                DeviceId.deviceId("of:0000000000000001"),
                PortNumber.portNumber("1"));

        MultiPointToSinglePointIntent intent =
                new MultiPointToSinglePointIntent(APPID,
                        selectorBuilder.build(), treatmentBuilder.build(),
                        ingressPoints, egressPoint);

        // Reset host service
        reset(hostService);
        Set<Host> hosts = new HashSet<Host>(1);
        Set<IpPrefix> ipPrefixes = new HashSet<IpPrefix>();
        ipPrefixes.add(IpPrefix.valueOf("192.168.10.1/32"));
        hosts.add(new DefaultHost(ProviderId.NONE, HostId.NONE,
                MacAddress.valueOf("00:00:00:00:00:01"), VlanId.NONE,
                new HostLocation(
                        DeviceId.deviceId("of:0000000000000001"),
                        PortNumber.portNumber(1), 1),
                        ipPrefixes));
        expect(hostService.getHostsByIp(
                IpPrefix.valueOf("192.168.10.1/32"))).andReturn(hosts);
        replay(hostService);

        // Set up test expectation
        reset(intentService);
        intentService.submit(intent);
        replay(intentService);

        // Call the processRouteAdd() method in Router class
        router.leaderChanged(true);
        TestUtils.setField(router, "isActivatedLeader", true);
        router.processRouteAdd(routeEntry);

        // Verify
        assertEquals(router.getRoutes().size(), 1);
        assertTrue(router.getRoutes().contains(routeEntry));
        assertEquals(router.getPushedRouteIntents().size(), 1);
        assertEquals(router.getPushedRouteIntents().iterator().next(),
                intent);
        verify(intentService);
    }

    /**
     * This method tests updating a route entry.
     *
     * @throws TestUtilsException
     */
    @Test
    public void testRouteUpdate() throws TestUtilsException {

        // Firstly add a route
        testProcessRouteAdd();

        // Construct the existing route entry
        RouteEntry routeEntry = new RouteEntry(
                IpPrefix.valueOf("1.1.1.0/24"),
                IpAddress.valueOf("192.168.10.1"));

        // Construct the existing MultiPointToSinglePointIntent intent
        TrafficSelector.Builder selectorBuilder =
                DefaultTrafficSelector.builder();
        selectorBuilder.matchEthType(Ethernet.TYPE_IPV4).matchIPDst(
                routeEntry.prefix());

        TrafficTreatment.Builder treatmentBuilder =
                DefaultTrafficTreatment.builder();
        treatmentBuilder.setEthDst(MacAddress.valueOf("00:00:00:00:00:01"));

        ConnectPoint egressPoint = new ConnectPoint(
                DeviceId.deviceId("of:0000000000000001"),
                PortNumber.portNumber("1"));

        Set<ConnectPoint> ingressPoints = new HashSet<ConnectPoint>();
        ingressPoints.add(new ConnectPoint(
                DeviceId.deviceId("of:0000000000000002"),
                PortNumber.portNumber("1")));
        ingressPoints.add(new ConnectPoint(
                DeviceId.deviceId("of:0000000000000003"),
                PortNumber.portNumber("1")));

        MultiPointToSinglePointIntent intent =
                new MultiPointToSinglePointIntent(APPID,
                        selectorBuilder.build(), treatmentBuilder.build(),
                        ingressPoints, egressPoint);

        // Start to construct a new route entry and new intent
        RouteEntry routeEntryUpdate = new RouteEntry(
                IpPrefix.valueOf("1.1.1.0/24"),
                IpAddress.valueOf("192.168.20.1"));

        // Construct a new MultiPointToSinglePointIntent intent
        TrafficSelector.Builder selectorBuilderNew =
                DefaultTrafficSelector.builder();
        selectorBuilderNew.matchEthType(Ethernet.TYPE_IPV4).matchIPDst(
                routeEntryUpdate.prefix());

        TrafficTreatment.Builder treatmentBuilderNew =
                DefaultTrafficTreatment.builder();
        treatmentBuilderNew.setEthDst(MacAddress.valueOf("00:00:00:00:00:02"));

        ConnectPoint egressPointNew = new ConnectPoint(
                DeviceId.deviceId("of:0000000000000002"),
                PortNumber.portNumber("1"));

        Set<ConnectPoint> ingressPointsNew = new HashSet<ConnectPoint>();
        ingressPointsNew.add(new ConnectPoint(
                DeviceId.deviceId("of:0000000000000001"),
                PortNumber.portNumber("1")));
        ingressPointsNew.add(new ConnectPoint(
                DeviceId.deviceId("of:0000000000000003"),
                PortNumber.portNumber("1")));

        MultiPointToSinglePointIntent intentNew =
                new MultiPointToSinglePointIntent(APPID,
                        selectorBuilderNew.build(),
                        treatmentBuilderNew.build(),
                        ingressPointsNew, egressPointNew);

        // Reset host service
        reset(hostService);
        Set<Host> hosts = new HashSet<Host>(1);
        Set<IpPrefix> ipPrefixes = new HashSet<IpPrefix>();
        ipPrefixes.add(IpPrefix.valueOf("192.168.20.1/32"));
        hosts.add(new DefaultHost(ProviderId.NONE, HostId.NONE,
                MacAddress.valueOf("00:00:00:00:00:02"), VlanId.NONE,
                new HostLocation(
                        DeviceId.deviceId("of:0000000000000002"),
                        PortNumber.portNumber(1), 1),
                        ipPrefixes));
        expect(hostService.getHostsByIp(
                IpPrefix.valueOf("192.168.20.1/32"))).andReturn(hosts);
        replay(hostService);

        // Set up test expectation
        reset(intentService);
        intentService.withdraw(intent);
        intentService.submit(intentNew);
        replay(intentService);

        // Call the processRouteAdd() method in Router class
        router.leaderChanged(true);
        TestUtils.setField(router, "isActivatedLeader", true);
        router.processRouteAdd(routeEntryUpdate);

        // Verify
        assertEquals(router.getRoutes().size(), 1);
        assertTrue(router.getRoutes().contains(routeEntryUpdate));
        assertEquals(router.getPushedRouteIntents().size(), 1);
        assertEquals(router.getPushedRouteIntents().iterator().next(),
                intentNew);
        verify(intentService);
    }

    /**
     * This method tests deleting a route entry.
     */
    @Test
    public void testProcessRouteDelete() throws TestUtilsException {

        // Firstly add a route
        testProcessRouteAdd();

        // Construct the existing route entry
        RouteEntry routeEntry = new RouteEntry(
                IpPrefix.valueOf("1.1.1.0/24"),
                IpAddress.valueOf("192.168.10.1"));

        // Construct the existing MultiPointToSinglePointIntent intent
        TrafficSelector.Builder selectorBuilder =
                DefaultTrafficSelector.builder();
        selectorBuilder.matchEthType(Ethernet.TYPE_IPV4).matchIPDst(
                routeEntry.prefix());

        TrafficTreatment.Builder treatmentBuilder =
                DefaultTrafficTreatment.builder();
        treatmentBuilder.setEthDst(MacAddress.valueOf("00:00:00:00:00:01"));

        ConnectPoint egressPoint = new ConnectPoint(
                DeviceId.deviceId("of:0000000000000001"),
                PortNumber.portNumber("1"));

        Set<ConnectPoint> ingressPoints = new HashSet<ConnectPoint>();
        ingressPoints.add(new ConnectPoint(
                DeviceId.deviceId("of:0000000000000002"),
                PortNumber.portNumber("1")));
        ingressPoints.add(new ConnectPoint(
                DeviceId.deviceId("of:0000000000000003"),
                PortNumber.portNumber("1")));

        MultiPointToSinglePointIntent intent =
                new MultiPointToSinglePointIntent(APPID,
                        selectorBuilder.build(), treatmentBuilder.build(),
                        ingressPoints, egressPoint);

        // Set up expectation
        reset(intentService);
        intentService.withdraw(intent);
        replay(intentService);

        // Call route deleting method in Router class
        router.leaderChanged(true);
        TestUtils.setField(router, "isActivatedLeader", true);
        router.processRouteDelete(routeEntry);

        // Verify
        assertEquals(router.getRoutes().size(), 0);
        assertEquals(router.getPushedRouteIntents().size(), 0);
        verify(intentService);
    }

    /**
     * This method tests when the next hop of a route is the local BGP speaker.
     *
     * @throws TestUtilsException
     */
    @Test
    public void testLocalRouteAdd() throws TestUtilsException {

        // Construct a route entry, the next hop is the local BGP speaker
        RouteEntry routeEntry = new RouteEntry(
                IpPrefix.valueOf("1.1.1.0/24"), IpAddress.valueOf("0.0.0.0"));

        // Reset intentService to check whether the submit method is called
        reset(intentService);
        replay(intentService);

        // Call the processRouteAdd() method in Router class
        router.leaderChanged(true);
        TestUtils.setField(router, "isActivatedLeader", true);
        router.processRouteAdd(routeEntry);

        // Verify
        assertEquals(router.getRoutes().size(), 1);
        assertTrue(router.getRoutes().contains(routeEntry));
        assertEquals(router.getPushedRouteIntents().size(), 0);
        verify(intentService);
    }
}
