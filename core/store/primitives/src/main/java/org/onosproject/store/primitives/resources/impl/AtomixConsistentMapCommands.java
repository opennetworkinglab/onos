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

import java.util.Collection;
import java.util.Map;
import java.util.Set;

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
import org.onosproject.store.primitives.MapUpdate;
import org.onosproject.store.primitives.TransactionId;
import org.onosproject.store.service.TransactionLog;
import org.onosproject.store.service.Versioned;

/**
 * {@link AtomixConsistentMap} resource state machine operations.
 */
public final class AtomixConsistentMapCommands {

    private AtomixConsistentMapCommands() {
    }

    /**
     * Abstract map command.
     */
    @SuppressWarnings("serial")
    public abstract static class MapCommand<V> implements Command<V>, CatalystSerializable {

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .toString();
        }

        @Override
        public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
        }

        @Override
        public void readObject(BufferInput<?> buffer, Serializer serializer) {
        }
    }

    /**
     * Abstract map query.
     */
    @SuppressWarnings("serial")
    public abstract static class MapQuery<V> implements Query<V>, CatalystSerializable {

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
        public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
        }

        @Override
        public void readObject(BufferInput<?> buffer, Serializer serializer) {
        }
    }

    /**
     * Abstract key-based query.
     */
    @SuppressWarnings("serial")
    public abstract static class KeyQuery<V> extends MapQuery<V> {
        protected String key;

        public KeyQuery() {
        }

        public KeyQuery(String key) {
            this.key = Assert.notNull(key, "key");
        }

        /**
         * Returns the key.
         * @return key
         */
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
    public abstract static class ValueQuery<V> extends MapQuery<V> {
        protected byte[] value;

        public ValueQuery() {
        }

        public ValueQuery(byte[] value) {
            this.value = Assert.notNull(value, "value");
        }

        /**
         * Returns the value.
         * @return value
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
            serializer.writeObject(value, buffer);
        }

        @Override
        public void readObject(BufferInput<?> buffer, Serializer serializer) {
            super.readObject(buffer, serializer);
            value = serializer.readObject(buffer);
        }
    }

    /**
     * Contains key command.
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
     * Transaction begin command.
     */
    public static class TransactionBegin extends MapCommand<Long> {
        private TransactionId transactionId;

        public TransactionBegin() {
        }

        public TransactionBegin(TransactionId transactionId) {
            this.transactionId = transactionId;
        }

        public TransactionId transactionId() {
            return transactionId;
        }

        @Override
        public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
            super.writeObject(buffer, serializer);
            serializer.writeObject(transactionId, buffer);
        }

        @Override
        public void readObject(BufferInput<?> buffer, Serializer serializer) {
            super.readObject(buffer, serializer);
            transactionId = serializer.readObject(buffer);
        }
    }

    /**
     * Map prepare command.
     */
    @SuppressWarnings("serial")
    public static class TransactionPrepare extends MapCommand<PrepareResult> {
        private TransactionLog<MapUpdate<String, byte[]>> transactionLog;

        public TransactionPrepare() {
        }

        public TransactionPrepare(TransactionLog<MapUpdate<String, byte[]>> transactionLog) {
            this.transactionLog = transactionLog;
        }

        public TransactionLog<MapUpdate<String, byte[]>> transactionLog() {
            return transactionLog;
        }

        @Override
        public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
            super.writeObject(buffer, serializer);
            serializer.writeObject(transactionLog, buffer);
        }

        @Override
        public void readObject(BufferInput<?> buffer, Serializer serializer) {
            super.readObject(buffer, serializer);
            transactionLog = serializer.readObject(buffer);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .add("transactionLog", transactionLog)
                    .toString();
        }
    }

    /**
     * Map prepareAndCommit command.
     */
    @SuppressWarnings("serial")
    public static class TransactionPrepareAndCommit extends TransactionPrepare {
        public TransactionPrepareAndCommit() {
        }

        public TransactionPrepareAndCommit(TransactionLog<MapUpdate<String, byte[]>> transactionLog) {
            super(transactionLog);
        }

        @Override
        public CompactionMode compaction() {
            return CompactionMode.TOMBSTONE;
        }
    }

    /**
     * Map transaction commit command.
     */
    @SuppressWarnings("serial")
    public static class TransactionCommit extends MapCommand<CommitResult> {
        private TransactionId transactionId;

        public TransactionCommit() {
        }

        public TransactionCommit(TransactionId transactionId) {
            this.transactionId = transactionId;
        }

        /**
         * Returns the transaction identifier.
         * @return transaction id
         */
        public TransactionId transactionId() {
            return transactionId;
        }

        @Override
        public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
            super.writeObject(buffer, serializer);
            serializer.writeObject(transactionId, buffer);
        }

        @Override
        public void readObject(BufferInput<?> buffer, Serializer serializer) {
            super.readObject(buffer, serializer);
            transactionId = serializer.readObject(buffer);
        }

        @Override
        public CompactionMode compaction() {
            return CompactionMode.TOMBSTONE;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .add("transactionId", transactionId)
                    .toString();
        }
    }

    /**
     * Map transaction rollback command.
     */
    @SuppressWarnings("serial")
    public static class TransactionRollback extends MapCommand<RollbackResult> {
        private TransactionId transactionId;

        public TransactionRollback() {
        }

        public TransactionRollback(TransactionId transactionId) {
            this.transactionId = transactionId;
        }

        /**
         * Returns the transaction identifier.
         * @return transaction id
         */
        public TransactionId transactionId() {
            return transactionId;
        }

        @Override
        public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
            super.writeObject(buffer, serializer);
            serializer.writeObject(transactionId, buffer);
        }

        @Override
        public void readObject(BufferInput<?> buffer, Serializer serializer) {
            super.readObject(buffer, serializer);
            transactionId = serializer.readObject(buffer);
        }

        @Override
        public CompactionMode compaction() {
            return CompactionMode.TOMBSTONE;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .add("transactionId", transactionId)
                    .toString();
        }
    }

    /**
     * Map update command.
     */
    @SuppressWarnings("serial")
    public static class UpdateAndGet extends MapCommand<MapEntryUpdateResult<String, byte[]>> {
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

        /**
         * Returns the key.
         * @return key
         */
        public String key() {
            return this.key;
        }

        /**
         * Returns the value.
         * @return value
         */
        public byte[] value() {
            return this.value;
        }

        /**
         * Returns the value match.
         * @return value match
         */
        public Match<byte[]> valueMatch() {
            return this.valueMatch;
        }

        /**
         * Returns the version match.
         * @return version match
         */
        public Match<Long> versionMatch() {
            return this.versionMatch;
        }

        @Override
        public CompactionMode compaction() {
          return value == null ? CompactionMode.TOMBSTONE : CompactionMode.FULL;
        }

        @Override
        public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
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
     * Get query.
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
     * Get or default query.
     */
    @SuppressWarnings("serial")
    public static class GetOrDefault extends KeyQuery<Versioned<byte[]>> {
        private byte[] defaultValue;

        public GetOrDefault() {
        }

        public GetOrDefault(String key, byte[] defaultValue) {
            super(key);
            this.defaultValue = defaultValue;
        }

        /**
         * Returns the default value.
         *
         * @return the default value
         */
        public byte[] defaultValue() {
            return defaultValue;
        }

        @Override
        public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
            super.writeObject(buffer, serializer);
            serializer.writeObject(defaultValue, buffer);
        }

        @Override
        public void readObject(BufferInput<?> buffer, Serializer serializer) {
            super.readObject(buffer, serializer);
            defaultValue = serializer.readObject(buffer);
        }
    }

    /**
     * Is empty query.
     */
    @SuppressWarnings("serial")
    public static class IsEmpty extends MapQuery<Boolean> {
    }

    /**
     * KeySet query.
     */
    @SuppressWarnings("serial")
    public static class KeySet extends MapQuery<Set<String>> {
    }

    /**
     * ValueSet query.
     */
    @SuppressWarnings("serial")
    public static class Values extends MapQuery<Collection<Versioned<byte[]>>> {
    }

    /**
     * EntrySet query.
     */
    @SuppressWarnings("serial")
    public static class EntrySet extends MapQuery<Set<Map.Entry<String, Versioned<byte[]>>>> {
    }

    /**
     * Size query.
     */
    @SuppressWarnings("serial")
    public static class Size extends MapQuery<Integer> {
    }

    /**
     * Clear command.
     */
    @SuppressWarnings("serial")
    public static class Clear extends MapCommand<MapEntryUpdateResult.Status> {
        @Override
        public CompactionMode compaction() {
          return CompactionMode.TOMBSTONE;
        }
    }

    /**
     * Change listen.
     */
    @SuppressWarnings("serial")
    public static class Listen implements Command<Void>, CatalystSerializable {
        @Override
        public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
        }

        @Override
        public void readObject(BufferInput<?> buffer, Serializer serializer) {
        }

        @Override
        public CompactionMode compaction() {
            return CompactionMode.QUORUM;
        }
    }

    /**
     * Change unlisten.
     */
    @SuppressWarnings("serial")
    public static class Unlisten implements Command<Void>, CatalystSerializable {
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
     * Map command type resolver.
     */
    public static class TypeResolver implements SerializableTypeResolver {
        @Override
        public void resolve(SerializerRegistry registry) {
            registry.register(ContainsKey.class, -761);
            registry.register(ContainsValue.class, -762);
            registry.register(Get.class, -763);
            registry.register(GetOrDefault.class, -778);
            registry.register(EntrySet.class, -764);
            registry.register(Values.class, -765);
            registry.register(KeySet.class, -766);
            registry.register(Clear.class, -767);
            registry.register(IsEmpty.class, -768);
            registry.register(Size.class, -769);
            registry.register(Listen.class, -770);
            registry.register(Unlisten.class, -771);
            registry.register(TransactionBegin.class, -777);
            registry.register(TransactionPrepare.class, -772);
            registry.register(TransactionCommit.class, -773);
            registry.register(TransactionRollback.class, -774);
            registry.register(TransactionPrepareAndCommit.class, -775);
            registry.register(UpdateAndGet.class, -776);
        }
    }
}
