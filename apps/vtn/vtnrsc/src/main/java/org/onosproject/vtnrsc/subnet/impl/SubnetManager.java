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
package org.onosproject.vtnrsc.subnet.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.IpAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.vtnrsc.AllocationPool;
import org.onosproject.vtnrsc.DefaultAllocationPool;
import org.onosproject.vtnrsc.DefaultHostRoute;
import org.onosproject.vtnrsc.DefaultSubnet;
import org.onosproject.vtnrsc.HostRoute;
import org.onosproject.vtnrsc.Subnet;
import org.onosproject.vtnrsc.SubnetId;
import org.onosproject.vtnrsc.TenantId;
import org.onosproject.vtnrsc.TenantNetworkId;
import org.onosproject.vtnrsc.subnet.SubnetService;
import org.onosproject.vtnrsc.tenantnetwork.TenantNetworkService;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provides implementation of the Subnet service.
 */
@Component(immediate = true)
@Service
public class SubnetManager implements SubnetService {

    private static final String SUBNET_ID_NULL = "Subnet ID cannot be null";
    private static final String SUBNET_NOT_NULL = "Subnet cannot be null";
    private static final String SUBNET = "vtn-subnet-store";
    private static final String VTNRSC_APP = "org.onosproject.vtnrsc";


    private final Logger log = getLogger(getClass());

    protected Map<SubnetId, Subnet> subnetStore;
    protected ApplicationId appId;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TenantNetworkService tenantNetworkService;

    @Activate
    public void activate() {

        appId = coreService.registerApplication(VTNRSC_APP);

        subnetStore = storageService.<SubnetId, Subnet>consistentMapBuilder()
                .withName(SUBNET)
                .withApplicationId(appId)
                .withPurgeOnUninstall()
                .withSerializer(Serializer.using(Arrays.asList(KryoNamespaces.API),
                                                 Subnet.class,
                                                 SubnetId.class,
                                                 TenantNetworkId.class,
                                                 TenantId.class,
                                                 HostRoute.class,
                                                 DefaultHostRoute.class,
                                                 Subnet.Mode.class,
                                                 AllocationPool.class,
                                                 DefaultAllocationPool.class,
                                                 DefaultSubnet.class,
                                                 IpAddress.Version.class))
                .build().asJavaMap();

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public Iterable<Subnet> getSubnets() {
        return Collections.unmodifiableCollection(subnetStore.values());
    }

    @Override
    public Subnet getSubnet(SubnetId subnetId) {
        checkNotNull(subnetId, SUBNET_ID_NULL);
        return subnetStore.get(subnetId);
    }

    @Override
    public boolean exists(SubnetId subnetId) {
        checkNotNull(subnetId, SUBNET_ID_NULL);
        return subnetStore.containsKey(subnetId);
    }

    @Override
    public boolean createSubnets(Iterable<Subnet> subnets) {
        checkNotNull(subnets, SUBNET_NOT_NULL);
        for (Subnet subnet : subnets) {
            if (!tenantNetworkService.exists(subnet.networkId())) {
                log.debug("The network identifier that the subnet {} belong to is not exist",
                          subnet.networkId().toString(), subnet.id().toString());
                return false;
            }
            subnetStore.put(subnet.id(), subnet);
            if (!subnetStore.containsKey(subnet.id())) {
                log.debug("The identified subnet whose identifier is {}  create failed",
                          subnet.id().toString());
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean updateSubnets(Iterable<Subnet> subnets) {
        checkNotNull(subnets, SUBNET_NOT_NULL);
        if (subnets != null) {
            for (Subnet subnet : subnets) {
                if (!subnetStore.containsKey(subnet.id())) {
                    log.debug("The subnet is not exist whose identifier is {}",
                              subnet.id().toString());
                    return false;
                }

                subnetStore.put(subnet.id(), subnet);

                if (!subnet.equals(subnetStore.get(subnet.id()))) {
                    log.debug("The subnet is updated failed whose identifier is {}",
                              subnet.id().toString());
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean removeSubnets(Iterable<SubnetId> subnetIds) {
        checkNotNull(subnetIds, SUBNET_ID_NULL);
        if (subnetIds != null) {
            for (SubnetId subnetId : subnetIds) {
                subnetStore.remove(subnetId);
                if (subnetStore.containsKey(subnetId)) {
                    log.debug("The subnet created is failed whose identifier is {}",
                              subnetId.toString());
                    return false;
                }
            }
        }
        return true;
    }

}
