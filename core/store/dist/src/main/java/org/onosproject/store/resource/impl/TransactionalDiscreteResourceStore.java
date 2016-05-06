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
package org.onosproject.store.resource.impl;

import com.google.common.collect.Sets;
import org.onosproject.net.resource.DiscreteResource;
import org.onosproject.net.resource.DiscreteResourceId;
import org.onosproject.net.resource.Resource;
import org.onosproject.net.resource.ResourceConsumer;
import org.onosproject.net.resource.Resources;
import org.onosproject.store.service.TransactionContext;
import org.onosproject.store.service.TransactionalMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.onosproject.store.resource.impl.ConsistentResourceStore.SERIALIZER;

class TransactionalDiscreteResourceStore {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final TransactionalMap<DiscreteResourceId, Set<DiscreteResource>> childMap;
    private final TransactionalMap<DiscreteResourceId, ResourceConsumer> consumers;

    TransactionalDiscreteResourceStore(TransactionContext tx) {
        this.childMap = tx.getTransactionalMap(MapNames.DISCRETE_CHILD_MAP, SERIALIZER);
        this.consumers = tx.getTransactionalMap(MapNames.DISCRETE_CONSUMER_MAP, SERIALIZER);
    }

    // check the existence in the set: O(1) operation
    Optional<Resource> lookup(DiscreteResourceId id) {
        if (!id.parent().isPresent()) {
            return Optional.of(Resource.ROOT);
        }

        Set<DiscreteResource> values = childMap.get(id.parent().get());
        if (values == null) {
            return Optional.empty();
        }

        DiscreteResource resource = Resources.discrete(id).resource();
        if (values.contains(resource)) {
            return Optional.of(resource);
        } else {
            return Optional.empty();
        }
    }

    boolean register(DiscreteResourceId key, List<DiscreteResource> values) {
        Set<DiscreteResource> requested = new LinkedHashSet<>(values);
        Set<DiscreteResource> oldValues = childMap.putIfAbsent(key, requested);
        if (oldValues == null) {
            return true;
        }

        Set<DiscreteResource> addedValues = Sets.difference(requested, oldValues);
        // no new value, then no-op
        if (addedValues.isEmpty()) {
            // don't write to map because all values are already stored
            return true;
        }

        Set<DiscreteResource> newValues = new LinkedHashSet<>(oldValues);
        newValues.addAll(addedValues);
        return childMap.replace(key, oldValues, newValues);
    }

    boolean unregister(DiscreteResourceId key, List<DiscreteResource> values) {
        Set<DiscreteResource> oldValues = childMap.putIfAbsent(key, new LinkedHashSet<>());
        if (oldValues == null) {
            log.trace("No-Op removing values. key {} did not exist", key);
            return true;
        }

        if (values.stream().allMatch(x -> !oldValues.contains(x))) {
            // don't write map because none of the values are stored
            log.trace("No-Op removing values. key {} did not contain {}", key, values);
            return true;
        }

        LinkedHashSet<DiscreteResource> newValues = new LinkedHashSet<>(oldValues);
        newValues.removeAll(values);
        return childMap.replace(key, oldValues, newValues);
    }

    boolean isAllocated(DiscreteResourceId id) {
        return consumers.get(id) != null;
    }

    boolean allocate(ResourceConsumer consumer, DiscreteResource resource) {
        // if the resource is not registered, then abort
        Optional<Resource> lookedUp = lookup(resource.id());
        if (!lookedUp.isPresent()) {
            return false;
        }

        ResourceConsumer oldValue = consumers.put(resource.id(), consumer);
        return oldValue == null;
    }

    boolean release(DiscreteResource resource, ResourceConsumer consumer) {
        // if this single release fails (because the resource is allocated to another consumer)
        // the whole release fails
        if (!consumers.remove(resource.id(), consumer)) {
            return false;
        }

        return true;
    }
}
