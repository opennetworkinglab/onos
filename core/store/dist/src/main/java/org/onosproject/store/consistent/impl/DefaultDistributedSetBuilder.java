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

import org.onosproject.core.ApplicationId;
import org.onosproject.store.service.ConsistentMapBuilder;
import org.onosproject.store.service.DistributedSet;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.DistributedSetBuilder;

/**
 * Default distributed set builder.
 *
 * @param <E> type for set elements
 */
public class DefaultDistributedSetBuilder<E> implements DistributedSetBuilder<E> {

    private String name;
    private ConsistentMapBuilder<E, Boolean>  mapBuilder;
    private boolean metering = true;

    public DefaultDistributedSetBuilder(DatabaseManager manager) {
        this.mapBuilder = manager.consistentMapBuilder();
        mapBuilder.withMeteringDisabled();
    }

    @Override
    public DistributedSetBuilder<E> withName(String name) {
        mapBuilder.withName(name);
        this.name = name;
        return this;
    }

    @Override
    public DistributedSetBuilder<E> withApplicationId(ApplicationId id) {
        mapBuilder.withApplicationId(id);
        return this;
    }

    @Override
    public DistributedSetBuilder<E> withPurgeOnUninstall() {
        mapBuilder.withPurgeOnUninstall();
        return this;
    }

    @Override
    public DistributedSetBuilder<E> withSerializer(Serializer serializer) {
        mapBuilder.withSerializer(serializer);
        return this;
    }

    @Override
    public DistributedSetBuilder<E> withUpdatesDisabled() {
        mapBuilder.withUpdatesDisabled();
        return this;
    }

    @Override
    public DistributedSetBuilder<E> withRelaxedReadConsistency() {
        mapBuilder.withRelaxedReadConsistency();
        return this;
    }

    @Override
    public DistributedSetBuilder<E> withPartitionsDisabled() {
        mapBuilder.withPartitionsDisabled();
        return this;
    }

    @Override
    public DistributedSetBuilder<E> withMeteringDisabled() {
        metering = false;
        return this;
    }

    @Override
    public DistributedSet<E> build() {
        return new DefaultDistributedSet<E>(name, metering, mapBuilder.build());
    }
}
