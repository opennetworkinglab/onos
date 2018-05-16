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
import org.onosproject.net.Port;

import static org.onosproject.odtn.utils.tapi.TapiGlobalClassUtil.setNameList;
import static org.onosproject.odtn.utils.tapi.TapiGlobalClassUtil.setUuid;

import org.onosproject.yang.gen.v1.tapicommon.rev20180307.tapicommon.DefaultContext;
import org.onosproject.yang.gen.v1.tapicommon.rev20180307.tapicommon.Uuid;
import org.onosproject.yang.gen.v1.tapitopology.rev20180307.tapitopology.node.DefaultOwnedNodeEdgePoint;
import org.onosproject.yang.gen.v1.tapitopology.rev20180307.tapitopology.nodeedgepoint.DefaultMappedServiceInterfacePoint;
import org.onosproject.yang.gen.v1.tapitopology.rev20180307.tapitopology.topology.DefaultNode;
import org.onosproject.yang.gen.v1.tapitopology.rev20180307.tapitopology.topology.NodeKeys;
import org.onosproject.yang.gen.v1.tapitopology.rev20180307.tapitopology.topologycontext.DefaultTopology;
import org.onosproject.yang.gen.v1.tapitopology.rev20180307.tapitopology.topologycontext.TopologyKeys;
import org.onosproject.yang.model.ModelObject;
import org.onosproject.yang.model.ModelObjectData;
import org.onosproject.yang.model.ModelObjectId;

/**
 * Utility builder class for TAPI nep creation with DCS.
 */
public final class TapiNepBuilder extends TapiInstanceBuilder {

    private Uuid topologyUuid;
    private Uuid nodeUuid;
    private DefaultOwnedNodeEdgePoint nep = new DefaultOwnedNodeEdgePoint();
    private ConnectPoint cp;
    private Map<String, String> kvs = new HashMap<>();

    private TapiNepBuilder() {
        setUuid(nep);
    }

    public static TapiNepBuilder builder() {
        return new TapiNepBuilder();
    }

    public TapiNepBuilder setTopologyUuid(Uuid topologyUuid) {
        this.topologyUuid = topologyUuid;
        return this;
    }

    public TapiNepBuilder setNodeUuid(Uuid nodeUuid) {
        this.nodeUuid = nodeUuid;
        return this;
    }

    public TapiNepBuilder setPort(Port port) {
        cp = new ConnectPoint(port.element().id(), port.number());
        kvs.put(ONOS_CP, cp.toString());
        return this;
    }

    public TapiNepBuilder setConnectPoint(ConnectPoint cp) {
        kvs.put(ONOS_CP, cp.toString());
        return this;
    }

    public TapiNepBuilder setSip(Uuid sipUuid) {
        DefaultMappedServiceInterfacePoint mappedSip = new DefaultMappedServiceInterfacePoint();
        mappedSip.serviceInterfacePointId(sipUuid);
        nep.addToMappedServiceInterfacePoint(mappedSip);
        return this;
    }

    public ConnectPoint getConnectPoint() {
        return cp;
    }

    @Override
    public ModelObject getModelObject() {
        return nep;
    }

    @Override
    public Uuid getUuid() {
        return nep.uuid();
    }

    @Override
    public ModelObjectData build() {
        setNameList(nep, kvs);

        TopologyKeys topologyKey = new TopologyKeys();
        topologyKey.uuid(topologyUuid);

        NodeKeys nodeKey = new NodeKeys();
        nodeKey.uuid(nodeUuid);

        ModelObjectId objId = ModelObjectId.builder()
                .addChild(DefaultContext.class)
                .addChild(DefaultTopology.class, topologyKey)
                .addChild(DefaultNode.class, nodeKey)
                .build();
        return getModelObjectData(nep, objId);
    }

}
