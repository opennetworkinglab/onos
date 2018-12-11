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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.odtn.utils.tapi.TapiGlobalClassUtil.addNameList;
import static org.onosproject.odtn.utils.tapi.TapiGlobalClassUtil.getUuid;
import static org.onosproject.odtn.utils.tapi.TapiGlobalClassUtil.setUuid;

import org.onosproject.net.DeviceId;
import org.onosproject.yang.gen.v1.tapicommon.rev20181210.tapicommon.DefaultContext;
import org.onosproject.yang.gen.v1.tapicommon.rev20181210.tapicommon.Uuid;
import org.onosproject.yang.gen.v1.tapitopology.rev20181210.tapitopology.context.augmentedtapicommoncontext.DefaultTopologyContext;
import org.onosproject.yang.gen.v1.tapitopology.rev20181210.tapitopology.node.OwnedNodeEdgePoint;
import org.onosproject.yang.gen.v1.tapitopology.rev20181210.tapitopology.topology.DefaultNode;
import org.onosproject.yang.gen.v1.tapitopology.rev20181210.tapitopology.topologycontext.DefaultTopology;
import org.onosproject.yang.gen.v1.tapitopology.rev20181210.tapitopology.topologycontext.TopologyKeys;
import org.onosproject.yang.model.ModelObjectId;

/**
 * Utility class to deal with TAPI Node with DCS.
 */
public final class TapiNodeHandler extends TapiObjectHandler<DefaultNode> {

    private Uuid topologyUuid;

    private TapiNodeHandler() {
        obj = new DefaultNode();
        setId();
    }

    public static TapiNodeHandler create() {
        return new TapiNodeHandler();
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
        checkNotNull(topologyUuid);

        TopologyKeys topologyKey = new TopologyKeys();
        topologyKey.uuid(topologyUuid);
        return ModelObjectId.builder()
                .addChild(DefaultContext.class)
                .addChild(DefaultTopologyContext.class)
                .addChild(DefaultTopology.class, topologyKey)
                .build();
    }

    public TapiNodeHandler setTopologyUuid(Uuid topologyUuid) {
        this.topologyUuid = topologyUuid;
        return this;
    }

    public TapiNodeHandler addNep(OwnedNodeEdgePoint nep) {
        obj.addToOwnedNodeEdgePoint(nep);
        return this;
    }

    public TapiNodeHandler setDeviceId(DeviceId deviceId) {
        Map<String, String> kvs = new HashMap<>();
        kvs.put(DEVICE_ID, deviceId.toString());
        addNameList(obj, kvs);
        return this;
    }

}
