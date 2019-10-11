/*
 * Copyright 2019-present Open Networking Foundation
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

package org.onosproject.store.service;

import com.google.common.collect.Multiset;
import org.onosproject.core.ApplicationId;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public class AsyncConsistentMultimapAdapter<K, V> implements AsyncConsistentMultimap<K, V> {

    @Override
    public String name() {
        return null;
    }

    @Override
    public Type primitiveType() {
        return null;
    }

    @Override
    public ApplicationId applicationId() {
        return null;
    }

    @Override
    public CompletableFuture<Void> destroy() {
        return null;
    }

    @Override
    public void addStatusChangeListener(Consumer<Status> listener) {

    }

    @Override
    public void removeStatusChangeListener(Consumer<Status> listener) {

    }

    @Override
    public Collection<Consumer<Status>> statusChangeListeners() {
        return null;
    }

    @Override
    public CompletableFuture<Integer> size() {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> isEmpty() {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> containsKey(K key) {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> containsValue(V value) {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> containsEntry(K key, V value) {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> put(K key, V value) {
        return null;
    }

    @Override
    public CompletableFuture<Versioned<Collection<? extends V>>> putAndGet(K key, V value) {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> remove(K key, V value) {
        return null;
    }

    @Override
    public CompletableFuture<Versioned<Collection<? extends V>>> removeAndGet(K key, V value) {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> removeAll(K key, Collection<? extends V> values) {
        return null;
    }

    @Override
    public CompletableFuture<Versioned<Collection<? extends V>>> removeAll(K key) {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> removeAll(Map<K, Collection<? extends V>> mapping) {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> putAll(K key, Collection<? extends V> values) {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> putAll(Map<K, Collection<? extends V>> mapping) {
        return null;
    }

    @Override
    public CompletableFuture<Versioned<Collection<? extends V>>> replaceValues(K key, Collection<V> values) {
        return null;
    }

    @Override
    public CompletableFuture<Void> clear() {
        return null;
    }

    @Override
    public CompletableFuture<Versioned<Collection<? extends V>>> get(K key) {
        return null;
    }

    @Override
    public CompletableFuture<Set<K>> keySet() {
        return null;
    }

    @Override
    public CompletableFuture<Multiset<K>> keys() {
        return null;
    }

    @Override
    public CompletableFuture<Multiset<V>> values() {
        return null;
    }

    @Override
    public CompletableFuture<Collection<Map.Entry<K, V>>> entries() {
        return null;
    }

    @Override
    public CompletableFuture<Void> addListener(MultimapEventListener<K, V> listener) {
        return null;
    }

    @Override
    public CompletableFuture<Void> addListener(MultimapEventListener<K, V> listener, Executor executor) {
        return null;
    }

    @Override
    public CompletableFuture<Void> removeListener(MultimapEventListener<K, V> listener) {
        return null;
    }

    @Override
    public CompletableFuture<Map<K, Collection<V>>> asMap() {
        return null;
    }

    @Override
    public ConsistentMultimap<K, V> asMultimap() {
        return null;
    }

    @Override
    public ConsistentMultimap<K, V> asMultimap(long timeoutMillis) {
        return null;
    }

    @Override
    public CompletableFuture<AsyncIterator<Map.Entry<K, V>>> iterator() {
        return null;
    }
}
