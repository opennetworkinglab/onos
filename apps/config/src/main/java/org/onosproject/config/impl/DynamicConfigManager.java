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
import org.onlab.util.KryoNamespace;
import org.onosproject.event.AbstractListenerManager;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.StorageService;
import org.onosproject.config.DynamicConfigEvent;
import org.onosproject.config.DynamicConfigListener;
import org.onosproject.config.DynamicConfigService;
import org.onosproject.config.DynamicConfigStore;
import org.onosproject.config.DynamicConfigStoreDelegate;
import org.onosproject.config.FailedException;
import org.onosproject.config.Filter;
import org.onosproject.yang.model.RpcHandler;
import org.onosproject.yang.model.RpcInput;
import org.onosproject.yang.model.RpcOutput;
import org.onosproject.yang.model.DataNode;
import org.onosproject.yang.model.ResourceId;
//TODO import org.onosproject.yang.model.RpcRegistry;
//TODO import org.onosproject.yang.model.RpcService;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of the Dynamic Config Service.
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
    //TODO after 14420 is merged
    //private ConsistentMap<RpcService, RpcHandler> handlerRegistry;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Activate
    public void activate() {
        store.setDelegate(storeDelegate);
        eventDispatcher.addSink(DynamicConfigEvent.class, listenerRegistry);
        log.info("Started");
        KryoNamespace.Builder kryoBuilder = new KryoNamespace.Builder()
                .register(KryoNamespaces.BASIC)
                .register(Class.class)
                .register(RpcHandler.class)
                .register(ResourceId.class);
        //TODO after 14420 is merged
        /*handlerRegistry = storageService.<RpcService, RpcHandler>consistentMapBuilder()
                .withSerializer(Serializer.using(kryoBuilder.build()))
                .withName("config-object-store")
                .withRelaxedReadConsistency()
                .build();*/
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        store.unsetDelegate(storeDelegate);
        eventDispatcher.removeSink(DynamicConfigEvent.class);
        log.info("Stopped");
    }

    public void createNode(ResourceId path, DataNode node) {
        store.addNode(path, node).join();
    }

    public DataNode readNode(ResourceId path, Filter filter) {
        return store.readNode(path, filter).join();
    }

    public void updateNode(ResourceId path, DataNode node) {
        store.updateNode(path, node).join();
    }

    public void deleteNode(ResourceId path) {
        store.deleteNodeRecursive(path).join();
    }

    public void replaceNode(ResourceId path, DataNode node) {
        throw new FailedException("Not yet implemented");
    }

    public Boolean nodeExist(ResourceId path) {
        return store.nodeExist(path).join();
    }

    //TODO after RPC abstractions are merged; else will lead to build failure
    /*public void registerHandler(RpcHandler handler, RpcCommand command) {
        handlerRegistry.put(command, handler);
    }

    public void unRegisterHandler(RpcHandler handler, RpcCommand command) {
        Versioned<RpcHandler> ret = handlerRegistry.get(command);
        if ((ret == null) || (ret.value() == null)) {
            throw new FailedException("No registered handler found, cannot unregister");
        }
        handlerRegistry.remove(command);
    }*/

    public CompletableFuture<RpcOutput> invokeRpc(ResourceId id, RpcInput input) {
        //TODO after RPC abstractions are merged; else will lead to build failure
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