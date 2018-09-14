/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.config;

import static org.onosproject.yang.model.RpcOutput.Status.RPC_NODATA;

import java.util.concurrent.CompletableFuture;

import org.onosproject.event.ListenerRegistry;
import org.onosproject.yang.model.DataNode;
import org.onosproject.yang.model.ResourceId;
import org.onosproject.yang.model.RpcInput;
import org.onosproject.yang.model.RpcOutput;

/**
 * Adapter for DynamicConfigService.
 */
public class DynamicConfigServiceAdapter
    implements DynamicConfigService {

    protected final ListenerRegistry<DynamicConfigEvent, DynamicConfigListener>
        listenerRegistry = new ListenerRegistry<>();



    @Override
    public void createNode(ResourceId path, DataNode node) {
    }

    @Override
    public DataNode readNode(ResourceId path, Filter filter) {
        return null;
    }

    @Override
    public Boolean nodeExist(ResourceId path) {
        return true;
    }

    @Override
    public void updateNode(ResourceId path, DataNode node) {
    }

    @Override
    public void replaceNode(ResourceId path, DataNode node) {
    }

    @Override
    public void deleteNode(ResourceId path) {
    }

    @Override
    public CompletableFuture<RpcOutput> invokeRpc(RpcInput input) {
        return CompletableFuture.completedFuture(new RpcOutput(RPC_NODATA, null));
    }

    @Override
    public void addListener(DynamicConfigListener listener) {
        listenerRegistry.addListener(listener);
    }

    @Override
    public void removeListener(DynamicConfigListener listener) {
        listenerRegistry.removeListener(listener);
    }

}
