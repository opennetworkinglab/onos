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

import org.onosproject.yang.gen.v1.tapicommon.rev20181210.tapicommon.Uuid;
import org.onosproject.yang.model.ModelObject;
import org.onosproject.yang.model.ModelObjectId;
import org.onosproject.yang.model.RpcInput;

/**
 * Utility abstract class to deal with TAPI RPC input with DCS.
 *
 * @param <T> RPCInput modelObject to be dealt with
 */
public abstract class TapiRpcInputHandler<T extends ModelObject> extends TapiObjectHandler<T> {

    // Uuid getter is not needed for RPC input
    @Override
    protected Uuid getIdDetail() {
        return null;
    }

    // Uuid getter is not needed for RPC input
    @Override
    protected void setIdDetail(Uuid uuid) {
    }

    // Root modelObjectId must be used
    @Override
    public ModelObjectId getParentModelObjectId() {
        return ModelObjectId.builder().build();
    }

    // Cannot set InputRpc modelObject into DCS
    @Override
    public final void add() {
    }

    // Cannot remove InputRpc modelObject into DCS
    @Override
    public final void remove() {
    }

    /**
     * Set RPCInput modelObject.
     *
     * @param input Tapi RpcInput modelObject
     */
    public void setRpcInput(RpcInput input) {
        obj = toModelObject(input.data(), input.id());
    }
}
