/*
 * Copyright 2014-2015 Open Networking Laboratory
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
import java.util.Objects;
import java.util.Set;

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
import org.onosproject.store.app.GossipApplicationStore.InternalState;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.MultiValuedTimestamp;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.WallClockTimestamp;
import org.slf4j.Logger;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;

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
    private String runnelOpTopoic = "tunnel-ops-ids";

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
    // tunnel name as map key in the store.
    private EventuallyConsistentMap<TunnelName, Set<TunnelId>> tunnelNameAsKeyStore;
    // maintains all the tunnels between source and destination.
    private EventuallyConsistentMap<TunnelKey, Set<TunnelId>> srcAndDstKeyStore;
    // maintains all the tunnels by tunnel type.
    private EventuallyConsistentMap<Tunnel.Type, Set<TunnelId>> typeKeyStore;
    // maintains records that app subscribes tunnel.
    private EventuallyConsistentMap<ApplicationId, Set<TunnelSubscription>> orderRelationship;

    private IdGenerator idGenerator;

    @Activate
    public void activate() {
        KryoNamespace.Builder serializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API)
                .register(MultiValuedTimestamp.class)
                .register(InternalState.class);
        tunnelIdAsKeyStore = storageService
                .<TunnelId, Tunnel>eventuallyConsistentMapBuilder()
                .withName("all_tunnel").withSerializer(serializer)
                .withTimestampProvider((k, v) -> new WallClockTimestamp()).build();
        tunnelNameAsKeyStore = storageService
                .<TunnelName, Set<TunnelId>>eventuallyConsistentMapBuilder()
                .withName("tunnel_name_tunnel").withSerializer(serializer)
                .withTimestampProvider((k, v) -> new WallClockTimestamp()).build();
        srcAndDstKeyStore = storageService
                .<TunnelKey, Set<TunnelId>>eventuallyConsistentMapBuilder()
                .withName("src_dst_tunnel").withSerializer(serializer)
                .withTimestampProvider((k, v) -> new WallClockTimestamp()).build();
        typeKeyStore = storageService
                .<Tunnel.Type, Set<TunnelId>>eventuallyConsistentMapBuilder()
                .withName("type_tunnel").withSerializer(serializer)
                .withTimestampProvider((k, v) -> new WallClockTimestamp()).build();
        orderRelationship = storageService
                .<ApplicationId, Set<TunnelSubscription>>eventuallyConsistentMapBuilder()
                .withName("type_tunnel").withSerializer(serializer)
                .withTimestampProvider((k, v) -> new WallClockTimestamp()).build();
        idGenerator = coreService.getIdGenerator(runnelOpTopoic);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        orderRelationship.destroy();
        tunnelIdAsKeyStore.destroy();
        srcAndDstKeyStore.destroy();
        typeKeyStore.destroy();
        tunnelNameAsKeyStore.destroy();
        log.info("Stopped");
    }

    @Override
    public TunnelId createOrUpdateTunnel(Tunnel tunnel) {
        // tunnelIdAsKeyStore.
        if (tunnel.tunnelId() != null && !"".equals(tunnel.tunnelId().toString())) {
            Tunnel old = tunnelIdAsKeyStore.get(tunnel.tunnelId());
            if (old == null) {
                log.info("This tunnel[" + tunnel.tunnelId() + "] is not available.");
                return tunnel.tunnelId();
            }
            DefaultAnnotations oldAnno = (DefaultAnnotations) old.annotations();
            SparseAnnotations newAnno = (SparseAnnotations) tunnel.annotations();
            Tunnel newT = new DefaultTunnel(old.providerId(), old.src(),
                                            old.dst(), old.type(),
                                            old.state(), old.groupId(),
                                            old.tunnelId(),
                                            old.tunnelName(),
                                            old.path(),
                                            DefaultAnnotations.merge(oldAnno, newAnno));
            tunnelIdAsKeyStore.put(tunnel.tunnelId(), newT);
            TunnelEvent event = new TunnelEvent(
                                                TunnelEvent.Type.TUNNEL_UPDATED,
                                                tunnel);
            notifyDelegate(event);
            return tunnel.tunnelId();
        } else {
            TunnelId tunnelId = TunnelId.valueOf(idGenerator.getNewId());
            Tunnel newT = new DefaultTunnel(tunnel.providerId(), tunnel.src(),
                                            tunnel.dst(), tunnel.type(),
                                            tunnel.state(), tunnel.groupId(),
                                            tunnelId,
                                            tunnel.tunnelName(),
                                            tunnel.path(),
                                            tunnel.annotations());
            TunnelKey key = TunnelKey.tunnelKey(tunnel.src(), tunnel.dst());
            tunnelIdAsKeyStore.put(tunnelId, newT);
            Set<TunnelId> tunnelnameSet = tunnelNameAsKeyStore.get(tunnel
                    .tunnelName());
            if (tunnelnameSet == null) {
                tunnelnameSet = new HashSet<TunnelId>();
            }
            tunnelnameSet.add(tunnelId);
            tunnelNameAsKeyStore.put(tunnel
                    .tunnelName(), tunnelnameSet);
            Set<TunnelId> srcAndDstKeySet = srcAndDstKeyStore.get(key);
            if (srcAndDstKeySet == null) {
                srcAndDstKeySet = new HashSet<TunnelId>();
            }
            srcAndDstKeySet.add(tunnelId);
            srcAndDstKeyStore.put(key, srcAndDstKeySet);
            Set<TunnelId> typeKeySet = typeKeyStore.get(tunnel.type());
            if (typeKeySet == null) {
                typeKeySet = new HashSet<TunnelId>();
            }
            typeKeySet.add(tunnelId);
            typeKeyStore.put(tunnel.type(), typeKeySet);
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
        tunnelNameAsKeyStore.get(deletedTunnel.tunnelName()).remove(tunnelId);
        tunnelIdAsKeyStore.remove(tunnelId);
        TunnelKey key = new TunnelKey(deletedTunnel.src(), deletedTunnel.dst());
        srcAndDstKeyStore.get(key).remove(tunnelId);
        typeKeyStore.get(deletedTunnel.type()).remove(tunnelId);
        TunnelEvent event = new TunnelEvent(TunnelEvent.Type.TUNNEL_REMOVED,
                                            deletedTunnel);
        notifyDelegate(event);
    }

    @Override
    public void deleteTunnel(TunnelEndPoint src, TunnelEndPoint dst,
                             ProviderId producerName) {
        TunnelKey key = TunnelKey.tunnelKey(src, dst);
        Set<TunnelId> idSet = srcAndDstKeyStore.get(key);
        if (idSet == null) {
            return;
        }
        Tunnel deletedTunnel = null;
        TunnelEvent event = null;
        List<TunnelEvent> ls = new ArrayList<TunnelEvent>();
        for (TunnelId id : idSet) {
            deletedTunnel = tunnelIdAsKeyStore.get(id);
            event = new TunnelEvent(TunnelEvent.Type.TUNNEL_REMOVED,
                                    deletedTunnel);
            ls.add(event);
            if (producerName.equals(deletedTunnel.providerId())) {
                tunnelIdAsKeyStore.remove(deletedTunnel.tunnelId());
                tunnelNameAsKeyStore.get(deletedTunnel.tunnelName())
                        .remove(deletedTunnel.tunnelId());
                srcAndDstKeyStore.get(key).remove(deletedTunnel.tunnelId());
                typeKeyStore.get(deletedTunnel.type())
                        .remove(deletedTunnel.tunnelId());
            }
        }
        notifyDelegate(ls);
    }

    @Override
    public void deleteTunnel(TunnelEndPoint src, TunnelEndPoint dst, Type type,
                             ProviderId producerName) {
        TunnelKey key = TunnelKey.tunnelKey(src, dst);
        Set<TunnelId> idSet = srcAndDstKeyStore.get(key);
        if (idSet == null) {
            return;
        }
        Tunnel deletedTunnel = null;
        TunnelEvent event = null;
        List<TunnelEvent> ls = new ArrayList<TunnelEvent>();
        for (TunnelId id : idSet) {
            deletedTunnel = tunnelIdAsKeyStore.get(id);
            event = new TunnelEvent(TunnelEvent.Type.TUNNEL_REMOVED,
                                    deletedTunnel);
            ls.add(event);
            if (producerName.equals(deletedTunnel.providerId())
                    && type.equals(deletedTunnel.type())) {
                tunnelIdAsKeyStore.remove(deletedTunnel.tunnelId());
                tunnelNameAsKeyStore.get(deletedTunnel.tunnelName())
                        .remove(deletedTunnel.tunnelId());
                srcAndDstKeyStore.get(key).remove(deletedTunnel.tunnelId());
                typeKeyStore.get(deletedTunnel.type())
                        .remove(deletedTunnel.tunnelId());
            }
        }
        notifyDelegate(ls);
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
        Set<TunnelId> idSet = srcAndDstKeyStore.get(key);
        if (idSet == null || idSet.size() == 0) {
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
        Set<TunnelId> idSet = srcAndDstKeyStore.get(key);
        if (idSet == null || idSet.size() == 0) {
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
        if (!isExist) {
            orderSet.add(order);
        }
        orderRelationship.put(appId, orderSet);
        Set<TunnelId> idSet = tunnelNameAsKeyStore.get(tunnelName);
        if (idSet == null || idSet.size() == 0) {
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
        Set<TunnelId> tunnelIds = typeKeyStore.get(type);
        if (tunnelIds == null) {
            return Collections.emptySet();
        }
        for (TunnelId id : tunnelIds) {
            result.add(tunnelIdAsKeyStore.get(id));
        }
        return result.size() == 0 ? Collections.emptySet() : ImmutableSet
                .copyOf(result);
    }

    @Override
    public Collection<Tunnel> queryTunnel(TunnelEndPoint src, TunnelEndPoint dst) {
        Collection<Tunnel> result = new HashSet<Tunnel>();
        TunnelKey key = TunnelKey.tunnelKey(src, dst);
        Set<TunnelId> tunnelIds = srcAndDstKeyStore.get(key);
        if (tunnelIds == null) {
            return Collections.emptySet();
        }
        for (TunnelId id : tunnelIds) {
            result.add(tunnelIdAsKeyStore.get(id));
        }
        return result.size() == 0 ? Collections.emptySet() : ImmutableSet
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
}
