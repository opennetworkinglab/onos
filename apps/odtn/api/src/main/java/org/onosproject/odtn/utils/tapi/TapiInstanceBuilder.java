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

import org.onosproject.yang.gen.v1.tapicommon.rev20180307.tapicommon.Uuid;
import org.onosproject.yang.model.DefaultModelObjectData;
import org.onosproject.yang.model.ModelObject;
import org.onosproject.yang.model.ModelObjectData;
import org.onosproject.yang.model.ModelObjectId;

/**
 * Utility builder class for TAPI modelobject creation with DCS.
 */
public abstract class TapiInstanceBuilder {

    public static final String ONOS_CP = "onos-cp";

    public static final String DEVICE_ID = "device_id";

    /**
     * Generate DCS modelObjectData.
     * @return ModelObjectData to be built
     */
    public abstract ModelObjectData build();

    /**
     * Get modelObject instance.
     * @return ModelObject of build target
     */
    public abstract ModelObject getModelObject();

    /**
     * Get modelObject uuid.
     * @return Uuid of build target
     */
    public abstract Uuid getUuid();


    ModelObjectData getModelObjectData(ModelObject obj, ModelObjectId objId) {
        return DefaultModelObjectData.builder()
                .addModelObject(obj)
                .identifier(objId)
                .build();
    }
}
