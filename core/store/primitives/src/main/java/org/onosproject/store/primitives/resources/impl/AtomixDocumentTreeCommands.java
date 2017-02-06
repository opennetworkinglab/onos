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

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.CatalystSerializable;
import io.atomix.catalyst.serializer.SerializableTypeResolver;
import io.atomix.catalyst.serializer.Serializer;
import io.atomix.catalyst.serializer.SerializerRegistry;
import io.atomix.copycat.Command;
import io.atomix.copycat.Query;

import java.util.Map;
import java.util.Optional;

import org.onlab.util.Match;
import org.onosproject.store.service.DocumentPath;
import org.onosproject.store.service.Versioned;

import com.google.common.base.MoreObjects;

/**
 * {@link AtomixDocumentTree} resource state machine operations.
 */
public class AtomixDocumentTreeCommands {

    /**
     * Abstract DocumentTree operation.
     */
    public abstract static class DocumentTreeOperation<V> implements CatalystSerializable {

        private DocumentPath path;

        DocumentTreeOperation(DocumentPath path) {
            this.path = path;
        }

        public DocumentPath path() {
            return path;
        }

        @Override
        public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
            serializer.writeObject(path, buffer);
        }

        @Override
        public void readObject(BufferInput<?> buffer, Serializer serializer) {
            path = serializer.readObject(buffer);
        }
    }

    /**
     * Abstract DocumentTree query.
     */
    @SuppressWarnings("serial")
    public abstract static class DocumentTreeQuery<V> extends DocumentTreeOperation<V> implements Query<V> {

         DocumentTreeQuery(DocumentPath path) {
             super(path);
        }

         @Override
         public ConsistencyLevel consistency() {
           return ConsistencyLevel.SEQUENTIAL;
         }
    }

    /**
     * Abstract DocumentTree command.
     */
    @SuppressWarnings("serial")
    public abstract static class DocumentTreeCommand<V> extends DocumentTreeOperation<V> implements Command<V> {

        DocumentTreeCommand(DocumentPath path) {
             super(path);
        }
    }

    /**
     * DocumentTree#get query.
     */
    @SuppressWarnings("serial")
    public static class Get extends DocumentTreeQuery<Versioned<byte[]>> {
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
    public static class GetChildren extends DocumentTreeQuery<Map<String, Versioned<byte[]>>> {
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
    public static class Update extends DocumentTreeCommand<DocumentTreeUpdateResult<byte[]>> {

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
        public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
            super.writeObject(buffer, serializer);
            serializer.writeObject(value, buffer);
            serializer.writeObject(valueMatch, buffer);
            serializer.writeObject(versionMatch, buffer);
        }

        @Override
        public void readObject(BufferInput<?> buffer, Serializer serializer) {
            super.readObject(buffer, serializer);
            value = serializer.readObject(buffer);
            valueMatch = serializer.readObject(buffer);
            versionMatch = serializer.readObject(buffer);
        }

        @Override
        public CompactionMode compaction() {
            return value == null ? CompactionMode.TOMBSTONE : CompactionMode.QUORUM;
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
     * Clear command.
     */
    @SuppressWarnings("serial")
    public static class Clear implements Command<Void>, CatalystSerializable {
        @Override
        public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
        }

        @Override
        public void readObject(BufferInput<?> buffer, Serializer serializer) {
        }

        @Override
        public CompactionMode compaction() {
            return CompactionMode.TOMBSTONE;
        }
    }

    /**
     * Change listen.
     */
    @SuppressWarnings("serial")
    public static class Listen extends DocumentTreeCommand<Void> {

        public Listen() {
            this(DocumentPath.from("root"));
        }

        public Listen(DocumentPath path) {
            super(path);
        }

        @Override
        public CompactionMode compaction() {
            return CompactionMode.QUORUM;
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
    public static class Unlisten extends DocumentTreeCommand<Void> {

        public Unlisten() {
            this(DocumentPath.from("root"));
        }

        public Unlisten(DocumentPath path) {
            super(path);
        }

        @Override
        public CompactionMode compaction() {
            return CompactionMode.TOMBSTONE;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .add("path", path())
                    .toString();
        }
    }

    /**
     * DocumentTree command type resolver.
     */
    public static class TypeResolver implements SerializableTypeResolver {
        @Override
        public void resolve(SerializerRegistry registry) {
            registry.register(Get.class, -911);
            registry.register(GetChildren.class, -912);
            registry.register(Update.class, -913);
            registry.register(Listen.class, -914);
            registry.register(Unlisten.class, -915);
            registry.register(Clear.class, -916);
        }
    }
}
