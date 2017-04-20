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
package org.onosproject.store.primitives.impl;

import org.onosproject.store.primitives.DistributedPrimitiveCreator;
import org.onosproject.store.service.AsyncConsistentTreeMap;
import org.onosproject.store.service.ConsistentTreeMap;
import org.onosproject.store.service.ConsistentTreeMapBuilder;

/**
 * Default {@link org.onosproject.store.service.AsyncConsistentTreeMap} builder.
 *
 * @param <V> type for map value
 */
public class DefaultConsistentTreeMapBuilder<V> extends ConsistentTreeMapBuilder<V> {

    private final DistributedPrimitiveCreator primitiveCreator;

    public DefaultConsistentTreeMapBuilder(DistributedPrimitiveCreator primitiveCreator) {
        this.primitiveCreator = primitiveCreator;
    }

    @Override
    public AsyncConsistentTreeMap<V> buildTreeMap() {
        return primitiveCreator.newAsyncConsistentTreeMap(name(), serializer(), executorSupplier());
    }

    @Override
    public ConsistentTreeMap<V> build() {
        return buildTreeMap().asTreeMap();
    }

}
