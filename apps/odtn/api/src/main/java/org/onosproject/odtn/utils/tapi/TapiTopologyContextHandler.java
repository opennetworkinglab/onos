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

import org.onosproject.yang.gen.v1.tapicommon.rev20181210.tapicommon.DefaultContext;
import org.onosproject.yang.gen.v1.tapicommon.rev20181210.tapicommon.Uuid;
import org.onosproject.yang.gen.v1.tapitopology.rev20181210.tapitopology.context.DefaultAugmentedTapiCommonContext;
import org.onosproject.yang.gen.v1.tapitopology.rev20181210.tapitopology.context.augmentedtapicommoncontext.DefaultTopologyContext;
import org.onosproject.yang.model.ModelObjectId;

/**
 * Utility class to deal with TAPI Augmented Topology Context with DCS.
 */
public final class TapiTopologyContextHandler extends TapiObjectHandler<DefaultTopologyContext> {

    private TapiTopologyContextHandler() {
        obj = new DefaultTopologyContext();
    }

    public static TapiTopologyContextHandler create() {
        return new TapiTopologyContextHandler();
    }

    @Override
    protected Uuid getIdDetail() {
        // The target yang object of this class is container, so no need to handle Id.
        return null;
    }

    @Override
    protected void setIdDetail(Uuid uuid) {
        // The target yang object of this class is container, so no need to handle Id.
    }

    @Override
    public ModelObjectId getParentModelObjectId() {
        DefaultAugmentedTapiCommonContext context = new DefaultAugmentedTapiCommonContext();
        context.topologyContext(obj);

        return ModelObjectId.builder()
                .addChild(DefaultContext.class).build();
    }
}
