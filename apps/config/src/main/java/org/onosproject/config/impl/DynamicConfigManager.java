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

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.config.ResourceIdParser;
import org.onosproject.config.RpcExecutor;
import org.onosproject.config.RpcMessageId;
import org.onosproject.event.AbstractListenerManager;
import org.onosproject.config.DynamicConfigEvent;
import org.onosproject.config.DynamicConfigListener;
import org.onosproject.config.DynamicConfigService;
import org.onosproject.config.DynamicConfigStore;
import org.onosproject.config.DynamicConfigStoreDelegate;
import org.onosproject.config.FailedException;
import org.onosproject.config.Filter;
import org.onosproject.yang.model.RpcInput;
import org.onosproject.yang.model.RpcOutput;
import org.onosproject.yang.model.DataNode;
import org.onosproject.yang.model.ResourceId;
import org.onosproject.yang.model.RpcRegistry;
import org.onosproject.yang.model.RpcService;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of the Dynamic Config Service.
 *
 */
@Component(immediate = true)
@Service
public class DynamicConfigManager
        extends AbstractListenerManager<DynamicConfigEvent, DynamicConfigListener>
        implements DynamicConfigService, RpcRegistry {
    private final Logger log = getLogger(getClass());
    private final DynamicConfigStoreDelegate storeDelegate = new InternalStoreDelegate();
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DynamicConfigStore store;
    private ConcurrentHashMap<String, RpcService> handlerRegistry = new ConcurrentHashMap<>();

    @Activate
    public void activate() {
        store.setDelegate(storeDelegate);
        eventDispatcher.addSink(DynamicConfigEvent.class, listenerRegistry);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        store.unsetDelegate(storeDelegate);
        eventDispatcher.removeSink(DynamicConfigEvent.class);
        handlerRegistry.clear();
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

    public Set<RpcService> getRpcServices() {
        Set<RpcService> res = new HashSet();
        for (Map.Entry<String, RpcService> e : handlerRegistry.entrySet()) {
            res.add(e.getValue());
        }
        return res;
    }

    public RpcService getRpcService(Class<? extends RpcService> intfc) {
        return handlerRegistry.get(intfc.getSimpleName());
    }

    public void registerRpcService(RpcService handler) {
        for (Class<?> intfc : handler.getClass().getInterfaces()) {
            if (RpcService.class.isAssignableFrom(intfc)) {
                handlerRegistry.put(intfc.getSimpleName(), handler);
            }
        }
    }

    public void unregisterRpcService(RpcService handler) {
        for (Class<?> intfc : handler.getClass().getInterfaces()) {
            if (RpcService.class.isAssignableFrom(intfc)) {
                String key = intfc.getSimpleName();
                if (handlerRegistry.get(key) == null) {
                    throw new FailedException("No registered handler found, cannot unregister");
                }
                handlerRegistry.remove(key);
            }
        }
    }

    private int getSvcId(RpcService handler, String srvc) {
        Class<?>[] intfcs = handler.getClass().getInterfaces();
        for (int i = 0; i < intfcs.length; i++) {
            if (intfcs[i].getSimpleName().compareTo(srvc) == 0) {
                return i;
            }
        }
        throw new FailedException("No handler found, cannot invoke");
    }

    public CompletableFuture<RpcOutput> invokeRpc(ResourceId id, RpcInput input) {
        String[] ctxt = ResourceIdParser.getService(id);
        RpcService handler = handlerRegistry.get(ctxt[0]);
        if (handler == null) {
            throw new FailedException("No registered handler found, cannot invoke");
        }
        return CompletableFuture.supplyAsync(
                new RpcExecutor(handler, getSvcId(handler, ctxt[0]), ctxt[1], RpcMessageId.generate(), input),
                Executors.newSingleThreadExecutor());
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