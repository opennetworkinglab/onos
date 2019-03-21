/*
 * Copyright 2016-present Open Networking Foundation
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

import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.Leadership;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.config.DynamicConfigEvent;
import org.onosproject.config.DynamicConfigListener;
import org.onosproject.config.DynamicConfigService;
import org.onosproject.config.DynamicConfigStore;
import org.onosproject.config.DynamicConfigStoreDelegate;
import org.onosproject.config.FailedException;
import org.onosproject.config.Filter;
import org.onosproject.config.RpcExecutor;
import org.onosproject.config.RpcMessageId;
import org.onosproject.d.config.DeviceResourceIds;
import org.onosproject.d.config.ResourceIds;
import org.onosproject.event.AbstractListenerManager;
import org.onosproject.yang.model.DataNode;
import org.onosproject.yang.model.DataNode.Type;
import org.onosproject.yang.model.InnerNode;
import org.onosproject.yang.model.ResourceId;
import org.onosproject.yang.model.RpcContext;
import org.onosproject.yang.model.RpcInput;
import org.onosproject.yang.model.RpcOutput;
import org.onosproject.yang.model.RpcRegistry;
import org.onosproject.yang.model.RpcService;
import org.onosproject.yang.model.SchemaContextProvider;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.d.config.DeviceResourceIds.DCS_NAMESPACE;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of the Dynamic Config Service.
 *
 */
@Component(immediate = true, service = { DynamicConfigService.class, RpcRegistry.class })
public class DynamicConfigManager
        extends AbstractListenerManager<DynamicConfigEvent, DynamicConfigListener>
        implements DynamicConfigService, RpcRegistry {

    protected static final String DCS_STORE_INIT = "dcs-store-init";
    private final Logger log = getLogger(getClass());
    private final DynamicConfigStoreDelegate storeDelegate = new InternalStoreDelegate();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DynamicConfigStore store;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected SchemaContextProvider contextProvider;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;



    // FIXME is it OK this is not using the Store?
    private ConcurrentHashMap<String, RpcService> handlerRegistry = new ConcurrentHashMap<>();

    @Activate
    public void activate() {
        NodeId localnodeId = clusterService.getLocalNode().id();
        Leadership leadership = leadershipService.runForLeadership(DCS_STORE_INIT);
        if (leadership.leaderNodeId().equals(localnodeId)) {
            initStore();
        }
        store.setDelegate(storeDelegate);
        eventDispatcher.addSink(DynamicConfigEvent.class, listenerRegistry);
        log.info("Started");
    }

    /**
     * Ensure built-in tree nodes exists.
     */
    private void initStore() {
        store.nodeExist(DeviceResourceIds.DEVICES_ID)
            .thenAccept(exists -> {
                if (!exists) {
                    log.info("devices node does not exist!, creating...");
                    store.addNode(ResourceIds.ROOT_ID,
                                  InnerNode.builder(DeviceResourceIds.DEVICES_NAME, DCS_NAMESPACE)
                                  .type(Type.SINGLE_INSTANCE_NODE).build());
                }
            }).join();
    }

    @Deactivate
    public void deactivate() {
        store.unsetDelegate(storeDelegate);
        eventDispatcher.removeSink(DynamicConfigEvent.class);
        handlerRegistry.clear();
        log.info("Stopped");
    }

    @Override
    public void createNode(ResourceId path, DataNode node) {
        store.addNode(path, node).join();
    }

    @Override
    public DataNode readNode(ResourceId path, Filter filter) {
        return store.readNode(path, filter).join();
    }

    @Override
    public void updateNode(ResourceId path, DataNode node) {
        store.updateNode(path, node).join();
    }

    @Override
    public void deleteNode(ResourceId path) {
        store.deleteNodeRecursive(path).join();
    }

    @Override
    public void replaceNode(ResourceId path, DataNode node) {
        throw new FailedException("Not yet implemented");
    }

    @Override
    public Boolean nodeExist(ResourceId path) {
        return store.nodeExist(path).join();
    }

    @Override
    public Set<RpcService> getRpcServices() {
        Set<RpcService> res = new HashSet();
        for (Map.Entry<String, RpcService> e : handlerRegistry.entrySet()) {
            res.add(e.getValue());
        }
        return res;
    }

    @Override
    public RpcService getRpcService(Class<? extends RpcService> intfc) {
        return handlerRegistry.get(intfc.getName());
    }

    @Override
    public void registerRpcService(RpcService handler) {
        for (Class<?> intfc : handler.getClass().getInterfaces()) {
            if (RpcService.class.isAssignableFrom(intfc)) {
                handlerRegistry.put(intfc.getName(), handler);
            }
        }
    }

    @Override
    public void unregisterRpcService(RpcService handler) {
        for (Class<?> intfc : handler.getClass().getInterfaces()) {
            if (RpcService.class.isAssignableFrom(intfc)) {
                String key = intfc.getName();
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
            if (intfcs[i].getName().compareTo(srvc) == 0) {
                return i;
            }
        }
        throw new FailedException("No handler found, cannot invoke");
    }

    @Override
    public CompletableFuture<RpcOutput> invokeRpc(RpcInput input) {
        checkNotNull(input);
        checkNotNull(input.id());
        RpcContext context = contextProvider.getRpcContext(input.id());
        String srvcIntf = context.serviceIntf().getName();
        RpcService handler = handlerRegistry.get(srvcIntf);
        if (handler == null) {
            throw new FailedException("No registered handler found, cannot invoke");
        }
        return CompletableFuture.supplyAsync(
            new RpcExecutor(handler, getSvcId(handler, srvcIntf),
                context.rpcName(), RpcMessageId.generate(), input));
    }

    /**
     * Auxiliary store delegate to receive notification about changes in the store.
     */
    private class InternalStoreDelegate implements DynamicConfigStoreDelegate {
        @Override
        public void notify(DynamicConfigEvent event) {
            post(event);
        }
    }
}