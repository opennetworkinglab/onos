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
import io.atomix.protocols.raft.operation.OperationId;
import io.atomix.protocols.raft.operation.OperationType;
import org.onlab.util.KryoNamespace;
import org.onlab.util.Match;
import org.onosproject.store.primitives.MapUpdate;
import org.onosproject.store.primitives.TransactionId;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.TransactionLog;
import org.onosproject.store.service.Versioned;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link AtomixConsistentMap} resource state machine operations.
 */
public enum AtomixConsistentMapOperations implements OperationId {
    IS_EMPTY("isEmpty", OperationType.QUERY),
    SIZE("size", OperationType.QUERY),
    CONTAINS_KEY("containsKey", OperationType.QUERY),
    CONTAINS_VALUE("containsValue", OperationType.QUERY),
    GET("get", OperationType.QUERY),
    GET_OR_DEFAULT("getOrDefault", OperationType.QUERY),
    KEY_SET("keySet", OperationType.QUERY),
    VALUES("values", OperationType.QUERY),
    ENTRY_SET("entrySet", OperationType.QUERY),
    UPDATE_AND_GET("updateAndGet", OperationType.COMMAND),
    CLEAR("clear", OperationType.COMMAND),
    ADD_LISTENER("addListener", OperationType.COMMAND),
    REMOVE_LISTENER("removeListener", OperationType.COMMAND),
    BEGIN("begin", OperationType.COMMAND),
    PREPARE("prepare", OperationType.COMMAND),
    PREPARE_AND_COMMIT("prepareAndCommit", OperationType.COMMAND),
    COMMIT("commit", OperationType.COMMAND),
    ROLLBACK("rollback", OperationType.COMMAND);

    private final String id;
    private final OperationType type;

    AtomixConsistentMapOperations(String id, OperationType type) {
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
            .register(ContainsKey.class)
            .register(ContainsValue.class)
            .register(Get.class)
            .register(GetOrDefault.class)
            .register(UpdateAndGet.class)
            .register(TransactionBegin.class)
            .register(TransactionPrepare.class)
            .register(TransactionPrepareAndCommit.class)
            .register(TransactionCommit.class)
            .register(TransactionRollback.class)
            .register(TransactionId.class)
            .register(TransactionLog.class)
            .register(MapUpdate.class)
            .register(MapUpdate.Type.class)
            .register(PrepareResult.class)
            .register(CommitResult.class)
            .register(RollbackResult.class)
            .register(Match.class)
            .register(MapEntryUpdateResult.class)
            .register(MapEntryUpdateResult.Status.class)
            .register(Versioned.class)
            .register(byte[].class)
            .build("AtomixConsistentMapOperations");

    /**
     * Abstract map command.
     */
    @SuppressWarnings("serial")
    public abstract static class MapOperation {
        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .toString();
        }
    }

    /**
     * Abstract key-based query.
     */
    @SuppressWarnings("serial")
    public abstract static class KeyOperation extends MapOperation {
        protected String key;

        public KeyOperation() {
        }

        public KeyOperation(String key) {
            this.key = checkNotNull(key, "key cannot be null");
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
    }

    /**
     * Abstract value-based query.
     */
    @SuppressWarnings("serial")
    public abstract static class ValueOperation extends MapOperation {
        protected byte[] value;

        public ValueOperation() {
        }

        public ValueOperation(byte[] value) {
            this.value = checkNotNull(value, "value cannot be null");
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
    }

    /**
     * Contains key command.
     */
    @SuppressWarnings("serial")
    public static class ContainsKey extends KeyOperation {
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
    public static class ContainsValue extends ValueOperation {
        public ContainsValue() {
        }

        public ContainsValue(byte[] value) {
            super(value);
        }
    }

    /**
     * Transaction begin command.
     */
    public static class TransactionBegin extends MapOperation {
        private TransactionId transactionId;

        public TransactionBegin() {
        }

        public TransactionBegin(TransactionId transactionId) {
            this.transactionId = transactionId;
        }

        public TransactionId transactionId() {
            return transactionId;
        }
    }

    /**
     * Map prepare command.
     */
    @SuppressWarnings("serial")
    public static class TransactionPrepare extends MapOperation {
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
    }

    /**
     * Map transaction commit command.
     */
    @SuppressWarnings("serial")
    public static class TransactionCommit extends MapOperation {
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
    public static class TransactionRollback extends MapOperation {
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
    public static class UpdateAndGet extends MapOperation {
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
    public static class Get extends KeyOperation {
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
    public static class GetOrDefault extends KeyOperation {
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
    }
}
