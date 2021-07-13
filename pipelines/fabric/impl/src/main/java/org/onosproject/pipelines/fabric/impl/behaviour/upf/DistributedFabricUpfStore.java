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
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import org.onlab.util.ImmutableByteSequence;
import org.onlab.util.KryoNamespace;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.WallClockTimestamp;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.stream.Collectors;

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

    // EC map to remember the mapping far_id -> rule_id this is mostly used during reads,
    // it can be definitely removed by simplifying the logical pipeline
    protected EventuallyConsistentMap<Integer, UpfRuleIdentifier> reverseFarIdMap;

    @Activate
    protected void activate() {
        // Allow unit test to inject reverseFarIdMap here.
        if (storageService != null) {
            this.reverseFarIdMap = storageService.<Integer, UpfRuleIdentifier>eventuallyConsistentMapBuilder()
                    .withName(FAR_ID_MAP_NAME)
                    .withSerializer(SERIALIZER)
                    .withTimestampProvider((k, v) -> new WallClockTimestamp())
                    .build();
        }

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        reverseFarIdMap.destroy();

        log.info("Stopped");
    }

    @Override
    public void reset() {
        reverseFarIdMap.clear();
    }

    @Override
    public Map<Integer, UpfRuleIdentifier> getReverseFarIdMap() {
        return reverseFarIdMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public int globalFarIdOf(UpfRuleIdentifier farIdPair) {
        int globalFarId = getGlobalFarIdOf(farIdPair);
        reverseFarIdMap.put(globalFarId, farIdPair);
        log.info("{} translated to GlobalFarId={}", farIdPair, globalFarId);
        return globalFarId;
    }

    @Override
    public int removeGlobalFarId(UpfRuleIdentifier farIdPair) {
        int globalFarId = getGlobalFarIdOf(farIdPair);
        reverseFarIdMap.remove(globalFarId);
        return globalFarId;
    }

    @Override
    public int globalFarIdOf(ImmutableByteSequence pfcpSessionId, int sessionLocalFarId) {
        UpfRuleIdentifier farId = new UpfRuleIdentifier(pfcpSessionId, sessionLocalFarId);
        return globalFarIdOf(farId);
    }

    @Override
    public int removeGlobalFarId(ImmutableByteSequence pfcpSessionId, int sessionLocalFarId) {
        UpfRuleIdentifier farId = new UpfRuleIdentifier(pfcpSessionId, sessionLocalFarId);
        return removeGlobalFarId(farId);
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

    // Compute global far id by hashing the pfcp session id and the session local far
    private int getGlobalFarIdOf(UpfRuleIdentifier farIdPair) {
        HashFunction hashFunction = Hashing.murmur3_32();
        HashCode hashCode = hashFunction.newHasher()
                .putInt(farIdPair.getSessionLocalId())
                .putBytes(farIdPair.getPfcpSessionId().asArray())
                .hash();
        return hashCode.asInt();
    }

}
