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

package org.onosproject.odtn.utils.tapi;

import java.util.HashMap;
import java.util.Map;
import org.onosproject.net.ConnectPoint;

import static org.onosproject.odtn.behaviour.OdtnDeviceDescriptionDiscovery.CONNECTION_ID;
import static org.onosproject.odtn.utils.tapi.TapiGlobalClassUtil.addNameList;
import static org.onosproject.odtn.utils.tapi.TapiGlobalClassUtil.getUuid;
import static org.onosproject.odtn.utils.tapi.TapiGlobalClassUtil.setUuid;

import org.onosproject.net.Port;
import org.onosproject.yang.gen.v1.tapicommon.rev20181210.tapicommon.DefaultContext;
import org.onosproject.yang.gen.v1.tapicommon.rev20181210.tapicommon.Uuid;

import org.onosproject.yang.gen.v1.tapiconnectivity.rev20181210.tapiconnectivity.ceplist.DefaultConnectionEndPoint;

import org.onosproject.yang.gen.v1.tapiconnectivity.rev20181210.tapiconnectivity.context.topologycontext.topology.node.ownednodeedgepoint.DefaultAugmentedTapiTopologyOwnedNodeEdgePoint;
import org.onosproject.yang.gen.v1.tapiconnectivity.rev20181210.tapiconnectivity.context.topologycontext.topology.node.ownednodeedgepoint.augmentedtapitopologyownednodeedgepoint.DefaultCepList;
import org.onosproject.yang.gen.v1.tapitopology.rev20181210.tapitopology.context.augmentedtapicommoncontext.DefaultTopologyContext;
import org.onosproject.yang.gen.v1.tapitopology.rev20181210.tapitopology.node.DefaultOwnedNodeEdgePoint;
import org.onosproject.yang.gen.v1.tapitopology.rev20181210.tapitopology.nodeedgepoint.DefaultMappedServiceInterfacePoint;
import org.onosproject.yang.gen.v1.tapitopology.rev20181210.tapitopology.topology.DefaultNode;
import org.onosproject.yang.gen.v1.tapitopology.rev20181210.tapitopology.topology.NodeKeys;
import org.onosproject.yang.gen.v1.tapitopology.rev20181210.tapitopology.topologycontext.DefaultTopology;
import org.onosproject.yang.gen.v1.tapitopology.rev20181210.tapitopology.topologycontext.TopologyKeys;
import org.onosproject.yang.model.ModelObjectId;

/**
 * Utility class to deal with TAPI NEP with DCS.
 */
public final class TapiNepHandler extends TapiObjectHandler<DefaultOwnedNodeEdgePoint> {

    private Uuid topologyUuid;
    private Uuid nodeUuid;

    private ConnectPoint cp;
    private Map<String, String> kvs = new HashMap<>();

    private TapiNepHandler() {
        obj = new DefaultOwnedNodeEdgePoint();
        setId();
    }

    public static TapiNepHandler create() {
        return new TapiNepHandler();
    }

    @Override
    protected Uuid getIdDetail() {
        return getUuid(obj);
    }

    @Override
    protected void setIdDetail(Uuid uuid) {
        setUuid(obj, uuid);
    }

    @Override
    public ModelObjectId getParentModelObjectId() {
        TopologyKeys topologyKey = new TopologyKeys();
        topologyKey.uuid(topologyUuid);

        NodeKeys nodeKey = new NodeKeys();
        nodeKey.uuid(nodeUuid);

        return ModelObjectId.builder()
                .addChild(DefaultContext.class)
                .addChild(DefaultTopologyContext.class)
                .addChild(DefaultTopology.class, topologyKey)
                .addChild(DefaultNode.class, nodeKey)
                .build();
    }

    public TapiNepHandler setTopologyUuid(Uuid topologyUuid) {
        this.topologyUuid = topologyUuid;
        return this;
    }

    public TapiNepHandler setNodeUuid(Uuid nodeUuid) {
        this.nodeUuid = nodeUuid;
        return this;
    }

    public TapiNepHandler setPort(Port port) {
        ConnectPoint cp = new ConnectPoint(port.element().id(), port.number());
        kvs.put(ODTN_PORT_TYPE, port.annotations().value(ODTN_PORT_TYPE));
        kvs.put(CONNECTION_ID, port.annotations().value(CONNECTION_ID));
        addNameList(obj, kvs);
        return setConnectPoint(cp);
    }

    public TapiNepHandler setConnectPoint(ConnectPoint cp) {
        kvs.put(ONOS_CP, cp.toString());
        addNameList(obj, kvs);
        return this;
    }

    public TapiNepHandler addSip(Uuid sipUuid) {
        DefaultMappedServiceInterfacePoint mappedSip = new DefaultMappedServiceInterfacePoint();
        mappedSip.serviceInterfacePointUuid(sipUuid);
        obj.addToMappedServiceInterfacePoint(mappedSip);
        return this;
    }

    public TapiNepHandler addCep(DefaultConnectionEndPoint cep) {

        DefaultCepList cepList = new DefaultCepList();
        cepList.addToConnectionEndPoint(cep);

        DefaultAugmentedTapiTopologyOwnedNodeEdgePoint augmentNep =
                new DefaultAugmentedTapiTopologyOwnedNodeEdgePoint();
        augmentNep.cepList(cepList);
        obj.addAugmentation(augmentNep);

        return this;
    }

    public ConnectPoint getConnectPoint() {
        return cp;
    }

}
