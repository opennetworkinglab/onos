/*
 * Copyright 2020-present Open Networking Foundation
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
package org.onosproject.k8snode.impl;

import org.apache.commons.net.util.SubnetUtils;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.k8snode.api.ExternalNetworkService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.k8snode.api.K8sNodeService.APP_ID;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * External network service implementation.
 */
@Component(
        immediate = true,
        service = { ExternalNetworkService.class }
)
public class ExternalNetworkManager implements ExternalNetworkService {

    private final Logger log = getLogger(getClass());

    private static final KryoNamespace
            SERIALIZER_EXTERNAL_NETWORK = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .build();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));

    private ConsistentMap<String, Set<String>> networkIpPool;

    @Activate
    protected void activate() {
        ApplicationId appId = coreService.registerApplication(APP_ID);
        networkIpPool = storageService.<String, Set<String>>consistentMapBuilder()
                .withSerializer(Serializer.using(SERIALIZER_EXTERNAL_NETWORK))
                .withName("external-network-ip-pool")
                .withApplicationId(appId)
                .build();
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        eventExecutor.shutdown();
        log.info("Stopped");
    }

    @Override
    public void registerNetwork(IpPrefix cidr) {
        if (!networkIpPool.containsKey(cidr.toString())) {
            SubnetUtils utils = new SubnetUtils(cidr.toString());
            utils.setInclusiveHostCount(false);
            SubnetUtils.SubnetInfo info = utils.getInfo();

            Set<String> all = Arrays.stream(info.getAllAddresses())
                    .collect(Collectors.toSet());
            all.remove(info.getNetworkAddress());
            all.remove(info.getHighAddress());
            all.remove(info.getLowAddress());
            all.remove(info.getBroadcastAddress());

            networkIpPool.put(cidr.toString(), all);
        }
    }

    @Override
    public void unregisterNetwork(IpPrefix cidr) {
        if (!networkIpPool.containsKey(cidr.toString())) {
            log.warn("The given network {} is not found!", cidr.toString());
        } else {
            networkIpPool.remove(cidr.toString());
        }
    }

    @Override
    public IpAddress getGatewayIp(IpPrefix cidr) {
        SubnetUtils utils = new SubnetUtils(cidr.toString());
        utils.setInclusiveHostCount(false);
        SubnetUtils.SubnetInfo info = utils.getInfo();

        return IpAddress.valueOf(info.getLowAddress());
    }

    @Override
    public IpAddress allocateIp(IpPrefix cidr) {
        if (!networkIpPool.containsKey(cidr.toString())) {
            log.error("The given network {} is not found", cidr.toString());
            return null;
        } else {
            Set<String> pool = networkIpPool.get(cidr.toString()).value();
            String ipStr = pool.stream().findFirst().orElse(null);
            if (ipStr == null) {
                log.error("No IPs are found in the given network {}", cidr.toString());
                return null;
            }

            pool.remove(ipStr);
            networkIpPool.put(cidr.toString(), pool);
            return IpAddress.valueOf(ipStr);
        }
    }

    @Override
    public void releaseIp(IpPrefix cidr, IpAddress ip) {
        if (!networkIpPool.containsKey(cidr.toString())) {
            log.error("The given network {} is not found", cidr.toString());
        } else {
            Set<String> pool = networkIpPool.get(cidr.toString()).value();
            pool.add(ip.toString());
            networkIpPool.put(cidr.toString(), pool);
        }
    }

    @Override
    public Set<String> getAllIps(IpPrefix cidr) {
        return networkIpPool.get(cidr.toString()).value();
    }
}
