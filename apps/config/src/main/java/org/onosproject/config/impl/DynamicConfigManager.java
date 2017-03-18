/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.config.impl;

import com.google.common.annotations.Beta;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.config.DynamicConfigEvent;
import org.onosproject.config.DynamicConfigListener;
import org.onosproject.config.DynamicConfigService;
import org.onosproject.config.DynamicConfigStore;
import org.onosproject.config.DynamicConfigStoreDelegate;
import org.onosproject.config.FailedException;
import org.onosproject.config.Filter;
import org.onosproject.config.RpcCaller;
import org.onosproject.config.RpcCommand;
import org.onosproject.config.RpcHandler;
import org.onosproject.config.RpcInput;
import org.onosproject.config.RpcOutput;
import org.onosproject.yang.model.DataNode;
import org.onosproject.yang.model.ResourceId;
import org.onosproject.event.AbstractListenerManager;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Demo application to use the DynamicConfig Service and DynamicConfigStore.
 *
 */
@Beta
@Component(immediate = true)
@Service
public class DynamicConfigManager
        extends AbstractListenerManager<DynamicConfigEvent, DynamicConfigListener>
        implements DynamicConfigService {
    private final Logger log = getLogger(getClass());
    private final DynamicConfigStoreDelegate storeDelegate = new InternalStoreDelegate();
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DynamicConfigStore store;

    @Activate
    public void activate() {
        store.setDelegate(storeDelegate);
        eventDispatcher.addSink(DynamicConfigEvent.class, listenerRegistry);
        log.info("DynamicConfigService Started");
    }

    @Deactivate
    public void deactivate() {
        store.unsetDelegate(storeDelegate);
        eventDispatcher.removeSink(DynamicConfigEvent.class);
        log.info("DynamicConfigService Stopped");
    }

    public void createNodeRecursive(ResourceId path, DataNode node) {
        Boolean stat = false;
        stat = this.store.addNode(path, node).join();
    }

    public DataNode readNode(ResourceId path, Filter filter) {
        return store.readNode(path, filter).join();
    }

    public void updateNode(ResourceId path, DataNode node) {
        throw new FailedException("Not yet implemented");
    }

    public void deleteNode(ResourceId path) {
        throw new FailedException("Not yet implemented");
    }

    public void deleteNodeRecursive(ResourceId path) {
        store.deleteNodeRecursive(path).join();
    }

    public void updateNodeRecursive(ResourceId path, DataNode node) {
        throw new FailedException("Not yet implemented");
    }

    public void replaceNode(ResourceId path, DataNode node) {
        throw new FailedException("Not yet implemented");
    }

    public Integer getNumberOfChildren(ResourceId path, Filter filter) {
        throw new FailedException("Not yet implemented");
    }

    public void registerHandler(RpcHandler handler, RpcCommand command) {
        throw new FailedException("Not yet implemented");
    }

    public void unRegisterHandler(RpcHandler handler, RpcCommand command) {
        //check obj1.getClass().equals(obj2.getClass())
        throw new FailedException("Not yet implemented");
    }

    public void invokeRpc(RpcCaller caller, Integer msgId, RpcCommand command, RpcInput input) {
        throw new FailedException("Not yet implemented");
    }

    public void rpcResponse(Integer msgId, RpcOutput output) {
        throw new FailedException("Not yet implemented");
    }
    /**
     * Auxiliary store delegate to receive notification about changes in the store.
     */
    private class InternalStoreDelegate implements DynamicConfigStoreDelegate {
        public void notify(DynamicConfigEvent event) {
            post(event);
        }
    }
}