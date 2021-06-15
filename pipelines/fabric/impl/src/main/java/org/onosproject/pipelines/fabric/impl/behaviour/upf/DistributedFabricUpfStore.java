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

package org.onosproject.pipelines.fabric.impl.behaviour.upf;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Maps;
import org.onlab.packet.Ip4Address;
import org.onlab.util.ImmutableByteSequence;
import org.onlab.util.KryoNamespace;
import org.onosproject.net.behaviour.upf.PacketDetectionRule;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.DistributedSet;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Distributed implementation of FabricUpfStore.
 */
// FIXME: this store is generic and not tied to a single device, should we have a store based on deviceId?
@Component(immediate = true, service = FabricUpfStore.class)
public final class DistributedFabricUpfStore implements FabricUpfStore {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    protected static final String FAR_ID_MAP_NAME = "fabric-upf-far-id";
    protected static final String BUFFER_FAR_ID_SET_NAME = "fabric-upf-buffer-far-id";
    protected static final String FAR_ID_UE_MAP_NAME = "fabric-upf-far-id-ue";
    protected static final KryoNamespace.Builder SERIALIZER = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(UpfRuleIdentifier.class);

    // TODO: check queue IDs for BMv2, is priority inverted?
    // Mapping between scheduling priority ranges with BMv2 priority queues
    private static final BiMap<Integer, Integer> SCHEDULING_PRIORITY_MAP
            = new ImmutableBiMap.Builder<Integer, Integer>()
            // Highest scheduling priority for 3GPP is 1 and highest BMv2 queue priority is 7
            .put(1, 5)
            .put(6, 4)
            .put(7, 3)
            .put(8, 2)
            .put(9, 1)
            .build();

    // Distributed local FAR ID to global FAR ID mapping
    protected ConsistentMap<UpfRuleIdentifier, Integer> farIdMap;
    private MapEventListener<UpfRuleIdentifier, Integer> farIdMapListener;
    // Local, reversed copy of farIdMapper for better reverse lookup performance
    protected Map<Integer, UpfRuleIdentifier> reverseFarIdMap;
    private int nextGlobalFarId = 1;

    protected DistributedSet<UpfRuleIdentifier> bufferFarIds;
    protected ConsistentMap<UpfRuleIdentifier, Set<Ip4Address>> farIdToUeAddrs;

    @Activate
    protected void activate() {
        // Allow unit test to inject farIdMap here.
        if (storageService != null) {
            this.farIdMap = storageService.<UpfRuleIdentifier, Integer>consistentMapBuilder()
                    .withName(FAR_ID_MAP_NAME)
                    .withRelaxedReadConsistency()
                    .withSerializer(Serializer.using(SERIALIZER.build()))
                    .build();
            this.bufferFarIds = storageService.<UpfRuleIdentifier>setBuilder()
                    .withName(BUFFER_FAR_ID_SET_NAME)
                    .withRelaxedReadConsistency()
                    .withSerializer(Serializer.using(SERIALIZER.build()))
                    .build().asDistributedSet();
            this.farIdToUeAddrs = storageService.<UpfRuleIdentifier, Set<Ip4Address>>consistentMapBuilder()
                    .withName(FAR_ID_UE_MAP_NAME)
                    .withRelaxedReadConsistency()
                    .withSerializer(Serializer.using(SERIALIZER.build()))
                    .build();

        }
        farIdMapListener = new FarIdMapListener();
        farIdMap.addListener(farIdMapListener);

        reverseFarIdMap = Maps.newHashMap();
        farIdMap.entrySet().forEach(entry -> reverseFarIdMap.put(entry.getValue().value(), entry.getKey()));

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        farIdMap.removeListener(farIdMapListener);
        farIdMap.destroy();
        reverseFarIdMap.clear();

        log.info("Stopped");
    }

    @Override
    public void reset() {
        farIdMap.clear();
        reverseFarIdMap.clear();
        bufferFarIds.clear();
        farIdToUeAddrs.clear();
        nextGlobalFarId = 0;
    }

    @Override
    public Map<UpfRuleIdentifier, Integer> getFarIdMap() {
        return Map.copyOf(farIdMap.asJavaMap());
    }

    @Override
    public int globalFarIdOf(UpfRuleIdentifier farIdPair) {
        int globalFarId = farIdMap.compute(farIdPair,
                (k, existingId) -> {
                    return Objects.requireNonNullElseGet(existingId, () -> nextGlobalFarId++);
                }).value();
        log.info("{} translated to GlobalFarId={}", farIdPair, globalFarId);
        return globalFarId;
    }

    @Override
    public int globalFarIdOf(ImmutableByteSequence pfcpSessionId, int sessionLocalFarId) {
        UpfRuleIdentifier farId = new UpfRuleIdentifier(pfcpSessionId, sessionLocalFarId);
        return globalFarIdOf(farId);

    }

    @Override
    public String queueIdOf(int schedulingPriority) {
        return (SCHEDULING_PRIORITY_MAP.get(schedulingPriority)).toString();
    }

    @Override
    public String schedulingPriorityOf(int queueId) {
        return (SCHEDULING_PRIORITY_MAP.inverse().get(queueId)).toString();
    }

    @Override
    public UpfRuleIdentifier localFarIdOf(int globalFarId) {
        return reverseFarIdMap.get(globalFarId);
    }

    public void learnFarIdToUeAddrs(PacketDetectionRule pdr) {
        UpfRuleIdentifier ruleId = UpfRuleIdentifier.of(pdr.sessionId(), pdr.farId());
        farIdToUeAddrs.compute(ruleId, (k, set) -> {
            if (set == null) {
                set = new HashSet<>();
            }
            set.add(pdr.ueAddress());
            return set;
        });
    }

    @Override
    public boolean isFarIdBuffering(UpfRuleIdentifier farId) {
        checkNotNull(farId);
        return bufferFarIds.contains(farId);
    }

    @Override
    public void learBufferingFarId(UpfRuleIdentifier farId) {
        checkNotNull(farId);
        bufferFarIds.add(farId);
    }

    @Override
    public void forgetBufferingFarId(UpfRuleIdentifier farId) {
        checkNotNull(farId);
        bufferFarIds.remove(farId);
    }

    @Override
    public void forgetUeAddr(Ip4Address ueAddr) {
        farIdToUeAddrs.keySet().forEach(
                farId -> farIdToUeAddrs.computeIfPresent(farId, (farIdz, ueAddrs) -> {
                    ueAddrs.remove(ueAddr);
                    return ueAddrs;
                }));
    }

    @Override
    public Set<Ip4Address> ueAddrsOfFarId(UpfRuleIdentifier farId) {
        return farIdToUeAddrs.getOrDefault(farId, Set.of()).value();
    }

    @Override
    public Set<UpfRuleIdentifier> getBufferFarIds() {
        return Set.copyOf(bufferFarIds);
    }

    @Override
    public Map<UpfRuleIdentifier, Set<Ip4Address>> getFarIdToUeAddrs() {
        return Map.copyOf(farIdToUeAddrs.asJavaMap());
    }

    // NOTE: FarIdMapListener is run on the same thread intentionally in order to ensure that
    //       reverseFarIdMap update always finishes right after farIdMap is updated
    private class FarIdMapListener implements MapEventListener<UpfRuleIdentifier, Integer> {
        @Override
        public void event(MapEvent<UpfRuleIdentifier, Integer> event) {
            switch (event.type()) {
                case INSERT:
                    reverseFarIdMap.put(event.newValue().value(), event.key());
                    break;
                case UPDATE:
                    reverseFarIdMap.remove(event.oldValue().value());
                    reverseFarIdMap.put(event.newValue().value(), event.key());
                    break;
                case REMOVE:
                    reverseFarIdMap.remove(event.oldValue().value());
                    break;
                default:
                    break;
            }
        }
    }
}
