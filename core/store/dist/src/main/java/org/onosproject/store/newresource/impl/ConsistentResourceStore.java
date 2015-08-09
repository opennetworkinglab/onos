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
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.net.newresource.Resource;
import org.onosproject.net.newresource.ResourceConsumer;
import org.onosproject.net.newresource.ResourceStore;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.TransactionContext;
import org.onosproject.store.service.TransactionException;
import org.onosproject.store.service.TransactionalMap;
import org.onosproject.store.service.Versioned;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of ResourceStore using TransactionalMap.
 */
@Component(immediate = true, enabled = false)
@Service
@Beta
public class ConsistentResourceStore implements ResourceStore {
    private static final Logger log = LoggerFactory.getLogger(ConsistentResourceStore.class);

    private static final String MAP_NAME = "onos-resource-consumers";
    private static final Serializer SERIALIZER = Serializer.using(KryoNamespaces.API);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService service;

    private ConsistentMap<Resource<?, ?>, ResourceConsumer> consumers;

    @Activate
    public void activate() {
        consumers = service.<Resource<?, ?>, ResourceConsumer>consistentMapBuilder()
                .withName(MAP_NAME)
                .withSerializer(SERIALIZER)
                .build();
    }

    @Override
    public <S, T> Optional<ResourceConsumer> getConsumer(Resource<S, T> resource) {
        checkNotNull(resource);

        Versioned<ResourceConsumer> consumer = consumers.get(resource);
        if (consumer == null) {
            return Optional.empty();
        }

        return Optional.of(consumer.value());
    }

    @Override
    public boolean allocate(List<? extends Resource<?, ?>> resources, ResourceConsumer consumer) {
        checkNotNull(resources);
        checkNotNull(consumer);

        TransactionContext tx = service.transactionContextBuilder().build();
        tx.begin();

        try {
            TransactionalMap<Resource<?, ?>, ResourceConsumer> txMap = tx.getTransactionalMap(MAP_NAME, SERIALIZER);
            for (Resource<?, ?> resource: resources) {
                ResourceConsumer existing = txMap.putIfAbsent(resource, consumer);
                // if the resource is already allocated to another consumer, the whole allocation fails
                if (existing != null) {
                    tx.abort();
                    return false;
                }
            }
            tx.commit();
            return true;
        } catch (Exception e) {
            log.error("Exception thrown, abort the transaction", e);
            tx.abort();
            return false;
        }
    }

    @Override
    public boolean release(List<? extends Resource<?, ?>> resources, List<ResourceConsumer> consumers) {
        checkNotNull(resources);
        checkNotNull(consumers);
        checkArgument(resources.size() == consumers.size());

        TransactionContext tx = service.transactionContextBuilder().build();
        tx.begin();

        try {
            TransactionalMap<Resource<?, ?>, ResourceConsumer> txMap = tx.getTransactionalMap(MAP_NAME, SERIALIZER);
            Iterator<? extends Resource<?, ?>> resourceIte = resources.iterator();
            Iterator<ResourceConsumer> consumerIte = consumers.iterator();

            while (resourceIte.hasNext() && consumerIte.hasNext()) {
                Resource<?, ?> resource = resourceIte.next();
                ResourceConsumer consumer = consumerIte.next();

                // if this single release fails (because the resource is allocated to another consumer,
                // the whole release fails
                if (!txMap.remove(resource, consumer)) {
                    tx.abort();
                    return false;
                }
            }

            return true;
        } catch (TransactionException e) {
            log.error("Exception thrown, abort the transaction", e);
            tx.abort();
            return false;
        }
    }

    @Override
    public Collection<Resource<?, ?>> getResources(ResourceConsumer consumer) {
        checkNotNull(consumer);

        // NOTE: getting all entries may become performance bottleneck
        // TODO: revisit for better backend data structure
        return consumers.entrySet().stream()
                .filter(x -> x.getValue().value().equals(consumer))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <S, T> Collection<Resource<S, T>> getAllocatedResources(S subject, Class<T> cls) {
        checkNotNull(subject);
        checkNotNull(cls);

        // NOTE: getting all entries may become performance bottleneck
        // TODO: revisit for better backend data structure
        return consumers.entrySet().stream()
                .filter(x -> x.getKey().subject().equals(subject) && x.getKey().resource().getClass() == cls)
                // cast is ensured by the above filter method
                .map(x -> (Resource<S, T>) x.getKey())
                .collect(Collectors.toList());
    }
}
