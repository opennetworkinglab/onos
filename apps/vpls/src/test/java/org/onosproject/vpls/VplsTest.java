/*
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
package org.onosproject.vpls;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Lists;
import com.google.common.collect.ImmutableSetMultimap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.TestApplicationId;
import org.onosproject.app.ApplicationService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.IdGenerator;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.incubator.net.intf.InterfaceListener;
import org.onosproject.incubator.net.intf.InterfaceService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultHost;
import org.onosproject.net.DeviceId;
import org.onosproject.net.EncapsulationType;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.PortNumber;
import org.onosproject.net.FilteredConnectPoint;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.net.host.HostServiceAdapter;
import org.onosproject.net.intent.ConnectivityIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.IntentServiceAdapter;
import org.onosproject.net.intent.IntentUtils;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.MultiPointToSinglePointIntent;
import org.onosproject.net.intent.SinglePointToMultiPointIntent;
import org.onosproject.net.intent.constraint.EncapsulationConstraint;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.routing.IntentSynchronizationAdminService;
import org.onosproject.routing.IntentSynchronizationService;
import org.onosproject.vpls.config.VplsConfigService;

import static java.lang.String.format;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import static org.onosproject.net.EncapsulationType.*;
import static org.onosproject.vpls.IntentInstaller.PREFIX_BROADCAST;
import static org.onosproject.vpls.IntentInstaller.PREFIX_UNICAST;

/**
 * Tests for the {@link Vpls} class.
 */
public class VplsTest {
    private static final String APP_NAME = "org.onosproject.vpls";
    private static final ApplicationId APPID = TestApplicationId.create(APP_NAME);
    private static final String DASH = "-";
    private static final int PRIORITY_OFFSET = 1000;
    private static final String VPLS1 = "vpls1";
    private static final String VPLS2 = "vpls2";
    private static final String VPLS3 = "vpls3";
    private static final String VPLS4 = "vpls4";

    private static final PortNumber P1 = PortNumber.portNumber(1);
    private static final PortNumber P2 = PortNumber.portNumber(2);

    private static final DeviceId DID1 = getDeviceId(1);
    private static final DeviceId DID2 = getDeviceId(2);
    private static final DeviceId DID3 = getDeviceId(3);
    private static final DeviceId DID4 = getDeviceId(4);
    private static final DeviceId DID5 = getDeviceId(5);
    private static final DeviceId DID6 = getDeviceId(6);

    private static final ConnectPoint CP1 = new ConnectPoint(DID1, P1);
    private static final ConnectPoint CP2 = new ConnectPoint(DID2, P1);
    private static final ConnectPoint CP3 = new ConnectPoint(DID3, P1);
    private static final ConnectPoint CP4 = new ConnectPoint(DID4, P1);
    private static final ConnectPoint CP5 = new ConnectPoint(DID5, P1);
    private static final ConnectPoint CP6 = new ConnectPoint(DID6, P1);
    private static final ConnectPoint CP7 = new ConnectPoint(DID4, P2);
    private static final ConnectPoint CP8 = new ConnectPoint(DID3, P2);
    private static final ConnectPoint CP9 = new ConnectPoint(DID5, P1);
    private static final ConnectPoint CP10 = new ConnectPoint(DID5, P2);

    private static final VlanId VLAN100 = VlanId.vlanId((short) 100);
    private static final VlanId VLAN200 = VlanId.vlanId((short) 200);
    private static final VlanId VLAN300 = VlanId.vlanId((short) 300);
    private static final VlanId VLAN400 = VlanId.vlanId((short) 400);
    private static final VlanId VLAN_NONE = VlanId.NONE;

    private static final MacAddress MAC1 = getMac(1);
    private static final MacAddress MAC2 = getMac(2);
    private static final MacAddress MAC3 = getMac(3);
    private static final MacAddress MAC4 = getMac(4);
    private static final MacAddress MAC5 = getMac(5);
    private static final MacAddress MAC6 = getMac(6);
    private static final MacAddress MAC7 = getMac(7);
    private static final MacAddress MAC8 = getMac(8);
    private static final MacAddress MAC9 = getMac(9);
    private static final MacAddress MAC10 = getMac(10);
    private static final MacAddress MAC11 = getMac(11);

    private static final Ip4Address IP1 = Ip4Address.valueOf("192.168.1.1");
    private static final Ip4Address IP2 = Ip4Address.valueOf("192.168.1.2");

    private static final HostId HID1 = HostId.hostId(MAC1, VLAN100);
    private static final HostId HID2 = HostId.hostId(MAC2, VLAN100);
    private static final HostId HID3 = HostId.hostId(MAC3, VLAN200);
    private static final HostId HID4 = HostId.hostId(MAC4, VLAN200);
    private static final HostId HID5 = HostId.hostId(MAC5, VLAN300);
    private static final HostId HID6 = HostId.hostId(MAC6, VLAN300);
    private static final HostId HID7 = HostId.hostId(MAC7, VLAN300);
    private static final HostId HID8 = HostId.hostId(MAC8, VLAN400);
    private static final HostId HID9 = HostId.hostId(MAC9);
    private static final HostId HID10 = HostId.hostId(MAC10);
    private static final HostId HID11 = HostId.hostId(MAC11);

    private static final ProviderId PID = new ProviderId("of", "foo");

    private static IdGenerator idGenerator;

    private static final Interface V100H1 =
            new Interface("v100h1", CP1, null, null, VLAN100);
    private static final Interface V100H2 =
            new Interface("v100h2", CP2, null, null, VLAN100);
    private static final Interface V200H1 =
            new Interface("v200h1", CP3, null, null, VLAN200);
    private static final Interface V200H2 =
            new Interface("v200h2", CP4, null, null, VLAN200);
    private static final Interface V300H1 =
            new Interface("v300h1", CP5, null, null, VLAN300);
    private static final Interface V300H2 =
            new Interface("v300h2", CP6, null, null, VLAN300);
    private static final Interface V400H1 =
            new Interface("v400h1", CP7, null, null, VLAN400);

    private static final Interface VNONEH1 =
            new Interface("vNoneh1", CP8, null, null, VLAN_NONE);
    private static final Interface VNONEH2 =
            new Interface("vNoneh2", CP9, null, null, VLAN_NONE);
    private static final Interface VNONEH3 =
            new Interface("vNoneh3", CP10, null, null, VLAN_NONE);

    private static final Host V100HOST1 =
            new DefaultHost(PID, HID1, MAC1, VLAN100,
                            getLocation(1), Collections.singleton(IP1));
    private static final Host V100HOST2 =
            new DefaultHost(PID, HID2, MAC2, VLAN100,
                            getLocation(2), Sets.newHashSet());
    private static final Host V200HOST1 =
            new DefaultHost(PID, HID3, MAC3, VLAN200,
                            getLocation(3), Collections.singleton(IP2));
    private static final Host V200HOST2 =
            new DefaultHost(PID, HID4, MAC4, VLAN200,
                            getLocation(4), Sets.newHashSet());
    private static final Host V300HOST1 =
            new DefaultHost(PID, HID5, MAC5, VLAN300,
                            getLocation(5), Sets.newHashSet());
    private static final Host V300HOST2 =
            new DefaultHost(PID, HID6, MAC6, VLAN300,
                            getLocation(6), Sets.newHashSet());
    private static final Host V300HOST3 =
            new DefaultHost(PID, HID7, MAC7, VLAN300,
                            getLocation(7), Sets.newHashSet());
    private static final Host V400HOST1 =
            new DefaultHost(PID, HID8, MAC8, VLAN400,
                            getLocation(4, 2), Sets.newHashSet());

    private static final Host VNONEHOST1 =
            new DefaultHost(PID, HID9, MAC9, VlanId.NONE,
                            getLocation(3, 2), Sets.newHashSet());
    private static final Host VNONEHOST2 =
            new DefaultHost(PID, HID10, MAC10, VlanId.NONE,
                            getLocation(5, 1), Sets.newHashSet());
    private static final Host VNONEHOST3 =
            new DefaultHost(PID, HID11, MAC11, VlanId.NONE,
                            getLocation(5, 2), Sets.newHashSet());

    private static final Set<Interface> AVAILABLE_INTERFACES =
            ImmutableSet.of(V100H1, V100H2, V200H1, V200H2, V300H1, V300H2,
                            V400H1, VNONEH1, VNONEH2);

    private static final Set<Host> AVAILABLE_HOSTS =
            ImmutableSet.of(V100HOST1, V100HOST2, V200HOST1,
                            V200HOST2, V300HOST1, V300HOST2, V300HOST3,
                            VNONEHOST1, VNONEHOST2,
                            V400HOST1, VNONEHOST3);

    private SetMultimap<String, Interface> interfacesByVpls = HashMultimap.create();

    private ApplicationService applicationService;
    private CoreService coreService;
    private HostListener hostListener;
    private NetworkConfigService configService;
    private Set<Host> hostsAvailable;
    private HostService hostService;
    private IntentService intentService;
    private InterfaceService interfaceService;
    private VplsConfigService vplsConfigService;
    private Vpls vpls;

    @Before
    public void setUp() throws Exception {
        idGenerator = new TestIdGenerator();
        Intent.bindIdGenerator(idGenerator);

        applicationService = createMock(ApplicationService.class);

        configService = createMock(NetworkConfigService.class);

        coreService = createMock(CoreService.class);
        expect(coreService.registerApplication(APP_NAME))
                .andReturn(APPID);
        replay(coreService);

        hostsAvailable = Sets.newHashSet();
        hostService = new TestHostService(hostsAvailable);

        intentService = new TestIntentService();

        TestIntentSynchronizer intentSynchronizer =
                new TestIntentSynchronizer(intentService);

        interfaceService = createMock(InterfaceService.class);
        interfaceService.addListener(anyObject(InterfaceListener.class));
        expectLastCall().anyTimes();
        addIfaceConfig();

        interfacesByVpls.put(VPLS1, V100H1);
        interfacesByVpls.put(VPLS1, V200H1);
        interfacesByVpls.put(VPLS1, V300H1);
        interfacesByVpls.put(VPLS2, V100H2);
        interfacesByVpls.put(VPLS2, V200H2);
        interfacesByVpls.put(VPLS2, V300H2);
        interfacesByVpls.put(VPLS3, VNONEH1);
        interfacesByVpls.put(VPLS3, VNONEH2);
        interfacesByVpls.put(VPLS4, V400H1);
        interfacesByVpls.put(VPLS4, VNONEH3);

        Map<String, EncapsulationType> encapByVpls = new HashMap<>();
        encapByVpls.put(VPLS1, VLAN);
        encapByVpls.put(VPLS2, NONE);
        encapByVpls.put(VPLS3, NONE);
        encapByVpls.put(VPLS4, NONE);

        vplsConfigService = new TestVplsConfigService(interfacesByVpls, encapByVpls);

        vpls = new Vpls();
        vpls.applicationService = applicationService;
        vpls.coreService = coreService;
        vpls.hostService = hostService;
        vpls.vplsConfigService = vplsConfigService;
        vpls.intentService = intentService;
        vpls.interfaceService = interfaceService;
        vpls.configService = configService;
        vpls.intentSynchronizer = intentSynchronizer;

    }

    @After
    public void tearDown() {
        Intent.unbindIdGenerator(idGenerator);
    }

    /**
     * Creates the interface configuration:
     *  On devices 1 and 2 is configured an interface on port 1 with vlan 100.
     *  On device 3 is configured an interface on port 3 with no vlan.
     *  On devices 3 and 4 is configured an interface on port 1 with vlan 200.
     *  On device 4 is an interface configured on port 2 with vlan 400.
     *  On device 5 are configured two interfaces on port 1 and 2 with no vlan.
     *  On device 5 and 6 is configured an interface on port 1 with vlan 300.
     */
    private void addIfaceConfig() {
        Set<Interface> interfaces = ImmutableSet.copyOf(AVAILABLE_INTERFACES);
        Set<Interface> vlanOneSet = ImmutableSet.of(V100H1, V100H2);
        Set<Interface> vlanTwoSet = ImmutableSet.of(V200H1, V200H2);
        Set<Interface> vlanThreeSet = ImmutableSet.of(VNONEH1, VNONEH2);
        Set<Interface> vlanFourSet = ImmutableSet.of(V400H1, VNONEH3);

        AVAILABLE_INTERFACES.forEach(intf -> {
            expect(interfaceService.getInterfacesByPort(intf.connectPoint()))
                    .andReturn(Sets.newHashSet(intf)).anyTimes();
        });
        expect(interfaceService.getInterfacesByVlan(VLAN100))
                .andReturn(vlanOneSet).anyTimes();
        expect(interfaceService.getInterfacesByVlan(VLAN200))
                .andReturn(vlanTwoSet).anyTimes();
        expect(interfaceService.getInterfacesByVlan(VLAN300))
                .andReturn(vlanThreeSet).anyTimes();
        expect(interfaceService.getInterfacesByVlan(VLAN400))
                .andReturn(vlanFourSet).anyTimes();
        expect(interfaceService.getInterfacesByVlan(VlanId.NONE))
                .andReturn(vlanFourSet).anyTimes();
        expect(interfaceService.getInterfaces()).andReturn(interfaces).anyTimes();

        replay(interfaceService);
    }

    /**
     * Seven ports are configured with VLANs, while three ports are not. No hosts are
     * registered by the HostService.
     *
     * The first three ports have an interface configured on VPLS 1,
     * the other three on VPLS 2. Two ports are defined for VPLS 3, while
     * the two remaining ports are configured on VPLS 4.
     *
     * The number of intents expected is 10: three for VPLS 1, three for VPLS 2,
     * two for VPLS 3, two for VPLS 4. Eight MP2SP intents.
     * Checks if the number of intents submitted to the intent framework is
     * equal to the number of intents expected and if all intents are equivalent.
     */
    @Test
    public void activateNoHosts() {
        vpls.activate();

        List<Intent> expectedIntents = Lists.newArrayList();
        Set<FilteredConnectPoint> fcPoints;

        fcPoints = buildFCPoints(ImmutableSet.of(V100H1, V200H1, V300H1));
        expectedIntents.addAll(generateVplsBrc(fcPoints, VPLS1, VLAN));

        fcPoints = buildFCPoints(ImmutableSet.of(V100H2, V200H2, V300H2));
        expectedIntents.addAll(generateVplsBrc(fcPoints, VPLS2, NONE));

        fcPoints = buildFCPoints(ImmutableSet.of(VNONEH1, VNONEH2));
        expectedIntents.addAll(generateVplsBrc(fcPoints, VPLS3, NONE));

        fcPoints = buildFCPoints(ImmutableSet.of(V400H1, VNONEH3));
        expectedIntents.addAll(generateVplsBrc(fcPoints, VPLS4, NONE));

        checkIntents(expectedIntents);
    }

    /**
     * Ten ports are configured with VLANs and ten hosts are registered by the
     * HostService.
     *
     * The first three ports have an interface configured on VPLS 1,
     * the other three on VPLS 2, two on VPLS3 and two on VPLS4.
     *
     * The number of intents expected is twenty: six
     * for VPLS 1, six for VPLS 2. four for VPLS 3, four for VPLS 4.
     * That is ten sp2mp intents, ten mp2sp intents. For VPLS 1
     * IPs are added to demonstrate this doesn't influence the number of intents
     * created. Checks if the number of intents submitted to the intent
     * framework is equal to the number of intents expected and if all intents
     * are equivalent.
     */
    @Test
    public void tenInterfacesConfiguredHostsPresent() {
        hostsAvailable.addAll(AVAILABLE_HOSTS);

        vpls.activate();

        List<Intent> expectedIntents = Lists.newArrayList();
        Set<FilteredConnectPoint> fcPoints;
        Set<Host> hosts;

        fcPoints = buildFCPoints(ImmutableSet.of(V100H1, V200H1, V300H1));
        hosts = ImmutableSet.of(V100HOST1, V200HOST1, V300HOST1);
        expectedIntents.addAll(generateVplsBrc(fcPoints, VPLS1, VLAN));
        expectedIntents.addAll(generateVplsUni(fcPoints, hosts, VPLS1, VLAN));

        fcPoints = buildFCPoints(ImmutableSet.of(V100H2, V200H2, V300H2));
        hosts = ImmutableSet.of(V100HOST2, V200HOST2, V300HOST2);
        expectedIntents.addAll(generateVplsBrc(fcPoints, VPLS2, NONE));
        expectedIntents.addAll(generateVplsUni(fcPoints, hosts, VPLS2, NONE));

        fcPoints = buildFCPoints(ImmutableSet.of(VNONEH1, VNONEH2));
        hosts = ImmutableSet.of(VNONEHOST1, VNONEHOST2);
        expectedIntents.addAll(generateVplsBrc(fcPoints, VPLS3, NONE));
        expectedIntents.addAll(generateVplsUni(fcPoints, hosts, VPLS3, NONE));

        fcPoints = buildFCPoints(ImmutableSet.of(V400H1, VNONEH3));
        hosts = ImmutableSet.of(V400HOST1, VNONEHOST3);
        expectedIntents.addAll(generateVplsBrc(fcPoints, VPLS4, NONE));
        expectedIntents.addAll(generateVplsUni(fcPoints, hosts, VPLS4, NONE));

        checkIntents(expectedIntents);
    }

    /**
     * Ten ports are configured; seven have VLANs and three do not.
     * Initially, no hosts are registered by the HostService.
     *
     * The first three ports have an interface configured on
     * VPLS 1, three have an interface configured on VPLS 2, two have an
     * interface configured on VPLS 3 and two have an interface configured
     * on VPLS 4, three have an interface configure. When the
     * module starts up, three hosts attached to device one, two and three -
     * port 1, are registered by the HostService and events are sent to the
     * application. sp2mp intents are created for all interfaces configured and
     * mp2sp intents are created only for the hosts attached.
     *
     * The number of intents expected is seventeen: six for VPLS 1,
     * three for VPLS 2, four for VPLS3 and four for VPLS4.
     * Ten sp2mp intents, seven mp2sp intents. IPs are added on the first two
     * hosts only to demonstrate this doesn't influence the number of intents
     * created.
     * An additional host is added on device seven - port 1, to demonstrate that
     * the application does not generate intents, even if the interface uses the
     * same VLAN Id of the other interfaces configured for the specifc VPLS.
     * Checks if the number of intents submitted to the intent framework is equal
     * to the number of intents expected and if all intents are equivalent.
     */
    @Test
    public void tenInterfacesThreeHostEventsSameVpls() {
        vpls.activate();

        List<Intent> expectedIntents = Lists.newArrayList();
        Set<FilteredConnectPoint> fcPoints;
        Set<Host> hosts;

        hostsAvailable.addAll(Sets.newHashSet(V100HOST1, V200HOST1,
                                              V300HOST1, V300HOST3,
                                              VNONEHOST1, VNONEHOST2,
                                              V400HOST1, VNONEHOST3));
        hostsAvailable.forEach(host ->
                                       hostListener.event(new HostEvent(HostEvent.Type.HOST_ADDED, host)));

        fcPoints = buildFCPoints(ImmutableSet.of(V100H1, V200H1, V300H1));
        hosts = ImmutableSet.of(V100HOST1, V200HOST1, V300HOST1);
        expectedIntents.addAll(generateVplsBrc(fcPoints, VPLS1, VLAN));
        expectedIntents.addAll(generateVplsUni(fcPoints, hosts, VPLS1, VLAN));

        fcPoints = buildFCPoints(ImmutableSet.of(V100H2, V200H2, V300H2));
        expectedIntents.addAll(generateVplsBrc(fcPoints, VPLS2, NONE));

        fcPoints = buildFCPoints(ImmutableSet.of(VNONEH1, VNONEH2));
        hosts = ImmutableSet.of(VNONEHOST1, VNONEHOST2);
        expectedIntents.addAll(generateVplsBrc(fcPoints, VPLS3, NONE));
        expectedIntents.addAll(generateVplsUni(fcPoints, hosts, VPLS3, NONE));

        fcPoints = buildFCPoints(ImmutableSet.of(V400H1, VNONEH3));
        hosts = ImmutableSet.of(V400HOST1, VNONEHOST3);
        expectedIntents.addAll(generateVplsBrc(fcPoints, VPLS4, NONE));
        expectedIntents.addAll(generateVplsUni(fcPoints, hosts, VPLS4, NONE));

        checkIntents(expectedIntents);
    }

    /**
     * Generates a list of the expected sp2mp intents for a VPLS.
     *
     * @param fcPoints the filtered connect point
     * @param name the name of the VPLS
     * @param encap the encapsulation type
     * @return the list of expected sp2mp intents for the given VPLS
     */
    private List<SinglePointToMultiPointIntent>
    generateVplsBrc(Set<FilteredConnectPoint> fcPoints, String name, EncapsulationType encap) {
        List<SinglePointToMultiPointIntent> intents = Lists.newArrayList();

        fcPoints.forEach(point -> {
            Set<FilteredConnectPoint> otherPoints =
                    fcPoints.stream()
                            .filter(fcp -> !fcp.equals(point))
                            .collect(Collectors.toSet());

            Key brckey = buildKey(PREFIX_BROADCAST,
                                  point.connectPoint(), name, MacAddress.BROADCAST);

            intents.add(buildBrcIntent(brckey, point, otherPoints, encap));
        });

        return intents;
    }

    /**
     * Generates a list of expected mp2sp intents for a given VPLS.
     *
     * @param fcPoints the filtered connect point
     * @param hosts the hosts
     * @param name the name of the VPLS
     * @param encap the encapsulation type
     * @return the list of expected mp2sp intents for the given VPLS
     */
    private List<MultiPointToSinglePointIntent>
    generateVplsUni(Set<FilteredConnectPoint> fcPoints, Set<Host> hosts,
                    String name, EncapsulationType encap) {
        List<MultiPointToSinglePointIntent> intents = Lists.newArrayList();

        hosts.forEach(host -> {
            FilteredConnectPoint hostPoint = getHostPoint(host, fcPoints);

            Set<FilteredConnectPoint> otherPoints =
                    fcPoints.stream()
                            .filter(fcp -> !fcp.equals(hostPoint))
                            .collect(Collectors.toSet());

            Key uniKey = buildKey(PREFIX_UNICAST,
                                  host.location(), name, host.mac());

            intents.add(buildUniIntent(uniKey, otherPoints, hostPoint, host, encap));
        });

        return intents;
    }

    /**
     * Checks if the number of intents submitted to the intent framework is equal
     * to the number of intents expected and if all intents are equivalent.
     *
     * @param intents the list of intents expected
     */
    private void checkIntents(List<Intent> intents) {
        assertEquals("The number of intents submitted differs from the number" +
                             " of intents expected. ",
                     intents.size(), intentService.getIntentCount());

        for (Intent intentOne : intents) {
            boolean found = false;
            for (Intent intentTwo : intentService.getIntents()) {
                if (intentOne.key().equals(intentTwo.key())) {
                    found = true;
                    assertTrue(format("The intent submitted is different from" +
                                              " the intent expected. %s %s",
                                      intentOne, intentTwo),
                               IntentUtils.intentsAreEqual(intentOne, intentTwo));
                    break;
                }
            }
            assertTrue("The intent submitted is not equal to any of the expected" +
                               " intents. ", found);
        }
    }

    /**
     * Builds a broadcast intent.
     *
     * @param key the key to identify the intent
     * @param src the ingress connect point
     * @param dsts the egress connect points
     * @return the generated single-point to multi-point intent
     */
    private SinglePointToMultiPointIntent buildBrcIntent(Key key,
                                                         FilteredConnectPoint src,
                                                         Set<FilteredConnectPoint> dsts,
                                                         EncapsulationType encap) {
        SinglePointToMultiPointIntent.Builder intentBuilder;

        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthDst(MacAddress.BROADCAST)
                .build();

        intentBuilder = SinglePointToMultiPointIntent.builder()
                .appId(APPID)
                .key(key)
                .selector(selector)
                .filteredIngressPoint(src)
                .filteredEgressPoints(dsts)
                .priority(PRIORITY_OFFSET);

        encap(intentBuilder, encap);

        return intentBuilder.build();
    }

    /**
     * Builds a unicast intent.
     *
     * @param key the key to identify the intent
     * @param srcs the ingress connect points
     * @param dst the egress connect point
     * @param host the destination Host
     * @return the generated multi-point to single-point intent
     */
    private MultiPointToSinglePointIntent buildUniIntent(Key key,
                                                         Set<FilteredConnectPoint> srcs,
                                                         FilteredConnectPoint dst,
                                                         Host host,
                                                         EncapsulationType encap) {
        MultiPointToSinglePointIntent.Builder intentBuilder;

        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthDst(host.mac())
                .build();

        intentBuilder = MultiPointToSinglePointIntent.builder()
                .appId(APPID)
                .key(key)
                .selector(selector)
                .filteredIngressPoints(srcs)
                .filteredEgressPoint(dst)
                .priority(PRIORITY_OFFSET);

        encap(intentBuilder, encap);

        return intentBuilder.build();
    }

    /**
     * Returns the filtered connect point associated to a given host.
     *
     * @param host the target host
     * @param fcps the filtered connected points
     * @return the filtered connect point associated to the given host; null
     * otherwise
     */
    private FilteredConnectPoint getHostPoint(Host host,
                                              Set<FilteredConnectPoint> fcps) {
        return fcps.stream()
                .filter(fcp -> fcp.connectPoint().equals(host.location()))
                .filter(fcp -> {
                    VlanIdCriterion vlanCriterion =
                            (VlanIdCriterion) fcp.trafficSelector().
                                    getCriterion(Criterion.Type.VLAN_VID);
                    return vlanCriterion == null ||
                            vlanCriterion.vlanId().equals(host.vlan());
                })
                .findFirst()
                .orElse(null);
    }

    /**
     * Computes a set of filtered connect points from a list of given interfaces.
     *
     * @param interfaces the interfaces to compute
     * @return the set of filtered connect points
     */
    private Set<FilteredConnectPoint> buildFCPoints(Collection<Interface> interfaces) {
        // Build all filtered connected points in the VPLS
        return interfaces
                .stream()
                .map(intf -> {
                    TrafficSelector.Builder selectorBuilder =
                            DefaultTrafficSelector.builder();
                    if (!intf.vlan().equals(VlanId.NONE)) {
                        selectorBuilder.matchVlanId(intf.vlan());
                    }
                    return new FilteredConnectPoint(intf.connectPoint(),
                                                    selectorBuilder.build());
                })
                .collect(Collectors.toSet());
    }

    /**
     * Builds an intent Key either for a single-point to multi-point or
     * multi-point to single-point intent, based on a prefix that defines
     * the intent type, the connection point representing the source or the
     * destination and the VLAN Id representing the VPLS.
     *
     * @param prefix the key prefix
     * @param cPoint the ingress/egress connect point
     * @param vplsName the VPLS name
     * @param hostMac the ingress/egress MAC address
     * @return the key to identify the intent
     */
    private Key buildKey(String prefix,
                         ConnectPoint cPoint,
                         String vplsName,
                         MacAddress hostMac) {
        String keyString = vplsName +
                DASH +
                prefix +
                DASH +
                cPoint.deviceId() +
                DASH +
                cPoint.port() +
                DASH +
                hostMac;

        return Key.of(keyString, APPID);
    }

    /**
     * Adds an encapsulation constraint to the builder given, if encap is not
     * equal to NONE.
     *
     * @param builder the intent builder
     * @param encap the encapsulation type
     */
    private static void encap(ConnectivityIntent.Builder builder,
                              EncapsulationType encap) {
        if (!encap.equals(NONE)) {
            builder.constraints(ImmutableList.of(
                    new EncapsulationConstraint(encap)));
        }
    }

    /**
     * Returns the device Id of the ith device.
     *
     * @param i the device to get the Id of
     * @return the device Id
     */
    private static DeviceId getDeviceId(int i) {
        return DeviceId.deviceId("" + i);
    }

    private static MacAddress getMac(int n) {
        return MacAddress.valueOf(String.format("00:00:00:00:00:%s", n));
    }

    private static HostLocation getLocation(int i) {
        return new HostLocation(new ConnectPoint(getDeviceId(i), P1), 123L);
    }

    private static HostLocation getLocation(int d, int p) {
        return new HostLocation(new ConnectPoint(getDeviceId(d),
                                                 PortNumber.portNumber(p)), 123L);
    }

    /**
     * Represents a fake IntentService class that allows to store and retrieve
     * intents without implementing the IntentService logic.
     */
    private class TestIntentService extends IntentServiceAdapter {

        private Map<Key, Intent> intents;

        public TestIntentService() {
            intents = Maps.newHashMap();
        }

        @Override
        public void submit(Intent intent) {
            intents.put(intent.key(), intent);
        }

        @Override
        public long getIntentCount() {
            return intents.size();
        }

        @Override
        public Iterable<Intent> getIntents() {
            return intents.values();
        }

        @Override
        public Intent getIntent(Key intentKey) {
            for (Intent intent : intents.values()) {
                if (intent.key().equals(intentKey)) {
                    return intent;
                }
            }
            return null;
        }
    }

    /**
     * Represents a fake HostService class which allows to add hosts manually
     * in each test, when needed.
     */
    private class TestHostService extends HostServiceAdapter {

        private Set<Host> hosts;

        public TestHostService(Set<Host> hosts) {
            this.hosts = hosts;
        }

        @Override
        public void addListener(HostListener listener) {
            VplsTest.this.hostListener = listener;
        }

        @Override
        public Set<Host> getConnectedHosts(ConnectPoint connectPoint) {
            return hosts.stream()
                    .filter(h -> h.location().equals(connectPoint))
                    .collect(Collectors.toSet());
        }

    }

    /**
     * Represents a fake IdGenerator class for intents.
     */
    private static class TestIdGenerator implements IdGenerator {

        private final AtomicLong id = new AtomicLong(0);

        @Override
        public long getNewId() {
            return id.getAndIncrement();
        }

    }

    /**
     * Test IntentSynchronizer that passes all intents straight through to the
     * intent service.
     */
    private class TestIntentSynchronizer implements IntentSynchronizationService,
            IntentSynchronizationAdminService {

        private final IntentService intentService;

        /**
         * Creates a new intent test synchronizer.
         *
         * @param intentService intent service
         */
        public TestIntentSynchronizer(IntentService intentService) {
            this.intentService = intentService;
        }

        @Override
        public void submit(Intent intent) {
            intentService.submit(intent);
        }

        @Override
        public void withdraw(Intent intent) {
            intentService.withdraw(intent);
        }

        @Override
        public void modifyPrimary(boolean isPrimary) {
        }

        @Override
        public void removeIntents() {
        }

        @Override
        public void removeIntentsByAppId(ApplicationId applicationId) {
        }
    }

    /**
     * Represents a fake VplsConfigService class which is needed for testing.
     */
    private class TestVplsConfigService extends VplsConfigServiceAdapter {

        private final SetMultimap<String, Interface> ifacesByVplsName;
        private final Map<String, EncapsulationType> encapsByVplsName;

        private Set<String> vplsAffectByApi = new HashSet<>();

        TestVplsConfigService(SetMultimap<String, Interface> ifacesByVplsName,
                              Map<String, EncapsulationType> encapsByVplsName) {
            this.ifacesByVplsName = ifacesByVplsName;
            this.encapsByVplsName = encapsByVplsName;
        }

        @Override
        public void addVpls(String vplsName, Set<String> ifaceNames, String encap) {
            if (!ifacesByVplsName.containsKey(vplsName)) {
                ifaceNames.forEach(ifaceName -> {
                    AVAILABLE_INTERFACES.forEach(iface -> {
                        if (iface.name().equals(ifaceName)) {
                            ifacesByVplsName.put(vplsName, iface);
                        }
                    });
                });
            }
            if (!ifacesByVplsName.containsKey(vplsName)) {
                encapsByVplsName.put(vplsName, valueOf(encap));
            }
        }

        @Override
        public void removeVpls(String vplsName) {
            if (ifacesByVplsName.containsKey(vplsName)) {
                ifacesByVplsName.removeAll(vplsName);
            }
        }

        @Override
        public void addIface(String vplsName, String iface) {
            if (!ifacesByVplsName.containsKey(vplsName)) {
                AVAILABLE_INTERFACES.forEach(intf -> {
                    if (intf.name().equals(iface)) {
                        ifacesByVplsName.put(vplsName, intf);
                    }
                });
            }
        }

        @Override
        public void setEncap(String vplsName, String encap) {
            encapsByVplsName.put(vplsName, EncapsulationType.enumFromString(encap));
        }

        @Override
        public void removeIface(String iface) {
            SetMultimap<String, Interface> search = HashMultimap.create(ifacesByVplsName);
            search.entries().forEach(e -> {
                if (e.getValue().name().equals(iface)) {
                    ifacesByVplsName.remove(e.getKey(), iface);
                }
            });
        }

        @Override
        public void cleanVplsConfig() {
            ifacesByVplsName.clear();
        }

        @Override
        public EncapsulationType encap(String vplsName) {
            EncapsulationType encap = null;
            if (encapsByVplsName.containsKey(vplsName)) {
                encap = encapsByVplsName.get(vplsName);
            }
            return encap;
        }

        @Override
        public Set<String> vplsAffectedByApi() {
            Set<String> vplsNames = ImmutableSet.copyOf(vplsAffectByApi);

            vplsAffectByApi.clear();

            return vplsNames;
        }

        @Override
        public Set<Interface> allIfaces() {
            return ifacesByVplsName.values()
                    .stream()
                    .collect(Collectors.toSet());
        }

        @Override
        public Set<Interface> ifaces(String name) {
            return ifacesByVplsName.get(name)
                    .stream()
                    .collect(Collectors.toSet());
        }

        @Override
        public Set<String> vplsNames() {
            return ifacesByVplsName.keySet();
        }

        @Override
        public Set<String> vplsNamesOld() {
            return ifacesByVplsName.keySet();
        }

        public SetMultimap<String, Interface> ifacesByVplsName() {
            return ImmutableSetMultimap.copyOf(ifacesByVplsName);
        }

        @Override
        public SetMultimap<String, Interface> ifacesByVplsName(VlanId vlan,
                                                               ConnectPoint connectPoint) {
            String vplsName =
                    ifacesByVplsName.entries().stream()
                            .filter(e -> e.getValue().connectPoint().equals(connectPoint))
                            .filter(e -> e.getValue().vlan().equals(vlan))
                            .map(Map.Entry::getKey)
                            .findFirst()
                            .orElse(null);
            SetMultimap<String, Interface> result = HashMultimap.create();
            if (vplsName != null && ifacesByVplsName.containsKey(vplsName)) {
                ifacesByVplsName.get(vplsName)
                        .forEach(intf -> result.put(vplsName, intf));
                return result;
            }
            return null;
        }
    }
}
