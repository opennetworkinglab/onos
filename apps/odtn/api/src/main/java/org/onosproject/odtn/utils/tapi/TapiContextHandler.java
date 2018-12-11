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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.onosproject.yang.gen.v1.tapicommon.rev20181210.tapicommon.DefaultContext;
import org.onosproject.yang.gen.v1.tapicommon.rev20181210.tapicommon.Uuid;
import org.onosproject.yang.gen.v1.tapiconnectivity.rev20181210.tapiconnectivity.connectivitycontext.DefaultConnectivityService;
import org.onosproject.yang.gen.v1.tapiconnectivity.rev20181210.tapiconnectivity.context.DefaultAugmentedTapiCommonContext;
import org.onosproject.yang.gen.v1.tapitopology.rev20181210.tapitopology.context.augmentedtapicommoncontext.DefaultTopologyContext;
import org.onosproject.yang.model.DefaultModelObjectData;
import org.onosproject.yang.model.ModelObjectData;
import org.onosproject.yang.model.ModelObjectId;

/**
 * Utility class to deal with TAPI Context with DCS.
 */
public final class TapiContextHandler extends TapiObjectHandler<DefaultContext> {

    private TapiContextHandler() {
        obj = new DefaultContext();
    }

    public static TapiContextHandler create() {
        return new TapiContextHandler();
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
        return ModelObjectId.builder().build();
    }

    @Override
    public ModelObjectData getChildModelObjectData() {

        ModelObjectId mId = ModelObjectId.builder()
                .addChild(DefaultContext.class)
                .build();

        DefaultTopologyContext mObj = new DefaultTopologyContext();

        return DefaultModelObjectData.builder()
                .addModelObject(mObj)
                .identifier(mId)
                .build();
    }

    public List<TapiConnectivityServiceHandler> getConnectivityServices() {

        DefaultAugmentedTapiCommonContext augmentedContext = obj.augmentation(DefaultAugmentedTapiCommonContext.class);
        try {
            return augmentedContext.connectivityContext().connectivityService().stream()
                    .map(connectivityService -> {
                        TapiConnectivityServiceHandler handler = TapiConnectivityServiceHandler.create();
                        handler.setModelObject((DefaultConnectivityService) connectivityService);
                        return handler;
                    }).collect(Collectors.toList());
        } catch (NullPointerException e) {
            return Collections.emptyList();
        }
    }

}
