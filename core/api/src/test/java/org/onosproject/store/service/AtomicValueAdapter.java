/*
 * Copyright 2017-present Open Networking Foundation
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

/**
 * Atomic value adapter.
 */
public class AtomicValueAdapter<V> implements AtomicValue<V> {
    private final String name;

    public AtomicValueAdapter() {
        this(null);
    }

    public AtomicValueAdapter(String name) {
        this.name = name;
    }

    @Override
    public String name() {
        return null;
    }

    @Override
    public Type primitiveType() {
        return null;
    }

    @Override
    public boolean compareAndSet(V expect, V update) {
        return false;
    }

    @Override
    public V get() {
        return null;
    }

    @Override
    public V getAndSet(V value) {
        return null;
    }

    @Override
    public void set(V value) {

    }

    @Override
    public void addListener(AtomicValueEventListener<V> listener) {

    }

    @Override
    public void removeListener(AtomicValueEventListener<V> listener) {

    }
}
