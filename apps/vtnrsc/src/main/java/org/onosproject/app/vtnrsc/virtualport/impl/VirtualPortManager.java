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

import java.util.Collection;
import java.util.Collections;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoNamespace;
import org.onosproject.app.vtnrsc.TenantId;
import org.onosproject.app.vtnrsc.TenantNetworkId;
import org.onosproject.app.vtnrsc.VirtualPort;
import org.onosproject.app.vtnrsc.VirtualPortId;
import org.onosproject.app.vtnrsc.tenantnetwork.TenantNetworkService;
import org.onosproject.app.vtnrsc.virtualport.VirtualPortService;
import org.onosproject.net.DeviceId;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.MultiValuedTimestamp;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.WallClockTimestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides implementation of the VirtualPort APIs.
 */
@Component(immediate = true)
@Service
public class VirtualPortManager implements VirtualPortService {
    private final Logger log = LoggerFactory.getLogger(getClass());

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
        return vPortStore.containsKey(vPortId);
    }

    @Override
    public VirtualPort getPort(VirtualPortId vPortId) {
        if (!exists(vPortId)) {
            return null;
        }
        return vPortStore.get(vPortId);
    }

    @Override
    public Collection<VirtualPort> getPorts() {
        return Collections.unmodifiableCollection(vPortStore.values());
    }

    @Override
    public Collection<VirtualPort> getPorts(TenantNetworkId networkId) {
        Collection<VirtualPort> vPortWithNetworkId =
            Collections.unmodifiableCollection(vPortStore.values());
        if (networkId == null || !networkService.exists(networkId)) {
            return null;
        }
        for (VirtualPort  vPort : vPortWithNetworkId) {
            if (!vPort.networkId().equals(networkId)) {
                vPortWithNetworkId.remove(vPort);
            }
        }
        return vPortWithNetworkId;
    }

    @Override
    public Collection<VirtualPort> getPorts(TenantId tenantId) {
        Collection<VirtualPort> vPortWithTenantId =
                Collections.unmodifiableCollection(vPortStore.values());
        if (tenantId == null) {
            return null;
        }
        for (VirtualPort  vPort : vPortWithTenantId) {
            if (!vPort.tenantId().equals(tenantId)) {
                vPortWithTenantId.remove(vPort);
            }
        }
        return vPortWithTenantId;
    }

    @Override
    public Collection<VirtualPort> getPorts(DeviceId deviceId) {
        Collection<VirtualPort> vPortWithDeviceId =
                Collections.unmodifiableCollection(vPortStore.values());
        if (deviceId == null) {
            return null;
        }
        for (VirtualPort  vPort : vPortWithDeviceId) {
            if (!vPort.deviceId().equals(deviceId)) {
                vPortWithDeviceId.remove(vPort);
            }
        }
        return vPortWithDeviceId;
    }

    @Override
    public boolean createPorts(Iterable<VirtualPort> vPorts) {
        for (VirtualPort vPort:vPorts) {
            log.info("vPortId is  {} ", vPort.portId().toString());
            vPortStore.put(vPort.portId(), vPort);
        }
        return true;
    }

    @Override
    public boolean updatePorts(Iterable<VirtualPort> vPorts) {
        Boolean flag = false;
        if (vPorts != null) {
            for (VirtualPort vPort:vPorts) {
                vPortStore.put(vPort.portId(), vPort);
                flag = true;
            }
        }
        return flag;
    }

    @Override
    public boolean removePorts(Iterable<VirtualPortId> vPortIds) {
        Boolean flag = false;
        if (vPortIds != null) {
            for (VirtualPortId vPortId:vPortIds) {
                vPortStore.remove(vPortId);
                flag = true;
                log.info("The result of removing vPortId is {}", flag.toString());
            }
        }
        return flag;
    }

}
