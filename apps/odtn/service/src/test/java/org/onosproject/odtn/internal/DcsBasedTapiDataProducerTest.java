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

package org.onosproject.odtn.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.odtn.utils.tapi.DcsBasedTapiObjectRefFactory;
import org.onosproject.odtn.utils.tapi.TapiNepHandler;
import org.onosproject.odtn.utils.tapi.TapiNepRef;
import org.onosproject.odtn.utils.tapi.TapiNodeHandler;
import org.onosproject.odtn.utils.tapi.TapiNodeRef;
import org.onosproject.odtn.utils.tapi.TapiSipHandler;
import org.onosproject.odtn.utils.tapi.TapiTopologyContextHandler;
import org.onosproject.odtn.utils.tapi.TapiTopologyHandler;
import org.onosproject.yang.gen.v1.tapicommon.rev20181210.tapicommon.DefaultContext;
import org.onosproject.yang.gen.v1.tapicommon.rev20181210.tapicommon.tapicontext.DefaultServiceInterfacePoint;
import org.onosproject.yang.gen.v1.tapitopology.rev20181210.tapitopology.context.DefaultAugmentedTapiCommonContext;
import org.onosproject.yang.gen.v1.tapitopology.rev20181210.tapitopology.context.augmentedtapicommoncontext.DefaultTopologyContext;
import org.onosproject.yang.gen.v1.tapitopology.rev20181210.tapitopology.node.DefaultOwnedNodeEdgePoint;
import org.onosproject.yang.gen.v1.tapitopology.rev20181210.tapitopology.topology.DefaultNode;
import org.onosproject.yang.gen.v1.tapitopology.rev20181210.tapitopology.topologycontext.DefaultTopology;
import org.onosproject.yang.model.Augmentable;

import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

public class DcsBasedTapiDataProducerTest {

    private DefaultContext context;
    private DefaultTopology topology;
    private DefaultNode node1, node2;
    private DefaultServiceInterfacePoint sip11, sip21;
    private DefaultOwnedNodeEdgePoint nep11, nep12, nep21, nep22;

    private DeviceId did1, did2;
    private ConnectPoint cp11, cp12, cp21, cp22;

    @Before
    public void setUp() {
        makeTestData();
    }

    private void makeTestData() {

        did1 = DeviceId.deviceId("netconf:127.0.0.1:11001");
        did2 = DeviceId.deviceId("netconf:127.0.0.1:11002");

        cp11 = new ConnectPoint(did1, PortNumber.portNumber(1, "TRANSCEIVER"));
        cp12 = new ConnectPoint(did1, PortNumber.portNumber(2, "TRANSCEIVER"));
        cp21 = new ConnectPoint(did2, PortNumber.portNumber(1, "TRANSCEIVER"));
        cp22 = new ConnectPoint(did2, PortNumber.portNumber(2, "TRANSCEIVER"));

        context = new DefaultContext();

        topology = TapiTopologyHandler.create().getModelObject();
        DefaultTopologyContext topologyContext = TapiTopologyContextHandler.create().getModelObject();

        DefaultAugmentedTapiCommonContext augmentedTopologyContext = new DefaultAugmentedTapiCommonContext();
        Augmentable augmentableContext = context;
        augmentableContext.addAugmentation(augmentedTopologyContext);

        augmentedTopologyContext.topologyContext(topologyContext);
        topologyContext.addToTopology(topology);

        node1 = TapiNodeHandler.create()
                .setTopologyUuid(topology.uuid())
                .setDeviceId(did1)
                .getModelObject();

        node2 = TapiNodeHandler.create()
                .setTopologyUuid(topology.uuid())
                .setDeviceId(did2)
                .getModelObject();

        sip11 = TapiSipHandler.create()
                .setConnectPoint(cp11).getModelObject();

        sip21 = TapiSipHandler.create()
                .setConnectPoint(cp21).getModelObject();

        nep11 = TapiNepHandler.create()
                .setTopologyUuid(topology.uuid())
                .setNodeUuid(node1.uuid())
                .setConnectPoint(cp11)
                .addSip(sip11.uuid())
                .getModelObject();

        nep12 = TapiNepHandler.create()
                .setTopologyUuid(topology.uuid())
                .setNodeUuid(node1.uuid())
                .setConnectPoint(cp12)
                .getModelObject();

        nep21 = TapiNepHandler.create()
                .setTopologyUuid(topology.uuid())
                .setNodeUuid(node2.uuid())
                .setConnectPoint(cp21)
                .addSip(sip21.uuid())
                .getModelObject();

        nep22 = TapiNepHandler.create()
                .setTopologyUuid(topology.uuid())
                .setNodeUuid(node2.uuid())
                .setConnectPoint(cp22)
                .getModelObject();

    }

    @Test
    public void testUpdateCache() {

        DcsBasedTapiDataProducer dataProvider = new DcsBasedTapiDataProducer();
        DefaultTapiResolver mockResolver = EasyMock.createMock(DefaultTapiResolver.class);

        topology.addToNode(node1);
        topology.addToNode(node2);
        context.addToServiceInterfacePoint(sip11);
        context.addToServiceInterfacePoint(sip21);
        node1.addToOwnedNodeEdgePoint(nep11);
        node1.addToOwnedNodeEdgePoint(nep12);
        node2.addToOwnedNodeEdgePoint(nep21);
        node2.addToOwnedNodeEdgePoint(nep22);

        List<TapiNodeRef> expectNodes = Arrays.asList(
                DcsBasedTapiObjectRefFactory.create(topology, node1).setDeviceId(did1),
                DcsBasedTapiObjectRefFactory.create(topology, node2).setDeviceId(did2)
        );
        List<TapiNepRef> expectNeps = Arrays.asList(
                DcsBasedTapiObjectRefFactory.create(topology, node1, nep11).setConnectPoint(cp11)
                        .setSipId(sip11.uuid().toString()),
                DcsBasedTapiObjectRefFactory.create(topology, node1, nep12).setConnectPoint(cp12),
                DcsBasedTapiObjectRefFactory.create(topology, node2, nep21).setConnectPoint(cp21)
                        .setSipId(sip21.uuid().toString()),
                DcsBasedTapiObjectRefFactory.create(topology, node2, nep22).setConnectPoint(cp22)
        );

        mockResolver.addNodeRefList(expectNodes);
        expectLastCall().once();
        mockResolver.addNepRefList(expectNeps);
        expectLastCall().once();
        replay(mockResolver);

        dataProvider.updateCache(mockResolver, context);
        verify(mockResolver);
    }

    @Test
    public void testUpdateCacheWithoutSip() {

        DcsBasedTapiDataProducer dataProvider = new DcsBasedTapiDataProducer();
        DefaultTapiResolver mockResolver = EasyMock.createMock(DefaultTapiResolver.class);

        topology.addToNode(node1);
        topology.addToNode(node2);
        node1.addToOwnedNodeEdgePoint(nep11);
        node2.addToOwnedNodeEdgePoint(nep21);
        node2.addToOwnedNodeEdgePoint(nep22);

        List<TapiNodeRef> expectNodes = Arrays.asList(
                DcsBasedTapiObjectRefFactory.create(topology, node1).setDeviceId(did1),
                DcsBasedTapiObjectRefFactory.create(topology, node2).setDeviceId(did2)
        );
        List<TapiNepRef> expectNeps = Arrays.asList(
                DcsBasedTapiObjectRefFactory.create(topology, node1, nep11).setConnectPoint(cp11),
                DcsBasedTapiObjectRefFactory.create(topology, node2, nep21).setConnectPoint(cp21),
                DcsBasedTapiObjectRefFactory.create(topology, node2, nep22).setConnectPoint(cp22)
        );

        mockResolver.addNodeRefList(expectNodes);
        expectLastCall().once();
        mockResolver.addNepRefList(expectNeps);
        expectLastCall().once();
        replay(mockResolver);

        dataProvider.updateCache(mockResolver, context);
        verify(mockResolver);
    }

    @Test
    public void testUpdateCacheWithoutNep() {

        DcsBasedTapiDataProducer dataProvider = new DcsBasedTapiDataProducer();
        DefaultTapiResolver mockResolver = EasyMock.createMock(DefaultTapiResolver.class);

        topology.addToNode(node1);
        topology.addToNode(node2);

        List<TapiNodeRef> expectNodes = Arrays.asList(
                DcsBasedTapiObjectRefFactory.create(topology, node1).setDeviceId(did1),
                DcsBasedTapiObjectRefFactory.create(topology, node2).setDeviceId(did2)
        );
        List<TapiNepRef> expectNeps = Collections.emptyList();

        mockResolver.addNodeRefList(expectNodes);
        expectLastCall().once();
        mockResolver.addNepRefList(expectNeps);
        expectLastCall().once();
        replay(mockResolver);

        dataProvider.updateCache(mockResolver, context);
        verify(mockResolver);
    }

    @Test
    public void testUpdateCacheWithoutNode() {

        DcsBasedTapiDataProducer dataProvider = new DcsBasedTapiDataProducer();
        DefaultTapiResolver mockResolver = EasyMock.createMock(DefaultTapiResolver.class);

        List<TapiNodeRef> expectNodes = Collections.emptyList();
        List<TapiNepRef> expectNeps = Collections.emptyList();

        mockResolver.addNodeRefList(expectNodes);
        expectLastCall().once();
        mockResolver.addNepRefList(expectNeps);
        expectLastCall().once();
        replay(mockResolver);

        dataProvider.updateCache(mockResolver, context);
        verify(mockResolver);
    }

}
