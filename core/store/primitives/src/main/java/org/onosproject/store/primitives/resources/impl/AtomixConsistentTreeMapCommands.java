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

import com.google.common.base.MoreObjects;
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
import org.onosproject.store.service.Versioned;

import java.util.Collection;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;

/**
 * {@link org.onosproject.store.service.AsyncConsistentTreeMap} Resource
 * state machine operations.
 */
public final class AtomixConsistentTreeMapCommands {

    private AtomixConsistentTreeMapCommands() {
    }

    /**
     * Abstract treeMap command.
     */
    @SuppressWarnings("serial")
    public abstract static class TreeCommand<V>
            implements Command<V>, CatalystSerializable {

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
     * Abstract treeMap query.
     */
    @SuppressWarnings("serial")
    public abstract static class TreeQuery<V>
            implements Query<V>, CatalystSerializable {
        @Override
        public ConsistencyLevel consistency() {
            return ConsistencyLevel.LINEARIZABLE_LEASE;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .toString();
        }

        @Override
        public void writeObject(BufferOutput<?> bufferOutput,
                                Serializer serializer) {

        }

        @Override
        public void readObject(BufferInput<?> bufferInput,
                               Serializer serializer) {

        }
    }
    /**
     * Abstract key-based query.
     */
    @SuppressWarnings("serial")
    public abstract static class KeyQuery<K> extends TreeQuery<K> {
        protected String key;

        public KeyQuery(String key) {
            this.key = Assert.notNull(key, "key");
        }

        public KeyQuery() {
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
        public void writeObject(BufferOutput<?> bufferOutput,
                                Serializer serializer) {
            super.writeObject(bufferOutput, serializer);
            serializer.writeObject(key, bufferOutput);
        }

        @Override
        public void readObject(BufferInput<?> bufferInput,
                               Serializer serializer) {
            super.readObject(bufferInput, serializer);
            key = serializer.readObject(bufferInput);
        }
    }

    /**
     * Abstract value-based query.
     */
    @SuppressWarnings("serial")
    public abstract static class ValueQuery<V> extends TreeQuery<V> {
        protected byte[] value;

        public ValueQuery() {}

        public ValueQuery(byte[] value) {
            this.value = Assert.notNull(value, "value");
        }

        public byte[] value() {
            return value;
        }

        @Override
        public void writeObject(BufferOutput<?> bufferOutput,
                                Serializer serializer) {
            super.writeObject(bufferOutput, serializer);
            serializer.writeObject(value, bufferOutput);
        }

        @Override
        public void readObject(BufferInput<?> bufferInput,
                               Serializer serializer) {
            super.readObject(bufferInput, serializer);
            value = serializer.readObject(bufferInput);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .add("value", value)
                    .toString();
        }
    }

    /**
     * Contains key command.
     */
    @SuppressWarnings("serial")
    public static class ContainsKey extends KeyQuery<Boolean> {

        public ContainsKey(String key) {
            super(key);
        }

        public ContainsKey() {
        }
    }
    /**
     * Contains value command.
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
     * AsyncConsistentTreeMap update command.
     */
    @SuppressWarnings("serial")
    public static class UpdateAndGet
            extends TreeCommand<MapEntryUpdateResult<String, byte[]>> {
        private String key;
        private byte[] value;
        private Match<byte[]> valueMatch;
        private Match<Long> versionMatch;
        public UpdateAndGet() {
        }

        public UpdateAndGet(String key,
                            byte[] value,
                            Match<byte[]> valueMatch,
                            Match<Long> versionMatch) {
            this.key = key;
            this.value = value;
            this.valueMatch = valueMatch;
            this.versionMatch = versionMatch;
        }

        public String key() {
            return this.key;
        }

        public byte[] value() {
            return this.value;
        }

        public Match<byte[]> valueMatch() {
            return this.valueMatch;
        }

        public Match<Long> versionMatch() {
            return this.versionMatch;
        }

        @Override
        public CompactionMode compaction() {
            return value == null ? CompactionMode.FULL : CompactionMode.QUORUM;
        }

        @Override
        public void writeObject(BufferOutput<?> buffer,
                                Serializer serializer) {
            super.writeObject(buffer, serializer);
            serializer.writeObject(key, buffer);
            serializer.writeObject(value, buffer);
            serializer.writeObject(valueMatch, buffer);
            serializer.writeObject(versionMatch, buffer);
        }

        @Override
        public void readObject(BufferInput<?> buffer, Serializer serializer) {
            super.readObject(buffer, serializer);
            key = serializer.readObject(buffer);
            value = serializer.readObject(buffer);
            valueMatch = serializer.readObject(buffer);
            versionMatch = serializer.readObject(buffer);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .add("key", key)
                    .add("value", value)
                    .add("valueMatch", valueMatch)
                    .add("versionMatch", versionMatch)
                    .toString();
        }
    }

    /**
     * Get query command.
     */
    @SuppressWarnings("serial")
    public static class Get extends KeyQuery<Versioned<byte[]>> {
        public Get() {
        }

        public Get(String key) {
            super(key);
        }
    }

    /**
     * Is empty query.
     */
    @SuppressWarnings("serial")
    public static class IsEmpty extends TreeQuery<Boolean> {

    }

    /**
     * Key set query.
     */
    @SuppressWarnings("serial")
    public static class KeySet extends TreeQuery<Set<String>> {
    }

    /**
     * Value set query.
     */
    @SuppressWarnings("serial")
    public static class Values
            extends TreeQuery<Collection<Versioned<byte[]>>> {
    }

    /**
     * Entry set query.
     */
    @SuppressWarnings("serial")
    public static class EntrySet
            extends TreeQuery<Set<Map.Entry<String, Versioned<byte[]>>>> {
    }

    /**
     * Size query.
     */
    @SuppressWarnings("serial")
    public static class Size extends TreeQuery<Integer> {
    }

    /**
     * Clear command.
     */
    @SuppressWarnings("serial")
    public static class Clear
            extends TreeCommand<MapEntryUpdateResult.Status> {
        @Override
        public CompactionMode compaction() {
            return CompactionMode.FULL;
        }
    }

    /**
     * Change listen.
     */
    @SuppressWarnings("serial")
    public static class Listen implements Command<Void>, CatalystSerializable {
        @Override
        public void writeObject(BufferOutput<?> buffer,
                                Serializer serializer) {
        }

        @Override
        public void readObject(BufferInput<?> buffer, Serializer serializer) {
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .toString();
        }
    }

    /**
     * Change unlisten.
     */
    @SuppressWarnings("serial")
    public static class Unlisten implements Command<Void>,
            CatalystSerializable {
        @Override
        public void writeObject(BufferOutput<?> buffer,
                                Serializer serializer) {
        }

        @Override
        public void readObject(BufferInput<?> buffer, Serializer serializer) {
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .toString();
        }
    }

        /* Tree map specific commands below */

    /**
     * First key query.
     */
    @SuppressWarnings("serial")
    public static class FirstKey<K> extends TreeQuery<K> {
    }

    /**
     * Last key query.
     */
    @SuppressWarnings("serial")
    public static class LastKey<K> extends TreeQuery<K> {
    }

    /**
     * First entry query.
     */
    @SuppressWarnings("serial")
    public static class FirstEntry<K, V> extends TreeQuery<Map.Entry<K, V>> {
    }

    /**
     * Last entry query.
     */
    @SuppressWarnings("serial")
    public static class LastEntry<K, V> extends TreeQuery<Map.Entry<K, V>> {
    }

    /**
     * First entry query, if none exists returns null.
     */
    @SuppressWarnings("serial")
    public static class PollFirstEntry<K, V>
            extends TreeQuery<Map.Entry<K, V>> {
    }

    /**
     * Last entry query, if none exists returns null.
     */
    @SuppressWarnings("serial")
    public static class PollLastEntry<K, V>
            extends TreeQuery<Map.Entry<K, V>> {
    }

    /**
     * Query returns the entry associated with the largest key less than the
     * passed in key.
     */
    @SuppressWarnings("serial")
    public static class LowerEntry<K, V> extends KeyQuery<K> {
        public LowerEntry() {
        }

        public LowerEntry(String key) {
            super(key);
        }
    }

    /**
     * Query returns the largest key less than the specified key.
     */
    @SuppressWarnings("serial")
    public static class LowerKey<K> extends KeyQuery<K> {
        public LowerKey() {
        }

        public LowerKey(String key) {
            super(key);
        }
    }

    /**
     * Query returns the entry associated with the largest key smaller than or
     * equal to the specified key.
     */
    @SuppressWarnings("serial")
    public static class FloorEntry<K, V> extends KeyQuery<Map.Entry<K, V>> {
        public FloorEntry() {
        }

        public FloorEntry(String key) {
            super(key);
        }
    }

    /**
     * Query returns the largest key smaller than or equal to the passed in
     * key.
     */
    @SuppressWarnings("serial")
    public static class FloorKey<K> extends KeyQuery<K> {
        public FloorKey() {
        }

        public FloorKey(String key) {
            super(key);
        }
    }

    /**
     * Returns the entry associated with the smallest key larger than or equal
     * to the specified key.
     */
    @SuppressWarnings("serial")
    public static class CeilingEntry<K, V> extends KeyQuery<Map.Entry<K, V>> {
        public CeilingEntry() {
        }

        public CeilingEntry(String key) {
            super(key);
        }
    }

    /**
     * Returns the smallest key larger than or equal to the specified key.
     *
     * @param <K> key type
     */
    @SuppressWarnings("serial")
    public static class CeilingKey<K> extends KeyQuery<K> {
        public CeilingKey() {
        }

        public CeilingKey(String key) {
            super(key);
        }
    }

    /**
     * Returns the entry associated with the smallest key larger than the
     * specified key.
     */
    @SuppressWarnings("serial")
    public static class HigherEntry<K, V> extends KeyQuery<Map.Entry<K, V>> {
        public HigherEntry() {
        }

        public HigherEntry(String key) {
            super(key);
        }
    }

    /**
     * Returns the smallest key larger than the specified key.
     */
    @SuppressWarnings("serial")
    public static class HigherKey<K> extends KeyQuery<K> {
        public HigherKey() {
        }

        public HigherKey(String key) {
            super(key);
        }
    }

    @SuppressWarnings("serial")
    public static class NavigableKeySet<K, V>
            extends TreeQuery<NavigableSet<K>> {
    }

    @SuppressWarnings("serial")
    public static class SubMap<K, V> extends TreeQuery<NavigableMap<K, V>> {
        private K fromKey;
        private K toKey;
        private boolean inclusiveFrom;
        private boolean inclusiveTo;

        public SubMap() {
        }

        public SubMap(K fromKey, K toKey, boolean inclusiveFrom,
                      boolean inclusiveTo) {
            this.fromKey = fromKey;
            this.toKey = toKey;
            this.inclusiveFrom = inclusiveFrom;
            this.inclusiveTo = inclusiveTo;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .add("getFromKey", fromKey)
                    .add("getToKey", toKey)
                    .add("inclusiveFrotBound", inclusiveFrom)
                    .add("inclusiveToBound", inclusiveTo)
                    .toString();
        }

        @Override
        public void writeObject(BufferOutput<?> bufferOutput,
                                Serializer serializer) {
            super.writeObject(bufferOutput, serializer);
            serializer.writeObject(fromKey, bufferOutput);
            serializer.writeObject(toKey, bufferOutput);
            serializer.writeObject(inclusiveFrom, bufferOutput);
            serializer.writeObject(inclusiveTo, bufferOutput);
        }

        @Override
        public void readObject(BufferInput<?> bufferInput,
                               Serializer serializer) {
            super.readObject(bufferInput, serializer);
            fromKey = serializer.readObject(bufferInput);
            toKey = serializer.readObject(bufferInput);
            inclusiveFrom = serializer.readObject(bufferInput);
            inclusiveTo = serializer.readObject(bufferInput);
        }

        public K fromKey() {
            return fromKey;
        }

        public K toKey() {
            return toKey;
        }

        public boolean isInclusiveFrom() {
            return inclusiveFrom;
        }

        public boolean isInclusiveTo() {
            return inclusiveTo;
        }
    }

    /**
     * Tree map command type resolver.
     */
    public static class TypeResolver implements SerializableTypeResolver {
        @Override
        public void resolve(SerializerRegistry registry) {
            //NOTE the registration values must be unique throughout the
            // project.
            registry.register(ContainsKey.class, -1161);
            registry.register(ContainsValue.class, -1162);
            registry.register(Get.class, -1163);
            registry.register(EntrySet.class, -1164);
            registry.register(Values.class, -1165);
            registry.register(KeySet.class, -1166);
            registry.register(Clear.class, -1167);
            registry.register(IsEmpty.class, -1168);
            registry.register(Size.class, -1169);
            registry.register(Listen.class, -1170);
            registry.register(Unlisten.class, -1171);
            //Transaction related commands will be added here with numbers
            // -1172 to -1174
            registry.register(UpdateAndGet.class, -1175);
            registry.register(FirstKey.class, -1176);
            registry.register(LastKey.class, -1177);
            registry.register(FirstEntry.class, -1178);
            registry.register(LastEntry.class, -1179);
            registry.register(PollFirstEntry.class, -1180);
            registry.register(PollLastEntry.class, -1181);
            registry.register(LowerEntry.class, -1182);
            registry.register(LowerKey.class, -1183);
            registry.register(FloorEntry.class, -1184);
            registry.register(FloorKey.class, -1185);
            registry.register(CeilingEntry.class, -1186);
            registry.register(CeilingKey.class, -1187);
            registry.register(HigherEntry.class, -1188);
            registry.register(HigherKey.class, -1189);
            registry.register(SubMap.class, -1190);
            registry.register(NavigableKeySet.class, -1191);
        }
    }
}
