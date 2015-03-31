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
package org.onosproject.store.consistent.impl;

import java.util.List;

import org.onosproject.store.service.DatabaseUpdate;
import org.onosproject.store.service.Transaction;

import com.google.common.collect.ImmutableList;

/**
 * A Default transaction implementation.
 */
public class DefaultTransaction implements Transaction {

    private final long transactionId;
    private final List<DatabaseUpdate> updates;
    private final State state;
    private final long lastUpdated;

    public DefaultTransaction(long transactionId, List<DatabaseUpdate> updates) {
        this(transactionId, updates, State.PREPARING, System.currentTimeMillis());
    }

    private DefaultTransaction(long transactionId, List<DatabaseUpdate> updates, State state, long lastUpdated) {
        this.transactionId = transactionId;
        this.updates = ImmutableList.copyOf(updates);
        this.state = state;
        this.lastUpdated = lastUpdated;
    }

    @Override
    public long id() {
        return transactionId;
    }

    @Override
    public List<DatabaseUpdate> updates() {
        return updates;
    }

    @Override
    public State state() {
        return state;
    }

    @Override
    public Transaction transition(State newState) {
        return new DefaultTransaction(transactionId, updates, newState, System.currentTimeMillis());
    }

    @Override
    public long lastUpdated() {
        return lastUpdated;
    }
}