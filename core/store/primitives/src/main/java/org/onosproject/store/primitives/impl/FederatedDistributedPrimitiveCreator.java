/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.store.primitives.impl;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.hash.Funnel;
import com.google.common.hash.Funnels;
import com.google.common.hash.Hashing;
import org.onlab.util.HexString;
import org.onosproject.cluster.PartitionId;
import org.onosproject.store.primitives.DistributedPrimitiveCreator;
import org.onosproject.store.service.AsyncAtomicCounter;
import org.onosproject.store.service.AsyncAtomicCounterMap;
import org.onosproject.store.service.AsyncAtomicIdGenerator;
import org.onosproject.store.service.AsyncAtomicValue;
import org.onosproject.store.service.AsyncConsistentMap;
import org.onosproject.store.service.AsyncConsistentMultimap;
import org.onosproject.store.service.AsyncConsistentTreeMap;
import org.onosproject.store.service.AsyncDistributedLock;
import org.onosproject.store.service.AsyncDistributedSet;
import org.onosproject.store.service.AsyncDocumentTree;
import org.onosproject.store.service.AsyncLeaderElector;
import org.onosproject.store.service.AtomicCounterMapOptions;
import org.onosproject.store.service.AtomicCounterOptions;
import org.onosproject.store.service.AtomicIdGeneratorOptions;
import org.onosproject.store.service.AtomicValueOptions;
import org.onosproject.store.service.ConsistentMapOptions;
import org.onosproject.store.service.ConsistentMultimapOptions;
import org.onosproject.store.service.ConsistentTreeMapOptions;
import org.onosproject.store.service.DistributedLockOptions;
import org.onosproject.store.service.DistributedSetOptions;
import org.onosproject.store.service.DocumentPath;
import org.onosproject.store.service.DocumentTreeOptions;
import org.onosproject.store.service.LeaderElectorOptions;
import org.onosproject.store.service.WorkQueue;
import org.onosproject.store.service.WorkQueueOptions;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@code DistributedPrimitiveCreator} that federates responsibility for creating
 * distributed primitives to a collection of other {@link DistributedPrimitiveCreator creators}.
 */
public class FederatedDistributedPrimitiveCreator implements DistributedPrimitiveCreator {

    private static final Funnel<Iterable<? extends CharSequence>> STR_LIST_FUNNEL =
                Funnels.sequentialFunnel(Funnels.unencodedCharsFunnel());

    private final TreeMap<PartitionId, DistributedPrimitiveCreator> members;
    private final List<PartitionId> sortedMemberPartitionIds;
    private final int buckets;

    public FederatedDistributedPrimitiveCreator(Map<PartitionId, DistributedPrimitiveCreator> members, int buckets) {
        this.members = Maps.newTreeMap();
        this.members.putAll(checkNotNull(members));
        this.sortedMemberPartitionIds = Lists.newArrayList(members.keySet());
        this.buckets = buckets;
    }

    @Override
    public <K, V> AsyncConsistentMap<K, V> newAsyncConsistentMap(ConsistentMapOptions options) {
        Map<PartitionId, AsyncConsistentMap<byte[], byte[]>> maps =
                Maps.transformValues(members,
                                     partition -> DistributedPrimitives.newTranscodingMap(
                                             partition.<String, byte[]>newAsyncConsistentMap(options.name(), null),
                                             HexString::toHexString,
                                             HexString::fromHexString,
                                             Function.identity(),
                                             Function.identity()));
        Hasher<byte[]> hasher = key -> {
            int bucket = Math.abs(Hashing.murmur3_32().hashBytes(key).asInt()) % buckets;
            return sortedMemberPartitionIds.get(Hashing.consistentHash(bucket, sortedMemberPartitionIds.size()));
        };
        AsyncConsistentMap<byte[], byte[]> partitionedMap =
            new PartitionedAsyncConsistentMap<>(options.name(), maps, hasher);
        return DistributedPrimitives.newTranscodingMap(partitionedMap,
                key -> options.serializer().encode(key),
                bytes -> options.serializer().decode(bytes),
                value -> value == null ? null : options.serializer().encode(value),
                bytes -> options.serializer().decode(bytes));
    }

    @Override
    public <V> AsyncConsistentTreeMap<V> newAsyncConsistentTreeMap(ConsistentTreeMapOptions options) {
        return getCreator(options.name()).newAsyncConsistentTreeMap(options.name(), options.serializer());
    }

    @Override
    public <K, V> AsyncConsistentMultimap<K, V> newAsyncConsistentSetMultimap(ConsistentMultimapOptions options) {
        return getCreator(options.name()).newAsyncConsistentSetMultimap(options);
    }

    @Override
    public <E> AsyncDistributedSet<E> newAsyncDistributedSet(DistributedSetOptions options) {
        return DistributedPrimitives.newSetFromMap(newAsyncConsistentMap(options.name(), options.serializer()));
    }

    @Override
    public <K> AsyncAtomicCounterMap<K> newAsyncAtomicCounterMap(AtomicCounterMapOptions options) {
        return getCreator(options.name()).newAsyncAtomicCounterMap(options);
    }

    @Override
    public AsyncAtomicCounter newAsyncCounter(AtomicCounterOptions options) {
        return getCreator(options.name()).newAsyncCounter(options);
    }

    @Override
    public AsyncAtomicIdGenerator newAsyncIdGenerator(AtomicIdGeneratorOptions options) {
        return getCreator(options.name()).newAsyncIdGenerator(options);
    }

    @Override
    public <V> AsyncAtomicValue<V> newAsyncAtomicValue(AtomicValueOptions options) {
        return getCreator(options.name()).newAsyncAtomicValue(options);
    }

    @Override
    public AsyncDistributedLock newAsyncDistributedLock(DistributedLockOptions options) {
        return getCreator(options.name()).newAsyncDistributedLock(options);
    }

    @Override
    public AsyncLeaderElector newAsyncLeaderElector(LeaderElectorOptions options) {
        Map<PartitionId, AsyncLeaderElector> leaderElectors =
                Maps.transformValues(members,
                                     partition -> partition.newAsyncLeaderElector(options));
        Hasher<String> hasher = topic -> {
            int hashCode = Hashing.sha256().hashString(topic, Charsets.UTF_8).asInt();
            return sortedMemberPartitionIds.get(Math.abs(hashCode) % members.size());
        };
        return new PartitionedAsyncLeaderElector(options.name(), leaderElectors, hasher);
    }

    @Override
    public <E> WorkQueue<E> newWorkQueue(WorkQueueOptions options) {
        return getCreator(options.name()).newWorkQueue(options);
    }

    @Override
    public <V> AsyncDocumentTree<V> newAsyncDocumentTree(DocumentTreeOptions options) {
        Map<PartitionId, AsyncDocumentTree<V>> trees =
                Maps.transformValues(members, part -> part.<V>newAsyncDocumentTree(options));
        Hasher<DocumentPath> hasher = key -> {
            int bucket = (key == null) ? 0 :
                    Math.abs(Hashing.murmur3_32()
                                  .hashObject(key.pathElements(), STR_LIST_FUNNEL)
                                  .asInt()) % buckets;
            return sortedMemberPartitionIds.get(Hashing.consistentHash(bucket, sortedMemberPartitionIds.size()));
        };
        return new PartitionedAsyncDocumentTree<>(options.name(), trees, hasher);
    }

    @Override
    public Set<String> getAsyncConsistentMapNames() {
        return members.values()
                      .stream()
                      .map(DistributedPrimitiveCreator::getAsyncConsistentMapNames)
                      .reduce(Sets::union)
                      .orElse(ImmutableSet.of());
    }

    @Override
    public Set<String> getAsyncAtomicCounterNames() {
        return members.values()
                      .stream()
                      .map(DistributedPrimitiveCreator::getAsyncAtomicCounterNames)
                      .reduce(Sets::union)
                      .orElse(ImmutableSet.of());
    }

    @Override
    public Set<String> getWorkQueueNames() {
        return members.values()
                      .stream()
                      .map(DistributedPrimitiveCreator::getWorkQueueNames)
                      .reduce(Sets::union)
                      .orElse(ImmutableSet.of());
    }

    /**
     * Returns the {@code DistributedPrimitiveCreator} to use for hosting a primitive.
     * @param name primitive name
     * @return primitive creator
     */
    private DistributedPrimitiveCreator getCreator(String name) {
        int hashCode = Hashing.sha256().hashString(name, Charsets.UTF_8).asInt();
        return members.get(sortedMemberPartitionIds.get(Math.abs(hashCode) % members.size()));
    }
}
