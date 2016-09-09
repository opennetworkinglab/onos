/*
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.tetopology.management.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;
import java.lang.annotation.ElementType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.IpAddress;
import org.onlab.util.KryoNamespace;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.tetopology.management.api.KeyId;
import org.onosproject.tetopology.management.api.Network;
import org.onosproject.tetopology.management.api.Networks;
import org.onosproject.tetopology.management.api.TeTopologyEvent;
import org.onosproject.tetopology.management.api.TeTopologyId;
import org.onosproject.tetopology.management.api.TeTopologyType;
import org.onosproject.tetopology.management.api.link.AsNumber;
import org.onosproject.tetopology.management.api.link.DefaultNetworkLink;
import org.onosproject.tetopology.management.api.link.ExternalDomain;
import org.onosproject.tetopology.management.api.link.TeIpv4;
import org.onosproject.tetopology.management.api.link.TeIpv6;
import org.onosproject.tetopology.management.api.link.Label;
import org.onosproject.tetopology.management.api.link.LinkProtectionType;
import org.onosproject.tetopology.management.api.link.NetworkLink;
import org.onosproject.tetopology.management.api.link.NetworkLinkKey;
import org.onosproject.tetopology.management.api.link.PathElement;
import org.onosproject.tetopology.management.api.link.TeLink;
import org.onosproject.tetopology.management.api.link.TeLinkAccessType;
import org.onosproject.tetopology.management.api.link.UnderlayBackupPath;
import org.onosproject.tetopology.management.api.link.UnderlayPrimaryPath;
import org.onosproject.tetopology.management.api.link.UnnumberedLink;
import org.onosproject.tetopology.management.api.link.UnreservedBandwidth;
import org.onosproject.tetopology.management.api.node.ConnectivityMatrix;
import org.onosproject.tetopology.management.api.node.DefaultNetworkNode;
import org.onosproject.tetopology.management.api.node.DefaultTerminationPoint;
import org.onosproject.tetopology.management.api.node.InterfaceSwitchingCapability;
import org.onosproject.tetopology.management.api.node.NetworkNode;
import org.onosproject.tetopology.management.api.node.NetworkNodeKey;
import org.onosproject.tetopology.management.api.node.TeNetworkTopologyId;
import org.onosproject.tetopology.management.api.node.TeNode;
import org.onosproject.tetopology.management.api.node.TeStatus;
import org.onosproject.tetopology.management.api.node.TeTerminationPoint;
import org.onosproject.tetopology.management.api.node.TerminationCapability;
import org.onosproject.tetopology.management.api.node.TerminationPoint;
import org.onosproject.tetopology.management.api.node.TerminationPointKey;
import org.onosproject.tetopology.management.api.node.TunnelTerminationPoint;
import org.onosproject.tetopology.management.api.TeTopologyStore;
import org.onosproject.tetopology.management.api.TeTopologyStoreDelegate;
import org.onosproject.tetopology.management.api.DefaultNetwork;
import org.onosproject.tetopology.management.api.DefaultNetworks;
import org.onosproject.tetopology.management.api.InternalTeNetwork;
import org.slf4j.Logger;

/**
 * Implementation of the IETF network store.
 */
@Component(immediate = true)
@Service
public class DistributedTeTopologyStore
         extends AbstractStore<TeTopologyEvent, TeTopologyStoreDelegate>
         implements TeTopologyStore {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    // Track networks by network key
    private ConsistentMap<KeyId, InternalTeNetwork> networkIdNetworkConsistentMap;
    private Map<KeyId, InternalTeNetwork> networkIdNetworkMap;

    // Listener for network events
    private final MapEventListener<KeyId, InternalTeNetwork> networkMapListener = new InternalNetworkMapListener();

    private static final Serializer NETWORK_SERIALIZER = Serializer
            .using(new KryoNamespace.Builder().register(KryoNamespaces.API)
                           .register(KeyId.class)
                           .register(InternalTeNetwork.class)
                           .register(TeTopologyId.class)
                           .register(DefaultNetwork.class)
                           .register(DefaultNetworks.class)
                           .register(InternalTeNetwork.class)
                           .register(Network.class)
                           .register(Networks.class)
                           .register(TeTopologyType.class)
                           .register(TeIpv4.class)
                           .register(NetworkLinkKey.class)
                           .register(NetworkLink.class)
                           .register(PathElement.class)
                           .register(TeLink.class)
                           .register(UnderlayBackupPath.class)
                           .register(UnderlayPrimaryPath.class)
                           .register(UnnumberedLink.class)
                           .register(UnreservedBandwidth.class)
                           .register(InterfaceSwitchingCapability.class)
                           .register(NetworkNode.class)
                           .register(TeNode.class)
                           .register(TerminationPoint.class)
                           .register(TeTerminationPoint.class)
                           .register(TerminationCapability.class)
                           .register(TeStatus.class)
                           .register(TunnelTerminationPoint.class)
                           .register(DefaultNetworkLink.class)
                           .register(DefaultNetworkNode.class)
                           .register(DefaultTerminationPoint.class)
                           .register(TerminationPointKey.class)
                           .register(TeNetworkTopologyId.class)
                           .register(NetworkNodeKey.class)
                           .register(ConnectivityMatrix.class)
                           .register(TeTopologyId.class)
                           .register(TeLinkAccessType.class)
                           .register(BigInteger.class)
                           .register(String.class)
                           .register(Long.class)
                           .register(Boolean.class)
                           .register(BigDecimal.class)
                           .register(Short.class)
                           .register(IpAddress.class)
                           .register(Integer.class)
                           .register(ExternalDomain.class)
                           .register(ElementType.class)
                           .register(LinkProtectionType.class)
                           .register(Label.class)
                           .register(TeIpv6.class)
                           .register(AsNumber.class)
                           .build());

    /**
     * Distributed network store service activate method.
     */
    @Activate
    public void activate() {
        log.info("TE topology store is activated");
        networkIdNetworkConsistentMap = storageService.<KeyId, InternalTeNetwork>consistentMapBuilder()
                .withSerializer(NETWORK_SERIALIZER)
                .withName("networkId-network")
                .withRelaxedReadConsistency()
                .build();
        networkIdNetworkConsistentMap.addListener(networkMapListener);
        networkIdNetworkMap = networkIdNetworkConsistentMap.asJavaMap();

        log.info("Started");
    }

    /**
     * Distributed network store service deactivate method.
     */
    @Deactivate
    public void deactivate() {
        networkIdNetworkConsistentMap.removeListener(networkMapListener);
        log.info("Stopped");
    }

    @Override
    public List<InternalTeNetwork> getNetworks(TeTopologyType type) {
       List<InternalTeNetwork> networks = new ArrayList<>();

       for (Map.Entry<KeyId, InternalTeNetwork> entry:networkIdNetworkMap.entrySet()) {
            KeyId networkId = entry.getKey();
            InternalTeNetwork network = entry.getValue();

            if (network.getTeTopologyType() == type ||
                 type == TeTopologyType.ANY) {
                 networks.add(network);
             }
        }

        return networks;
    }

    @Override
    public InternalTeNetwork getNetwork(KeyId networkId) {
        return networkIdNetworkMap.get(networkId);
    }

    @Override
    public void updateNetwork(InternalTeNetwork network) {
        //TODO - check the validity of the network before updating
        log.info("network = {}", network);
        networkIdNetworkMap.put(network.networkId(), network);
    }

    @Override
    public void removeNetwork(KeyId networkId) {
        networkIdNetworkMap.remove(networkId);
    }

    /**
     * Listener class to map listener map events to the network events.
     */
    private class InternalNetworkMapListener implements MapEventListener<KeyId, InternalTeNetwork> {
        @Override
        public void event(MapEvent<KeyId, InternalTeNetwork> event) {
            TeTopologyEvent.Type type = null;
            TeTopologyEvent topologyEvent = null;
            switch (event.type()) {
            case INSERT:
                type = TeTopologyEvent.Type.NETWORK_ADDED;
                // Need to check if nodes/links are already in, otherwise errors
                topologyEvent = new TeTopologyEvent(type, event.newValue().value());
                break;
            case UPDATE:
                // Need to check what attributes change, and coordinate with other Node/Link events.
                if ((event.oldValue().value() != null) && (event.newValue().value() == null)) {
                    type = TeTopologyEvent.Type.NETWORK_REMOVED;
                    topologyEvent = new TeTopologyEvent(type, event.oldValue().value());
                } else {
                    type = TeTopologyEvent.Type.NETWORK_UPDATED;
                    topologyEvent = new TeTopologyEvent(type, event.newValue().value());
                }
                break;
            case REMOVE:
                type = TeTopologyEvent.Type.NETWORK_REMOVED;
                topologyEvent = new TeTopologyEvent(type, event.oldValue().value());
                break;
            default:
                log.error("Unsupported event type: {}", event.type());
            }
            log.info("Event type {}, Event {}", type, topologyEvent);
            if (topologyEvent != null) {
                notifyDelegate(topologyEvent);
            }
        }
    }

}

