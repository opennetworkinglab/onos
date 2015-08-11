/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.app.vtnrsc.virtualport.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.Collections;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoNamespace;
import org.onosproject.net.DeviceId;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.MultiValuedTimestamp;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.WallClockTimestamp;
import org.onosproject.app.vtnrsc.TenantId;
import org.onosproject.app.vtnrsc.TenantNetworkId;
import org.onosproject.app.vtnrsc.VirtualPort;
import org.onosproject.app.vtnrsc.VirtualPortId;
import org.onosproject.app.vtnrsc.tenantnetwork.TenantNetworkService;
import org.onosproject.app.vtnrsc.virtualport.VirtualPortService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides implementation of the VirtualPort APIs.
 */
@Component(immediate = true)
@Service
public class VirtualPortManager implements VirtualPortService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String VIRTUALPORT_ID_NULL = "VirtualPort ID cannot be null";
    private static final String VIRTUALPORT_NOT_NULL = "VirtualPort  cannot be null";
    private static final String TENANTID_NOT_NULL = "TenantId  cannot be null";
    private static final String NETWORKID_NOT_NULL = "NetworkId  cannot be null";
    private static final String DEVICEID_NOT_NULL = "DeviceId  cannot be null";

    private EventuallyConsistentMap<VirtualPortId, VirtualPort> vPortStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TenantNetworkService networkService;

    @Activate
    public void activate() {
        KryoNamespace.Builder seriallizer = KryoNamespace.newBuilder()
                .register(MultiValuedTimestamp.class);
        vPortStore = storageService
                .<VirtualPortId, VirtualPort>eventuallyConsistentMapBuilder()
                .withName("vPortId_vPort").withSerializer(seriallizer)
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .build();
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
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
    public Collection<VirtualPort> getPorts() {
        return Collections.unmodifiableCollection(vPortStore.values());
    }

    @Override
    public Collection<VirtualPort> getPorts(TenantNetworkId networkId) {
        checkNotNull(networkId, NETWORKID_NOT_NULL);
        Collection<VirtualPort> vPortWithNetworkIds = vPortStore.values();
        for (VirtualPort vPort : vPortWithNetworkIds) {
            if (!vPort.networkId().equals(networkId)) {
                vPortWithNetworkIds.remove(vPort);
            }
        }
        return vPortWithNetworkIds;
    }

    @Override
    public Collection<VirtualPort> getPorts(TenantId tenantId) {
        checkNotNull(tenantId, TENANTID_NOT_NULL);
        Collection<VirtualPort> vPortWithTenantIds = vPortStore.values();
        for (VirtualPort vPort : vPortWithTenantIds) {
            if (!vPort.tenantId().equals(tenantId)) {
                vPortWithTenantIds.remove(vPort);
            }
        }
        return vPortWithTenantIds;
    }

    @Override
    public Collection<VirtualPort> getPorts(DeviceId deviceId) {
        checkNotNull(deviceId, DEVICEID_NOT_NULL);
        Collection<VirtualPort> vPortWithDeviceIds = vPortStore.values();
        for (VirtualPort vPort : vPortWithDeviceIds) {
            if (!vPort.deviceId().equals(deviceId)) {
                vPortWithDeviceIds.remove(vPort);
            }
        }
        return vPortWithDeviceIds;
    }

    @Override
    public boolean createPorts(Iterable<VirtualPort> vPorts) {
        checkNotNull(vPorts, VIRTUALPORT_NOT_NULL);
        for (VirtualPort vPort : vPorts) {
            log.debug("vPortId is  {} ", vPort.portId().toString());
            vPortStore.put(vPort.portId(), vPort);
            if (!vPortStore.containsKey(vPort.portId())) {
                log.debug("the virtualPort created failed whose identifier was {} ",
                          vPort.portId().toString());
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean updatePorts(Iterable<VirtualPort> vPorts) {
        checkNotNull(vPorts, VIRTUALPORT_NOT_NULL);
        if (vPorts != null) {
            for (VirtualPort vPort : vPorts) {
                vPortStore.put(vPort.portId(), vPort);
                if (!vPortStore.containsKey(vPort.portId())) {
                    log.debug("the virtualPort  did not exist whose identifier was {}",
                              vPort.portId().toString());
                    return false;
                }

                vPortStore.put(vPort.portId(), vPort);

                if (!vPort.equals(vPortStore.get(vPort.portId()))) {
                    log.debug("the virtualPort updated failed whose  identifier was {}",
                              vPort.portId().toString());
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean removePorts(Iterable<VirtualPortId> vPortIds) {
        checkNotNull(vPortIds, VIRTUALPORT_ID_NULL);
        if (vPortIds != null) {
            for (VirtualPortId vPortId : vPortIds) {
                vPortStore.remove(vPortId);
                if (vPortStore.containsKey(vPortId)) {
                    log.debug("the virtualPort removed failed whose identifier was {}",
                              vPortId.toString());
                    return false;
                }
            }
        }
        return true;
    }

}
