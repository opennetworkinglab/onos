/*
 * Copyright 2016 Open Networking Laboratory
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

import com.google.common.base.MoreObjects;
import com.google.common.collect.Multiset;
import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.CatalystSerializable;
import io.atomix.catalyst.serializer.SerializableTypeResolver;
import io.atomix.catalyst.serializer.Serializer;
import io.atomix.catalyst.serializer.SerializerRegistry;
import io.atomix.catalyst.util.Assert;
import io.atomix.copycat.Command;
import io.atomix.copycat.Query;
import org.onlab.util.Match;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * AsyncConsistentMultimap state machine commands.
 */
public final class AsyncConsistentMultimapCommands {

    private AsyncConsistentMultimapCommands() {
    }

    /**
     * Abstract multimap command.
     */
    @SuppressWarnings("serial")
    public abstract static class MultimapCommand<V> implements Command<V>,
            CatalystSerializable {
        @Override
        public ConsistencyLevel consistency() {
            return ConsistencyLevel.SEQUENTIAL;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .toString();
        }

        @Override
        public void writeObject(BufferOutput<?> buffer,
                                Serializer serializer) {
        }

        @Override
        public void readObject(BufferInput<?> buffer, Serializer serializer) {
        }
    }

    /**
     * Abstract multimap query.
     */
    @SuppressWarnings("serial")
    public abstract static class MultimapQuery<V> implements Query<V>,
            CatalystSerializable {
        @Override
        public ConsistencyLevel consistency() {
            return ConsistencyLevel.SEQUENTIAL;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .toString();
        }

        @Override
        public void writeObject(BufferOutput<?> buffer,
                                Serializer serializer) {
        }

        @Override
        public void readObject(BufferInput<?> buffer,
                               Serializer serializer) {
        }
    }

    /**
     * Abstract key-based multimap query.
     */
    @SuppressWarnings("serial")
    public abstract static class KeyQuery<V> extends MultimapQuery<V> {
        protected String key;

        public KeyQuery() {
        }

        public KeyQuery(String key) {
            this.key = Assert.notNull(key, "key");
        }

        public String key() {
            return key;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .add("key", key)
                    .toString();
        }

        @Override
        public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
            super.writeObject(buffer, serializer);
            serializer.writeObject(key, buffer);
        }

        @Override
        public void readObject(BufferInput<?> buffer, Serializer serializer) {
            super.readObject(buffer, serializer);
            key = serializer.readObject(buffer);
        }
    }

    /**
     * Abstract value-based query.
     */
    @SuppressWarnings("serial")
    public abstract static class ValueQuery<V> extends MultimapQuery<V> {
        protected byte[] value;

        public ValueQuery() {
        }

        public ValueQuery(byte[] value) {
            this.value = Assert.notNull(value, "value");
        }

        /**
         * Returns the value.
         *
         * @return value.
         */
        public byte[] value() {
            return value;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .add("value", value)
                    .toString();
        }

        @Override
        public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
            super.writeObject(buffer, serializer);
        }

        @Override
        public void readObject(BufferInput<?> buffer, Serializer serializer) {
            super.readObject(buffer, serializer);
        }
    }

    /**
     * Size query.
     */
    public static class Size extends MultimapQuery<Integer> {
    }

    /**
     * Is empty query.
     */
    public static class IsEmpty extends MultimapQuery<Boolean> {
    }

    /**
     * Contains key query.
     */
    @SuppressWarnings("serial")
    public static class ContainsKey extends KeyQuery<Boolean> {
        public ContainsKey() {
        }

        public ContainsKey(String key) {
            super(key);
        }

    }

    /**
     * Contains value query.
     */
    @SuppressWarnings("serial")
    public static class ContainsValue extends ValueQuery<Boolean> {
        public ContainsValue() {
        }

        public ContainsValue(byte[] value) {
            super(value);
        }
    }

    /**
     * Contains entry query.
     */
    @SuppressWarnings("serial")
    public static class ContainsEntry extends MultimapQuery<Boolean> {
        protected String key;
        protected byte[] value;

        public ContainsEntry() {
        }

        public ContainsEntry(String key, byte[] value) {
            this.key = Assert.notNull(key, "key");
            this.value = Assert.notNull(value, "value");
        }

        public String key() {
            return key;
        }

        public byte[] value() {
            return value;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .add("key", key)
                    .add("value", value)
                    .toString();
        }

        @Override
        public void writeObject(BufferOutput<?> buffer,
                                Serializer serializer) {
            super.writeObject(buffer, serializer);
            serializer.writeObject(key, buffer);
            serializer.writeObject(value, buffer);
        }

        @Override
        public void readObject(BufferInput<?> buffer, Serializer serializer) {
            super.readObject(buffer, serializer);
            key = serializer.readObject(buffer);
            value = serializer.readObject(buffer);

        }
    }

    /**
     * Update and get command. Note that corresponding values must have the
     * same index in the respective arrays.
     */
    @SuppressWarnings("serial")
    public static class UpdateAndGet extends
            MultimapCommand<MapEntryUpdateResult<String, Collection<byte[]>>> {
        private String key;
        private List<byte[]> values;
        private List<Match<byte[]>> valueMatches;
        private List<Match<Long>> versionMatches;

        public UpdateAndGet() {
        }

        public UpdateAndGet(String key, List<byte[]> values,
                            List<Match<byte[]>> valueMatches,
                            List<Match<Long>> versionMatches) {
            this.key = key;
            this.values = values;
            this.valueMatches = valueMatches;
            this.versionMatches = versionMatches;
        }

        public String key() {
            return this.key;
        }

        public List<byte[]> values() {
            return values;
        }

        public List<Match<byte[]>> valueMatches() {
            return valueMatches;
        }

        public List<Match<Long>> versionMatches() {
            return versionMatches;
        }

        @Override
        public CompactionMode compaction() {
            return values == null ? CompactionMode.FULL :
                    CompactionMode.QUORUM;
        }

        @Override
        public void writeObject(BufferOutput<?> buffer,
                                Serializer serializer) {
            super.writeObject(buffer, serializer);
            serializer.writeObject(key, buffer);
            serializer.writeObject(values, buffer);
            serializer.writeObject(valueMatches, buffer);
            serializer.writeObject(versionMatches, buffer);
        }

        @Override
        public void readObject(BufferInput<?> buffer, Serializer serializer) {
            super.readObject(buffer, serializer);
            key = serializer.readObject(buffer);
            values = serializer.readObject(buffer);
            valueMatches = serializer.readObject(buffer);
            versionMatches = serializer.readObject(buffer);
        }

        @Override
        public String toString() {
            return super.toString();
        }
    }

    /**
     * Clear multimap command.
     */
    @SuppressWarnings("serial")
    public static class Clear extends MultimapCommand<Void> {
    }

    /**
     * Key set query.
     */
    @SuppressWarnings("serial")
    public static class KeySet extends MultimapQuery<Set<String>> {
    }

    /**
     * Key multiset query.
     */
    @SuppressWarnings("serial")
    public static class Keys extends MultimapQuery<Multiset<String>> {
    }

    /**
     * Value collection query.
     */
    @SuppressWarnings("serial")
    public static class Values extends MultimapQuery<Collection<byte[]>> {
    }

    /**
     * Entry set query.
     */
    @SuppressWarnings("serial")
    public static class Entries extends
            MultimapQuery<Collection<Map.Entry<String, byte[]>>> {
    }

    /**
     * Get value query.
     */
    public static class Get extends KeyQuery<Collection<byte[]>> {
    }

    /**
     * Multimap command type resolver.
     */
    @SuppressWarnings("serial")
    public static class TypeResolver implements SerializableTypeResolver {
        @Override
        public void resolve(SerializerRegistry registry) {
            registry.register(ContainsKey.class, -1000);
            registry.register(ContainsValue.class, -1001);
            registry.register(ContainsEntry.class, -1002);
            registry.register(UpdateAndGet.class, -1003);
            registry.register(Clear.class, -1004);
            registry.register(KeySet.class, -1005);
            registry.register(Keys.class, -1006);
            registry.register(Values.class, -1007);
            registry.register(Entries.class, -1008);
            registry.register(Size.class, -1009);
            registry.register(IsEmpty.class, -1010);
            registry.register(Get.class, -1011);
        }
    }
}
