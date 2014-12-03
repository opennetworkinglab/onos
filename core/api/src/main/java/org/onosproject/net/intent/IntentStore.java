/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.net.intent;

import static com.google.common.base.Preconditions.checkNotNull;

import org.onosproject.net.intent.IntentStore.BatchWrite.Operation;
import org.onosproject.store.Store;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages inventory of end-station intents; not intended for direct use.
 */
public interface IntentStore extends Store<IntentEvent, IntentStoreDelegate> {

    /**
     * Submits a new intent into the store. If the returned event is not
     * null, the manager is expected to dispatch the event and then to kick
     * off intent compilation process. Otherwise, another node has been elected
     * to perform the compilation process and the node will learn about
     * the submittal and results of the intent compilation via the delegate
     * mechanism.
     *
     * @param intent intent to be submitted
     */
    @Deprecated
    void createIntent(Intent intent);

    /**
     * Removes the specified intent from the inventory.
     *
     * @param intentId intent identification
     */
    @Deprecated
    void removeIntent(IntentId intentId);

    /**
     * Returns the number of intents in the store.
     *
     * @return the number of intents in the store
     */
    long getIntentCount();

    /**
     * Returns a collection of all intents in the store.
     *
     * @return iterable collection of all intents
     */
    Iterable<Intent> getIntents();

    /**
     * Returns the intent with the specified identifier.
     *
     * @param intentId intent identification
     * @return intent or null if not found
     */
    Intent getIntent(IntentId intentId);

    /**
     * Returns the state of the specified intent.
     *
     * @param intentId intent identification
     * @return current intent state
     */
    IntentState getIntentState(IntentId intentId);

    /**
     * Sets the state of the specified intent to the new state.
     *
     * @param intent   intent whose state is to be changed
     * @param newState new state
     */
    void setState(Intent intent, IntentState newState);

    /**
     * Sets the installable intents which resulted from compilation of the
     * specified original intent.
     *
     * @param intentId           original intent identifier
     * @param installableIntents compiled installable intents
     */
    void setInstallableIntents(IntentId intentId, List<Intent> installableIntents);

    /**
     * Returns the list of the installable events associated with the specified
     * original intent.
     *
     * @param intentId original intent identifier
     * @return compiled installable intents
     */
    List<Intent> getInstallableIntents(IntentId intentId);

    /**
     * Removes any installable intents which resulted from compilation of the
     * specified original intent.
     *
     * @param intentId original intent identifier
     */
    void removeInstalledIntents(IntentId intentId);


    /**
     * Returns a new empty batch write operation buider.
     *
     * @return BatchWrite
     */
    default BatchWrite newBatchWrite() {
        return new BatchWrite();
    }

    /**
     * Execute writes in a batch.
     *
     * @param batch BatchWrite to execute
     * @return failed operations
     */
    List<Operation> batchWrite(BatchWrite batch);

    public static class BatchWrite {

        public enum OpType {
            CREATE_INTENT,
            REMOVE_INTENT,
            SET_STATE,
            SET_INSTALLABLE,
            REMOVE_INSTALLED
        }

        List<Operation> operations = new ArrayList<>();

        public List<Operation> operations() {
            return Collections.unmodifiableList(operations);
        }

        public boolean isEmpty() {
            return operations.isEmpty();
        }

        public BatchWrite createIntent(Intent intent) {
            operations.add(Operation.of(OpType.CREATE_INTENT,
                                        ImmutableList.of(intent)));
            return this;
        }

        public BatchWrite removeIntent(IntentId intentId) {
            operations.add(Operation.of(OpType.REMOVE_INTENT,
                                        ImmutableList.of(intentId)));
            return this;
        }

        public BatchWrite setState(Intent intent, IntentState newState) {
            operations.add(Operation.of(OpType.SET_STATE,
                                        ImmutableList.of(intent, newState)));
            return this;
        }

        public BatchWrite setInstallableIntents(IntentId intentId, List<Intent> installableIntents) {
            operations.add(Operation.of(OpType.SET_INSTALLABLE,
                                        ImmutableList.of(intentId, installableIntents)));
            return this;
        }

        public BatchWrite removeInstalledIntents(IntentId intentId) {
            operations.add(Operation.of(OpType.REMOVE_INSTALLED,
                                        ImmutableList.of(intentId)));
            return this;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .add("operations", operations)
                    .toString();
        }

        public static class Operation {
            final OpType type;
            final ImmutableList<Object> args;

            public static Operation of(OpType type, List<Object> args) {
                return new Operation(type, args);
            }

            public Operation(OpType type, List<Object> args) {
                this.type = checkNotNull(type);
                this.args = ImmutableList.copyOf(args);
            }

            public OpType type() {
                return type;
            }

            public ImmutableList<Object> args() {
                return args;
            }

            @SuppressWarnings("unchecked")
            public <T> T arg(int i) {
                return (T) args.get(i);
            }

            @Override
            public String toString() {
                return MoreObjects.toStringHelper(getClass())
                        .add("type", type)
                        .add("args", args)
                        .toString();
            }
        }
    }
}
