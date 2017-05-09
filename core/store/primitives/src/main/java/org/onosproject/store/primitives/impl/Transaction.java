/*
 * Copyright 2017-present Open Networking Laboratory
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.onosproject.store.primitives.TransactionId;
import org.onosproject.store.service.TransactionLog;
import org.onosproject.store.service.Transactional;
import org.onosproject.store.service.Version;
import org.onosproject.store.service.TransactionContext;
import org.onosproject.store.service.TransactionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkState;

/**
 * Manages a transaction within the context of a single primitive.
 * <p>
 * The {@code Transaction} object is used to manage the transaction for a single partition primitive that implements
 * the {@link Transactional} interface. It's used as a proxy for {@link TransactionContext}s to manage the transaction
 * as it relates to a single piece of atomic state.
 */
public class Transaction<T> {

    /**
     * Transaction state.
     * <p>
     * The transaction state is used to indicate the phase within which the transaction is currently running.
     */
    enum State {

        /**
         * Active transaction state.
         * <p>
         * The {@code ACTIVE} state represents a transaction in progress. Active transactions may or may not affect
         * concurrently running transactions depending on the transaction's isolation level.
         */
        ACTIVE,

        /**
         * Preparing transaction state.
         * <p>
         * Once a transaction commitment begins, it enters the {@code PREPARING} phase of the two-phase commit protocol.
         */
        PREPARING,

        /**
         * Prepared transaction state.
         * <p>
         * Once the first phase of the two-phase commit protocol is complete, the transaction's state is set to
         * {@code PREPARED}.
         */
        PREPARED,

        /**
         * Committing transaction state.
         * <p>
         * The {@code COMMITTING} state represents a transaction within the second phase of the two-phase commit
         * protocol.
         */
        COMMITTING,

        /**
         * Committed transaction state.
         * <p>
         * Once the second phase of the two-phase commit protocol is complete, the transaction's state is set to
         * {@code COMMITTED}.
         */
        COMMITTED,

        /**
         * Rolling back transaction state.
         * <p>
         * In the event of a two-phase lock failure, when the transaction is rolled back it will enter the
         * {@code ROLLING_BACK} state while the rollback is in progress.
         */
        ROLLING_BACK,

        /**
         * Rolled back transaction state.
         * <p>
         * Once a transaction has been rolled back, it will enter the {@code ROLLED_BACK} state.
         */
        ROLLED_BACK,
    }

    private static final String TX_OPEN_ERROR = "transaction already open";
    private static final String TX_CLOSED_ERROR = "transaction not open";
    private static final String TX_INACTIVE_ERROR = "transaction is not active";
    private static final String TX_UNPREPARED_ERROR = "transaction has not been prepared";

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final TransactionId transactionId;
    protected final Transactional<T> transactionalObject;
    private final AtomicBoolean open = new AtomicBoolean();
    private volatile State state = State.ACTIVE;
    private volatile Version lock;

    public Transaction(TransactionId transactionId, Transactional<T> transactionalObject) {
        this.transactionId = transactionId;
        this.transactionalObject = transactionalObject;
    }

    /**
     * Returns the transaction identifier.
     *
     * @return the transaction identifier
     */
    public TransactionId transactionId() {
        return transactionId;
    }

    /**
     * Returns the current transaction state.
     *
     * @return the current transaction state
     */
    public State state() {
        return state;
    }

    /**
     * Returns a boolean indicating whether the transaction is open.
     *
     * @return indicates whether the transaction is open
     */
    public boolean isOpen() {
        return open.get();
    }

    /**
     * Opens the transaction, throwing an {@link IllegalStateException} if it's already open.
     */
    protected void open() {
        if (!open.compareAndSet(false, true)) {
            throw new IllegalStateException(TX_OPEN_ERROR);
        }
    }

    /**
     * Checks that the transaction is open and throws an {@link IllegalStateException} if not.
     */
    protected void checkOpen() {
        checkState(isOpen(), TX_CLOSED_ERROR);
    }

    /**
     * Checks that the transaction state is {@code ACTIVE} and throws an {@link IllegalStateException} if not.
     */
    protected void checkActive() {
        checkState(state == State.ACTIVE, TX_INACTIVE_ERROR);
    }

    /**
     * Checks that the transaction state is {@code PREPARED} and throws an {@link IllegalStateException} if not.
     */
    protected void checkPrepared() {
        checkState(state == State.PREPARED, TX_UNPREPARED_ERROR);
    }

    /**
     * Updates the transaction state.
     *
     * @param state the updated transaction state
     */
    protected void setState(State state) {
        this.state = state;
    }

    /**
     * Begins the transaction.
     * <p>
     * Locks are acquired when the transaction is begun to prevent concurrent transactions from operating on the shared
     * resource to which this transaction relates.
     *
     * @return a completable future to be completed once the transaction has been started
     */
    public CompletableFuture<Version> begin() {
        log.debug("Beginning transaction {} for {}", transactionId, transactionalObject);
        open();
        return transactionalObject.begin(transactionId).thenApply(lock -> {
            this.lock = lock;
            log.trace("Transaction lock acquired: {}", lock);
            return lock;
        });
    }

    /**
     * Prepares the transaction.
     * <p>
     * When preparing the transaction, the given list of updates for the shared resource will be prepared, and
     * concurrent modification checks will be performed. The returned future may be completed with a
     * {@link TransactionException} if a concurrent modification is detected for an isolation level that does
     * not allow such modifications.
     *
     * @param updates the transaction updates
     * @return a completable future to be completed once the transaction has been prepared
     */
    public CompletableFuture<Boolean> prepare(List<T> updates) {
        checkOpen();
        checkActive();
        log.debug("Preparing transaction {} for {}", transactionId, transactionalObject);
        Version lock = this.lock;
        checkState(lock != null, TX_INACTIVE_ERROR);
        setState(State.PREPARING);
        return transactionalObject.prepare(new TransactionLog<T>(transactionId, lock.value(), updates))
                .thenApply(succeeded -> {
                    setState(State.PREPARED);
                    return succeeded;
                });
    }

    /**
     * Prepares and commits the transaction in a single atomic operation.
     * <p>
     * Both the prepare and commit phases of the protocol must be executed within a single atomic operation. This method
     * is used to optimize committing transactions that operate only on a single partition within a single primitive.
     *
     * @param updates the transaction updates
     * @return a completable future to be completed once the transaction has been prepared
     */
    public CompletableFuture<Boolean> prepareAndCommit(List<T> updates) {
        checkOpen();
        checkActive();
        log.debug("Preparing and committing transaction {} for {}", transactionId, transactionalObject);
        Version lock = this.lock;
        checkState(lock != null, TX_INACTIVE_ERROR);
        setState(State.PREPARING);
        return transactionalObject.prepareAndCommit(new TransactionLog<T>(transactionId, lock.value(), updates))
                .thenApply(succeeded -> {
                    setState(State.COMMITTED);
                    return succeeded;
                });
    }

    /**
     * Commits the transaction.
     * <p>
     * Performs the second phase of the two-phase commit protocol, committing the previously
     * {@link #prepare(List) prepared} updates.
     *
     * @return a completable future to be completed once the transaction has been committed
     */
    public CompletableFuture<Void> commit() {
        checkOpen();
        checkPrepared();
        log.debug("Committing transaction {} for {}", transactionId, transactionalObject);
        setState(State.COMMITTING);
        return transactionalObject.commit(transactionId).thenRun(() -> {
            setState(State.COMMITTED);
        });
    }

    /**
     * Rolls back the transaction.
     * <p>
     * Rolls back the first phase of the two-phase commit protocol, cancelling prepared updates.
     *
     * @return a completable future to be completed once the transaction has been rolled back
     */
    public CompletableFuture<Void> rollback() {
        checkOpen();
        checkPrepared();
        log.debug("Rolling back transaction {} for {}", transactionId, transactionalObject);
        setState(State.ROLLING_BACK);
        return transactionalObject.rollback(transactionId).thenRun(() -> {
            setState(State.ROLLED_BACK);
        });
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("transactionId", transactionId)
                .add("state", state)
                .toString();
    }
}