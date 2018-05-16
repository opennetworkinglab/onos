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
import static org.onosproject.odtn.utils.tapi.TapiGlobalClassUtil.setNameList;
import static org.onosproject.odtn.utils.tapi.TapiGlobalClassUtil.setUuid;

import org.onosproject.net.DeviceId;
import org.onosproject.yang.gen.v1.tapicommon.rev20180307.tapicommon.DefaultContext;
import org.onosproject.yang.gen.v1.tapicommon.rev20180307.tapicommon.Uuid;
import org.onosproject.yang.gen.v1.tapitopology.rev20180307.tapitopology.node.OwnedNodeEdgePoint;
import org.onosproject.yang.gen.v1.tapitopology.rev20180307.tapitopology.topology.DefaultNode;
import org.onosproject.yang.gen.v1.tapitopology.rev20180307.tapitopology.topologycontext.DefaultTopology;
import org.onosproject.yang.gen.v1.tapitopology.rev20180307.tapitopology.topologycontext.TopologyKeys;
import org.onosproject.yang.model.ModelObject;
import org.onosproject.yang.model.ModelObjectData;
import org.onosproject.yang.model.ModelObjectId;

/**
 * Utility builder class for TAPI node creation with DCS.
 */
public final class TapiNodeBuilder extends TapiInstanceBuilder {

    private Uuid topologyUuid;
    private DefaultNode node = new DefaultNode();
    private Map<String, String> kvs = new HashMap<>();

    private TapiNodeBuilder() {
        setUuid(node);
    }

    public static TapiNodeBuilder builder() {
        return new TapiNodeBuilder();
    }

    public TapiNodeBuilder setTopologyUuid(Uuid topologyUuid) {
        this.topologyUuid = topologyUuid;
        return this;
    }

    public TapiNodeBuilder setNep(OwnedNodeEdgePoint nep) {
        node.addToOwnedNodeEdgePoint(nep);
        return this;
    }

    public TapiNodeBuilder setDeviceId(DeviceId deviceId) {
        kvs.put(DEVICE_ID, deviceId.toString());
        return this;
    }

    @Override
    public ModelObject getModelObject() {
        return node;
    }

    @Override
    public Uuid getUuid() {
        return node.uuid();
    }

    @Override
    public ModelObjectData build() {
        setNameList(node, kvs);

        TopologyKeys topologyKey = new TopologyKeys();
        topologyKey.uuid(topologyUuid);
        ModelObjectId objId = ModelObjectId.builder()
                .addChild(DefaultContext.class)
                .addChild(DefaultTopology.class, topologyKey)
                .build();
        return getModelObjectData(node, objId);
    }

}
