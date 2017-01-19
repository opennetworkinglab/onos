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
package org.onosproject.vtnrsc.virtualport.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.event.AbstractListenerManager;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.EventuallyConsistentMapEvent;
import org.onosproject.store.service.EventuallyConsistentMapListener;
import org.onosproject.store.service.MultiValuedTimestamp;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.WallClockTimestamp;
import org.onosproject.vtnrsc.AllowedAddressPair;
import org.onosproject.vtnrsc.BindingHostId;
import org.onosproject.vtnrsc.DefaultFloatingIp;
import org.onosproject.vtnrsc.DefaultVirtualPort;
import org.onosproject.vtnrsc.FixedIp;
import org.onosproject.vtnrsc.FloatingIp;
import org.onosproject.vtnrsc.FloatingIpId;
import org.onosproject.vtnrsc.RouterId;
import org.onosproject.vtnrsc.SecurityGroup;
import org.onosproject.vtnrsc.SubnetId;
import org.onosproject.vtnrsc.TenantId;
import org.onosproject.vtnrsc.TenantNetwork;
import org.onosproject.vtnrsc.TenantNetworkId;
import org.onosproject.vtnrsc.TenantRouter;
import org.onosproject.vtnrsc.VirtualPort;
import org.onosproject.vtnrsc.VirtualPortId;
import org.onosproject.vtnrsc.tenantnetwork.TenantNetworkService;
import org.onosproject.vtnrsc.virtualport.VirtualPortEvent;
import org.onosproject.vtnrsc.virtualport.VirtualPortListener;
import org.onosproject.vtnrsc.virtualport.VirtualPortService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides implementation of the VirtualPort APIs.
 */
@Component(immediate = true)
@Service
public class VirtualPortManager extends AbstractListenerManager<VirtualPortEvent, VirtualPortListener>
implements VirtualPortService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String VIRTUALPORT = "vtn-virtual-port-store";
    private static final String VTNRSC_APP = "org.onosproject.vtnrsc";

    private static final String VIRTUALPORT_ID_NULL = "VirtualPort ID cannot be null";
    private static final String VIRTUALPORT_NOT_NULL = "VirtualPort  cannot be null";
    private static final String TENANTID_NOT_NULL = "TenantId  cannot be null";
    private static final String NETWORKID_NOT_NULL = "NetworkId  cannot be null";
    private static final String DEVICEID_NOT_NULL = "DeviceId  cannot be null";
    private static final String FIXEDIP_NOT_NULL = "FixedIp  cannot be null";
    private static final String MAC_NOT_NULL = "Mac address  cannot be null";
    private static final String IP_NOT_NULL = "Ip  cannot be null";
    private static final String EVENT_NOT_NULL = "event cannot be null";

    protected EventuallyConsistentMap<VirtualPortId, VirtualPort> vPortStore;
    protected ApplicationId appId;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TenantNetworkService networkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    private EventuallyConsistentMapListener<VirtualPortId, VirtualPort> virtualPortListener =
            new InnerVirtualPortStoreListener();

    @Activate
    public void activate() {

        appId = coreService.registerApplication(VTNRSC_APP);

        eventDispatcher.addSink(VirtualPortEvent.class, listenerRegistry);

        KryoNamespace.Builder serializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API)
                .register(MultiValuedTimestamp.class)
                .register(TenantNetworkId.class)
                .register(Host.class)
                .register(TenantNetwork.class)
                .register(TenantNetworkId.class)
                .register(TenantId.class)
                .register(SubnetId.class)
                .register(VirtualPortId.class)
                .register(VirtualPort.State.class)
                .register(AllowedAddressPair.class)
                .register(FixedIp.class)
                .register(FloatingIp.class)
                .register(FloatingIpId.class)
                .register(FloatingIp.Status.class)
                .register(UUID.class)
                .register(DefaultFloatingIp.class)
                .register(BindingHostId.class)
                .register(SecurityGroup.class)
                .register(IpAddress.class)
                .register(DefaultVirtualPort.class)
                .register(RouterId.class)
                .register(TenantRouter.class)
                .register(VirtualPort.class);
        vPortStore = storageService
                .<VirtualPortId, VirtualPort>eventuallyConsistentMapBuilder()
                .withName(VIRTUALPORT).withSerializer(serializer)
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .build();

        vPortStore.addListener(virtualPortListener);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        vPortStore.removeListener(virtualPortListener);
        vPortStore.destroy();
        log.info("Stoppped");
    }

    @Override
    public boolean exists(VirtualPortId vPortId) {
        checkNotNull(vPortId, VIRTUALPORT_ID_NULL);
        return vPortStore.containsKey(vPortId);
    }

    @Override
    public VirtualPort getPort(VirtualPortId vPortId) {
        checkNotNull(vPortId, VIRTUALPORT_ID_NULL);
        return vPortStore.get(vPortId);
    }

    @Override
    public VirtualPort getPort(FixedIp fixedIP) {
        checkNotNull(fixedIP, FIXEDIP_NOT_NULL);
        List<VirtualPort> vPorts = new ArrayList<>();
        vPortStore.values().forEach(p -> {
            Iterator<FixedIp> fixedIps = p.fixedIps().iterator();
            while (fixedIps.hasNext()) {
                if (fixedIps.next().equals(fixedIP)) {
                    vPorts.add(p);
                    break;
                }
            }
        });
        if (vPorts.isEmpty()) {
            return null;
        }
        return vPorts.get(0);
    }

    @Override
    public VirtualPort getPort(MacAddress mac) {
        checkNotNull(mac, MAC_NOT_NULL);
        List<VirtualPort> vPorts = new ArrayList<>();
        vPortStore.values().forEach(p -> {
            if (p.macAddress().equals(mac)) {
                vPorts.add(p);
            }
        });
        if (vPorts.isEmpty()) {
            return null;
        }
        return vPorts.get(0);
    }

    @Override
    public VirtualPort getPort(TenantNetworkId networkId, IpAddress ip) {
        checkNotNull(networkId, NETWORKID_NOT_NULL);
        checkNotNull(ip, IP_NOT_NULL);
        List<VirtualPort> vPorts = new ArrayList<>();
        vPortStore.values().stream().filter(p -> p.networkId().equals(networkId))
                .forEach(p -> {
                    Iterator<FixedIp> fixedIps = p.fixedIps().iterator();
                    while (fixedIps.hasNext()) {
                        if (fixedIps.next().ip().equals(ip)) {
                            vPorts.add(p);
                            break;
                        }
                    }
                });
        if (vPorts.isEmpty()) {
            return null;
        }
        return vPorts.get(0);
    }

    @Override
    public Collection<VirtualPort> getPorts() {
        return Collections.unmodifiableCollection(vPortStore.values());
    }

    @Override
    public Collection<VirtualPort> getPorts(TenantNetworkId networkId) {
        checkNotNull(networkId, NETWORKID_NOT_NULL);
        return vPortStore.values().stream().filter(d -> d.networkId().equals(networkId))
                .collect(Collectors.toList());
    }

    @Override
    public Collection<VirtualPort> getPorts(TenantId tenantId) {
        checkNotNull(tenantId, TENANTID_NOT_NULL);
        return vPortStore.values().stream().filter(d -> d.tenantId().equals(tenantId))
                .collect(Collectors.toList());
    }

    @Override
    public Collection<VirtualPort> getPorts(DeviceId deviceId) {
        checkNotNull(deviceId, DEVICEID_NOT_NULL);
        return vPortStore.values().stream().filter(d -> d.deviceId().equals(deviceId))
                .collect(Collectors.toList());
    }

    @Override
    public boolean createPorts(Iterable<VirtualPort> vPorts) {
        checkNotNull(vPorts, VIRTUALPORT_NOT_NULL);
        for (VirtualPort vPort : vPorts) {
            log.debug("vPortId is  {} ", vPort.portId().toString());
            vPortStore.put(vPort.portId(), vPort);
            if (!vPortStore.containsKey(vPort.portId())) {
                log.debug("The virtualPort is created failed whose identifier is {} ",
                          vPort.portId().toString());
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean updatePorts(Iterable<VirtualPort> vPorts) {
        checkNotNull(vPorts, VIRTUALPORT_NOT_NULL);
        for (VirtualPort vPort : vPorts) {
            vPortStore.put(vPort.portId(), vPort);
            if (!vPortStore.containsKey(vPort.portId())) {
                log.debug("The virtualPort is not exist whose identifier is {}",
                          vPort.portId().toString());
                return false;
            }

            vPortStore.put(vPort.portId(), vPort);

            if (!vPort.equals(vPortStore.get(vPort.portId()))) {
                log.debug("The virtualPort is updated failed whose  identifier is {}",
                          vPort.portId().toString());
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean removePorts(Iterable<VirtualPortId> vPortIds) {
        checkNotNull(vPortIds, VIRTUALPORT_ID_NULL);
        for (VirtualPortId vPortId : vPortIds) {
            vPortStore.remove(vPortId);
            if (vPortStore.containsKey(vPortId)) {
                log.debug("The virtualPort is removed failed whose identifier is {}",
                          vPortId.toString());
                return false;
            }
        }
        return true;
    }

    private class InnerVirtualPortStoreListener
    implements
    EventuallyConsistentMapListener<VirtualPortId, VirtualPort> {

        @Override
        public void event(EventuallyConsistentMapEvent<VirtualPortId, VirtualPort> event) {
            checkNotNull(event, EVENT_NOT_NULL);
            log.info("virtual port event raised");
            VirtualPort virtualPort = event.value();
            if (EventuallyConsistentMapEvent.Type.PUT == event.type()) {
                notifyListeners(new VirtualPortEvent(
                                                     VirtualPortEvent.Type.VIRTUAL_PORT_PUT,
                                                     virtualPort));
            }
            if (EventuallyConsistentMapEvent.Type.REMOVE == event.type()) {
                notifyListeners(new VirtualPortEvent(
                                                     VirtualPortEvent.Type.VIRTUAL_PORT_DELETE,
                                                     virtualPort));
            }
        }
    }

    /**
     * Notifies specify event to all listeners.
     *
     * @param event virtual port event
     */
    private void notifyListeners(VirtualPortEvent event) {
        checkNotNull(event, EVENT_NOT_NULL);
        post(event);
    }
}
