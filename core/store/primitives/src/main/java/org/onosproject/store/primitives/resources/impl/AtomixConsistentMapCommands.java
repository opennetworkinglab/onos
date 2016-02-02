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

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.CatalystSerializable;
import io.atomix.catalyst.serializer.Serializer;
import io.atomix.catalyst.util.Assert;
import io.atomix.copycat.client.Command;
import io.atomix.copycat.client.Query;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.onlab.util.Match;
import org.onosproject.store.service.Versioned;

import com.google.common.base.MoreObjects;

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
        public ConsistencyLevel consistency() {
          return ConsistencyLevel.LINEARIZABLE;
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
     * Abstract map query.
     */
    @SuppressWarnings("serial")
    public abstract static class MapQuery<V> implements Query<V>, CatalystSerializable {

        @Override
        public ConsistencyLevel consistency() {
          return ConsistencyLevel.BOUNDED_LINEARIZABLE;
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
     * Abstract key-based query.
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
         * Returns the key.
         * @return key
         */
        public byte[] value() {
            return value;
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
     * Contains key command.
     */
    @SuppressWarnings("serial")
    public static class ContainsValue extends ValueQuery<Boolean> {
        public ContainsValue() {
        }

        public ContainsValue(byte[] value) {
            super(value);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .add("value", value)
                    .toString();
        }
    }

    /**
     * Map prepare command.
     */
    @SuppressWarnings("serial")
    public static class TransactionPrepare extends MapCommand<PrepareResult> {
        private TransactionalMapUpdate<String, byte[]> update;

        public TransactionPrepare() {
        }

        public TransactionPrepare(TransactionalMapUpdate<String, byte[]> update) {
            this.update = update;
        }

        public TransactionalMapUpdate<String, byte[]> transactionUpdate() {
            return update;
        }

        @Override
        public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
            super.writeObject(buffer, serializer);
            serializer.writeObject(update, buffer);
        }

        @Override
        public void readObject(BufferInput<?> buffer, Serializer serializer) {
            super.readObject(buffer, serializer);
            update = serializer.readObject(buffer);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .add("update", update)
                    .toString();
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
          return value == null ? CompactionMode.FULL : CompactionMode.QUORUM;
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
     * KeySet query.
     */
    @SuppressWarnings("serial")
    public static class Values extends MapQuery<Collection<Versioned<byte[]>>> {
    }

    /**
     * KeySet query.
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
          return CompactionMode.FULL;
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
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .toString();
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
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .toString();
        }
    }
}
