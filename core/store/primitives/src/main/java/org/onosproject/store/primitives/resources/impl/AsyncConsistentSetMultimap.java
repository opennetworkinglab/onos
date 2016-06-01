/*
 * Copyright 2016 Open Networking Laboratory
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

import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import io.atomix.copycat.client.CopycatClient;
import io.atomix.resource.AbstractResource;
import io.atomix.resource.ResourceTypeInfo;
import org.onosproject.store.service.AsyncConsistentMultimap;
import org.onosproject.store.service.Versioned;

import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.onosproject.store.primitives.resources.impl.AsyncConsistentMultimapCommands.Clear;
import static org.onosproject.store.primitives.resources.impl.AsyncConsistentMultimapCommands.ContainsEntry;
import static org.onosproject.store.primitives.resources.impl.AsyncConsistentMultimapCommands.ContainsKey;
import static org.onosproject.store.primitives.resources.impl.AsyncConsistentMultimapCommands.ContainsValue;
import static org.onosproject.store.primitives.resources.impl.AsyncConsistentMultimapCommands.Entries;
import static org.onosproject.store.primitives.resources.impl.AsyncConsistentMultimapCommands.Get;
import static org.onosproject.store.primitives.resources.impl.AsyncConsistentMultimapCommands.IsEmpty;
import static org.onosproject.store.primitives.resources.impl.AsyncConsistentMultimapCommands.KeySet;
import static org.onosproject.store.primitives.resources.impl.AsyncConsistentMultimapCommands.Keys;
import static org.onosproject.store.primitives.resources.impl.AsyncConsistentMultimapCommands.MultiRemove;
import static org.onosproject.store.primitives.resources.impl.AsyncConsistentMultimapCommands.Put;
import static org.onosproject.store.primitives.resources.impl.AsyncConsistentMultimapCommands.RemoveAll;
import static org.onosproject.store.primitives.resources.impl.AsyncConsistentMultimapCommands.Replace;
import static org.onosproject.store.primitives.resources.impl.AsyncConsistentMultimapCommands.Size;
import static org.onosproject.store.primitives.resources.impl.AsyncConsistentMultimapCommands.Values;

/**
 * Set based implementation of the {@link AsyncConsistentMultimap}.
 * <p>
 * Note: this implementation does not allow null entries or duplicate entries.
 */
@ResourceTypeInfo(id = -153, factory = AsyncConsistentSetMultimapFactory.class)
public class AsyncConsistentSetMultimap
        extends AbstractResource<AsyncConsistentSetMultimap>
        implements AsyncConsistentMultimap<String, byte[]> {

    public AsyncConsistentSetMultimap(CopycatClient client,
                                      Properties properties) {
        super(client, properties);
    }

    @Override
    public CompletableFuture<AsyncConsistentSetMultimap> open() {
        return super.open();
        //TODO
    }

    @Override
    public CompletableFuture<Integer> size() {
        return submit(new Size());
    }

    @Override
    public CompletableFuture<Boolean> isEmpty() {
        return submit(new IsEmpty());
    }

    @Override
    public CompletableFuture<Boolean> containsKey(String key) {
        return submit(new ContainsKey(key));
    }

    @Override
    public CompletableFuture<Boolean> containsValue(byte[] value) {
        return submit(new ContainsValue(value));
    }

    @Override
    public CompletableFuture<Boolean> containsEntry(String key, byte[] value) {
        return submit(new ContainsEntry(key, value));
    }

    @Override
    public CompletableFuture<Boolean> put(String key, byte[] value) {
        return submit(new Put(key, Lists.newArrayList(value), null));
    }

    @Override
    public CompletableFuture<Boolean> remove(String key, byte[] value) {
        return submit(new MultiRemove(key,
                                      Lists.newArrayList(value),
                                      null));
    }

    @Override
    public CompletableFuture<Boolean> removeAll(
            String key, Collection<? extends byte[]> values) {
        return submit(new MultiRemove(key, (Collection<byte[]>) values, null));
    }

    @Override
    public CompletableFuture<
            Versioned<Collection<? extends byte[]>>> removeAll(String key) {
        return submit(new RemoveAll(key, null));
    }

    @Override
    public CompletableFuture<Boolean> putAll(
            String key, Collection<? extends byte[]> values) {
        return submit(new Put(key, values, null));
    }

    @Override
    public CompletableFuture<
            Versioned<Collection<? extends byte[]>>> replaceValues(
            String key, Collection<byte[]> values) {
        return submit(new Replace(key, values, null));
    }

    @Override
    public CompletableFuture<Void> clear() {
        return submit(new Clear());
    }

    @Override
    public CompletableFuture<
            Versioned<Collection<? extends byte[]>>> get(String key) {
        return submit(new Get(key));
    }

    @Override
    public CompletableFuture<Set<String>> keySet() {
        return submit(new KeySet());
    }

    @Override
    public CompletableFuture<Multiset<String>> keys() {
        return submit(new Keys());
    }

    @Override
    public CompletableFuture<Multiset<byte[]>> values() {
        return submit(new Values());
    }

    @Override
    public CompletableFuture<Collection<Map.Entry<String, byte[]>>> entries() {
        return submit(new Entries());
    }

    @Override
    public CompletableFuture<Map<String, Collection<byte[]>>> asMap() {
        //TODO
        throw new UnsupportedOperationException("Expensive operation.");
    }

    @Override
    public String name() {
        return null;
    }

    /**
     * Helper to check if there was a lock based issue.
     * @param status the status of an update result
     */
    private void throwIfLocked(MapEntryUpdateResult.Status status) {
        if (status == MapEntryUpdateResult.Status.WRITE_LOCK) {
            throw new ConcurrentModificationException("Cannot update map: " +
                                                      "Another transaction " +
                                                      "in progress");
        }
    }
}
