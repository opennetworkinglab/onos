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

import org.onosproject.net.resource.DiscreteResource;
import org.onosproject.net.resource.DiscreteResourceId;
import org.onosproject.net.resource.Resource;
import org.onosproject.net.resource.ResourceConsumerId;
import org.onosproject.store.service.TransactionContext;
import org.onosproject.store.service.TransactionalMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Set;

import static org.onosproject.store.resource.impl.ConsistentResourceStore.SERIALIZER;

class TransactionalDiscreteResourceSubStore {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final TransactionalMap<DiscreteResourceId, DiscreteResources> childMap;
    private final TransactionalMap<DiscreteResourceId, ResourceConsumerId> consumers;

    TransactionalDiscreteResourceSubStore(TransactionContext tx) {
        this.childMap = tx.getTransactionalMap(MapNames.DISCRETE_CHILD_MAP, SERIALIZER);
        this.consumers = tx.getTransactionalMap(MapNames.DISCRETE_CONSUMER_MAP, SERIALIZER);
    }

    // check the existence in the set: O(1) operation
    Optional<DiscreteResource> lookup(DiscreteResourceId id) {
        if (!id.parent().isPresent()) {
            return Optional.of(Resource.ROOT);
        }

        DiscreteResources values = childMap.get(id.parent().get());
        if (values == null) {
            return Optional.empty();
        }

        return values.lookup(id);
    }

    boolean register(DiscreteResourceId key, Set<DiscreteResource> values) {
        // short-circuit: receiving empty resource is regarded as success
        if (values.isEmpty()) {
            return true;
        }

        DiscreteResources requested = DiscreteResources.of(values);
        DiscreteResources oldValues = childMap.putIfAbsent(key, requested);
        if (oldValues == null) {
            return true;
        }

        DiscreteResources addedValues = requested.difference(oldValues);
        // no new value, then no-op
        if (addedValues.isEmpty()) {
            // don't write to map because all values are already stored
            return true;
        }

        DiscreteResources newValues = oldValues.add(addedValues);
        return childMap.replace(key, oldValues, newValues);
    }

    boolean unregister(DiscreteResourceId key, Set<DiscreteResource> values) {
        // short-circuit: receiving empty resource is regarded as success
        if (values.isEmpty()) {
            return true;
        }

        // even if one of the resources is allocated to a consumer,
        // all unregistrations are regarded as failure
        boolean allocated = values.stream().anyMatch(x -> isAllocated(x.id()));
        if (allocated) {
            log.warn("Failed to unregister {}: allocation exists", key);
            return false;
        }

        DiscreteResources oldValues = childMap.putIfAbsent(key, DiscreteResources.empty());
        if (oldValues == null) {
            log.trace("No-Op removing values. key {} did not exist", key);
            return true;
        }

        if (!oldValues.containsAny(values)) {
            // don't write map because none of the values are stored
            log.trace("No-Op removing values. key {} did not contain {}", key, values);
            return true;
        }

        DiscreteResources requested = DiscreteResources.of(values);
        DiscreteResources newValues = oldValues.difference(requested);
        return childMap.replace(key, oldValues, newValues);
    }

    private boolean isAllocated(DiscreteResourceId id) {
        return consumers.get(id) != null;
    }

    boolean allocate(ResourceConsumerId consumerId, DiscreteResource resource) {
        // if the resource is not registered, then abort
        Optional<DiscreteResource> lookedUp = lookup(resource.id());
        if (!lookedUp.isPresent()) {
            return false;
        }

        ResourceConsumerId oldValue = consumers.put(resource.id(), consumerId);
        return oldValue == null;
    }

    boolean release(DiscreteResource resource, ResourceConsumerId consumerId) {
        // if this single release fails (because the resource is allocated to another consumer)
        // the whole release fails
        if (!consumers.remove(resource.id(), consumerId)) {
            return false;
        }

        return true;
    }
}
