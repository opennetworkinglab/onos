/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.sdnip;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onlab.junit.TestUtils.TestUtilsException;
import org.onlab.packet.Ethernet;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.TestApplicationId;
import org.onosproject.core.ApplicationId;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.InterfaceIpAddress;
import org.onosproject.net.intent.AbstractIntentTest;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.MultiPointToSinglePointIntent;
import org.onosproject.routing.RouteEntry;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

/**
 * This class tests the intent synchronization function in the
 * IntentSynchronizer class.
 */
public class IntentSyncTest extends AbstractIntentTest {

    private IntentService intentService;

    private static final ConnectPoint SW1_ETH1 = new ConnectPoint(
            DeviceId.deviceId("of:0000000000000001"),
            PortNumber.portNumber(1));

    private static final ConnectPoint SW2_ETH1 = new ConnectPoint(
            DeviceId.deviceId("of:0000000000000002"),
            PortNumber.portNumber(1));

    private static final ConnectPoint SW3_ETH1 = new ConnectPoint(
            DeviceId.deviceId("of:0000000000000003"),
            PortNumber.portNumber(1));

    private static final ConnectPoint SW4_ETH1 = new ConnectPoint(
            DeviceId.deviceId("of:0000000000000004"),
            PortNumber.portNumber(1));

    private IntentSynchronizer intentSynchronizer;
    private final Set<Interface> interfaces = Sets.newHashSet();

    private static final ApplicationId APPID = TestApplicationId.create("SDNIP");

    @Before
    public void setUp() throws Exception {
        super.setUp();

        setUpInterfaceService();

        intentService = createMock(IntentService.class);

        intentSynchronizer = new IntentSynchronizer(APPID, intentService,
                MoreExecutors.newDirectExecutorService());
    }

    /**
     * Sets up InterfaceService.
     */
    private void setUpInterfaceService() {
        Set<InterfaceIpAddress> interfaceIpAddresses1 = Sets.newHashSet();
        interfaceIpAddresses1.add(new InterfaceIpAddress(
                IpAddress.valueOf("192.168.10.101"),
                IpPrefix.valueOf("192.168.10.0/24")));
        Interface sw1Eth1 = new Interface(SW1_ETH1,
                interfaceIpAddresses1, MacAddress.valueOf("00:00:00:00:00:01"),
                VlanId.NONE);
        interfaces.add(sw1Eth1);

        Set<InterfaceIpAddress> interfaceIpAddresses2 = Sets.newHashSet();
        interfaceIpAddresses2.add(
                new InterfaceIpAddress(IpAddress.valueOf("192.168.20.101"),
                                       IpPrefix.valueOf("192.168.20.0/24")));
        Interface sw2Eth1 = new Interface(SW2_ETH1,
                interfaceIpAddresses2, MacAddress.valueOf("00:00:00:00:00:02"),
                VlanId.NONE);
        interfaces.add(sw2Eth1);

        Set<InterfaceIpAddress> interfaceIpAddresses3 = Sets.newHashSet();
        interfaceIpAddresses3.add(
                new InterfaceIpAddress(IpAddress.valueOf("192.168.30.101"),
                                       IpPrefix.valueOf("192.168.30.0/24")));
        Interface sw3Eth1 = new Interface(SW3_ETH1,
                interfaceIpAddresses3, MacAddress.valueOf("00:00:00:00:00:03"),
                VlanId.NONE);
        interfaces.add(sw3Eth1);

        InterfaceIpAddress interfaceIpAddress4 =
                new InterfaceIpAddress(IpAddress.valueOf("192.168.40.101"),
                                       IpPrefix.valueOf("192.168.40.0/24"));
        Interface sw4Eth1 = new Interface(SW4_ETH1,
                                          Sets.newHashSet(interfaceIpAddress4),
                                          MacAddress.valueOf("00:00:00:00:00:04"),
                                          VlanId.vlanId((short) 1));

        interfaces.add(sw4Eth1);
    }

    /**
     * Tests the synchronization behavior of intent synchronizer. We set up
     * a discrepancy between the intent service state and the intent
     * synchronizer's state and ensure that this is reconciled correctly.
     *
     * @throws TestUtilsException
     */
    @Test
    public void testIntentSync() throws TestUtilsException {

        //
        // Construct routes and intents.
        // This test simulates the following cases during the master change
        // time interval:
        // 1. RouteEntry1 did not change and the intent also did not change.
        // 2. RouteEntry2 was deleted, but the intent was not deleted.
        // 3. RouteEntry3 was newly added, and the intent was also submitted.
        // 4. RouteEntry4 was updated to RouteEntry4Update, and the intent was
        // also updated to a new one.
        // 5. RouteEntry5 did not change, but its intent id changed.
        // 6. RouteEntry6 was newly added, but the intent was not submitted.
        //
        RouteEntry routeEntry1 = new RouteEntry(
                Ip4Prefix.valueOf("1.1.1.0/24"),
                Ip4Address.valueOf("192.168.10.1"));

        RouteEntry routeEntry2 = new RouteEntry(
                Ip4Prefix.valueOf("2.2.2.0/24"),
                Ip4Address.valueOf("192.168.20.1"));

        RouteEntry routeEntry3 = new RouteEntry(
                Ip4Prefix.valueOf("3.3.3.0/24"),
                Ip4Address.valueOf("192.168.30.1"));

        RouteEntry routeEntry4 = new RouteEntry(
                Ip4Prefix.valueOf("4.4.4.0/24"),
                Ip4Address.valueOf("192.168.30.1"));

        RouteEntry routeEntry4Update = new RouteEntry(
                Ip4Prefix.valueOf("4.4.4.0/24"),
                Ip4Address.valueOf("192.168.20.1"));

        RouteEntry routeEntry5 = new RouteEntry(
                Ip4Prefix.valueOf("5.5.5.0/24"),
                Ip4Address.valueOf("192.168.10.1"));

        RouteEntry routeEntry6 = new RouteEntry(
                Ip4Prefix.valueOf("6.6.6.0/24"),
                Ip4Address.valueOf("192.168.10.1"));

        RouteEntry routeEntry7 = new RouteEntry(
                Ip4Prefix.valueOf("7.7.7.0/24"),
                Ip4Address.valueOf("192.168.10.1"));

        MultiPointToSinglePointIntent intent1 = intentBuilder(
                routeEntry1.prefix(), "00:00:00:00:00:01", SW1_ETH1);
        MultiPointToSinglePointIntent intent2 = intentBuilder(
                routeEntry2.prefix(), "00:00:00:00:00:02", SW2_ETH1);
        MultiPointToSinglePointIntent intent3 = intentBuilder(
                routeEntry3.prefix(), "00:00:00:00:00:03", SW3_ETH1);
        MultiPointToSinglePointIntent intent4 = intentBuilder(
                routeEntry4.prefix(), "00:00:00:00:00:03", SW3_ETH1);
        MultiPointToSinglePointIntent intent4Update = intentBuilder(
                routeEntry4Update.prefix(), "00:00:00:00:00:02", SW2_ETH1);
        MultiPointToSinglePointIntent intent5 = intentBuilder(
                routeEntry5.prefix(), "00:00:00:00:00:01",  SW1_ETH1);
        MultiPointToSinglePointIntent intent7 = intentBuilder(
                routeEntry7.prefix(), "00:00:00:00:00:01",  SW1_ETH1);

        // Compose a intent, which is equal to intent5 but the id is different.
        MultiPointToSinglePointIntent intent5New =
                staticIntentBuilder(intent5, routeEntry5, "00:00:00:00:00:01");
        assertThat(IntentUtils.equals(intent5, intent5New), is(true));
        assertFalse(intent5.equals(intent5New));

        MultiPointToSinglePointIntent intent6 = intentBuilder(
                routeEntry6.prefix(), "00:00:00:00:00:01",  SW1_ETH1);

        // Set up expectation
        Set<Intent> intents = new HashSet<>();
        intents.add(intent1);
        expect(intentService.getIntentState(intent1.key()))
                .andReturn(IntentState.INSTALLED).anyTimes();
        intents.add(intent2);
        expect(intentService.getIntentState(intent2.key()))
                .andReturn(IntentState.INSTALLED).anyTimes();
        intents.add(intent4);
        expect(intentService.getIntentState(intent4.key()))
                .andReturn(IntentState.INSTALLED).anyTimes();
        intents.add(intent5);
        expect(intentService.getIntentState(intent5.key()))
                .andReturn(IntentState.INSTALLED).anyTimes();
        intents.add(intent7);
        expect(intentService.getIntentState(intent7.key()))
                .andReturn(IntentState.WITHDRAWING).anyTimes();
        expect(intentService.getIntents()).andReturn(intents).anyTimes();

        // These are the operations that should be done to the intentService
        // during synchronization
        intentService.withdraw(intent2);
        intentService.submit(intent3);
        intentService.submit(intent4Update);
        intentService.submit(intent6);
        intentService.submit(intent7);
        replay(intentService);

        // Start the test

        // Simulate some input from the clients. The intent synchronizer has not
        // gained the global leadership yet, but it will remember this input for
        // when it does.
        intentSynchronizer.submit(intent1);
        intentSynchronizer.submit(intent2);
        intentSynchronizer.withdraw(intent2);
        intentSynchronizer.submit(intent3);
        intentSynchronizer.submit(intent4);
        intentSynchronizer.submit(intent4Update);
        intentSynchronizer.submit(intent5);
        intentSynchronizer.submit(intent6);
        intentSynchronizer.submit(intent7);

        // Give the leadership to the intent synchronizer. It will now attempt
        // to synchronize the intents in the store with the intents it has
        // recorded based on the earlier user input.
        intentSynchronizer.leaderChanged(true);

        verify(intentService);
    }

    /**
     * Tests the behavior of the submit API, both when the synchronizer has
     * leadership and when it does not.
     */
    @Test
    public void testSubmit() {
        IpPrefix prefix = Ip4Prefix.valueOf("1.1.1.0/24");
        Intent intent = intentBuilder(prefix, "00:00:00:00:00:01", SW1_ETH1);

        // Set up expectations
        intentService.submit(intent);
        expect(intentService.getIntents()).andReturn(Collections.emptyList())
                .anyTimes();
        replay(intentService);

        // Give the intent synchronizer leadership so it will submit intents
        // to the intent service
        intentSynchronizer.leaderChanged(true);

        // Test the submit
        intentSynchronizer.submit(intent);

        verify(intentService);

        // Now we'll remove leadership from the intent synchronizer and verify
        // that it does not submit any intents to the intent service when we
        // call the submit API
        reset(intentService);
        replay(intentService);

        intentSynchronizer.leaderChanged(false);

        intentSynchronizer.submit(intent);

        verify(intentService);
    }

    /**
     * Tests the behavior of the withdraw API, both when the synchronizer has
     * leadership and when it does not.
     */
    @Test
    public void testWithdraw() {
        IpPrefix prefix = Ip4Prefix.valueOf("1.1.1.0/24");
        Intent intent = intentBuilder(prefix, "00:00:00:00:00:01", SW1_ETH1);

        // Submit an intent first so we can withdraw it later
        intentService.submit(intent);
        intentService.withdraw(intent);
        expect(intentService.getIntents()).andReturn(Collections.emptyList())
                .anyTimes();
        replay(intentService);

        // Give the intent synchronizer leadership so it will submit intents
        // to the intent service
        intentSynchronizer.leaderChanged(true);

        // Test the submit then withdraw
        intentSynchronizer.submit(intent);
        intentSynchronizer.withdraw(intent);

        verify(intentService);

        // Now we'll remove leadership from the intent synchronizer and verify
        // that it does not withdraw any intents to the intent service when we
        // call the withdraw API
        reset(intentService);
        replay(intentService);

        intentSynchronizer.leaderChanged(false);

        intentSynchronizer.submit(intent);
        intentSynchronizer.withdraw(intent);

        verify(intentService);
    }

    /**
     * MultiPointToSinglePointIntent builder.
     *
     * @param ipPrefix the ipPrefix to match
     * @param nextHopMacAddress to which the destination MAC address in packet
     * should be rewritten
     * @param egressPoint to which packets should be sent
     * @return the constructed MultiPointToSinglePointIntent
     */
    private MultiPointToSinglePointIntent intentBuilder(IpPrefix ipPrefix,
            String nextHopMacAddress, ConnectPoint egressPoint) {

        TrafficSelector.Builder selectorBuilder =
                DefaultTrafficSelector.builder();
        if (ipPrefix.isIp4()) {
            selectorBuilder.matchEthType(Ethernet.TYPE_IPV4);
            selectorBuilder.matchIPDst(ipPrefix);
        } else {
            selectorBuilder.matchEthType(Ethernet.TYPE_IPV6);
            selectorBuilder.matchIPv6Dst(ipPrefix);
        }

        TrafficTreatment.Builder treatmentBuilder =
                DefaultTrafficTreatment.builder();
        treatmentBuilder.setEthDst(MacAddress.valueOf(nextHopMacAddress));

        Set<ConnectPoint> ingressPoints = new HashSet<>();
        for (Interface intf : interfaces) {
            if (!intf.connectPoint().equals(egressPoint)) {
                ConnectPoint srcPort = intf.connectPoint();
                ingressPoints.add(srcPort);
            }
        }
        MultiPointToSinglePointIntent intent =
                MultiPointToSinglePointIntent.builder()
                        .appId(APPID)
                        .key(Key.of(ipPrefix.toString(), APPID))
                        .selector(selectorBuilder.build())
                        .treatment(treatmentBuilder.build())
                        .ingressPoints(ingressPoints)
                        .egressPoint(egressPoint)
                        .constraints(SdnIpFib.CONSTRAINTS)
                        .build();
        return intent;
    }

    /**
     * A static MultiPointToSinglePointIntent builder, the returned intent is
     * equal to the input intent except that the id is different.
     *
     * @param intent the intent to be used for building a new intent
     * @param routeEntry the relative routeEntry of the intent
     * @return the newly constructed MultiPointToSinglePointIntent
     * @throws TestUtilsException
     */
    private MultiPointToSinglePointIntent staticIntentBuilder(
            MultiPointToSinglePointIntent intent, RouteEntry routeEntry,
            String nextHopMacAddress) throws TestUtilsException {

        // Use a different egress ConnectPoint with that in intent
        // to generate a different id
        MultiPointToSinglePointIntent intentNew = intentBuilder(
                routeEntry.prefix(), nextHopMacAddress, SW2_ETH1);
        TestUtils.setField(intentNew, "egressPoint", intent.egressPoint());
        TestUtils.setField(intentNew,
                "ingressPoints", intent.ingressPoints());
        return intentNew;
    }
}
