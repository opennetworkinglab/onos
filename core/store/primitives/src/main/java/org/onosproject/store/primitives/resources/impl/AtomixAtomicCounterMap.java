/*
 * Copyright 2017-present Open Networking Laboratory
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

import io.atomix.copycat.client.CopycatClient;
import io.atomix.resource.AbstractResource;
import io.atomix.resource.ResourceTypeInfo;
import org.onosproject.store.primitives.resources.impl.AtomixAtomicCounterMapCommands.AddAndGet;
import org.onosproject.store.primitives.resources.impl.AtomixAtomicCounterMapCommands.Clear;
import org.onosproject.store.primitives.resources.impl.AtomixAtomicCounterMapCommands.DecrementAndGet;
import org.onosproject.store.primitives.resources.impl.AtomixAtomicCounterMapCommands.Get;
import org.onosproject.store.primitives.resources.impl.AtomixAtomicCounterMapCommands.GetAndAdd;
import org.onosproject.store.primitives.resources.impl.AtomixAtomicCounterMapCommands.GetAndDecrement;
import org.onosproject.store.primitives.resources.impl.AtomixAtomicCounterMapCommands.GetAndIncrement;
import org.onosproject.store.primitives.resources.impl.AtomixAtomicCounterMapCommands.IncrementAndGet;
import org.onosproject.store.primitives.resources.impl.AtomixAtomicCounterMapCommands.IsEmpty;
import org.onosproject.store.primitives.resources.impl.AtomixAtomicCounterMapCommands.Put;
import org.onosproject.store.primitives.resources.impl.AtomixAtomicCounterMapCommands.PutIfAbsent;
import org.onosproject.store.primitives.resources.impl.AtomixAtomicCounterMapCommands.Remove;
import org.onosproject.store.primitives.resources.impl.AtomixAtomicCounterMapCommands.RemoveValue;
import org.onosproject.store.primitives.resources.impl.AtomixAtomicCounterMapCommands.Replace;
import org.onosproject.store.primitives.resources.impl.AtomixAtomicCounterMapCommands.Size;
import org.onosproject.store.service.AsyncAtomicCounterMap;

import java.util.Properties;
import java.util.concurrent.CompletableFuture;

/**
 * {@code AsyncAtomicCounterMap} implementation backed by Atomix.
 */
@ResourceTypeInfo(id = -157, factory = AtomixAtomicCounterMapFactory.class)
public class AtomixAtomicCounterMap extends AbstractResource<AtomixAtomicCounterMap>
    implements AsyncAtomicCounterMap<String> {

    public AtomixAtomicCounterMap(CopycatClient client, Properties options) {
        super(client, options);
    }

    @Override
    public String name() {
        return null;
    }

    @Override
    public CompletableFuture<Long> incrementAndGet(String key) {
        return client.submit(new IncrementAndGet(key));
    }

    @Override
    public CompletableFuture<Long> decrementAndGet(String key) {
        return client.submit(new DecrementAndGet(key));
    }

    @Override
    public CompletableFuture<Long> getAndIncrement(String key) {
        return client.submit(new GetAndIncrement(key));
    }

    @Override
    public CompletableFuture<Long> getAndDecrement(String key) {
        return client.submit(new GetAndDecrement(key));
    }

    @Override
    public CompletableFuture<Long> addAndGet(String key, long delta) {
        return client.submit(new AddAndGet(key, delta));
    }

    @Override
    public CompletableFuture<Long> getAndAdd(String key, long delta) {
        return client.submit(new GetAndAdd(key, delta));
    }

    @Override
    public CompletableFuture<Long> get(String key) {
        return client.submit(new Get(key));
    }

    @Override
    public CompletableFuture<Long> put(String key, long newValue) {
        return client.submit(new Put(key, newValue));
    }

    @Override
    public CompletableFuture<Long> putIfAbsent(String key, long newValue) {
        return client.submit(new PutIfAbsent(key, newValue));
    }

    @Override
    public CompletableFuture<Boolean> replace(String key, long expectedOldValue, long newValue) {
        return client.submit(new Replace(key, expectedOldValue, newValue));
    }

    @Override
    public CompletableFuture<Long> remove(String key) {
        return client.submit(new Remove(key));
    }

    @Override
    public CompletableFuture<Boolean> remove(String key, long value) {
        return client.submit(new RemoveValue(key, value));
    }

    @Override
    public CompletableFuture<Integer> size() {
        return client.submit(new Size());
    }

    @Override
    public CompletableFuture<Boolean> isEmpty() {
        return client.submit(new IsEmpty());
    }

    @Override
    public CompletableFuture<Void> clear() {
        return client.submit(new Clear());
    }
}
