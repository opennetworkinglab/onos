/*
 * Copyright 2017-present Open Networking Laboratory
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

import io.atomix.protocols.raft.event.EventType;
import org.onlab.util.KryoNamespace;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.DocumentTreeEvent;

/**
 * Atomix document tree events.
 */
public enum AtomixDocumentTreeEvents implements EventType {
    CHANGE("change");

    private final String id;

    AtomixDocumentTreeEvents(String id) {
        this.id = id;
    }

    @Override
    public String id() {
        return id;
    }

    public static final KryoNamespace NAMESPACE = KryoNamespace.newBuilder()
            .nextId(KryoNamespaces.BEGIN_USER_CUSTOM_ID + 50)
            .register(DocumentTreeEvent.class)
            .register(DocumentTreeEvent.Type.class)
            .build("AtomixDocumentTreeEvents");
}
