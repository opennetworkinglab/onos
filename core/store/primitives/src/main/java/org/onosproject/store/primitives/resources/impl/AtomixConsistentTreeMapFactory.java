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

import io.atomix.catalyst.serializer.SerializableTypeResolver;
import io.atomix.copycat.client.CopycatClient;
import io.atomix.resource.ResourceFactory;
import io.atomix.resource.ResourceStateMachine;

import java.util.Properties;

/**
 * Factory for {@link AtomixConsistentTreeMap}.
 */
public class AtomixConsistentTreeMapFactory implements ResourceFactory<AtomixConsistentTreeMap> {
    @Override
    public SerializableTypeResolver createSerializableTypeResolver() {
        return new AtomixConsistentTreeMapCommands.TypeResolver();
    }

    @Override
    public ResourceStateMachine createStateMachine(Properties config) {
        return new AtomixConsistentTreeMapState(config);
    }

    @Override
    public AtomixConsistentTreeMap createInstance(CopycatClient client, Properties options) {
        return new AtomixConsistentTreeMap(client, options);
    }
}
