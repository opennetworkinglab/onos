/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.store.persistence;

import java.util.Map;

import org.onosproject.persistence.PersistentMapBuilder;
import org.onosproject.persistence.PersistentSetBuilder;
import org.onosproject.store.service.Serializer;

import com.google.common.collect.Maps;

/**
 * PersistenceService that produces in memory maps for use in unit testing.
 */
public class TestPersistenceService extends PersistenceServiceAdapter {
    @Override
    public <K, V> PersistentMapBuilder<K, V> persistentMapBuilder() {
        return new TestPersistentMapBuilder<K, V>();
    }

    @Override
    public <E> PersistentSetBuilder<E> persistentSetBuilder() {
        throw new UnsupportedOperationException();
    }

    private static class TestPersistentMapBuilder<K, V> implements PersistentMapBuilder<K, V> {

        @Override
        public PersistentMapBuilder<K, V> withName(String name) {
            return this;
        }

        @Override
        public PersistentMapBuilder<K, V> withSerializer(Serializer serializer) {
            return this;
        }

        @Override
        public Map<K, V> build() {
            return Maps.newConcurrentMap();
        }
    }
}
