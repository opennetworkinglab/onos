/*
 * Copyright 2016-present Open Networking Foundation
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

import java.util.LinkedHashMap;
import java.util.Optional;

import com.google.common.base.MoreObjects;
import io.atomix.protocols.raft.operation.OperationId;
import io.atomix.protocols.raft.operation.OperationType;
import org.onlab.util.KryoNamespace;
import org.onlab.util.Match;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.DocumentPath;
import org.onosproject.store.service.Versioned;

/**
 * {@link AtomixDocumentTree} resource state machine operations.
 */
public enum AtomixDocumentTreeOperations implements OperationId {
    ADD_LISTENER("set", OperationType.COMMAND),
    REMOVE_LISTENER("compareAndSet", OperationType.COMMAND),
    GET("incrementAndGet", OperationType.QUERY),
    GET_CHILDREN("getAndIncrement", OperationType.QUERY),
    UPDATE("addAndGet", OperationType.COMMAND),
    CLEAR("getAndAdd", OperationType.COMMAND);

    private final String id;
    private final OperationType type;

    AtomixDocumentTreeOperations(String id, OperationType type) {
        this.id = id;
        this.type = type;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public OperationType type() {
        return type;
    }

    public static final KryoNamespace NAMESPACE = KryoNamespace.newBuilder()
            .register(KryoNamespaces.BASIC)
            .nextId(KryoNamespaces.BEGIN_USER_CUSTOM_ID)
            .register(LinkedHashMap.class)
            .register(Listen.class)
            .register(Unlisten.class)
            .register(Get.class)
            .register(GetChildren.class)
            .register(Update.class)
            .register(DocumentPath.class)
            .register(Match.class)
            .register(Versioned.class)
            .register(DocumentTreeResult.class)
            .register(DocumentTreeResult.Status.class)
            .build("AtomixDocumentTreeOperations");

    /**
     * Abstract DocumentTree command.
     */
    @SuppressWarnings("serial")
    public abstract static class DocumentTreeOperation {
        private DocumentPath path;

        DocumentTreeOperation(DocumentPath path) {
            this.path = path;
        }

        public DocumentPath path() {
            return path;
        }
    }

    /**
     * DocumentTree#get query.
     */
    @SuppressWarnings("serial")
    public static class Get extends DocumentTreeOperation {
        public Get() {
            super(null);
        }

        public Get(DocumentPath path) {
            super(path);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .add("path", path())
                    .toString();
        }
    }

    /**
     * DocumentTree#getChildren query.
     */
    @SuppressWarnings("serial")
    public static class GetChildren extends DocumentTreeOperation {
        public GetChildren() {
            super(null);
        }

        public GetChildren(DocumentPath path) {
            super(path);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .add("path", path())
                    .toString();
        }
    }

    /**
     * DocumentTree update command.
     */
    @SuppressWarnings("serial")
    public static class Update extends DocumentTreeOperation {
        private Optional<byte[]> value;
        private Match<byte[]> valueMatch;
        private Match<Long> versionMatch;

        public Update() {
            super(null);
            this.value = null;
            this.valueMatch = null;
            this.versionMatch = null;
        }

        public Update(DocumentPath path, Optional<byte[]> value, Match<byte[]> valueMatch, Match<Long> versionMatch) {
            super(path);
            this.value = value;
            this.valueMatch = valueMatch;
            this.versionMatch = versionMatch;
        }

        public Optional<byte[]> value() {
            return value;
        }

        public Match<byte[]> valueMatch() {
            return valueMatch;
        }

        public Match<Long> versionMatch() {
            return versionMatch;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .add("path", path())
                    .add("value", value)
                    .add("valueMatch", valueMatch)
                    .add("versionMatch", versionMatch)
                    .toString();
        }
    }

    /**
     * Change listen.
     */
    @SuppressWarnings("serial")
    public static class Listen extends DocumentTreeOperation {
        public Listen() {
            this(DocumentPath.from("root"));
        }

        public Listen(DocumentPath path) {
            super(path);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .add("path", path())
                    .toString();
        }
    }

    /**
     * Change unlisten.
     */
    @SuppressWarnings("serial")
    public static class Unlisten extends DocumentTreeOperation {
        public Unlisten() {
            this(DocumentPath.from("root"));
        }

        public Unlisten(DocumentPath path) {
            super(path);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .add("path", path())
                    .toString();
        }
    }
}
