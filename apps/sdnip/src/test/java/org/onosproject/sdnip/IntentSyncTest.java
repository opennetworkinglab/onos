/*
 * Copyright 2014 Open Networking Laboratory
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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onlab.junit.TestUtils.TestUtilsException;
import org.onosproject.core.ApplicationId;
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
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.net.host.InterfaceIpAddress;
import org.onosproject.net.intent.AbstractIntentTest;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentOperations;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.MultiPointToSinglePointIntent;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.sdnip.config.Interface;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;

import com.google.common.collect.Sets;
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultByteArrayNodeFactory;
import com.googlecode.concurrenttrees.radixinverted.ConcurrentInvertedRadixTree;
import com.googlecode.concurrenttrees.radixinverted.InvertedRadixTree;

/**
 * This class tests the intent synchronization function in the
 * IntentSynchronizer class.
 */
public class IntentSyncTest extends AbstractIntentTest {

    private InterfaceService interfaceService;
    private IntentService intentService;
    private HostService hostService;

    private static final ConnectPoint SW1_ETH1 = new ConnectPoint(
            DeviceId.deviceId("of:0000000000000001"),
            PortNumber.portNumber(1));

    private static final ConnectPoint SW2_ETH1 = new ConnectPoint(
            DeviceId.deviceId("of:0000000000000002"),
            PortNumber.portNumber(1));

    private static final ConnectPoint SW3_ETH1 = new ConnectPoint(
            DeviceId.deviceId("of:0000000000000003"),
            PortNumber.portNumber(1));

    private IntentSynchronizer intentSynchronizer;
    private Router router;

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

    @Before
    public void setUp() throws Exception {
        super.setUp();
        setUpInterfaceService();
        setUpHostService();
        intentService = createMock(IntentService.class);

        intentSynchronizer = new IntentSynchronizer(APPID, intentService);
        router = new Router(APPID, intentSynchronizer, null, interfaceService,
                            hostService);
    }

    /**
     * Sets up InterfaceService.
     */
    private void setUpInterfaceService() {

        interfaceService = createMock(InterfaceService.class);

        Set<Interface> interfaces = Sets.newHashSet();

        Set<InterfaceIpAddress> interfaceIpAddresses1 = Sets.newHashSet();
        interfaceIpAddresses1.add(new InterfaceIpAddress(
                IpAddress.valueOf("192.168.10.101"),
                IpPrefix.valueOf("192.168.10.0/24")));
        Interface sw1Eth1 = new Interface(SW1_ETH1,
                interfaceIpAddresses1, MacAddress.valueOf("00:00:00:00:00:01"));
        interfaces.add(sw1Eth1);

        Set<InterfaceIpAddress> interfaceIpAddresses2 = Sets.newHashSet();
        interfaceIpAddresses2.add(new InterfaceIpAddress(
                IpAddress.valueOf("192.168.20.101"),
                IpPrefix.valueOf("192.168.20.0/24")));
        Interface sw2Eth1 = new Interface(SW2_ETH1,
                interfaceIpAddresses2, MacAddress.valueOf("00:00:00:00:00:02"));
        interfaces.add(sw2Eth1);

        Set<InterfaceIpAddress> interfaceIpAddresses3 = Sets.newHashSet();
        interfaceIpAddresses3.add(new InterfaceIpAddress(
                IpAddress.valueOf("192.168.30.101"),
                IpPrefix.valueOf("192.168.30.0/24")));
        Interface sw3Eth1 = new Interface(SW3_ETH1,
                interfaceIpAddresses3, MacAddress.valueOf("00:00:00:00:00:03"));
        interfaces.add(sw3Eth1);

        expect(interfaceService.getInterface(SW1_ETH1)).andReturn(
                sw1Eth1).anyTimes();
        expect(interfaceService.getInterface(SW2_ETH1)).andReturn(
                sw2Eth1).anyTimes();
        expect(interfaceService.getInterface(SW3_ETH1)).andReturn(
                sw3Eth1).anyTimes();
        expect(interfaceService.getInterfaces()).andReturn(
                interfaces).anyTimes();
        replay(interfaceService);
    }

    /**
     * Sets up the host service with details of hosts.
     */
    private void setUpHostService() {
        hostService = createMock(HostService.class);

        hostService.addListener(anyObject(HostListener.class));
        expectLastCall().anyTimes();

        IpAddress host1Address = IpAddress.valueOf("192.168.10.1");
        Host host1 = new DefaultHost(ProviderId.NONE, HostId.NONE,
                MacAddress.valueOf("00:00:00:00:00:01"), VlanId.NONE,
                new HostLocation(SW1_ETH1, 1),
                        Sets.newHashSet(host1Address));

        expect(hostService.getHostsByIp(host1Address))
                .andReturn(Sets.newHashSet(host1)).anyTimes();
        hostService.startMonitoringIp(host1Address);
        expectLastCall().anyTimes();


        IpAddress host2Address = IpAddress.valueOf("192.168.20.1");
        Host host2 = new DefaultHost(ProviderId.NONE, HostId.NONE,
                MacAddress.valueOf("00:00:00:00:00:02"), VlanId.NONE,
                new HostLocation(SW2_ETH1, 1),
                        Sets.newHashSet(host2Address));

        expect(hostService.getHostsByIp(host2Address))
                .andReturn(Sets.newHashSet(host2)).anyTimes();
        hostService.startMonitoringIp(host2Address);
        expectLastCall().anyTimes();


        IpAddress host3Address = IpAddress.valueOf("192.168.30.1");
        Host host3 = new DefaultHost(ProviderId.NONE, HostId.NONE,
                MacAddress.valueOf("00:00:00:00:00:03"), VlanId.NONE,
                new HostLocation(SW3_ETH1, 1),
                        Sets.newHashSet(host3Address));

        expect(hostService.getHostsByIp(host3Address))
                .andReturn(Sets.newHashSet(host3)).anyTimes();
        hostService.startMonitoringIp(host3Address);
        expectLastCall().anyTimes();


        replay(hostService);
    }

    /**
     * This method tests the behavior of intent Synchronizer.
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
        assertThat(IntentSynchronizer.IntentKey.equalIntents(
                        intent5, intent5New),
                   is(true));
        assertFalse(intent5.equals(intent5New));

        MultiPointToSinglePointIntent intent6 = intentBuilder(
                routeEntry6.prefix(), "00:00:00:00:00:01",  SW1_ETH1);

        // Set up the ribTable field in Router class and routeIntents fields
        // in IntentSynchronizer class
        InvertedRadixTree<RouteEntry> ribTable =
                new ConcurrentInvertedRadixTree<>(
                new DefaultByteArrayNodeFactory());
        ribTable.put(RouteEntry.createBinaryString(routeEntry1.prefix()),
                     routeEntry1);
        ribTable.put(RouteEntry.createBinaryString(routeEntry3.prefix()),
                     routeEntry3);
        ribTable.put(RouteEntry.createBinaryString(routeEntry4Update.prefix()),
                     routeEntry4Update);
        ribTable.put(RouteEntry.createBinaryString(routeEntry5.prefix()),
                     routeEntry5);
        ribTable.put(RouteEntry.createBinaryString(routeEntry6.prefix()),
                     routeEntry6);
        ribTable.put(RouteEntry.createBinaryString(routeEntry7.prefix()),
                     routeEntry7);
        TestUtils.setField(router, "ribTable4", ribTable);

        ConcurrentHashMap<IpPrefix, MultiPointToSinglePointIntent>
            routeIntents =  new ConcurrentHashMap<>();
        routeIntents.put(routeEntry1.prefix(), intent1);
        routeIntents.put(routeEntry3.prefix(), intent3);
        routeIntents.put(routeEntry4Update.prefix(), intent4Update);
        routeIntents.put(routeEntry5.prefix(), intent5New);
        routeIntents.put(routeEntry6.prefix(), intent6);
        routeIntents.put(routeEntry7.prefix(), intent7);
        TestUtils.setField(intentSynchronizer, "routeIntents", routeIntents);

        // Set up expectation
        reset(intentService);
        Set<Intent> intents = new HashSet<Intent>();
        intents.add(intent1);
        expect(intentService.getIntentState(intent1.id()))
                .andReturn(IntentState.INSTALLED).anyTimes();
        intents.add(intent2);
        expect(intentService.getIntentState(intent2.id()))
                .andReturn(IntentState.INSTALLED).anyTimes();
        intents.add(intent4);
        expect(intentService.getIntentState(intent4.id()))
                .andReturn(IntentState.INSTALLED).anyTimes();
        intents.add(intent5);
        expect(intentService.getIntentState(intent5.id()))
                .andReturn(IntentState.INSTALLED).anyTimes();
        intents.add(intent7);
        expect(intentService.getIntentState(intent7.id()))
                .andReturn(IntentState.WITHDRAWING).anyTimes();
        expect(intentService.getIntents()).andReturn(intents).anyTimes();

        IntentOperations.Builder builder = IntentOperations.builder(APPID);
        builder.addWithdrawOperation(intent2.id());
        builder.addWithdrawOperation(intent4.id());
        intentService.execute(TestIntentServiceHelper.eqExceptId(
                                builder.build()));

        builder = IntentOperations.builder(APPID);
        builder.addSubmitOperation(intent3);
        builder.addSubmitOperation(intent4Update);
        builder.addSubmitOperation(intent6);
        builder.addSubmitOperation(intent7);
        intentService.execute(TestIntentServiceHelper.eqExceptId(
                                builder.build()));
        replay(intentService);

        // Start the test
        intentSynchronizer.leaderChanged(true);
        /*
        TestUtils.callMethod(intentSynchronizer, "synchronizeIntents",
                             new Class<?>[] {});
        */
        intentSynchronizer.synchronizeIntents();

        // Verify
        assertEquals(router.getRoutes4().size(), 6);
        assertTrue(router.getRoutes4().contains(routeEntry1));
        assertTrue(router.getRoutes4().contains(routeEntry3));
        assertTrue(router.getRoutes4().contains(routeEntry4Update));
        assertTrue(router.getRoutes4().contains(routeEntry5));
        assertTrue(router.getRoutes4().contains(routeEntry6));

        assertEquals(intentSynchronizer.getRouteIntents().size(), 6);
        assertTrue(intentSynchronizer.getRouteIntents().contains(intent1));
        assertTrue(intentSynchronizer.getRouteIntents().contains(intent3));
        assertTrue(intentSynchronizer.getRouteIntents().contains(intent4Update));
        assertTrue(intentSynchronizer.getRouteIntents().contains(intent5));
        assertTrue(intentSynchronizer.getRouteIntents().contains(intent6));

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
        if (ipPrefix.version() == Ip4Address.VERSION) {
            selectorBuilder.matchEthType(Ethernet.TYPE_IPV4);   // IPv4
        } else {
            selectorBuilder.matchEthType(Ethernet.TYPE_IPV6);   // IPv6
        }
        selectorBuilder.matchIPDst(ipPrefix);

        TrafficTreatment.Builder treatmentBuilder =
                DefaultTrafficTreatment.builder();
        treatmentBuilder.setEthDst(MacAddress.valueOf(nextHopMacAddress));

        Set<ConnectPoint> ingressPoints = new HashSet<ConnectPoint>();
        for (Interface intf : interfaceService.getInterfaces()) {
            if (!intf.equals(interfaceService.getInterface(egressPoint))) {
                ConnectPoint srcPort = intf.connectPoint();
                ingressPoints.add(srcPort);
            }
        }
        MultiPointToSinglePointIntent intent =
                new MultiPointToSinglePointIntent(APPID,
                        selectorBuilder.build(), treatmentBuilder.build(),
                        ingressPoints, egressPoint);
        return intent;
    }

    /**
     * A static MultiPointToSinglePointIntent builder, the returned intent is
     * equal to the input intent except that the id is different.
     *
     *
     * @param intent the intent to be used for building a new intent
     * @param routeEntry the relative routeEntry of the intent
     * @return the newly constructed MultiPointToSinglePointIntent
     * @throws TestUtilsException
     */
    private  MultiPointToSinglePointIntent staticIntentBuilder(
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
