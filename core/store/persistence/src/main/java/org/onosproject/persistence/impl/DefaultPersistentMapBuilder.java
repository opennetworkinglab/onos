/*
 * Copyright 2015-present Open Networking Laboratory
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

package org.onosproject.persistence.impl;

import org.mapdb.DB;
import org.onosproject.persistence.PersistentMapBuilder;
import org.onosproject.store.service.Serializer;

import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default builder for persistent maps stored in the mapDB local database via the persistence service.
 */
public class DefaultPersistentMapBuilder<K, V> implements PersistentMapBuilder<K, V> {

    private final DB localDB;

    private String name = null;

    private Serializer serializer = null;


    public DefaultPersistentMapBuilder(DB localDB) {
        checkNotNull(localDB, "The local database cannot be null.");
        this.localDB = localDB;
    }

    public PersistentMapBuilder<K, V> withName(String name) {
        this.name = PersistenceManager.MAP_PREFIX + checkNotNull(name);
        return this;
    }

    public PersistentMapBuilder<K, V> withSerializer(Serializer serializer) {
        checkArgument(this.serializer == null);
        checkNotNull(serializer);
        this.serializer = serializer;
        return this;
    }

    public Map<K, V> build() {
        checkNotNull(name, "The name must be assigned.");
        checkNotNull(serializer, "The key serializer must be assigned.");

        return new PersistentMap<K, V>(serializer, localDB, name);
    }
}