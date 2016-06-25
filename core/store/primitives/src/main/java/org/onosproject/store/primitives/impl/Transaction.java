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
package org.onosproject.store.primitives.impl;

import java.util.List;

import org.onosproject.store.primitives.MapUpdate;
import org.onosproject.store.primitives.TransactionId;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;

/**
 * An immutable transaction object.
 */
public class Transaction {

    enum State {
        /**
         * Indicates a new transaction that is about to be prepared. All transactions
         * start their life in this state.
         */
        PREPARING,

        /**
         * Indicates a transaction that is successfully prepared i.e. all participants voted to commit
         */
        PREPARED,

        /**
         * Indicates a transaction that is about to be committed.
         */
        COMMITTING,

        /**
         * Indicates a transaction that has successfully committed.
         */
        COMMITTED,

        /**
         * Indicates a transaction that is about to be rolled back.
         */
        ROLLINGBACK,

        /**
         * Indicates a transaction that has been rolled back and all locks are released.
         */
        ROLLEDBACK
    }

    private final TransactionId transactionId;
    private final List<MapUpdate<String, byte[]>> updates;
    private final State state;

    public Transaction(TransactionId transactionId, List<MapUpdate<String, byte[]>> updates) {
        this(transactionId, updates, State.PREPARING);
    }

    private Transaction(TransactionId transactionId,
            List<MapUpdate<String, byte[]>> updates,
            State state) {
        this.transactionId = transactionId;
        this.updates = ImmutableList.copyOf(updates);
        this.state = state;
    }

    public TransactionId id() {
        return transactionId;
    }

    public List<MapUpdate<String, byte[]>> updates() {
        return updates;
    }

    public State state() {
        return state;
    }

    public Transaction transition(State newState) {
        return new Transaction(transactionId, updates, newState);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("transactionId", transactionId)
                .add("updates", updates)
                .add("state", state)
                .toString();
    }
}