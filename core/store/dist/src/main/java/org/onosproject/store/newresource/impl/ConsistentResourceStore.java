/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.store.newresource.impl;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableList;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.net.newresource.ResourceConsumer;
import org.onosproject.net.newresource.ResourceEvent;
import org.onosproject.net.newresource.ResourcePath;
import org.onosproject.net.newresource.ResourceStore;
import org.onosproject.net.newresource.ResourceStoreDelegate;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.TransactionContext;
import org.onosproject.store.service.TransactionalMap;
import org.onosproject.store.service.Versioned;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.newresource.ResourceEvent.Type.*;

/**
 * Implementation of ResourceStore using TransactionalMap.
 */
@Component(immediate = true)
@Service
@Beta
public class ConsistentResourceStore extends AbstractStore<ResourceEvent, ResourceStoreDelegate>
        implements ResourceStore {
    private static final Logger log = LoggerFactory.getLogger(ConsistentResourceStore.class);

    private static final String CONSUMER_MAP = "onos-resource-consumers";
    private static final String CHILD_MAP = "onos-resource-children";
    private static final Serializer SERIALIZER = Serializer.using(
            Arrays.asList(KryoNamespaces.BASIC, KryoNamespaces.API));

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService service;

    private ConsistentMap<ResourcePath, ResourceConsumer> consumerMap;
    private ConsistentMap<ResourcePath, List<ResourcePath>> childMap;

    @Activate
    public void activate() {
        consumerMap = service.<ResourcePath, ResourceConsumer>consistentMapBuilder()
                .withName(CONSUMER_MAP)
                .withSerializer(SERIALIZER)
                .build();
        childMap = service.<ResourcePath, List<ResourcePath>>consistentMapBuilder()
                .withName(CHILD_MAP)
                .withSerializer(SERIALIZER)
                .build();

        childMap.put(ResourcePath.ROOT, ImmutableList.of());
        log.info("Started");
    }

    @Override
    public Optional<ResourceConsumer> getConsumer(ResourcePath resource) {
        checkNotNull(resource);

        Versioned<ResourceConsumer> consumer = consumerMap.get(resource);
        if (consumer == null) {
            return Optional.empty();
        }

        return Optional.of(consumer.value());
    }

    @Override
    public boolean register(List<ResourcePath> resources) {
        checkNotNull(resources);

        TransactionContext tx = service.transactionContextBuilder().build();
        tx.begin();

        TransactionalMap<ResourcePath, List<ResourcePath>> childTxMap =
                tx.getTransactionalMap(CHILD_MAP, SERIALIZER);

        Map<ResourcePath, List<ResourcePath>> resourceMap = resources.stream()
                .filter(x -> x.parent().isPresent())
                .collect(Collectors.groupingBy(x -> x.parent().get()));

        for (Map.Entry<ResourcePath, List<ResourcePath>> entry: resourceMap.entrySet()) {
            if (!isRegistered(childTxMap, entry.getKey())) {
                return abortTransaction(tx);
            }

            if (!appendValues(childTxMap, entry.getKey(), entry.getValue())) {
                return abortTransaction(tx);
            }
        }

        boolean success = tx.commit();
        if (success) {
            List<ResourceEvent> events = resources.stream()
                    .filter(x -> x.parent().isPresent())
                    .map(x -> new ResourceEvent(RESOURCE_ADDED, x))
                    .collect(Collectors.toList());
            notifyDelegate(events);
        }
        return success;
    }

    @Override
    public boolean unregister(List<ResourcePath> resources) {
        checkNotNull(resources);

        TransactionContext tx = service.transactionContextBuilder().build();
        tx.begin();

        TransactionalMap<ResourcePath, List<ResourcePath>> childTxMap =
                tx.getTransactionalMap(CHILD_MAP, SERIALIZER);
        TransactionalMap<ResourcePath, ResourceConsumer> consumerTxMap =
                tx.getTransactionalMap(CONSUMER_MAP, SERIALIZER);

        Map<ResourcePath, List<ResourcePath>> resourceMap = resources.stream()
                .filter(x -> x.parent().isPresent())
                .collect(Collectors.groupingBy(x -> x.parent().get()));

        // even if one of the resources is allocated to a consumer,
        // all unregistrations are regarded as failure
        for (Map.Entry<ResourcePath, List<ResourcePath>> entry: resourceMap.entrySet()) {
            if (entry.getValue().stream().anyMatch(x -> consumerTxMap.get(x) != null)) {
                return abortTransaction(tx);
            }

            if (!removeValues(childTxMap, entry.getKey(), entry.getValue())) {
                return abortTransaction(tx);
            }
        }

        boolean success = tx.commit();
        if (success) {
            List<ResourceEvent> events = resources.stream()
                    .filter(x -> x.parent().isPresent())
                    .map(x -> new ResourceEvent(RESOURCE_REMOVED, x))
                    .collect(Collectors.toList());
            notifyDelegate(events);
        }
        return success;
    }

    @Override
    public boolean allocate(List<ResourcePath> resources, ResourceConsumer consumer) {
        checkNotNull(resources);
        checkNotNull(consumer);

        TransactionContext tx = service.transactionContextBuilder().build();
        tx.begin();

        TransactionalMap<ResourcePath, List<ResourcePath>> childTxMap =
                tx.getTransactionalMap(CHILD_MAP, SERIALIZER);
        TransactionalMap<ResourcePath, ResourceConsumer> consumerTxMap =
                tx.getTransactionalMap(CONSUMER_MAP, SERIALIZER);

        for (ResourcePath resource: resources) {
            if (!isRegistered(childTxMap, resource)) {
                return abortTransaction(tx);
            }

            ResourceConsumer oldValue = consumerTxMap.put(resource, consumer);
            if (oldValue != null) {
                return abortTransaction(tx);
            }
        }

        return tx.commit();
    }

    @Override
    public boolean release(List<ResourcePath> resources, List<ResourceConsumer> consumers) {
        checkNotNull(resources);
        checkNotNull(consumers);
        checkArgument(resources.size() == consumers.size());

        TransactionContext tx = service.transactionContextBuilder().build();
        tx.begin();

        TransactionalMap<ResourcePath, ResourceConsumer> consumerTxMap =
                tx.getTransactionalMap(CONSUMER_MAP, SERIALIZER);
        Iterator<ResourcePath> resourceIte = resources.iterator();
        Iterator<ResourceConsumer> consumerIte = consumers.iterator();

        while (resourceIte.hasNext() && consumerIte.hasNext()) {
            ResourcePath resource = resourceIte.next();
            ResourceConsumer consumer = consumerIte.next();

            // if this single release fails (because the resource is allocated to another consumer,
            // the whole release fails
            if (!consumerTxMap.remove(resource, consumer)) {
                return abortTransaction(tx);
            }
        }

        return tx.commit();
    }

    @Override
    public Collection<ResourcePath> getResources(ResourceConsumer consumer) {
        checkNotNull(consumer);

        // NOTE: getting all entries may become performance bottleneck
        // TODO: revisit for better backend data structure
        return consumerMap.entrySet().stream()
                .filter(x -> x.getValue().value().equals(consumer))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<ResourcePath> getChildResources(ResourcePath parent) {
        checkNotNull(parent);

        Versioned<List<ResourcePath>> children = childMap.get(parent);
        if (children == null) {
            return Collections.emptyList();
        }

        return children.value();
    }

    @Override
    public <T> Collection<ResourcePath> getAllocatedResources(ResourcePath parent, Class<T> cls) {
        checkNotNull(parent);
        checkNotNull(cls);

        Versioned<List<ResourcePath>> children = childMap.get(parent);
        if (children == null) {
            return Collections.emptyList();
        }

        return children.value().stream()
                .filter(x -> x.last().getClass().equals(cls))
                .filter(consumerMap::containsKey)
                .collect(Collectors.toList());
    }

    /**
     * Abort the transaction.
     *
     * @param tx transaction context
     * @return always false
     */
    private boolean abortTransaction(TransactionContext tx) {
        tx.abort();
        return false;
    }

    /**
     * Appends the values to the existing values associated with the specified key.
     * If the map already has all the given values, appending will not happen.
     *
     * @param map map holding multiple values for a key
     * @param key key specifying values
     * @param values values to be appended
     * @param <K> type of the key
     * @param <V> type of the element of the list
     * @return true if the operation succeeds, false otherwise.
     */
    private <K, V> boolean appendValues(TransactionalMap<K, List<V>> map, K key, List<V> values) {
        List<V> oldValues = map.putIfAbsent(key, new ArrayList<>(values));
        if (oldValues == null) {
            return true;
        }

        LinkedHashSet<V> oldSet = new LinkedHashSet<>(oldValues);
        if (oldSet.containsAll(values)) {
            // don't write to map because all values are already stored
            return true;
        }

        oldSet.addAll(values);
        return map.replace(key, oldValues, new ArrayList<>(oldSet));
    }

    /**
     * Removes the values from the existing values associated with the specified key.
     * If the map doesn't contain the given values, removal will not happen.
     *
     * @param map map holding multiple values for a key
     * @param key key specifying values
     * @param values values to be removed
     * @param <K> type of the key
     * @param <V> type of the element of the list
     * @return true if the operation succeeds, false otherwise
     */
    private <K, V> boolean removeValues(TransactionalMap<K, List<V>> map, K key, List<V> values) {
        List<V> oldValues = map.get(key);
        if (oldValues == null) {
            map.put(key, new ArrayList<>());
            return true;
        }

        LinkedHashSet<V> oldSet = new LinkedHashSet<>(oldValues);
        if (values.stream().allMatch(x -> !oldSet.contains(x))) {
            // don't write map because none of the values are stored
            return true;
        }

        oldSet.removeAll(values);
        return map.replace(key, oldValues, new ArrayList<>(oldSet));
    }

    /**
     * Checks if the specified resource is registered as a child of a resource in the map.
     *
     * @param map map storing parent - child relationship of resources
     * @param resource resource to be checked
     * @return true if the resource is registered, false otherwise.
     */
    private boolean isRegistered(TransactionalMap<ResourcePath, List<ResourcePath>> map, ResourcePath resource) {
        // root is always regarded to be registered
        if (!resource.parent().isPresent()) {
            return true;
        }

        List<ResourcePath> value = map.get(resource.parent().get());
        return value != null && value.contains(resource);
    }
}
