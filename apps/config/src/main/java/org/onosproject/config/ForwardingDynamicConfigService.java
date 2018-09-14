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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.CompletableFuture;

import org.onosproject.yang.model.DataNode;
import org.onosproject.yang.model.ResourceId;
import org.onosproject.yang.model.RpcInput;
import org.onosproject.yang.model.RpcOutput;

/**
 * A DynamicConfigService which forwards all its method calls
 * to another DynamicConfigService.
 */
public class ForwardingDynamicConfigService implements DynamicConfigService {

    private final DynamicConfigService delegate;

    protected ForwardingDynamicConfigService(DynamicConfigService delegate) {
        this.delegate = checkNotNull(delegate);
    }

    protected DynamicConfigService delegate() {
        return delegate;
    }

    @Override
    public void addListener(DynamicConfigListener listener) {
        delegate.addListener(listener);
    }

    @Override
    public void removeListener(DynamicConfigListener listener) {
        delegate.removeListener(listener);
    }

    @Override
    public void createNode(ResourceId path, DataNode node) {
        delegate.createNode(path, node);
    }

    @Override
    public DataNode readNode(ResourceId path, Filter filter) {
        return delegate.readNode(path, filter);
    }

    @Override
    public Boolean nodeExist(ResourceId path) {
        return delegate.nodeExist(path);
    }

    @Override
    public void updateNode(ResourceId path, DataNode node) {
        delegate.updateNode(path, node);
    }

    @Override
    public void replaceNode(ResourceId path, DataNode node) {
        delegate.replaceNode(path, node);
    }

    @Override
    public void deleteNode(ResourceId path) {
        delegate.deleteNode(path);
    }

    @Override
    public CompletableFuture<RpcOutput> invokeRpc(RpcInput input) {
        return delegate.invokeRpc(input);
    }
}
