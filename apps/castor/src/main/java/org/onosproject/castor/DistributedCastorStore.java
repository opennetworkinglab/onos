/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.castor;

import com.google.common.collect.ImmutableSet;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.util.KryoNamespace;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.MultiPointToSinglePointIntent;
import org.onosproject.net.intent.PointToPointIntent;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.DistributedSet;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
import org.onosproject.store.serializers.KryoNamespaces;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Distributed Store for Castor.
 */

@Component(immediate = true)
@Service
public class DistributedCastorStore implements CastorStore {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    private ConsistentMap<IpAddress, MacAddress> addressMap;
    private ConsistentMap<Key, PointToPointIntent> peerIntents;
    private ConsistentMap<String, MultiPointToSinglePointIntent> layer2Intents;
    private DistributedSet<Peer> allPeers;
    private DistributedSet<Peer> customers;
    private DistributedSet<Peer> routeServers;


    @Activate
    protected void activate() {

        addressMap = storageService.<IpAddress, MacAddress>consistentMapBuilder()
                .withName("castor-addressMap")
                .withSerializer(Serializer.using(KryoNamespaces.API))
                .build();

        peerIntents = storageService.<Key, PointToPointIntent>consistentMapBuilder()
                .withName("castor-peerIntents")
                .withSerializer(Serializer.using(KryoNamespaces.API))
                .build();

        layer2Intents = storageService.<String, MultiPointToSinglePointIntent>consistentMapBuilder()
                .withName("castor-layer2Intents")
                .withSerializer(Serializer.using(KryoNamespaces.API))
                .build();

        allPeers = storageService.<Peer>setBuilder()
                .withName("castor-allPeers")
                .withSerializer(Serializer.using(
                        new KryoNamespace.Builder()
                                .register(KryoNamespaces.API)
                                .register(Peer.class)
                                .build()))
                .build()
                .asDistributedSet();

        customers = storageService.<Peer>setBuilder()
                .withName("castor-customers")
                .withSerializer(Serializer.using(
                        new KryoNamespace.Builder()
                                .register(KryoNamespaces.API)
                                .register(Peer.class)
                                .build()))
                .build()
                .asDistributedSet();

        routeServers = storageService.<Peer>setBuilder()
                .withName("castor-routeServers")
                .withSerializer(Serializer.using(
                        new KryoNamespace.Builder()
                                .register(KryoNamespaces.API)
                                .register(Peer.class)
                                .build()))
                .build()
                .asDistributedSet();

        log.info("Started");

    }

    @Deactivate
    protected void deactivate() {

    }

    @Override
    public Set<Peer> getAllPeers() {
        return ImmutableSet.copyOf(allPeers);
    }

    @Override
    public void storePeer(Peer peer) {
        allPeers.add(peer);
    }

    @Override
    public Set<Peer> getServers() {
        return ImmutableSet.copyOf(routeServers);
    }

    @Override
    public void storeServer(Peer server) {
        routeServers.add(server);
    }

    @Override
    public Set<Peer> getCustomers() {
        return ImmutableSet.copyOf(customers);
    }

    @Override
    public void storeCustomer(Peer customer) {
        customers.add(customer);
    }

    @Override
    public Map<IpAddress, MacAddress> getAddressMap() {
        Map<IpAddress, MacAddress> validMapping = new HashMap<>();
        for (Map.Entry<IpAddress, Versioned<MacAddress>> entry: addressMap.entrySet()) {
            validMapping.put(entry.getKey(), entry.getValue().value());
        }
        return validMapping;
    }

    @Override
    public void setAddressMap(IpAddress ip, MacAddress mac) {
        addressMap.put(ip, mac);
    }

    @Override
    public Map<Key, PointToPointIntent> getPeerIntents() {
        Map<Key, PointToPointIntent> validMapping = new HashMap<>();
        for (Map.Entry<Key, Versioned<PointToPointIntent>> entry: peerIntents.entrySet()) {
            validMapping.put(entry.getKey(), entry.getValue().value());
        }
        return validMapping;
    }

    @Override
    public void storePeerIntent(Key key, PointToPointIntent intent) {
        peerIntents.put(key, intent);

    }

    @Override
    public Map<String, MultiPointToSinglePointIntent> getLayer2Intents() {
        Map<String, MultiPointToSinglePointIntent> validMapping = new HashMap<>();
        for (Map.Entry<String, Versioned<MultiPointToSinglePointIntent>> entry: layer2Intents.entrySet()) {
            validMapping.put(entry.getKey(), entry.getValue().value());
        }
        return validMapping;
    }

    @Override
    public void storeLayer2Intent(String key, MultiPointToSinglePointIntent intent) {
        layer2Intents.put(key, intent);
    }

    @Override
    public Map<String, String> getCustomersMap() {
        Map<String, String> peerMap = new HashMap<>();
        for (Peer cust : customers) {
            peerMap.put(cust.getName(), cust.getIpAddress());
        }
        return peerMap;
    }

    @Override
    public void removeCustomer(Peer peer) {
        customers.remove(peer);
        allPeers.remove(peer);
    }

    @Override
    public void removePeerIntent(Key key) {
        peerIntents.remove(key);

    }

    @Override
    public void removeLayer2Intent(String key) {
        layer2Intents.remove(key);

    }
}

