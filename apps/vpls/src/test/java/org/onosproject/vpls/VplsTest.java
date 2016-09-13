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

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
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
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.net.host.HostServiceAdapter;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.IntentServiceAdapter;
import org.onosproject.net.intent.IntentUtils;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.MultiPointToSinglePointIntent;
import org.onosproject.net.intent.SinglePointToMultiPointIntent;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.routing.IntentSynchronizationAdminService;
import org.onosproject.routing.IntentSynchronizationService;

import com.google.common.collect.Sets;

import static java.lang.String.format;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the {@link Vpls} class.
 */
public class VplsTest {

    private static final int NUM_DEVICES = 7;

    private static final MacAddress MAC1 = MacAddress.valueOf("00:00:00:00:00:01");
    private static final MacAddress MAC2 = MacAddress.valueOf("00:00:00:00:00:02");
    private static final MacAddress MAC3 = MacAddress.valueOf("00:00:00:00:00:03");
    private static final MacAddress MAC4 = MacAddress.valueOf("00:00:00:00:00:04");
    private static final MacAddress MAC5 = MacAddress.valueOf("00:00:00:00:00:05");
    private static final MacAddress MAC6 = MacAddress.valueOf("00:00:00:00:00:06");
    private static final MacAddress MAC7 = MacAddress.valueOf("00:00:00:00:00:07");

    private static final Ip4Address IP1 = Ip4Address.valueOf("192.168.1.1");
    private static final Ip4Address IP2 = Ip4Address.valueOf("192.168.1.2");

    private static final PortNumber P1 = PortNumber.portNumber(1);

    private static final VlanId VLAN1 = VlanId.vlanId((short) 1);
    private static final VlanId VLAN2 = VlanId.vlanId((short) 2);

    private static final int PRIORITY_OFFSET = 1000;
    private static final String PREFIX_BROADCAST = "brc";
    private static final String PREFIX_UNICAST = "uni";

    private static final DeviceId DID1 = getDeviceId(1);
    private static final DeviceId DID2 = getDeviceId(2);
    private static final DeviceId DID3 = getDeviceId(3);
    private static final DeviceId DID4 = getDeviceId(4);
    private static final DeviceId DID5 = getDeviceId(5);
    private static final DeviceId DID6 = getDeviceId(6);

    private static final ConnectPoint C1 = new ConnectPoint(DID1, P1);
    private static final ConnectPoint C2 = new ConnectPoint(DID2, P1);
    private static final ConnectPoint C3 = new ConnectPoint(DID3, P1);
    private static final ConnectPoint C4 = new ConnectPoint(DID4, P1);
    private static final ConnectPoint C5 = new ConnectPoint(DID5, P1);
    private static final ConnectPoint C6 = new ConnectPoint(DID6, P1);

    private static final HostId HID1 = HostId.hostId(MAC1, VLAN1);
    private static final HostId HID2 = HostId.hostId(MAC2, VLAN1);
    private static final HostId HID3 = HostId.hostId(MAC3, VLAN1);
    private static final HostId HID4 = HostId.hostId(MAC4, VLAN2);
    private static final HostId HID5 = HostId.hostId(MAC5, VLAN2);
    private static final HostId HID6 = HostId.hostId(MAC6, VLAN2);
    private static final HostId HID7 = HostId.hostId(MAC7, VlanId.NONE);

    private ApplicationService applicationService;
    private CoreService coreService;
    private HostListener hostListener;
    private Set<Host> hostsAvailable;
    private HostService hostService;
    private IntentService intentService;
    private InterfaceService interfaceService;
    private Vpls vpls;

    private static final String APP_NAME = "org.onosproject.vpls";
    private static final ApplicationId APPID = TestApplicationId.create(APP_NAME);

    private static final ProviderId PID = new ProviderId("of", "foo");

    private static IdGenerator idGenerator;

    @Before
    public void setUp() throws Exception {
        idGenerator = new TestIdGenerator();
        Intent.bindIdGenerator(idGenerator);

        applicationService = createMock(ApplicationService.class);

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
        addIntfConfig();

        vpls = new Vpls();
        vpls.applicationService = applicationService;
        vpls.coreService = coreService;
        vpls.hostService = hostService;
        vpls.intentService = intentService;
        vpls.interfaceService = interfaceService;
        vpls.intentSynchronizer = intentSynchronizer;

    }

    @After
    public void tearDown() {
        Intent.unbindIdGenerator(idGenerator);
    }

    /**
     * Creates the interface configuration. On devices 1, 2 and 3 is configured
     * an interface on port 1 with vlan 1. On devices 4, 5 and 6 is configured
     * an interface on port 1 with vlan 2. On device 5 no interfaces are
     * configured.
     */
    private void addIntfConfig() {
        Set<Interface> interfaces = Sets.newHashSet();
        Set<Interface> vlanOneSet = Sets.newHashSet();
        Set<Interface> vlanTwoSet = Sets.newHashSet();

        for (int i = 1; i <= NUM_DEVICES - 1; i++) {
            ConnectPoint cp = new ConnectPoint(getDeviceId(i), P1);

            Interface intf =
                    new Interface("intfOne", cp, Collections.emptyList(), null,
                                  VlanId.NONE);

            if (i <= 3) {
                intf = new Interface("intfTwo", cp, Collections.emptyList(),
                                     null, VLAN1);
                interfaces.add(intf);
                vlanOneSet.add(intf);
            } else if (i > 3 && i <= 6) {
                intf = new Interface("intfThree", cp, Collections.emptyList(),
                                     null, VLAN2);
                interfaces.add(intf);
                vlanTwoSet.add(intf);
            }
            expect(interfaceService.getInterfacesByPort(cp))
                    .andReturn(Sets.newHashSet(intf)).anyTimes();
        }
        expect(interfaceService.getInterfacesByVlan(VLAN1))
                .andReturn(vlanOneSet).anyTimes();
        expect(interfaceService.getInterfacesByVlan(VLAN2))
                .andReturn(vlanTwoSet).anyTimes();
        expect(interfaceService.getInterfaces()).andReturn(interfaces).anyTimes();

        replay(interfaceService);
    }

    /**
     * Checks the case in which six ports are configured with VLANs but no
     * hosts are registered by the HostService. The first three ports have an
     * interface configured on VLAN1, the other three on VLAN2. The number of
     * intents expected is six: three for VLAN1, three for VLAN2. three sp2mp
     * intents, three mp2sp intents.
     */
    @Test
    public void testActivateNoHosts() {
        vpls.activate();

        List<Intent> expectedIntents = Lists.newArrayList();
        expectedIntents.addAll(generateVlanOneBrc());
        expectedIntents.addAll(generateVlanTwoBrc());

        checkIntents(expectedIntents);
    }

    /**
     * Checks the case in which six ports are configured with VLANs and four
     * hosts are registered by the HostService. The first three ports have an
     * interface configured on VLAN1, the other three on VLAN2. The number of
     * intents expected is twelve: six for VLAN1, six for VLAN2. six sp2mp
     * intents, six mp2sp intents. For VLAN1 IPs are added to demonstrate it
     * doesn't influence the number of intents created.
     */
    @Test
    public void testFourInterfacesConfiguredHostsPresent() {
        Host h1 = new DefaultHost(PID, HID1, MAC1, VLAN1, getLocation(1),
                                  Collections.singleton(IP1));
        Host h2 = new DefaultHost(PID, HID2, MAC2, VLAN1, getLocation(2),
                                  Collections.singleton(IP2));
        Host h3 = new DefaultHost(PID, HID3, MAC3, VLAN1, getLocation(3),
                                  Collections.EMPTY_SET);
        Host h4 = new DefaultHost(PID, HID4, MAC4, VLAN2, getLocation(4),
                                  Collections.EMPTY_SET);
        Host h5 = new DefaultHost(PID, HID5, MAC5, VLAN2, getLocation(5),
                                  Collections.EMPTY_SET);
        Host h6 = new DefaultHost(PID, HID6, MAC6, VLAN2, getLocation(6),
                                  Collections.EMPTY_SET);
        hostsAvailable.addAll(Sets.newHashSet(h1, h2, h3, h4, h5, h6));

        vpls.activate();

        List<Intent> expectedIntents = Lists.newArrayList();
        expectedIntents.addAll(generateVlanOneBrc());
        expectedIntents.addAll(generateVlanOneUni());
        expectedIntents.addAll(generateVlanTwoBrc());
        expectedIntents.addAll(generateVlanTwoUni());

        checkIntents(expectedIntents);
    }

    /**
     * Checks the case in which six ports are configured with VLANs and
     * initially no hosts are registered by the HostService. The first three
     * ports have an interface configured on VLAN1, the other three have an
     * interface configured on VLAN2. When the module starts up, three hosts -
     * on device one, two and three - port 1 (both on VLAN1), are registered by
     * the HostService and events are sent to the application. sp2mp intents
     * are created for all interfaces configured and mp2sp intents are created
     * only for the hosts attached.
     * The number of intents expected is nine: six for VLAN1, three for VLAN2.
     * Six sp2mp intents, three mp2sp intents. IPs are added on the first two
     * hosts only to demonstrate it doesn't influence the number of intents
     * created.
     * An additional host is added on device seven, port one to demonstrate
     * that, even if it's on the same VLAN of other interfaces configured in
     * the system, it doesn't let the application generate intents, since it's
     * not connected to the interface configured.
     */
    @Test
    public void testFourInterfacesThreeHostEventsSameVlan() {
        vpls.activate();

        Host h1 = new DefaultHost(PID, HID1, MAC1, VLAN1, getLocation(1),
                                  Collections.singleton(IP1));
        Host h2 = new DefaultHost(PID, HID2, MAC2, VLAN1, getLocation(2),
                                  Collections.singleton(IP2));
        Host h3 = new DefaultHost(PID, HID3, MAC3, VLAN1, getLocation(3),
                                  Collections.EMPTY_SET);
        Host h7 = new DefaultHost(PID, HID7, MAC7, VLAN1, getLocation(7),
                                  Collections.EMPTY_SET);
        hostsAvailable.addAll(Sets.newHashSet(h1, h2, h3, h7));

        hostsAvailable.forEach(host ->
            hostListener.event(new HostEvent(HostEvent.Type.HOST_ADDED, host)));

        List<Intent> expectedIntents = Lists.newArrayList();
        expectedIntents.addAll(generateVlanOneBrc());
        expectedIntents.addAll(generateVlanOneUni());
        expectedIntents.addAll(generateVlanTwoBrc());

        checkIntents(expectedIntents);
    }

    /**
     * Checks the case in which six ports are configured with VLANs and
     * initially no hosts are registered by the HostService. The first three
     * ports have an interface configured on VLAN1, the other three have an
     * interface configured on VLAN2. When the module starts up, two hosts -
     * on device one and four - port 1 (VLAN 1 and VLAN 2), are registered by
     * the HostService and events are sent to the application. sp2mp intents
     * are created for all interfaces configured and no mp2sp intents are created
     * at all, since the minimum number of hosts needed on the same vlan to
     * create mp2sp intents is 2.
     * The number of intents expected is six: three for VLAN1, three for VLAN2.
     * six sp2mp intents, zero mp2sp intents. IPs are added on the first host
     * only to demonstrate it doesn't influence the number of intents created.
     */
    @Test
    public void testFourInterfacesTwoHostEventsDifferentVlan() {
        vpls.activate();

        Host h1 = new DefaultHost(PID, HID1, MAC1, VLAN1, getLocation(1),
                                  Collections.singleton(IP1));
        Host h4 = new DefaultHost(PID, HID4, MAC4, VLAN2, getLocation(4),
                                  Collections.EMPTY_SET);
        hostsAvailable.addAll(Sets.newHashSet(h1, h4));

        hostsAvailable.forEach(host -> {
            hostListener.event(new HostEvent(HostEvent.Type.HOST_ADDED, host));
        });

        List<Intent> expectedIntents = Lists.newArrayList();
        expectedIntents.addAll(generateVlanOneBrc());
        expectedIntents.addAll(generateVlanTwoBrc());

        checkIntents(expectedIntents);
    }

    /**
     * Checks both that the number of intents in submitted in the intent
     * framework it's equal to the number of intents expected and that all
     * intents are equivalent.
     *
     * @param intents the list of intents expected
     */
    private void checkIntents(List<Intent> intents) {
        assertEquals(intents.size(), intentService.getIntentCount());

        for (Intent intentOne : intents) {
            boolean found = false;
            for (Intent intentTwo : intentService.getIntents()) {
                if (intentOne.key().equals(intentTwo.key())) {
                    found = true;
                    assertTrue(format("Comparing %s and %s", intentOne, intentTwo),
                               IntentUtils.intentsAreEqual(intentOne, intentTwo));
                    break;
                }
            }
            assertTrue(found);
        }
    }

    /**
     * Generates the list of the expected sp2mp intents for VLAN 1.
     *
     * @return the list of expected sp2mp intents for VLAN 1
     */
    private List<SinglePointToMultiPointIntent> generateVlanOneBrc() {
        Key key = null;

        List<SinglePointToMultiPointIntent> intents = Lists.newArrayList();

        // Building sp2mp intent for H1 - VLAN1
        key = Key.of((PREFIX_BROADCAST + "-" + DID1 + "-" + P1 + "-" + VLAN1),
                     APPID);
        intents.add(buildBrcIntent(key, C1, Sets.newHashSet(C2, C3), VLAN1));

        // Building sp2mp intent for H2 - VLAN1
        key = Key.of((PREFIX_BROADCAST + "-" + DID2 + "-" + P1 + "-" + VLAN1),
                     APPID);
        intents.add(buildBrcIntent(key, C2, Sets.newHashSet(C1, C3), VLAN1));

        // Building sp2mp intent for H3 - VLAN1
        key = Key.of((PREFIX_BROADCAST + "-" + DID3 + "-" + P1 + "-" + VLAN1),
                     APPID);
        intents.add(buildBrcIntent(key, C3, Sets.newHashSet(C1, C2), VLAN1));

        return intents;
    }

    /**
     * Generates the list of the expected mp2sp intents for VLAN 1.
     *
     * @return the list of expected mp2sp intents for VLAN 1
     */
    private List<MultiPointToSinglePointIntent> generateVlanOneUni() {
        Key key = null;

        List<MultiPointToSinglePointIntent> intents = Lists.newArrayList();

        // Building mp2sp intent for H1 - VLAN1
        key = Key.of((PREFIX_UNICAST + "-" + DID1 + "-" + P1 + "-" + VLAN1),
                     APPID);
        intents.add(buildUniIntent(key, Sets.newHashSet(C2, C3), C1, VLAN1, MAC1));

        // Building mp2sp intent for H2 - VLAN1
        key = Key.of((PREFIX_UNICAST + "-" + DID2 + "-" + P1 + "-" + VLAN1),
                     APPID);
        intents.add(buildUniIntent(key, Sets.newHashSet(C1, C3), C2, VLAN1, MAC2));

        // Building mp2sp intent for H3 - VLAN1
        key = Key.of((PREFIX_UNICAST + "-" + DID3 + "-" + P1 + "-" + VLAN1),
                     APPID);
        intents.add(buildUniIntent(key, Sets.newHashSet(C1, C2), C3, VLAN1, MAC3));

        return intents;
    }

    /**
     * Generates the list of the expected sp2mp intents for VLAN 2.
     *
     * @return the list of expected sp2mp intents for VLAN 2
     */
    private List<SinglePointToMultiPointIntent> generateVlanTwoBrc() {
        Key key = null;

        List<SinglePointToMultiPointIntent> intents = Lists.newArrayList();

        // Building sp2mp intent for H4 - VLAN2
        key = Key.of((PREFIX_BROADCAST + "-" + DID4 + "-" + P1 + "-" + VLAN2),
                     APPID);
        intents.add(buildBrcIntent(key, C4, Sets.newHashSet(C5, C6), VLAN2));

        // Building sp2mp intent for H5 - VLAN2
        key = Key.of((PREFIX_BROADCAST + "-" + DID5 + "-" + P1 + "-" + VLAN2),
                     APPID);
        intents.add(buildBrcIntent(key, C5, Sets.newHashSet(C4, C6), VLAN2));

        // Building sp2mp intent for H6 - VLAN2
        key = Key.of((PREFIX_BROADCAST + "-" + DID6 + "-" + P1 + "-" + VLAN2),
                     APPID);
        intents.add(buildBrcIntent(key, C6, Sets.newHashSet(C4, C5), VLAN2));

        return intents;
    }

    /**
     * Generates the list of the expected mp2sp intents for VLAN 2.
     *
     * @return the list of expected mp2sp intents for VLAN 2
     */
    private List<MultiPointToSinglePointIntent> generateVlanTwoUni() {
        Key key = null;

        List<MultiPointToSinglePointIntent> intents = Lists.newArrayList();

        // Building mp2sp intent for H4 - VLAN2
        key = Key.of((PREFIX_UNICAST + "-" + DID4 + "-" + P1 + "-" + VLAN2),
                     APPID);
        intents.add(buildUniIntent(key, Sets.newHashSet(C5, C6), C4, VLAN2, MAC4));

        // Building mp2sp intent for H5 - VLAN2
        key = Key.of((PREFIX_UNICAST + "-" + DID5 + "-" + P1 + "-" + VLAN2),
                     APPID);
        intents.add(buildUniIntent(key, Sets.newHashSet(C4, C6), C5, VLAN2, MAC5));

        // Building mp2sp intent for H6 - VLAN2
        key = Key.of((PREFIX_UNICAST + "-" + DID6 + "-" + P1 + "-" + VLAN2),
                     APPID);
        intents.add(buildUniIntent(key, Sets.newHashSet(C4, C5), C6, VLAN2, MAC6));

        return intents;
    }

    /**
     * Builds a Single Point to Multi Point intent.
     *
     * @param key  The intent key
     * @param src  The source Connect Point
     * @param dsts The destination Connect Points
     * @return Single Point to Multi Point intent generated.
     */
    private SinglePointToMultiPointIntent buildBrcIntent(Key key,
                                                         ConnectPoint src,
                                                         Set<ConnectPoint> dsts,
                                                         VlanId vlanId) {
        SinglePointToMultiPointIntent intent;

        TrafficTreatment treatment = DefaultTrafficTreatment.emptyTreatment();

        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthDst(MacAddress.BROADCAST)
                .matchVlanId(vlanId)
                .build();

        intent = SinglePointToMultiPointIntent.builder()
                .appId(APPID)
                .key(key)
                .selector(selector)
                .treatment(treatment)
                .ingressPoint(src)
                .egressPoints(dsts)
                .priority(PRIORITY_OFFSET)
                .build();
        return intent;
    }

    /**
     * Builds a Multi Point to Single Point intent.
     *
     * @param key  The intent key
     * @param srcs The source Connect Points
     * @param dst  The destination Connect Point
     * @return Multi Point to Single Point intent generated.
     */
    private MultiPointToSinglePointIntent buildUniIntent(Key key,
                                                         Set<ConnectPoint> srcs,
                                                         ConnectPoint dst,
                                                         VlanId vlanId,
                                                         MacAddress mac) {
        MultiPointToSinglePointIntent intent;

        TrafficTreatment treatment = DefaultTrafficTreatment.emptyTreatment();

        TrafficSelector.Builder builder = DefaultTrafficSelector.builder()
                .matchEthDst(mac)
                .matchVlanId(vlanId);

        TrafficSelector selector = builder.build();

        intent = MultiPointToSinglePointIntent.builder()
                .appId(APPID)
                .key(key)
                .selector(selector)
                .treatment(treatment)
                .ingressPoints(srcs)
                .egressPoint(dst)
                .priority(PRIORITY_OFFSET)
                .build();
        return intent;
    }

    /**
     * Returns the device ID of the ith device.
     *
     * @param i device to get the ID of
     * @return the device ID
     */
    private static DeviceId getDeviceId(int i) {
        return DeviceId.deviceId("" + i);
    }

    private static HostLocation getLocation(int i) {
        return new HostLocation(new ConnectPoint(getDeviceId(i), P1), 123L);
    }

    /**
     * Represents a fake IntentService class that easily allows to store and
     * retrieve intents without implementing the IntentService logic.
     */
    private class TestIntentService extends IntentServiceAdapter {

        private Set<Intent> intents;

        public TestIntentService() {
            intents = Sets.newHashSet();
        }

        @Override
        public void submit(Intent intent) {
            intents.add(intent);
        }

        @Override
        public long getIntentCount() {
            return intents.size();
        }

        @Override
        public Iterable<Intent> getIntents() {
            return intents;
        }

        @Override
        public Intent getIntent(Key intentKey) {
            for (Intent intent : intents) {
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
                    .filter(h -> h.location().elementId().equals(connectPoint.elementId())
                              && h.location().port().equals(connectPoint.port()))
                    .collect(Collectors.toSet());
        }

    }

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
         * Creates a new test intent synchronizer.
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

}
