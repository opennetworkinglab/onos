/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.kubevirtnetworking.impl;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import org.onlab.packet.IpAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.event.ListenerRegistry;
import org.onosproject.kubevirtnetworking.api.KubevirtFlowRuleService;
import org.onosproject.kubevirtnetworking.api.KubevirtNetwork;
import org.onosproject.kubevirtnetworking.api.KubevirtNetworkAdminService;
import org.onosproject.kubevirtnetworking.api.KubevirtNetworkEvent;
import org.onosproject.kubevirtnetworking.api.KubevirtNetworkListener;
import org.onosproject.kubevirtnetworking.api.KubevirtNetworkService;
import org.onosproject.kubevirtnetworking.api.KubevirtNetworkStore;
import org.onosproject.kubevirtnetworking.api.KubevirtNetworkStoreDelegate;
import org.onosproject.kubevirtnetworking.api.KubevirtRouterService;
import org.onosproject.kubevirtnode.api.KubevirtNodeService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.packet.PacketService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.kubevirtnetworking.api.Constants.KUBEVIRT_NETWORKING_APP_ID;
import static org.onosproject.kubevirtnetworking.api.KubevirtNetwork.Type.GENEVE;
import static org.onosproject.kubevirtnetworking.api.KubevirtNetwork.Type.GRE;
import static org.onosproject.kubevirtnetworking.api.KubevirtNetwork.Type.STT;
import static org.onosproject.kubevirtnetworking.api.KubevirtNetwork.Type.VXLAN;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provides implementation of administering and interfacing kubevirt network.
 */
@Component(
        immediate = true,
        service = {KubevirtNetworkAdminService.class, KubevirtNetworkService.class}
)
public class KubevirtNetworkManager
        extends ListenerRegistry<KubevirtNetworkEvent, KubevirtNetworkListener>
        implements KubevirtNetworkAdminService, KubevirtNetworkService {

    protected final Logger log = getLogger(getClass());

    private static final String MSG_NETWORK  = "Kubernetes network %s %s";

    private static final String MSG_CREATED = "created";
    private static final String MSG_UPDATED = "updated";
    private static final String MSG_REMOVED = "removed";

    private static final String ERR_NULL_NETWORK  = "Kubernetes network cannot be null";
    private static final String ERR_NULL_NETWORK_ID  = "Kubernetes network ID cannot be null";
    private static final String ERR_NULL_IP = "IP address cannot be null";

    private static final String ERR_IN_USE = " still in use";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtNetworkStore networkStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtRouterService routerService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtNodeService kubevirtNodeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtFlowRuleService kubevirtFlowRuleService;

    private final KubevirtNetworkStoreDelegate delegate = new InternalNetworkStorageDelegate();

    private ApplicationId appId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(KUBEVIRT_NETWORKING_APP_ID);

        networkStore.setDelegate(delegate);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        networkStore.unsetDelegate(delegate);
        log.info("Stopped");
    }

    @Override
    public void createNetwork(KubevirtNetwork network) {
        checkNotNull(network, ERR_NULL_NETWORK);
        checkArgument(!Strings.isNullOrEmpty(network.networkId()), ERR_NULL_NETWORK_ID);

        networkStore.createNetwork(network);

        log.info(String.format(MSG_NETWORK, network.name(), MSG_CREATED));
    }

    @Override
    public void updateNetwork(KubevirtNetwork network) {
        checkNotNull(network, ERR_NULL_NETWORK);
        checkArgument(!Strings.isNullOrEmpty(network.networkId()), ERR_NULL_NETWORK_ID);

        networkStore.updateNetwork(network);

        log.info(String.format(MSG_NETWORK, network.networkId(), MSG_UPDATED));
    }

    @Override
    public void removeNetwork(String networkId) {
        checkArgument(!Strings.isNullOrEmpty(networkId), ERR_NULL_NETWORK_ID);

        synchronized (this) {
//            if (isNetworkInUse(networkId)) {
//                final String error = String.format(MSG_NETWORK, networkId, ERR_IN_USE);
//                throw new IllegalStateException(error);
//            }
            KubevirtNetwork network = networkStore.removeNetwork(networkId);

            if (network != null) {
                log.info(String.format(MSG_NETWORK, network.name(), MSG_REMOVED));
            }
        }
    }

    @Override
    public IpAddress allocateIp(String networkId) {
        checkArgument(!Strings.isNullOrEmpty(networkId), ERR_NULL_NETWORK_ID);

        try {
            KubevirtNetwork network = networkStore.network(networkId);
            IpAddress ip = network.ipPool().allocateIp();
            networkStore.updateNetwork(network);
            return ip;
        } catch (Exception e) {
            log.error("Failed to allocate IP address", e);
        }
        return null;
    }

    @Override
    public boolean reserveIp(String networkId, IpAddress ip) {
        checkArgument(!Strings.isNullOrEmpty(networkId), ERR_NULL_NETWORK_ID);
        checkArgument(ip != null, ERR_NULL_IP);

        KubevirtNetwork network = networkStore.network(networkId);
        boolean result = network.ipPool().reserveIp(ip);
        if (result) {
            networkStore.updateNetwork(network);
        } else {
            log.warn("Failed to reserve IP address");
        }

        return result;
    }

    @Override
    public void releaseIp(String networkId, IpAddress ip) {
        checkArgument(!Strings.isNullOrEmpty(networkId), ERR_NULL_NETWORK_ID);
        checkArgument(ip != null, ERR_NULL_IP);

        try {
            KubevirtNetwork network = networkStore.network(networkId);
            network.ipPool().releaseIp(ip);
            networkStore.updateNetwork(network);
        } catch (Exception e) {
            log.error("Failed to allocate IP address");
        }
    }

    @Override
    public void clear() {
        networkStore.clear();
    }

    @Override
    public KubevirtNetwork network(String networkId) {
        checkArgument(!Strings.isNullOrEmpty(networkId), ERR_NULL_NETWORK_ID);
        return networkStore.network(networkId);
    }

    @Override
    public Set<KubevirtNetwork> networks() {
        return ImmutableSet.copyOf(networkStore.networks());
    }

    @Override
    public Set<KubevirtNetwork> networks(KubevirtNetwork.Type type) {
        return ImmutableSet.copyOf(networkStore.networks().stream()
                .filter(n -> n.type() == type).collect(Collectors.toSet()));
    }

    @Override
    public Set<KubevirtNetwork> tenantNetworks() {
        return ImmutableSet.copyOf(networkStore.networks().stream()
                .filter(n -> n.type() == VXLAN ||
                             n.type() == GRE ||
                             n.type() == GENEVE ||
                             n.type() == STT)
                .collect(Collectors.toSet()));
    }

    private class InternalNetworkStorageDelegate implements KubevirtNetworkStoreDelegate {

        @Override
        public void notify(KubevirtNetworkEvent event) {
            if (event != null) {
                log.trace("send kubevirt networking event {}", event);
                process(event);
            }
        }
    }
}
