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

package org.onosproject.intentsync;

import com.google.common.util.concurrent.MoreExecutors;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ethernet;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onosproject.TestApplicationId;
import org.onosproject.cluster.ClusterServiceAdapter;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.DefaultControllerNode;
import org.onosproject.cluster.LeadershipServiceAdapter;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.AbstractIntentTest;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.MultiPointToSinglePointIntent;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * This class tests the intent synchronization function in the
 * IntentSynchronizer class.
 */
public class IntentSynchronizerTest extends AbstractIntentTest {

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
    private final Set<ConnectPoint> connectPoints = new HashSet<>();

    private static final ApplicationId APPID =
            TestApplicationId.create("intent-sync-test");

    private static final ControllerNode LOCAL_NODE =
            new DefaultControllerNode(new NodeId("foo"), IpAddress.valueOf("127.0.0.1"));

    @Before
    public void setUp() throws Exception {
        super.setUp();

        setUpConnectPoints();

        intentService = EasyMock.createMock(IntentService.class);

        intentSynchronizer = new TestIntentSynchronizer();

        intentSynchronizer.coreService = new TestCoreService();
        intentSynchronizer.clusterService = new TestClusterService();
        intentSynchronizer.leadershipService = new LeadershipServiceAdapter();
        intentSynchronizer.intentService = intentService;

        intentSynchronizer.activate();
    }

    /**
     * Sets up connect points.
     */
    private void setUpConnectPoints() {
        connectPoints.add(SW1_ETH1);
        connectPoints.add(SW2_ETH1);
        connectPoints.add(SW3_ETH1);
        connectPoints.add(SW4_ETH1);
    }

    /**
     * Tests the synchronization behavior of intent synchronizer. We set up
     * a discrepancy between the intent service state and the intent
     * synchronizer's state and ensure that this is reconciled correctly.
     */
    @Test
    public void testIntentSync() {

        // Construct routes and intents.
        // This test simulates the following cases during the master change
        // time interval:
        // 1. intent1 did not change and the intent also did not change.
        // 2. intent2 was deleted, but the intent was not deleted.
        // 3. intent3 was newly added, and the intent was also submitted.
        // 4. intent4 was updated to RouteEntry4Update, and the intent was
        // also updated to a new one.
        // 5. intent5 did not change, but its intent id changed.
        // 6. intent6 was newly added, but the intent was not submitted.

        MultiPointToSinglePointIntent intent1 = intentBuilder(
                Ip4Prefix.valueOf("1.1.1.0/24"), "00:00:00:00:00:01", SW1_ETH1);
        MultiPointToSinglePointIntent intent2 = intentBuilder(
                Ip4Prefix.valueOf("2.2.2.0/24"), "00:00:00:00:00:02", SW2_ETH1);
        MultiPointToSinglePointIntent intent3 = intentBuilder(
                Ip4Prefix.valueOf("3.3.3.0/24"), "00:00:00:00:00:03", SW3_ETH1);
        MultiPointToSinglePointIntent intent4 = intentBuilder(
                Ip4Prefix.valueOf("4.4.4.0/24"), "00:00:00:00:00:03", SW3_ETH1);
        MultiPointToSinglePointIntent intent4Update = intentBuilder(
                Ip4Prefix.valueOf("4.4.4.0/24"), "00:00:00:00:00:02", SW2_ETH1);
        MultiPointToSinglePointIntent intent5 = intentBuilder(
                Ip4Prefix.valueOf("5.5.5.0/24"), "00:00:00:00:00:01",  SW1_ETH1);
        MultiPointToSinglePointIntent intent7 = intentBuilder(
                Ip4Prefix.valueOf("7.7.7.0/24"), "00:00:00:00:00:01",  SW1_ETH1);

        MultiPointToSinglePointIntent intent6 = intentBuilder(
                Ip4Prefix.valueOf("6.6.6.0/24"), "00:00:00:00:00:01",  SW1_ETH1);

        // Set up expectation
        Set<Intent> intents = new HashSet<>();
        intents.add(intent1);
        EasyMock.expect(intentService.getIntentState(intent1.key()))
                .andReturn(IntentState.INSTALLED).anyTimes();
        intents.add(intent2);
        EasyMock.expect(intentService.getIntentState(intent2.key()))
                .andReturn(IntentState.INSTALLED).anyTimes();
        intents.add(intent4);
        EasyMock.expect(intentService.getIntentState(intent4.key()))
                .andReturn(IntentState.INSTALLED).anyTimes();
        intents.add(intent5);
        EasyMock.expect(intentService.getIntentState(intent5.key()))
                .andReturn(IntentState.INSTALLED).anyTimes();
        intents.add(intent7);
        EasyMock.expect(intentService.getIntentState(intent7.key()))
                .andReturn(IntentState.WITHDRAWING).anyTimes();
        EasyMock.expect(intentService.getIntents()).andReturn(intents).anyTimes();

        // These are the operations that should be done to the intentService
        // during synchronization
        intentService.withdraw(intent2);
        intentService.submit(intent3);
        intentService.submit(intent4Update);
        intentService.submit(intent6);
        intentService.submit(intent7);
        EasyMock.replay(intentService);

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
        intentSynchronizer.modifyPrimary(true);

        EasyMock.verify(intentService);
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
        EasyMock.expect(intentService.getIntents()).andReturn(Collections.emptyList())
                .anyTimes();
        EasyMock.replay(intentService);

        // Give the intent synchronizer leadership so it will submit intents
        // to the intent service
        intentSynchronizer.modifyPrimary(true);

        // Test the submit
        intentSynchronizer.submit(intent);

        EasyMock.verify(intentService);

        // Now we'll remove leadership from the intent synchronizer and verify
        // that it does not submit any intents to the intent service when we
        // call the submit API
        EasyMock.reset(intentService);
        EasyMock.replay(intentService);

        intentSynchronizer.modifyPrimary(false);

        intentSynchronizer.submit(intent);

        EasyMock.verify(intentService);
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
        EasyMock.expect(intentService.getIntents()).andReturn(Collections.emptyList())
                .anyTimes();
        EasyMock.replay(intentService);

        // Give the intent synchronizer leadership so it will submit intents
        // to the intent service
        intentSynchronizer.modifyPrimary(true);

        // Test the submit then withdraw
        intentSynchronizer.submit(intent);
        intentSynchronizer.withdraw(intent);

        EasyMock.verify(intentService);

        // Now we'll remove leadership from the intent synchronizer and verify
        // that it does not withdraw any intents to the intent service when we
        // call the withdraw API
        EasyMock.reset(intentService);
        EasyMock.replay(intentService);

        intentSynchronizer.modifyPrimary(false);

        intentSynchronizer.submit(intent);
        intentSynchronizer.withdraw(intent);

        EasyMock.verify(intentService);
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

        Set<ConnectPoint> ingressPoints = new HashSet<>(connectPoints);
        ingressPoints.remove(egressPoint);

        MultiPointToSinglePointIntent intent =
                MultiPointToSinglePointIntent.builder()
                        .appId(APPID)
                        .key(Key.of(ipPrefix.toString(), APPID))
                        .selector(selectorBuilder.build())
                        .treatment(treatmentBuilder.build())
                        .ingressPoints(ingressPoints)
                        .egressPoint(egressPoint)
                        .build();
        return intent;
    }

    private class TestIntentSynchronizer extends IntentSynchronizer {
        @Override
        protected ExecutorService createExecutor() {
            return MoreExecutors.newDirectExecutorService();
        }
    }

    private class TestCoreService extends CoreServiceAdapter {
        @Override
        public ApplicationId registerApplication(String name) {
            return APPID;
        }
    }

    private class TestClusterService extends ClusterServiceAdapter {
        @Override
        public ControllerNode getLocalNode() {
            return LOCAL_NODE;
        }
    }
}
