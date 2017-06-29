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
package org.onosproject.store.primitives.resources.impl;

import java.util.concurrent.CompletableFuture;

import io.atomix.protocols.raft.proxy.RaftProxy;
import org.onlab.util.KryoNamespace;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.AsyncAtomicCounter;
import org.onosproject.store.service.Serializer;

import static org.onosproject.store.primitives.resources.impl.AtomixCounterOperations.ADD_AND_GET;
import static org.onosproject.store.primitives.resources.impl.AtomixCounterOperations.AddAndGet;
import static org.onosproject.store.primitives.resources.impl.AtomixCounterOperations.COMPARE_AND_SET;
import static org.onosproject.store.primitives.resources.impl.AtomixCounterOperations.CompareAndSet;
import static org.onosproject.store.primitives.resources.impl.AtomixCounterOperations.GET;
import static org.onosproject.store.primitives.resources.impl.AtomixCounterOperations.GET_AND_ADD;
import static org.onosproject.store.primitives.resources.impl.AtomixCounterOperations.GET_AND_INCREMENT;
import static org.onosproject.store.primitives.resources.impl.AtomixCounterOperations.GetAndAdd;
import static org.onosproject.store.primitives.resources.impl.AtomixCounterOperations.INCREMENT_AND_GET;
import static org.onosproject.store.primitives.resources.impl.AtomixCounterOperations.SET;
import static org.onosproject.store.primitives.resources.impl.AtomixCounterOperations.Set;

/**
 * Atomix counter implementation.
 */
public class AtomixCounter extends AbstractRaftPrimitive implements AsyncAtomicCounter {
    private static final Serializer SERIALIZER = Serializer.using(KryoNamespace.newBuilder()
            .register(KryoNamespaces.BASIC)
            .register(AtomixCounterOperations.NAMESPACE)
            .build());

    public AtomixCounter(RaftProxy proxy) {
        super(proxy);
    }

    private long nullOrZero(Long value) {
        return value != null ? value : 0;
    }

    @Override
    public CompletableFuture<Long> get() {
        return proxy.<Long>invoke(GET, SERIALIZER::decode).thenApply(this::nullOrZero);
    }

    @Override
    public CompletableFuture<Void> set(long value) {
        return proxy.invoke(SET, SERIALIZER::encode, new Set(value));
    }

    @Override
    public CompletableFuture<Boolean> compareAndSet(long expectedValue, long updateValue) {
        return proxy.invoke(COMPARE_AND_SET, SERIALIZER::encode,
                new CompareAndSet(expectedValue, updateValue), SERIALIZER::decode);
    }

    @Override
    public CompletableFuture<Long> addAndGet(long delta) {
        return proxy.invoke(ADD_AND_GET, SERIALIZER::encode, new AddAndGet(delta), SERIALIZER::decode);
    }

    @Override
    public CompletableFuture<Long> getAndAdd(long delta) {
        return proxy.invoke(GET_AND_ADD, SERIALIZER::encode, new GetAndAdd(delta), SERIALIZER::decode);
    }

    @Override
    public CompletableFuture<Long> incrementAndGet() {
        return proxy.invoke(INCREMENT_AND_GET, SERIALIZER::decode);
    }

    @Override
    public CompletableFuture<Long> getAndIncrement() {
        return proxy.invoke(GET_AND_INCREMENT, SERIALIZER::decode);
    }
}