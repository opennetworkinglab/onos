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

package org.onosproject.persistence.impl;

import org.mapdb.DB;
import org.onosproject.persistence.PersistentSetBuilder;
import org.onosproject.store.service.Serializer;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default builder for persistent sets stored in the mapDB local database via the persistence service..
 */
public class DefaultPersistentSetBuilder<E> implements PersistentSetBuilder<E> {

    private final DB localDB;

    private String name = null;

    private Serializer serializer = null;

    public DefaultPersistentSetBuilder(DB localDB) {
        this.localDB = checkNotNull(localDB, "The local database cannot be null.");
    }

    public PersistentSetBuilder<E> withName(String name) {
        this.name = PersistenceManager.SET_PREFIX + checkNotNull(name);
        return this;
    }

    public PersistentSetBuilder<E> withSerializer(Serializer serializer) {
        checkArgument(this.serializer == null);
        checkNotNull(serializer);
        this.serializer = serializer;
        return this;
    }

    public PersistentSet<E> build() {
        checkNotNull(name, "The name must be assigned.");
        checkNotNull(serializer, "The serializer must be assigned.");

        return new PersistentSet<E>(serializer, localDB, name);
    }
}