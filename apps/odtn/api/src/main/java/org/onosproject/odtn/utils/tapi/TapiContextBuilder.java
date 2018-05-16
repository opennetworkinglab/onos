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

import org.onosproject.yang.gen.v1.tapicommon.rev20180307.tapicommon.DefaultContext;
import org.onosproject.yang.gen.v1.tapicommon.rev20180307.tapicommon.Uuid;
import org.onosproject.yang.model.ModelObject;
import org.onosproject.yang.model.ModelObjectData;
import org.onosproject.yang.model.ModelObjectId;

/**
 * Utility builder class for TAPI context creation with DCS.
 */
public final class TapiContextBuilder extends TapiInstanceBuilder {

    private DefaultContext context;

    private TapiContextBuilder(DefaultContext context) {
        this.context = context;
    }

    public static TapiContextBuilder builder(DefaultContext context) {
        return new TapiContextBuilder(context);
    }

    @Override
    public ModelObjectData build() {
        ModelObjectId objId = ModelObjectId.builder().build();
        return getModelObjectData(context, objId);
    }

    @Override
    public ModelObject getModelObject() {
        return context;
    }

    @Override
    public Uuid getUuid() {
        return null;
    }
}
