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

import static org.onosproject.odtn.utils.tapi.TapiGlobalClassUtil.setUuid;

import org.onosproject.yang.gen.v1.tapicommon.rev20180307.tapicommon.DefaultContext;
import org.onosproject.yang.gen.v1.tapicommon.rev20180307.tapicommon.Uuid;
import org.onosproject.yang.gen.v1.tapitopology.rev20180307.tapitopology.context.DefaultAugmentedTapiCommonContext;
import org.onosproject.yang.gen.v1.tapitopology.rev20180307.tapitopology.topologycontext.DefaultTopology;
import org.onosproject.yang.model.ModelObjectId;

/**
 * Utility builder class for TAPI topology creation with DCS.
 */
public final class TapiTopologyBuilder extends TapiInstanceBuilder {

    private DefaultTopology topology;

    private TapiTopologyBuilder(DefaultTopology topology) {
        this.topology = topology;
        setUuid(this.topology);
    }

    public static TapiTopologyBuilder builder(DefaultTopology topology) {
        return new TapiTopologyBuilder(topology);
    }


    @Override
    public ModelObjectId getModelObjectId() {

        DefaultAugmentedTapiCommonContext topologyContext = new DefaultAugmentedTapiCommonContext();
        topologyContext.addToTopology(topology);

        return ModelObjectId.builder().addChild(DefaultContext.class).build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public DefaultTopology getModelObject() {
        return topology;
    }

    @Override
    public Uuid getUuid() {
        return topology.uuid();
    }

}
