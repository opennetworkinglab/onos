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

import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMultimapCommands.Clear;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMultimapCommands.ContainsEntry;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMultimapCommands.ContainsKey;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMultimapCommands.ContainsValue;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMultimapCommands.Entries;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMultimapCommands.Get;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMultimapCommands.IsEmpty;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMultimapCommands.KeySet;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMultimapCommands.Keys;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMultimapCommands.MultiRemove;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMultimapCommands.Put;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMultimapCommands.RemoveAll;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMultimapCommands.Replace;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMultimapCommands.Size;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentMultimapCommands.Values;

/**
 * Set based implementation of the {@link AsyncConsistentMultimap}.
 * <p>
 * Note: this implementation does not allow null entries or duplicate entries.
 */
@ResourceTypeInfo(id = -153, factory = AtomixConsistentSetMultimapFactory.class)
public class AtomixConsistentSetMultimap
        extends AbstractResource<AtomixConsistentSetMultimap>
        implements AsyncConsistentMultimap<String, byte[]> {

    public AtomixConsistentSetMultimap(CopycatClient client,
                                       Properties properties) {
        super(client, properties);
    }

    @Override
    public CompletableFuture<AtomixConsistentSetMultimap> open() {
        return super.open();
        //TODO
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
    public CompletableFuture<Boolean> containsKey(String key) {
        return client.submit(new ContainsKey(key));
    }

    @Override
    public CompletableFuture<Boolean> containsValue(byte[] value) {
        return client.submit(new ContainsValue(value));
    }

    @Override
    public CompletableFuture<Boolean> containsEntry(String key, byte[] value) {
        return client.submit(new ContainsEntry(key, value));
    }

    @Override
    public CompletableFuture<Boolean> put(String key, byte[] value) {
        return client.submit(new Put(key, Lists.newArrayList(value), null));
    }

    @Override
    public CompletableFuture<Boolean> remove(String key, byte[] value) {
        return client.submit(new MultiRemove(key,
                                             Lists.newArrayList(value),
                                             null));
    }

    @Override
    public CompletableFuture<Boolean> removeAll(String key, Collection<? extends byte[]> values) {
        return client.submit(new MultiRemove(key, (Collection<byte[]>) values, null));
    }

    @Override
    public CompletableFuture<Versioned<Collection<? extends byte[]>>> removeAll(String key) {
        return client.submit(new RemoveAll(key, null));
    }

    @Override
    public CompletableFuture<Boolean> putAll(
            String key, Collection<? extends byte[]> values) {
        return client.submit(new Put(key, values, null));
    }

    @Override
    public CompletableFuture<Versioned<Collection<? extends byte[]>>> replaceValues(
            String key, Collection<byte[]> values) {
        return client.submit(new Replace(key, values, null));
    }

    @Override
    public CompletableFuture<Void> clear() {
        return client.submit(new Clear());
    }

    @Override
    public CompletableFuture<Versioned<Collection<? extends byte[]>>> get(String key) {
        return client.submit(new Get(key));
    }

    @Override
    public CompletableFuture<Set<String>> keySet() {
        return client.submit(new KeySet());
    }

    @Override
    public CompletableFuture<Multiset<String>> keys() {
        return client.submit(new Keys());
    }

    public CompletableFuture<Multiset<byte[]>> values() {
        return client.submit(new Values());
    }

    @Override
    public CompletableFuture<Collection<Map.Entry<String, byte[]>>> entries() {
        return client.submit(new Entries());
    }

    @Override
    public CompletableFuture<Map<String, Collection<byte[]>>> asMap() {
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
