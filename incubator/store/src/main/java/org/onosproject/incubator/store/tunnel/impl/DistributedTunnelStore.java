/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.incubator.store.tunnel.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.Maps;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoNamespace;
import org.onosproject.cluster.ClusterService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.IdGenerator;
import org.onosproject.incubator.net.tunnel.DefaultTunnel;
import org.onosproject.incubator.net.tunnel.Tunnel;
import org.onosproject.incubator.net.tunnel.Tunnel.State;
import org.onosproject.incubator.net.tunnel.Tunnel.Type;
import org.onosproject.incubator.net.tunnel.TunnelEndPoint;
import org.onosproject.incubator.net.tunnel.TunnelEvent;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.incubator.net.tunnel.TunnelName;
import org.onosproject.incubator.net.tunnel.TunnelStore;
import org.onosproject.incubator.net.tunnel.TunnelStoreDelegate;
import org.onosproject.incubator.net.tunnel.TunnelSubscription;
import org.onosproject.net.Annotations;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.EventuallyConsistentMapEvent;
import org.onosproject.store.service.EventuallyConsistentMapListener;
import org.onosproject.store.service.MultiValuedTimestamp;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.WallClockTimestamp;
import org.slf4j.Logger;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;

import static org.onosproject.store.service.EventuallyConsistentMapEvent.Type.PUT;
import static org.onosproject.store.service.EventuallyConsistentMapEvent.Type.REMOVE;

/**
 * Manages inventory of tunnel in distributed data store that uses optimistic
 * replication and gossip based techniques.
 */
@Component(immediate = true)
@Service
public class DistributedTunnelStore
        extends AbstractStore<TunnelEvent, TunnelStoreDelegate>
        implements TunnelStore {

    private final Logger log = getLogger(getClass());

    /**
     * The topic used for obtaining globally unique ids.
     */
    private String tunnelOpTopic = "tunnel-ops-ids";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterCommunicationService clusterCommunicator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    // tunnel identity as map key in the store.
    private EventuallyConsistentMap<TunnelId, Tunnel> tunnelIdAsKeyStore;
    // maintains records that app subscribes tunnel.
    private EventuallyConsistentMap<ApplicationId, Set<TunnelSubscription>> orderRelationship;

    // tunnel name as map key.
    private final Map<TunnelName, Set<TunnelId>> tunnelNameAsKeyMap = Maps.newConcurrentMap();
    // maintains all the tunnels between source and destination.
    private final Map<TunnelKey, Set<TunnelId>> srcAndDstKeyMap = Maps.newConcurrentMap();
    // maintains all the tunnels by tunnel type.
    private final Map<Tunnel.Type, Set<TunnelId>> typeKeyMap = Maps.newConcurrentMap();

    private IdGenerator idGenerator;

    private EventuallyConsistentMapListener<TunnelId, Tunnel> tunnelUpdateListener =
            new InternalTunnelChangeEventListener();

    @Activate
    public void activate() {
        KryoNamespace.Builder serializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API)
                .register(MultiValuedTimestamp.class);
        tunnelIdAsKeyStore = storageService
                .<TunnelId, Tunnel>eventuallyConsistentMapBuilder()
                .withName("all_tunnel").withSerializer(serializer)
                .withTimestampProvider((k, v) -> new WallClockTimestamp()).build();
        orderRelationship = storageService
                .<ApplicationId, Set<TunnelSubscription>>eventuallyConsistentMapBuilder()
                .withName("type_tunnel").withSerializer(serializer)
                .withTimestampProvider((k, v) -> new WallClockTimestamp()).build();
        idGenerator = coreService.getIdGenerator(tunnelOpTopic);
        tunnelIdAsKeyStore.addListener(tunnelUpdateListener);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        tunnelIdAsKeyStore.removeListener(tunnelUpdateListener);
        orderRelationship.destroy();
        tunnelIdAsKeyStore.destroy();
        srcAndDstKeyMap.clear();
        typeKeyMap.clear();
        tunnelNameAsKeyMap.clear();
        log.info("Stopped");
    }

    @Override
    public TunnelId createOrUpdateTunnel(Tunnel tunnel) {
        return handleCreateOrUpdateTunnel(tunnel, null);
    }

    @Override
    public TunnelId createOrUpdateTunnel(Tunnel tunnel, State state) {
        return handleCreateOrUpdateTunnel(tunnel, state);
    }

    private TunnelId handleCreateOrUpdateTunnel(Tunnel tunnel, State state) {
        // tunnelIdAsKeyStore.
        if (tunnel.tunnelId() != null && !"".equals(tunnel.tunnelId().toString())) {
            Tunnel old = tunnelIdAsKeyStore.get(tunnel.tunnelId());
            if (old == null) {
                log.info("This tunnel[" + tunnel.tunnelId() + "] is not available.");
                return tunnel.tunnelId();
            }
            DefaultAnnotations oldAnno = (DefaultAnnotations) old.annotations();
            SparseAnnotations newAnno = (SparseAnnotations) tunnel.annotations();
            State newTunnelState = (state != null) ? state : old.state();
            Tunnel newT = new DefaultTunnel(old.providerId(), old.src(),
                                            old.dst(), old.type(),
                                            newTunnelState, old.groupId(),
                                            old.tunnelId(),
                                            old.tunnelName(),
                                            old.path(),
                                            old.resource(),
                                            DefaultAnnotations.merge(oldAnno, newAnno));
            tunnelIdAsKeyStore.put(tunnel.tunnelId(), newT);
            TunnelEvent event = new TunnelEvent(TunnelEvent.Type.TUNNEL_UPDATED,
                                                tunnel);
            notifyDelegate(event);
            return tunnel.tunnelId();
        } else {
            TunnelId tunnelId = TunnelId.valueOf(String.valueOf(idGenerator.getNewId()));
            State tunnelState = (state != null) ? state : tunnel.state();
            Tunnel newT = new DefaultTunnel(tunnel.providerId(), tunnel.src(),
                                            tunnel.dst(), tunnel.type(),
                                            tunnelState, tunnel.groupId(),
                                            tunnelId,
                                            tunnel.tunnelName(),
                                            tunnel.path(),
                                            tunnel.resource(),
                                            tunnel.annotations());
            tunnelIdAsKeyStore.put(tunnelId, newT);

            TunnelEvent event = new TunnelEvent(TunnelEvent.Type.TUNNEL_ADDED,
                                                tunnel);
            notifyDelegate(event);
            return tunnelId;
        }
    }

    @Override
    public void deleteTunnel(TunnelId tunnelId) {
        Tunnel deletedTunnel = tunnelIdAsKeyStore.get(tunnelId);
        if (deletedTunnel == null) {
            return;
        }

        tunnelIdAsKeyStore.remove(tunnelId);

        TunnelEvent event = new TunnelEvent(TunnelEvent.Type.TUNNEL_REMOVED,
                                            deletedTunnel);
        notifyDelegate(event);
    }

    @Override
    public void deleteTunnel(TunnelEndPoint src, TunnelEndPoint dst,
                             ProviderId producerName) {
        TunnelKey key = TunnelKey.tunnelKey(src, dst);
        Set<TunnelId> idSet = srcAndDstKeyMap.get(key);
        if (idSet == null) {
            return;
        }
        Tunnel deletedTunnel = null;
        TunnelEvent event = null;
        List<TunnelEvent> ls = new ArrayList<TunnelEvent>();
        for (TunnelId id : idSet) {
            deletedTunnel = tunnelIdAsKeyStore.get(id);

            if (producerName == null || (producerName != null
                    && producerName.equals(deletedTunnel.providerId()))) {
                tunnelIdAsKeyStore.remove(deletedTunnel.tunnelId());

                event = new TunnelEvent(TunnelEvent.Type.TUNNEL_REMOVED,
                                        deletedTunnel);
                ls.add(event);
            }
        }

        if (!ls.isEmpty()) {
            notifyDelegate(ls);
        }
    }

    @Override
    public void deleteTunnel(TunnelEndPoint src, TunnelEndPoint dst, Type type,
                             ProviderId producerName) {
        TunnelKey key = TunnelKey.tunnelKey(src, dst);
        Set<TunnelId> idSet = srcAndDstKeyMap.get(key);
        if (idSet == null) {
            return;
        }
        Tunnel deletedTunnel = null;
        TunnelEvent event = null;
        List<TunnelEvent> ls = new ArrayList<TunnelEvent>();
        for (TunnelId id : idSet) {
            deletedTunnel = tunnelIdAsKeyStore.get(id);

            if (type.equals(deletedTunnel.type()) && (producerName == null || (producerName != null
                    && producerName.equals(deletedTunnel.providerId())))) {
                tunnelIdAsKeyStore.remove(deletedTunnel.tunnelId());

                event = new TunnelEvent(TunnelEvent.Type.TUNNEL_REMOVED,
                                        deletedTunnel);
                ls.add(event);
            }
        }

        if (!ls.isEmpty()) {
            notifyDelegate(ls);
        }
    }

    @Override
    public Tunnel borrowTunnel(ApplicationId appId, TunnelId tunnelId,
                               Annotations... annotations) {
        Set<TunnelSubscription> orderSet = orderRelationship.get(appId);
        if (orderSet == null) {
            orderSet = new HashSet<TunnelSubscription>();
        }
        TunnelSubscription order = new TunnelSubscription(appId, null, null, tunnelId, null, null,
                                annotations);
        Tunnel result = tunnelIdAsKeyStore.get(tunnelId);
        if (result == null || Tunnel.State.INACTIVE.equals(result.state())) {
            return null;
        }

        orderSet.add(order);
        orderRelationship.put(appId, orderSet);
        return result;
    }

    @Override
    public Collection<Tunnel> borrowTunnel(ApplicationId appId,
                                           TunnelEndPoint src,
                                           TunnelEndPoint dst,
                                           Annotations... annotations) {
        Set<TunnelSubscription> orderSet = orderRelationship.get(appId);
        if (orderSet == null) {
            orderSet = new HashSet<TunnelSubscription>();
        }
        TunnelSubscription order = new TunnelSubscription(appId, src, dst, null, null, null, annotations);
        boolean isExist = orderSet.contains(order);
        if (!isExist) {
            orderSet.add(order);
        }
        orderRelationship.put(appId, orderSet);
        TunnelKey key = TunnelKey.tunnelKey(src, dst);
        Set<TunnelId> idSet = srcAndDstKeyMap.get(key);
        if (idSet == null || idSet.isEmpty()) {
            return Collections.emptySet();
        }
        Collection<Tunnel> tunnelSet = new HashSet<Tunnel>();
        for (TunnelId tunnelId : idSet) {
            Tunnel result = tunnelIdAsKeyStore.get(tunnelId);
            if (Tunnel.State.ACTIVE.equals(result.state())) {
                tunnelSet.add(result);
            }
        }
        return tunnelSet;
    }

    @Override
    public Collection<Tunnel> borrowTunnel(ApplicationId appId,
                                           TunnelEndPoint src,
                                           TunnelEndPoint dst, Type type,
                                           Annotations... annotations) {
        Set<TunnelSubscription> orderSet = orderRelationship.get(appId);
        if (orderSet == null) {
            orderSet = new HashSet<TunnelSubscription>();
        }
        TunnelSubscription order = new TunnelSubscription(appId, src, dst, null, type, null, annotations);
        boolean isExist = orderSet.contains(order);
        if (!isExist) {
            orderSet.add(order);
        }
        orderRelationship.put(appId, orderSet);
        TunnelKey key = TunnelKey.tunnelKey(src, dst);
        Set<TunnelId> idSet = srcAndDstKeyMap.get(key);
        if (idSet == null || idSet.isEmpty()) {
            return Collections.emptySet();
        }
        Collection<Tunnel> tunnelSet = new HashSet<Tunnel>();
        for (TunnelId tunnelId : idSet) {
            Tunnel result = tunnelIdAsKeyStore.get(tunnelId);
            if (type.equals(result.type())
                    && Tunnel.State.ACTIVE.equals(result.state())) {
                tunnelSet.add(result);
            }
        }
        return tunnelSet;
    }

    @Override
    public Collection<Tunnel> borrowTunnel(ApplicationId appId,
                                           TunnelName tunnelName,
                                           Annotations... annotations) {
        Set<TunnelSubscription> orderSet = orderRelationship.get(appId);
        if (orderSet == null) {
            orderSet = new HashSet<TunnelSubscription>();
        }
        TunnelSubscription order = new TunnelSubscription(appId, null, null, null, null, tunnelName,
                                annotations);
        boolean isExist = orderSet.contains(order);

        Set<TunnelId> idSet = tunnelNameAsKeyMap.get(tunnelName);
        if (idSet == null || idSet.isEmpty()) {
            return Collections.emptySet();
        }
        Collection<Tunnel> tunnelSet = new HashSet<Tunnel>();
        for (TunnelId tunnelId : idSet) {
            Tunnel result = tunnelIdAsKeyStore.get(tunnelId);
            if (Tunnel.State.ACTIVE.equals(result.state())) {
                tunnelSet.add(result);
            }
        }

        if (!tunnelSet.isEmpty() && !isExist) {
            orderSet.add(order);
            orderRelationship.put(appId, orderSet);
        }

        return tunnelSet;
    }

    @Override
    public boolean returnTunnel(ApplicationId appId, TunnelName tunnelName,
                                Annotations... annotations) {
        TunnelSubscription order = new TunnelSubscription(appId, null, null, null, null, tunnelName,
                                annotations);
        return deleteOrder(order);
    }

    @Override
    public boolean returnTunnel(ApplicationId appId, TunnelId tunnelId,
                                Annotations... annotations) {
        TunnelSubscription order = new TunnelSubscription(appId, null, null, tunnelId, null, null,
                                annotations);
        return deleteOrder(order);
    }

    @Override
    public boolean returnTunnel(ApplicationId appId, TunnelEndPoint src,
                                TunnelEndPoint dst, Type type,
                                Annotations... annotations) {
        TunnelSubscription order = new TunnelSubscription(appId, src, dst, null, type, null, annotations);
        return deleteOrder(order);
    }

    @Override
    public boolean returnTunnel(ApplicationId appId, TunnelEndPoint src,
                                TunnelEndPoint dst, Annotations... annotations) {
        TunnelSubscription order = new TunnelSubscription(appId, src, dst, null, null, null, annotations);
        return deleteOrder(order);
    }

    private boolean deleteOrder(TunnelSubscription order) {
        Set<TunnelSubscription> orderSet = orderRelationship.get(order.consumerId());
        if (orderSet == null) {
            return true;
        }
        if (orderSet.contains(order)) {
            orderSet.remove(order);
            return true;
        }
        return false;
    }

    @Override
    public Tunnel queryTunnel(TunnelId tunnelId) {
        return tunnelIdAsKeyStore.get(tunnelId);
    }

    @Override
    public Collection<TunnelSubscription> queryTunnelSubscription(ApplicationId appId) {
        return orderRelationship.get(appId) != null ? ImmutableSet.copyOf(orderRelationship
                .get(appId)) : Collections.emptySet();
    }

    @Override
    public Collection<Tunnel> queryTunnel(Type type) {
        Collection<Tunnel> result = new HashSet<Tunnel>();
        Set<TunnelId> tunnelIds = typeKeyMap.get(type);
        if (tunnelIds == null) {
            return Collections.emptySet();
        }
        for (TunnelId id : tunnelIds) {
            result.add(tunnelIdAsKeyStore.get(id));
        }
        return result.isEmpty() ? Collections.emptySet() : ImmutableSet
                .copyOf(result);
    }

    @Override
    public Collection<Tunnel> queryTunnel(TunnelEndPoint src, TunnelEndPoint dst) {
        Collection<Tunnel> result = new HashSet<Tunnel>();
        TunnelKey key = TunnelKey.tunnelKey(src, dst);
        Set<TunnelId> tunnelIds = srcAndDstKeyMap.get(key);
        if (tunnelIds == null) {
            return Collections.emptySet();
        }
        for (TunnelId id : tunnelIds) {
            result.add(tunnelIdAsKeyStore.get(id));
        }
        return result.isEmpty() ? Collections.emptySet() : ImmutableSet
                .copyOf(result);
    }

    @Override
    public Collection<Tunnel> queryAllTunnels() {
        return tunnelIdAsKeyStore.values();
    }

    @Override
    public int tunnelCount() {
        return tunnelIdAsKeyStore.size();
    }

    /**
     * Uses source TunnelPoint and destination TunnelPoint as map key.
     */
    private static final class TunnelKey {
        private final TunnelEndPoint src;
        private final TunnelEndPoint dst;

        private TunnelKey(TunnelEndPoint src, TunnelEndPoint dst) {
            this.src = src;
            this.dst = dst;

        }

        /**
         * create a map key.
         *
         * @param src
         * @param dst
         * @return a key using source ip and destination ip
         */
        static TunnelKey tunnelKey(TunnelEndPoint src, TunnelEndPoint dst) {
            return new TunnelKey(src, dst);
        }

        @Override
        public int hashCode() {
            return Objects.hash(src, dst);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof TunnelKey) {
                final TunnelKey other = (TunnelKey) obj;
                return Objects.equals(this.src, other.src)
                        && Objects.equals(this.dst, other.dst);
            }
            return false;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass()).add("src", src)
                    .add("dst", dst).toString();
        }
    }

    /**
     * Eventually consistent map listener for tunnel change event which updated the local map based on event.
     */
    private class InternalTunnelChangeEventListener
            implements EventuallyConsistentMapListener<TunnelId, Tunnel> {
        @Override
        public void event(EventuallyConsistentMapEvent<TunnelId, Tunnel> event) {
            TunnelId tunnelId = event.key();
            Tunnel tunnel = event.value();

            if (event.type() == PUT) {

                // Update tunnel name map
                Set<TunnelId> tunnelNameSet = tunnelNameAsKeyMap.get(tunnel
                        .tunnelName());
                if (tunnelNameSet == null) {
                    tunnelNameSet = new HashSet<TunnelId>();
                }
                tunnelNameSet.add(tunnelId);
                tunnelNameAsKeyMap.put(tunnel.tunnelName(), tunnelNameSet);

                // Update tunnel source and destination map
                TunnelKey key = TunnelKey.tunnelKey(tunnel.src(), tunnel.dst());
                Set<TunnelId> srcAndDstKeySet = srcAndDstKeyMap.get(key);
                if (srcAndDstKeySet == null) {
                    srcAndDstKeySet = new HashSet<TunnelId>();
                }
                srcAndDstKeySet.add(tunnelId);
                srcAndDstKeyMap.put(key, srcAndDstKeySet);

                // Update tunnel type map
                Set<TunnelId> typeKeySet = typeKeyMap.get(tunnel.type());
                if (typeKeySet == null) {
                    typeKeySet = new HashSet<TunnelId>();
                }
                typeKeySet.add(tunnelId);
                typeKeyMap.put(tunnel.type(), typeKeySet);
            } else if (event.type() == REMOVE) {

                // Update tunnel name map
                tunnelNameAsKeyMap.get(tunnel.tunnelName()).remove(tunnelId);
                if (tunnelNameAsKeyMap.get(tunnel.tunnelName()).isEmpty()) {
                    tunnelNameAsKeyMap.remove(tunnel.tunnelName());
                }

                // Update tunnel source and destination map
                TunnelKey key = TunnelKey.tunnelKey(tunnel.src(), tunnel.dst());
                srcAndDstKeyMap.get(key).remove(tunnelId);
                if (srcAndDstKeyMap.get(key).isEmpty()) {
                    srcAndDstKeyMap.remove(key);
                }

                // Update tunnel type map
                typeKeyMap.get(tunnel.type()).remove(tunnelId);
                if (typeKeyMap.get(tunnel.type()).isEmpty()) {
                    typeKeyMap.remove(tunnel.type());
                }
            }
        }
    }
}
