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
package org.onosproject.vtnrsc.floatingip.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.IpAddress;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.EventuallyConsistentMapEvent;
import org.onosproject.store.service.EventuallyConsistentMapListener;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.WallClockTimestamp;
import org.onosproject.vtnrsc.DefaultFloatingIp;
import org.onosproject.vtnrsc.FloatingIp;
import org.onosproject.vtnrsc.FloatingIpId;
import org.onosproject.vtnrsc.TenantId;
import org.onosproject.vtnrsc.TenantNetworkId;
import org.onosproject.vtnrsc.VirtualPortId;
import org.onosproject.vtnrsc.RouterId;
import org.onosproject.vtnrsc.floatingip.FloatingIpEvent;
import org.onosproject.vtnrsc.floatingip.FloatingIpListener;
import org.onosproject.vtnrsc.floatingip.FloatingIpService;
import org.onosproject.vtnrsc.router.RouterService;
import org.onosproject.vtnrsc.tenantnetwork.TenantNetworkService;
import org.onosproject.vtnrsc.virtualport.VirtualPortService;
import org.slf4j.Logger;

import com.google.common.collect.Sets;

/**
 * Provides implementation of the FloatingIp service.
 */
@Component(immediate = true)
@Service
public class FloatingIpManager implements FloatingIpService {
    private static final String FLOATINGIP_ID_NOT_NULL = "Floatingip ID cannot be null";
    private static final String FLOATINGIP_NOT_NULL = "Floatingip cannot be null";
    private static final String FLOATINGIPSTORE = "vtn-floatingip-store";
    private static final String FLOATINGIPBINDSTORE = "vtn-floatingip-bind-store";
    private static final String VTNRSC_APP = "org.onosproject.vtnrsc";
    private static final String LISTENER_NOT_NULL = "Listener cannot be null";
    private static final String EVENT_NOT_NULL = "event cannot be null";

    private final Logger log = getLogger(getClass());
    private final Set<FloatingIpListener> listeners = Sets
            .newCopyOnWriteArraySet();
    private EventuallyConsistentMapListener<FloatingIpId, FloatingIp> floatingIpListener =
            new InnerFloatingIpStoreListener();
    protected EventuallyConsistentMap<FloatingIpId, FloatingIp> floatingIpStore;
    protected EventuallyConsistentMap<FloatingIpId, FloatingIp> floatingIpBindStore;
    protected ApplicationId appId;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TenantNetworkService tenantNetworkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected VirtualPortService virtualPortService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected RouterService routerService;

    @Activate
    public void activate() {
        appId = coreService.registerApplication(VTNRSC_APP);
        KryoNamespace.Builder serializer = KryoNamespace
                .newBuilder()
                .register(KryoNamespaces.API)
                .register(FloatingIp.class, FloatingIpId.class,
                          TenantNetworkId.class, TenantId.class,
                          FloatingIp.Status.class, RouterId.class,
                          VirtualPortId.class, DefaultFloatingIp.class,
                          UUID.class);
        floatingIpStore = storageService
                .<FloatingIpId, FloatingIp>eventuallyConsistentMapBuilder()
                .withName(FLOATINGIPSTORE).withSerializer(serializer)
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .build();
        floatingIpBindStore = storageService
                .<FloatingIpId, FloatingIp>eventuallyConsistentMapBuilder()
                .withName(FLOATINGIPBINDSTORE).withSerializer(serializer)
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .build();
        floatingIpStore.addListener(floatingIpListener);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        floatingIpStore.removeListener(floatingIpListener);
        floatingIpStore.destroy();
        floatingIpBindStore.destroy();
        listeners.clear();
        log.info("Stopped");
    }

    @Override
    public Collection<FloatingIp> getFloatingIps() {
        return Collections.unmodifiableCollection(floatingIpStore.values());
    }

    @Override
    public FloatingIp getFloatingIp(FloatingIpId floatingIpId) {
        checkNotNull(floatingIpId, FLOATINGIP_ID_NOT_NULL);
        return floatingIpStore.get(floatingIpId);
    }

    @Override
    public boolean exists(FloatingIpId floatingIpId) {
        checkNotNull(floatingIpId, FLOATINGIP_ID_NOT_NULL);
        return floatingIpStore.containsKey(floatingIpId);
    }

    @Override
    public boolean floatingIpIsUsed(IpAddress floatingIpAddr,
                                    FloatingIpId floatingIpId) {
        checkNotNull(floatingIpAddr, "Floating IP address cannot be null");
        checkNotNull(floatingIpId, "Floating IP Id cannot be null");
        Collection<FloatingIp> floatingIps = getFloatingIps();
        for (FloatingIp floatingIp : floatingIps) {
            if (floatingIp.floatingIp().equals(floatingIpAddr)
                    && !floatingIp.id().equals(floatingIpId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean fixedIpIsUsed(IpAddress fixedIpAddr, TenantId tenantId,
                                 FloatingIpId floatingIpId) {
        checkNotNull(fixedIpAddr, "Fixed IP address cannot be null");
        checkNotNull(tenantId, "Tenant Id cannot be null");
        checkNotNull(floatingIpId, "Floating IP Id cannot be null");
        Collection<FloatingIp> floatingIps = getFloatingIps();
        for (FloatingIp floatingIp : floatingIps) {
            IpAddress fixedIp = floatingIp.fixedIp();
            if (fixedIp != null) {
                if (fixedIp.equals(fixedIpAddr)
                        && floatingIp.tenantId().equals(tenantId)
                        && !floatingIp.id().equals(floatingIpId)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean createFloatingIps(Collection<FloatingIp> floatingIps) {
        checkNotNull(floatingIps, FLOATINGIP_NOT_NULL);
        boolean result = true;
        for (FloatingIp floatingIp : floatingIps) {
            verifyFloatingIpData(floatingIp);
            floatingIpStore.put(floatingIp.id(), floatingIp);
            if (!floatingIpStore.containsKey(floatingIp.id())) {
                log.debug("The floating Ip is created failed whose identifier is {}",
                          floatingIp.id().toString());
                result = false;
            }
        }
        return result;
    }

    @Override
    public boolean updateFloatingIps(Collection<FloatingIp> floatingIps) {
        checkNotNull(floatingIps, FLOATINGIP_NOT_NULL);
        boolean result = true;
        for (FloatingIp floatingIp : floatingIps) {
            verifyFloatingIpData(floatingIp);
            FloatingIp oldFloatingIp = floatingIpStore.get(floatingIp.id());
            floatingIpBindStore.put(floatingIp.id(), oldFloatingIp);
            floatingIpStore.put(floatingIp.id(), floatingIp);
            if (!floatingIpStore.containsKey(floatingIp.id())) {
                log.debug("The floating Ip is updated failed whose identifier is {}",
                          floatingIp.id().toString());
                result = false;
            }
        }
        return result;
    }

    @Override
    public boolean removeFloatingIps(Collection<FloatingIpId> floatingIpIds) {
        checkNotNull(floatingIpIds, FLOATINGIP_ID_NOT_NULL);
        boolean result = true;
        for (FloatingIpId floatingIpId : floatingIpIds) {
            if (!floatingIpStore.containsKey(floatingIpId)) {
                log.debug("The floatingIp is not exist whose identifier is {}",
                          floatingIpId.toString());
                throw new IllegalArgumentException(
                                                   "FloatingIP ID doesn't exist");
            }
            FloatingIp floatingIp = floatingIpStore.get(floatingIpId);
            floatingIpStore.remove(floatingIpId, floatingIp);
            floatingIpBindStore.remove(floatingIpId);
            if (floatingIpStore.containsKey(floatingIpId)) {
                log.debug("The floating Ip is deleted failed whose identifier is {}",
                          floatingIpId.toString());
                result = false;
            }
        }
        return result;
    }

    @Override
    public void addListener(FloatingIpListener listener) {
        checkNotNull(listener, LISTENER_NOT_NULL);
        listeners.add(listener);
    }

    @Override
    public void removeListener(FloatingIpListener listener) {
        checkNotNull(listener, LISTENER_NOT_NULL);
        listeners.remove(listener);
    }

    /**
     * Verifies validity of FloatingIp data.
     *
     * @param floatingIps floatingIp instance
     */
    private void verifyFloatingIpData(FloatingIp floatingIps) {
        checkNotNull(floatingIps, FLOATINGIP_NOT_NULL);
        if (!tenantNetworkService.exists(floatingIps.networkId())) {
            log.debug("The network identifier {} that the floating Ip {} create for is not exist",
                      floatingIps.networkId().toString(), floatingIps.id()
                              .toString());
            throw new IllegalArgumentException(
                                               "Floating network ID doesn't exist");
        }

        VirtualPortId portId = floatingIps.portId();
        if (portId != null && !virtualPortService.exists(portId)) {
            log.debug("The port identifier {} that the floating Ip {} create for is not exist",
                      floatingIps.portId().toString(), floatingIps.id()
                              .toString());
            throw new IllegalArgumentException("Port ID doesn't exist");
        }

        RouterId routerId = floatingIps.routerId();
        if (routerId != null && !routerService.exists(routerId)) {
            log.debug("The router identifier {} that the floating Ip {} create for is not exist",
                      floatingIps.routerId().toString(), floatingIps.id()
                              .toString());
            throw new IllegalArgumentException("Router ID doesn't exist");
        }

        if (floatingIpIsUsed(floatingIps.floatingIp(), floatingIps.id())) {
            log.debug("The floaing Ip {} that the floating Ip {} create for is used",
                      floatingIps.floatingIp().toString(), floatingIps.id()
                              .toString());
            throw new IllegalArgumentException(
                                               "The floating IP address is used");
        }

        IpAddress fixedIp = floatingIps.fixedIp();
        if (fixedIp != null
                && fixedIpIsUsed(fixedIp, floatingIps.tenantId(),
                                 floatingIps.id())) {
            log.debug("The fixed Ip {} that the floating Ip {} create for is used",
                      floatingIps.fixedIp().toString(), floatingIps.id()
                              .toString());
            throw new IllegalArgumentException("The fixed IP address is used");
        }
    }

    private class InnerFloatingIpStoreListener
            implements
            EventuallyConsistentMapListener<FloatingIpId, FloatingIp> {

        @Override
        public void event(EventuallyConsistentMapEvent<FloatingIpId, FloatingIp> event) {
            checkNotNull(event, EVENT_NOT_NULL);
            FloatingIp floatingIp = event.value();
            if (EventuallyConsistentMapEvent.Type.PUT == event.type()) {
                notifyListeners(new FloatingIpEvent(
                                                    FloatingIpEvent.Type.FLOATINGIP_PUT,
                                                    floatingIp));
                if (floatingIp.portId() != null) {
                    notifyListeners(new FloatingIpEvent(
                                                        FloatingIpEvent.Type.FLOATINGIP_BIND,
                                                        floatingIp));
                } else {
                    FloatingIp oldFloatingIp = floatingIpBindStore.get(floatingIp.id());
                    if (oldFloatingIp != null) {
                        notifyListeners(new FloatingIpEvent(
                                                            FloatingIpEvent.Type.FLOATINGIP_UNBIND,
                                                            oldFloatingIp));
                    }
                }
            }
            if (EventuallyConsistentMapEvent.Type.REMOVE == event.type()) {
                notifyListeners(new FloatingIpEvent(
                                                    FloatingIpEvent.Type.FLOATINGIP_DELETE,
                                                    floatingIp));
                if (floatingIp.portId() != null) {
                    notifyListeners(new FloatingIpEvent(
                                                        FloatingIpEvent.Type.FLOATINGIP_UNBIND,
                                                        floatingIp));
                }
            }
        }
    }

    /**
     * Notifies specify event to all listeners.
     *
     * @param event Floating IP event
     */
    private void notifyListeners(FloatingIpEvent event) {
        checkNotNull(event, EVENT_NOT_NULL);
        listeners.forEach(listener -> listener.event(event));
    }
}
