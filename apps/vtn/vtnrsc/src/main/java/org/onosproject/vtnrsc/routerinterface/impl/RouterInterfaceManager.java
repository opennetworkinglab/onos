/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.vtnrsc.routerinterface.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.EventuallyConsistentMapEvent;
import org.onosproject.store.service.EventuallyConsistentMapListener;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.WallClockTimestamp;
import org.onosproject.vtnrsc.RouterId;
import org.onosproject.vtnrsc.RouterInterface;
import org.onosproject.vtnrsc.SubnetId;
import org.onosproject.vtnrsc.TenantId;
import org.onosproject.vtnrsc.VirtualPortId;
import org.onosproject.vtnrsc.router.RouterService;
import org.onosproject.vtnrsc.routerinterface.RouterInterfaceEvent;
import org.onosproject.vtnrsc.routerinterface.RouterInterfaceListener;
import org.onosproject.vtnrsc.routerinterface.RouterInterfaceService;
import org.onosproject.vtnrsc.subnet.SubnetService;
import org.onosproject.vtnrsc.virtualport.VirtualPortService;
import org.slf4j.Logger;

import com.google.common.collect.Sets;

/**
 * Provides implementation of the Router interface service.
 */
@Component(immediate = true)
@Service
public class RouterInterfaceManager implements RouterInterfaceService {
    private static final String SUBNET_ID_NULL = "Subnet ID cannot be null";
    private static final String ROUTER_INTERFACE_NULL = "Router Interface cannot be null";
    private static final String ROUTER_INTERFACE = "vtn-router-interface-store";
    private static final String VTNRSC_APP = "org.onosproject.vtnrsc";
    private static final String LISTENER_NOT_NULL = "Listener cannot be null";
    private static final String EVENT_NOT_NULL = "event cannot be null";

    private final Logger log = getLogger(getClass());
    private final Set<RouterInterfaceListener> listeners = Sets
            .newCopyOnWriteArraySet();
    private EventuallyConsistentMapListener<SubnetId, RouterInterface> routerInterfaceListener =
            new InnerRouterInterfaceStoreListener();
    protected EventuallyConsistentMap<SubnetId, RouterInterface> routerInterfaceStore;
    protected ApplicationId appId;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected VirtualPortService virtualPortService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected SubnetService subnetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected RouterService routerService;

    @Activate
    public void activate() {
        appId = coreService.registerApplication(VTNRSC_APP);
        KryoNamespace.Builder serializer = KryoNamespace
                .newBuilder()
                .register(KryoNamespaces.API)
                .register(RouterId.class, TenantId.class, VirtualPortId.class,
                          RouterInterface.class, SubnetId.class);
        routerInterfaceStore = storageService
                .<SubnetId, RouterInterface>eventuallyConsistentMapBuilder()
                .withName(ROUTER_INTERFACE).withSerializer(serializer)
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .build();
        routerInterfaceStore.addListener(routerInterfaceListener);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        routerInterfaceStore.removeListener(routerInterfaceListener);
        routerInterfaceStore.destroy();
        listeners.clear();
        log.info("Stopped");
    }

    @Override
    public boolean exists(SubnetId subnetId) {
        checkNotNull(subnetId, SUBNET_ID_NULL);
        return routerInterfaceStore.containsKey(subnetId);
    }

    @Override
    public Collection<RouterInterface> getRouterInterfaces() {
        return Collections
                .unmodifiableCollection(routerInterfaceStore.values());
    }

    @Override
    public RouterInterface getRouterInterface(SubnetId subnetId) {
        checkNotNull(subnetId, SUBNET_ID_NULL);
        return routerInterfaceStore.get(subnetId);
    }

    @Override
    public boolean addRouterInterface(RouterInterface routerInterface) {
        checkNotNull(routerInterface, ROUTER_INTERFACE_NULL);
        if (!virtualPortService.exists(routerInterface.portId())) {
            log.debug("The port ID of interface is not exist whose identifier is {}",
                      routerInterface.portId().toString());
            throw new IllegalArgumentException(
                                               "port ID of interface doesn't exist");
        }
        verifyRouterInterfaceData(routerInterface);
        routerInterfaceStore.put(routerInterface.subnetId(), routerInterface);
        if (!routerInterfaceStore.containsKey(routerInterface.subnetId())) {
            log.debug("The router interface is created failed whose identifier is {}",
                      routerInterface.subnetId().toString());
            return false;
        }
        return true;
    }

    @Override
    public boolean removeRouterInterface(RouterInterface routerInterface) {
        checkNotNull(routerInterface, ROUTER_INTERFACE_NULL);
        if (!routerInterfaceStore.containsKey(routerInterface.subnetId())) {
            log.debug("The router interface is not exist whose identifier is {}",
                      routerInterface.subnetId().toString());
            throw new IllegalArgumentException("subnet ID doesn't exist");
        }
        verifyRouterInterfaceData(routerInterface);
        routerInterfaceStore
                .remove(routerInterface.subnetId(), routerInterface);
        if (routerInterfaceStore.containsKey(routerInterface.subnetId())) {
            log.debug("The router interface deleted is failed whose identifier is {}",
                      routerInterface.subnetId().toString());
            return false;
        }
        return true;
    }

    @Override
    public void addListener(RouterInterfaceListener listener) {
        checkNotNull(listener, LISTENER_NOT_NULL);
        listeners.add(listener);
    }

    @Override
    public void removeListener(RouterInterfaceListener listener) {
        checkNotNull(listener, LISTENER_NOT_NULL);
        listeners.remove(listener);
    }

    /**
     * Verifies validity of Router interface data.
     *
     * @param routers router instance
     */
    private void verifyRouterInterfaceData(RouterInterface routerInterface) {
        checkNotNull(routerInterface, ROUTER_INTERFACE_NULL);
        if (!subnetService.exists(routerInterface.subnetId())) {
            log.debug("The subnet ID of interface is not exist whose identifier is {}",
                      routerInterface.subnetId().toString());
            throw new IllegalArgumentException(
                                               "subnet ID of interface doesn't exist");
        }
        if (!routerService.exists(routerInterface.routerId())) {
            log.debug("The router ID of interface is not exist whose identifier is {}",
                      routerInterface.routerId().toString());
            throw new IllegalArgumentException(
                                               "router ID of interface doesn't exist");
        }
    }

    private class InnerRouterInterfaceStoreListener
            implements
            EventuallyConsistentMapListener<SubnetId, RouterInterface> {

        @Override
        public void event(EventuallyConsistentMapEvent<SubnetId, RouterInterface> event) {
            checkNotNull(event, EVENT_NOT_NULL);
            RouterInterface routerInterface = event.value();
            if (EventuallyConsistentMapEvent.Type.PUT == event.type()) {
                notifyListeners(new RouterInterfaceEvent(
                                                         RouterInterfaceEvent.Type.ROUTER_INTERFACE_PUT,
                                                         routerInterface));
            }
            if (EventuallyConsistentMapEvent.Type.REMOVE == event.type()) {
                notifyListeners(new RouterInterfaceEvent(
                                                         RouterInterfaceEvent.Type.ROUTER_INTERFACE_DELETE,
                                                         routerInterface));
            }
        }
    }

    /**
     * Notifies specify event to all listeners.
     *
     * @param event Floating IP event
     */
    private void notifyListeners(RouterInterfaceEvent event) {
        checkNotNull(event, EVENT_NOT_NULL);
        listeners.forEach(listener -> listener.event(event));
    }
}
