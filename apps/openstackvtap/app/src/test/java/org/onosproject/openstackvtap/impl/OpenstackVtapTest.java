/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.openstackvtap.impl;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.onlab.packet.IPv4;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.TpPort;
import org.onosproject.TestApplicationId;
import org.onosproject.cfg.ComponentConfigAdapter;
import org.onosproject.cluster.ClusterServiceAdapter;
import org.onosproject.cluster.LeadershipServiceAdapter;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.event.DefaultEventSinkRegistry;
import org.onosproject.event.Event;
import org.onosproject.event.EventDeliveryService;
import org.onosproject.event.EventSink;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceServiceAdapter;
import org.onosproject.net.flow.FlowRuleServiceAdapter;
import org.onosproject.net.group.GroupServiceAdapter;
import org.onosproject.net.host.HostServiceAdapter;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeListener;
import org.onosproject.openstacknode.api.OpenstackNodeService;
import org.onosproject.openstackvtap.api.OpenstackVtap;
import org.onosproject.openstackvtap.api.OpenstackVtapCriterion;
import org.onosproject.openstackvtap.api.OpenstackVtapEvent;
import org.onosproject.openstackvtap.api.OpenstackVtapId;
import org.onosproject.openstackvtap.api.OpenstackVtapListener;
import org.onosproject.openstackvtap.api.OpenstackVtapNetwork;

import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkState;

/**
 * Provides a set of test OpenstackVtapNetwork, OpenstackVtap parameters
 * for use with OpenstackVtapNetwork, OpenstackVtap related tests.
 */
public abstract class OpenstackVtapTest {

    protected static final String APP_NAME = "org.onosproject.openstackvtap";
    protected static final ApplicationId APP_ID = TestApplicationId.create(APP_NAME);

    protected static final String ERR_SIZE = "Number of vtap or vtap network did not match";
    protected static final String ERR_NOT_MATCH = "Vtap or vtap network did not match";
    protected static final String ERR_NOT_FOUND = "Vtap or vtap network did not exist";

    protected static final int VTAP_NETWORK_KEY = 0;

    protected static final OpenstackVtapNetwork.Mode VTAP_NETWORK_MODE_1 = OpenstackVtapNetwork.Mode.VXLAN;
    protected static final OpenstackVtapNetwork.Mode VTAP_NETWORK_MODE_2 = OpenstackVtapNetwork.Mode.GRE;

    protected static final int VTAP_NETWORK_NETWORK_ID_1 = 1;
    protected static final int VTAP_NETWORK_NETWORK_ID_2 = 2;

    protected static final IpAddress SERVER_IP_1 = IpAddress.valueOf("20.10.10.1");
    protected static final IpAddress SERVER_IP_2 = IpAddress.valueOf("20.10.20.1");

    protected static final OpenstackVtapNetwork VTAP_NETWORK_1 =
            createVtapNetwork(VTAP_NETWORK_MODE_1, VTAP_NETWORK_NETWORK_ID_1, SERVER_IP_1);
    protected static final OpenstackVtapNetwork VTAP_NETWORK_2 =
            createVtapNetwork(VTAP_NETWORK_MODE_2, VTAP_NETWORK_NETWORK_ID_2, SERVER_IP_2);

    protected static final IpPrefix SRC_IP_PREFIX_1 =
            IpPrefix.valueOf(IpAddress.valueOf("10.10.10.1"), 32);
    protected static final IpPrefix SRC_IP_PREFIX_2 =
            IpPrefix.valueOf(IpAddress.valueOf("10.10.20.1"), 32);
    protected static final IpPrefix DST_IP_PREFIX_1 =
            IpPrefix.valueOf(IpAddress.valueOf("10.10.30.2"), 32);
    protected static final IpPrefix DST_IP_PREFIX_2 =
            IpPrefix.valueOf(IpAddress.valueOf("10.10.40.2"), 32);

    protected static final TpPort SRC_PORT_1 = TpPort.tpPort(10);
    protected static final TpPort SRC_PORT_2 = TpPort.tpPort(20);
    protected static final TpPort DST_PORT_1 = TpPort.tpPort(30);
    protected static final TpPort DST_PORT_2 = TpPort.tpPort(40);

    protected static final byte IP_PROTOCOL_1 = IPv4.PROTOCOL_TCP;
    protected static final byte IP_PROTOCOL_2 = IPv4.PROTOCOL_UDP;

    protected static final OpenstackVtapCriterion VTAP_CRITERION_1 =
            createCriterion(SRC_IP_PREFIX_1, DST_IP_PREFIX_1, SRC_PORT_1, DST_PORT_1, IP_PROTOCOL_1);
    protected static final OpenstackVtapCriterion VTAP_CRITERION_2 =
            createCriterion(SRC_IP_PREFIX_2, DST_IP_PREFIX_2, SRC_PORT_2, DST_PORT_2, IP_PROTOCOL_2);

    protected static final OpenstackVtapId VTAP_ID_1 = OpenstackVtapId.vtapId();
    protected static final OpenstackVtapId VTAP_ID_2 = OpenstackVtapId.vtapId();

    protected static final OpenstackVtap.Type VTAP_TYPE_1 = OpenstackVtap.Type.VTAP_ALL;
    protected static final OpenstackVtap.Type VTAP_TYPE_2 = OpenstackVtap.Type.VTAP_TX;

    protected static final DeviceId DEVICE_ID_1 = DeviceId.deviceId("of:00000000000000a1");
    protected static final DeviceId DEVICE_ID_2 = DeviceId.deviceId("of:00000000000000a2");
    protected static final DeviceId DEVICE_ID_3 = DeviceId.deviceId("of:00000000000000a3");
    protected static final DeviceId DEVICE_ID_4 = DeviceId.deviceId("of:00000000000000a4");

    protected static final Set<DeviceId> TX_DEVICE_IDS_1 = ImmutableSet.of(DEVICE_ID_1);
    protected static final Set<DeviceId> RX_DEVICE_IDS_1 = ImmutableSet.of(DEVICE_ID_2);
    protected static final Set<DeviceId> TX_DEVICE_IDS_2 = ImmutableSet.of(DEVICE_ID_3);
    protected static final Set<DeviceId> RX_DEVICE_IDS_2 = ImmutableSet.of(DEVICE_ID_4);

    protected static final OpenstackVtap VTAP_1 =
            createVtap(VTAP_ID_1, VTAP_TYPE_1, VTAP_CRITERION_1, TX_DEVICE_IDS_1, RX_DEVICE_IDS_1);
    protected static final OpenstackVtap VTAP_2 =
            createVtap(VTAP_ID_2, VTAP_TYPE_2, VTAP_CRITERION_2, TX_DEVICE_IDS_2, RX_DEVICE_IDS_2);


    public static class TestOpenstackVtapListener implements OpenstackVtapListener {
        public List<OpenstackVtapEvent> events = Lists.newArrayList();

        @Override
        public void event(OpenstackVtapEvent event) {
            events.add(event);
        }
    }

    public static class TestCoreService extends CoreServiceAdapter {

        @Override
        public ApplicationId registerApplication(String name) {
            return APP_ID;
        }
    }

    public static class TestClusterService extends ClusterServiceAdapter {

    }

    public static class TestLeadershipService extends LeadershipServiceAdapter {

    }

    public static class TestFlowRuleService extends FlowRuleServiceAdapter {

    }

    public static class TestGroupService extends GroupServiceAdapter {

    }

    public static class TestDeviceService extends DeviceServiceAdapter {

    }

    public static class TestOpenstackNodeService implements OpenstackNodeService {

        @Override
        public Set<OpenstackNode> nodes() {
            return ImmutableSet.of();
        }

        @Override
        public Set<OpenstackNode> nodes(OpenstackNode.NodeType type) {
            return ImmutableSet.of();
        }

        @Override
        public Set<OpenstackNode> completeNodes() {
            return ImmutableSet.of();
        }

        @Override
        public Set<OpenstackNode> completeNodes(OpenstackNode.NodeType type) {
            return ImmutableSet.of();
        }

        @Override
        public OpenstackNode node(String hostname) {
            return null;
        }

        @Override
        public OpenstackNode node(DeviceId deviceId) {
            return null;
        }

        @Override
        public OpenstackNode node(IpAddress mgmtIp) {
            return null;
        }

        @Override
        public void addVfPort(OpenstackNode osNode, String portName) {
        }

        @Override
        public void removeVfPort(OpenstackNode osNode, String portName) {
        }


        @Override
        public void addListener(OpenstackNodeListener listener) {

        }

        @Override
        public void removeListener(OpenstackNodeListener listener) {

        }
    }

    public static class TestHostService extends HostServiceAdapter {

    }

    public static class TestComponentConfigService extends ComponentConfigAdapter {

    }

    /**
     * A mock of event dispatcher.
     */
    public class TestEventDispatcher extends DefaultEventSinkRegistry
            implements EventDeliveryService {
        @Override
        @SuppressWarnings("unchecked")
        public synchronized void post(Event event) {
            EventSink sink = getSink(event.getClass());
            checkState(sink != null, "No sink for event %s", event);
            sink.process(event);
        }

        @Override
        public void setDispatchTimeLimit(long millis) {
        }

        @Override
        public long getDispatchTimeLimit() {
            return 0;
        }
    }

    public static OpenstackVtapNetwork createVtapNetwork(OpenstackVtapNetwork.Mode mode,
                                                         Integer networkId,
                                                         IpAddress serverIp) {
        return DefaultOpenstackVtapNetwork.builder()
                .mode(mode)
                .networkId(networkId)
                .serverIp(serverIp)
                .build();
    }

    public static OpenstackVtap createVtap(OpenstackVtapId id,
                                           OpenstackVtap.Type type,
                                           OpenstackVtapCriterion vtapCriterion,
                                           Set<DeviceId> txDevices,
                                           Set<DeviceId> rxDevices) {
        return DefaultOpenstackVtap.builder()
                .id(id)
                .type(type)
                .vtapCriterion(vtapCriterion)
                .txDeviceIds(txDevices)
                .rxDeviceIds(rxDevices)
                .build();
    }

    public static OpenstackVtapCriterion createCriterion(IpPrefix srcIpPrefix, IpPrefix dstIpPrefix,
                                                         TpPort srcPort, TpPort dstPort,
                                                         byte ipProtocol) {
        return DefaultOpenstackVtapCriterion.builder()
                .srcIpPrefix(srcIpPrefix)
                .dstIpPrefix(dstIpPrefix)
                .srcTpPort(srcPort)
                .dstTpPort(dstPort)
                .ipProtocol(ipProtocol)
                .build();
    }

}
