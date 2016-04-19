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
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import io.atomix.copycat.client.CopycatClient;
import io.atomix.resource.AbstractResource;
import org.onlab.util.Match;
import org.onosproject.store.service.AsyncConsistentMultimap;
import org.onosproject.store.service.Versioned;

import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.onosproject.store.primitives.resources.impl.AsyncConsistentMultimapCommands.*;

/**
 * Set based implementation of the {@link AsyncConsistentMultimap}.
 * <p>
 * Note: this implementation does not allow null entries or duplicate entries.
 */
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
        return submit(new UpdateAndGet(key, Lists.newArrayList(value),
                                       Lists.newArrayList(Match.NULL),
                                       Lists.newArrayList(Match.NULL)))
                .whenComplete((result, e) -> throwIfLocked(result.status()))
                .thenApply(result ->
                                   result.status() == MapEntryUpdateResult.Status.OK);
    }

    @Override
    public CompletableFuture<Boolean> remove(String key, byte[] value) {
        return submit(new UpdateAndGet(key, Lists.newArrayList(value),
                                       Lists.newArrayList(Match.ifValue(value)),
                                       Lists.newArrayList(Match.NULL)))
                .whenComplete((result, e) -> throwIfLocked(result.status()))
                .thenApply(result ->
                                   result.status() == MapEntryUpdateResult.Status.OK);
    }

    @Override
    public CompletableFuture<Boolean> removeAll(String key, Iterable<? extends byte[]> values) {

        throw new UnsupportedOperationException("This operation cannot be " +
                                                        "used without support for " +
                                                        "transactions.");
    }

    @Override
    public CompletableFuture<Versioned<Collection<byte[]>>> removeAll(String key) {
        return submit(new UpdateAndGet(key, null, null, null))
                .whenComplete((result, e) -> throwIfLocked(result.status()))
                .thenApply(result -> result.oldValue());
    }

    @Override
    public CompletableFuture<Boolean> putAll(String key, Iterable<? extends byte[]> values) {
        throw new UnsupportedOperationException("This operation cannot be " +
                                                        "used without support for " +
                                                        "transactions.");
    }

    @Override
    public CompletableFuture<Boolean> putAll(Multimap<? extends String, ? extends byte[]> multiMap) {
        throw new UnsupportedOperationException("This operation cannot be " +
                                                        "used without support for " +
                                                        "transactions.");
    }

    @Override
    public CompletableFuture<Collection<byte[]>> replaceValues(String key, Iterable<byte[]> values) {
        throw new UnsupportedOperationException("This operation cannot be " +
                                                        "used without support for " +
                                                        "transactions.");
    }

    @Override
    public CompletableFuture<Void> clear() {
        return submit(new AsyncConsistentMultimapCommands.Clear());
    }

    @Override
    public CompletableFuture<Collection<byte[]>> get(String key) {
        return submit(new Get());
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
    public CompletableFuture<Collection<byte[]>> values() {
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
            throw new ConcurrentModificationException("Cannot update map: Another transaction in progress");
        }
    }
}
