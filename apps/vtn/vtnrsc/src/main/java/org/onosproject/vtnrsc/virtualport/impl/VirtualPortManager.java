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
package org.onosproject.vtnrsc.virtualport.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.IpAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.vtnrsc.AllowedAddressPair;
import org.onosproject.vtnrsc.BindingHostId;
import org.onosproject.vtnrsc.DefaultVirtualPort;
import org.onosproject.vtnrsc.FixedIp;
import org.onosproject.vtnrsc.SecurityGroup;
import org.onosproject.vtnrsc.SubnetId;
import org.onosproject.vtnrsc.TenantId;
import org.onosproject.vtnrsc.TenantNetworkId;
import org.onosproject.vtnrsc.VirtualPort;
import org.onosproject.vtnrsc.VirtualPortId;
import org.onosproject.vtnrsc.tenantnetwork.TenantNetworkService;
import org.onosproject.vtnrsc.virtualport.VirtualPortService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides implementation of the VirtualPort APIs.
 */
@Component(immediate = true)
@Service
public class VirtualPortManager implements VirtualPortService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String VIRTUALPORT = "vtn-virtual-port";
    private static final String VTNRSC_APP = "org.onosproject.vtnrsc";

    private static final String VIRTUALPORT_ID_NULL = "VirtualPort ID cannot be null";
    private static final String VIRTUALPORT_NOT_NULL = "VirtualPort  cannot be null";
    private static final String TENANTID_NOT_NULL = "TenantId  cannot be null";
    private static final String NETWORKID_NOT_NULL = "NetworkId  cannot be null";
    private static final String DEVICEID_NOT_NULL = "DeviceId  cannot be null";

    protected Map<VirtualPortId, VirtualPort> vPortStore;
    protected ApplicationId appId;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TenantNetworkService networkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Activate
    public void activate() {

        appId = coreService.registerApplication(VTNRSC_APP);

        vPortStore = storageService.<VirtualPortId, VirtualPort>consistentMapBuilder()
                .withName(VIRTUALPORT)
                .withApplicationId(appId)
                .withPurgeOnUninstall()
                .withSerializer(Serializer.using(Arrays.asList(KryoNamespaces.API),
                                                 VirtualPortId.class,
                                                 TenantNetworkId.class,
                                                 VirtualPort.State.class,
                                                 TenantId.class,
                                                 AllowedAddressPair.class,
                                                 FixedIp.class,
                                                 BindingHostId.class,
                                                 SecurityGroup.class,
                                                 SubnetId.class,
                                                 IpAddress.class,
                                                 DefaultVirtualPort.class))
                .build().asJavaMap();
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        vPortStore.clear();
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
        if (vPorts != null) {
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
                    log.debug("The virtualPort is removed failed whose identifier is {}",
                              vPortId.toString());
                    return false;
                }
            }
        }
        return true;
    }

}
