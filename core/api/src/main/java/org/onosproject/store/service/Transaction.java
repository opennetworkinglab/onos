/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.store.service;

import java.util.List;

/**
 * An immutable transaction object.
 */
public interface Transaction {

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

    /**
     * Returns the transaction Id.
     *
     * @return transaction id
     */
    long id();

    /**
     * Returns the list of updates that are part of this transaction.
     *
     * @return list of database updates
     */
    List<DatabaseUpdate> updates();

    /**
     * Returns the current state of this transaction.
     *
     * @return transaction state
     */
    State state();

    /**
     * Returns true if this transaction has completed execution.
     *
     * @return true is yes, false otherwise
     */
    default boolean isDone() {
        return state() == State.COMMITTED || state() == State.ROLLEDBACK;
    }

    /**
     * Returns a new transaction that is created by transitioning this one to the specified state.
     *
     * @param newState destination state
     * @return a new transaction instance similar to the current one but its state set to specified state
     */
    Transaction transition(State newState);

    /**
     * Returns the system time when the transaction was last updated.
     *
     * @return last update time
     */
    long lastUpdated();
}
