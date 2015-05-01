/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.store.consistent.impl;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.Set;

import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.SetBuilder;

/**
 * Default Set builder.
 *
 * @param <E> type for set elements
 */
public class DefaultSetBuilder<E> implements SetBuilder<E> {

    private Serializer serializer;
    private String name;
    private final Database database;
    private boolean readOnly;

    public DefaultSetBuilder(Database database) {
        this.database = checkNotNull(database);
    }

    @Override
    public SetBuilder<E> withName(String name) {
        checkArgument(name != null && !name.isEmpty());
        this.name = name;
        return this;
    }

    @Override
    public SetBuilder<E> withSerializer(Serializer serializer) {
        checkArgument(serializer != null);
        this.serializer = serializer;
        return this;
    }

    @Override
    public SetBuilder<E> withUpdatesDisabled() {
        readOnly = true;
        return this;
    }

    private boolean validInputs() {
        return name != null && serializer != null;
    }

    @Override
    public Set<E> build() {
        checkState(validInputs());
        return new DefaultDistributedSet<>(name, database, serializer, readOnly);
    }
}
