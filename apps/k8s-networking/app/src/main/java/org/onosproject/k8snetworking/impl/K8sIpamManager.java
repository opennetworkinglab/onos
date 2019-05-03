/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.k8snetworking.impl;

import org.onlab.packet.IpAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.event.ListenerRegistry;
import org.onosproject.k8snetworking.api.DefaultK8sIpam;
import org.onosproject.k8snetworking.api.K8sIpam;
import org.onosproject.k8snetworking.api.K8sIpamAdminService;
import org.onosproject.k8snetworking.api.K8sIpamEvent;
import org.onosproject.k8snetworking.api.K8sIpamListener;
import org.onosproject.k8snetworking.api.K8sIpamService;
import org.onosproject.k8snetworking.api.K8sIpamStore;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Set;
import java.util.stream.Collectors;

import static org.onosproject.k8snetworking.api.Constants.K8S_NETWORKING_APP_ID;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provides implementation of administering and interfacing kubernetes IPAM.
 */
@Component(
        immediate = true,
        service = {K8sIpamAdminService.class, K8sIpamService.class}
)
public class K8sIpamManager
        extends ListenerRegistry<K8sIpamEvent, K8sIpamListener>
        implements K8sIpamAdminService, K8sIpamService {

    protected final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sIpamStore k8sIpamStore;

    private ApplicationId appId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(K8S_NETWORKING_APP_ID);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
    }

    @Override
    public IpAddress allocateIp(String networkId) {
        IpAddress availableIp = availableIps(networkId).stream()
                .findFirst().orElse(null);
        if (availableIp != null) {
            String ipamId = networkId + "-" + availableIp.toString();
            k8sIpamStore.removeAvailableIp(ipamId);
            k8sIpamStore.createAllocatedIp(
                    new DefaultK8sIpam(ipamId, availableIp, networkId));

            log.info("Allocate a new IP {}", availableIp.toString());

            return availableIp;
        } else {
            log.warn("No IPs are available for allocating.");
        }
        return null;
    }

    @Override
    public void reserveIp(String networkId, IpAddress ipAddress) {
        if (!allocatedIps(networkId).contains(ipAddress)) {
            String ipamId = networkId + "-" + ipAddress.toString();
            k8sIpamStore.removeAvailableIp(ipamId);
            k8sIpamStore.createAllocatedIp(
                    new DefaultK8sIpam(ipamId, ipAddress, networkId));

            log.info("Reserved the IP {}", ipAddress.toString());
        }
    }

    @Override
    public boolean releaseIp(String networkId, IpAddress ipAddress) {
        IpAddress releasedIp = allocatedIps(networkId).stream()
                .filter(ip -> ip.equals(ipAddress))
                .findFirst().orElse(null);
        if (releasedIp != null) {
            String ipamId = networkId + "-" + releasedIp.toString();
            k8sIpamStore.removeAllocatedIp(ipamId);
            k8sIpamStore.createAvailableIp(
                    new DefaultK8sIpam(ipamId, releasedIp, networkId));

            log.info("Release the IP {}", releasedIp.toString());

            return true;
        } else {
            log.warn("Failed to find requested IP {} for releasing...", ipAddress.toString());
        }

        return false;
    }

    @Override
    public void initializeIpPool(String networkId, Set<IpAddress> ipAddresses) {
        ipAddresses.forEach(ip -> {
            String ipamId = networkId + "-" + ip;
            K8sIpam ipam = new DefaultK8sIpam(ipamId, ip, networkId);
            k8sIpamStore.createAvailableIp(ipam);
        });
    }

    @Override
    public void purgeIpPool(String networkId) {
        k8sIpamStore.clear(networkId);
    }

    @Override
    public Set<IpAddress> allocatedIps(String networkId) {
        return k8sIpamStore.allocatedIps().stream()
                .filter(i -> i.networkId().equals(networkId))
                .map(K8sIpam::ipAddress).collect(Collectors.toSet());
    }

    @Override
    public Set<IpAddress> availableIps(String networkId) {
        return k8sIpamStore.availableIps().stream()
                .filter(i -> i.networkId().equals(networkId))
                .map(K8sIpam::ipAddress).collect(Collectors.toSet());
    }
}
