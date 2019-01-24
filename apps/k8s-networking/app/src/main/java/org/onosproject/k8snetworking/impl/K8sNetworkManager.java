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

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.event.ListenerRegistry;
import org.onosproject.k8snetworking.api.K8sNetwork;
import org.onosproject.k8snetworking.api.K8sNetwork.Type;
import org.onosproject.k8snetworking.api.K8sNetworkAdminService;
import org.onosproject.k8snetworking.api.K8sNetworkEvent;
import org.onosproject.k8snetworking.api.K8sNetworkListener;
import org.onosproject.k8snetworking.api.K8sNetworkService;
import org.onosproject.k8snetworking.api.K8sNetworkStore;
import org.onosproject.k8snetworking.api.K8sNetworkStoreDelegate;
import org.onosproject.k8snetworking.api.K8sPort;
import org.onosproject.k8snetworking.api.K8sPort.State;
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
import static org.onosproject.k8snetworking.api.Constants.K8S_NETWORKING_APP_ID;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provides implementation of administering and interfacing kubernetes network,
 * and port.
 */
@Component(
        immediate = true,
        service = {K8sNetworkAdminService.class, K8sNetworkService.class }
)
public class K8sNetworkManager
        extends ListenerRegistry<K8sNetworkEvent, K8sNetworkListener>
        implements K8sNetworkAdminService, K8sNetworkService {

    protected final Logger log = getLogger(getClass());

    private static final String MSG_NETWORK  = "Kubernetes network %s %s";
    private static final String MSG_PORT = "Kubernetes port %s %s";
    private static final String MSG_CREATED = "created";
    private static final String MSG_UPDATED = "updated";
    private static final String MSG_REMOVED = "removed";

    private static final String ERR_NULL_NETWORK  = "Kubernetes network cannot be null";
    private static final String ERR_NULL_NETWORK_ID  = "Kubernetes network ID cannot be null";
    private static final String ERR_NULL_PORT = "Kubernetes port cannot be null";
    private static final String ERR_NULL_PORT_ID = "Kubernetes port ID cannot be null";
    private static final String ERR_NULL_PORT_NET_ID = "Kubernetes port network ID cannot be null";

    private static final String ERR_IN_USE = " still in use";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sNetworkStore k8sNetworkStore;

    private final K8sNetworkStoreDelegate
            delegate = new InternalNetworkStorageDelegate();

    private ApplicationId appId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(K8S_NETWORKING_APP_ID);

        k8sNetworkStore.setDelegate(delegate);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        k8sNetworkStore.unsetDelegate(delegate);
        log.info("Stopped");
    }

    @Override
    public void createNetwork(K8sNetwork network) {
        checkNotNull(network, ERR_NULL_NETWORK);
        checkArgument(!Strings.isNullOrEmpty(network.networkId()), ERR_NULL_NETWORK_ID);

        k8sNetworkStore.createNetwork(network);

        log.info(String.format(MSG_NETWORK, network.name(), MSG_CREATED));
    }

    @Override
    public void updateNetwork(K8sNetwork network) {
        checkNotNull(network, ERR_NULL_NETWORK);
        checkArgument(!Strings.isNullOrEmpty(network.networkId()), ERR_NULL_NETWORK_ID);

        k8sNetworkStore.updateNetwork(network);

        log.info(String.format(MSG_NETWORK, network.networkId(), MSG_UPDATED));
    }

    @Override
    public void removeNetwork(String networkId) {
        checkArgument(!Strings.isNullOrEmpty(networkId), ERR_NULL_NETWORK_ID);

        synchronized (this) {
            if (isNetworkInUse(networkId)) {
                final String error = String.format(MSG_NETWORK, networkId, ERR_IN_USE);
                throw new IllegalStateException(error);
            }
            K8sNetwork network = k8sNetworkStore.removeNetwork(networkId);

            if (network != null) {
                log.info(String.format(MSG_NETWORK, network.name(), MSG_REMOVED));
            }
        }
    }

    @Override
    public K8sNetwork network(String networkId) {
        checkArgument(!Strings.isNullOrEmpty(networkId), ERR_NULL_NETWORK_ID);
        return k8sNetworkStore.network(networkId);
    }

    @Override
    public Set<K8sNetwork> networks() {
        return ImmutableSet.copyOf(k8sNetworkStore.networks());
    }

    @Override
    public Set<K8sNetwork> networks(Type type) {
        return ImmutableSet.copyOf(k8sNetworkStore.networks().stream()
                .filter(n -> n.type() == type).collect(Collectors.toSet()));
    }

    @Override
    public void createPort(K8sPort port) {
        checkNotNull(port, ERR_NULL_PORT);
        checkArgument(!Strings.isNullOrEmpty(port.portId()), ERR_NULL_PORT_ID);
        checkArgument(!Strings.isNullOrEmpty(port.networkId()), ERR_NULL_PORT_NET_ID);

        k8sNetworkStore.createPort(port);
        log.info(String.format(MSG_PORT, port.portId(), MSG_CREATED));
    }

    @Override
    public void updatePort(K8sPort port) {
        checkNotNull(port, ERR_NULL_PORT);
        checkArgument(!Strings.isNullOrEmpty(port.portId()), ERR_NULL_PORT_ID);
        checkArgument(!Strings.isNullOrEmpty(port.networkId()), ERR_NULL_PORT_NET_ID);

        k8sNetworkStore.updatePort(port);
        log.info(String.format(MSG_PORT, port.portId(), MSG_UPDATED));
    }

    @Override
    public void removePort(String portId) {
        checkArgument(!Strings.isNullOrEmpty(portId), ERR_NULL_PORT_ID);
        synchronized (this) {
            if (isPortInUse(portId)) {
                final String error = String.format(MSG_PORT, portId, ERR_IN_USE);
                throw new IllegalStateException(error);
            }
            K8sPort port = k8sNetworkStore.removePort(portId);
            if (port != null) {
                log.info(String.format(MSG_PORT, port.portId(), MSG_REMOVED));
            }
        }
    }

    @Override
    public K8sPort port(String portId) {
        checkArgument(!Strings.isNullOrEmpty(portId), ERR_NULL_PORT_ID);
        return k8sNetworkStore.port(portId);
    }

    @Override
    public Set<K8sPort> ports() {
        return ImmutableSet.copyOf(k8sNetworkStore.ports());
    }

    @Override
    public Set<K8sPort> ports(State state) {
        return ImmutableSet.copyOf(k8sNetworkStore.ports().stream()
                .filter(p -> p.state() == state).collect(Collectors.toSet()));
    }

    @Override
    public Set<K8sPort> ports(String networkId) {
        checkArgument(!Strings.isNullOrEmpty(networkId), ERR_NULL_PORT_NET_ID);
        return ImmutableSet.copyOf(k8sNetworkStore.ports().stream()
                .filter(p -> p.networkId().equals(networkId))
                .collect(Collectors.toSet()));
    }

    @Override
    public void clear() {
        k8sNetworkStore.clear();
    }

    private boolean isNetworkInUse(String networkId) {
        return !ports(networkId).isEmpty();
    }

    private boolean isPortInUse(String portId) {
        return false;
    }

    private class InternalNetworkStorageDelegate implements K8sNetworkStoreDelegate {

        @Override
        public void notify(K8sNetworkEvent event) {
            if (event != null) {
                log.trace("send kubernetes networking event {}", event);
                process(event);
            }
        }
    }
}
