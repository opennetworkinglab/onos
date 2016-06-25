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

import io.atomix.catalyst.concurrent.Listener;
import io.atomix.catalyst.concurrent.ThreadContext;
import io.atomix.catalyst.serializer.Serializer;
import io.atomix.catalyst.transport.Address;
import io.atomix.catalyst.transport.Transport;
import io.atomix.copycat.Command;
import io.atomix.copycat.Query;
import io.atomix.copycat.client.CopycatClient;
import io.atomix.copycat.session.Session;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * {@code CopycatClient} that merely delegates control to
 * another CopycatClient.
 */
public class DelegatingCopycatClient implements CopycatClient {

    protected final CopycatClient client;

    DelegatingCopycatClient(CopycatClient client) {
        this.client = client;
    }

    @Override
    public State state() {
        return client.state();
    }

    @Override
    public Listener<State> onStateChange(Consumer<State> callback) {
        return client.onStateChange(callback);
    }

    @Override
    public ThreadContext context() {
        return client.context();
    }

    @Override
    public Transport transport() {
        return client.transport();
    }

    @Override
    public Serializer serializer() {
        return client.serializer();
    }

    @Override
    public Session session() {
        return client.session();
    }

    @Override
    public <T> CompletableFuture<T> submit(Command<T> command) {
        return client.submit(command);
    }

    @Override
    public <T> CompletableFuture<T> submit(Query<T> query) {
        return client.submit(query);
    }

    @Override
    public Listener<Void> onEvent(String event, Runnable callback) {
        return client.onEvent(event, callback);
    }

    @Override
    public <T> Listener<T> onEvent(String event, Consumer<T> callback) {
        return client.onEvent(event, callback);
    }

    @Override
    public CompletableFuture<CopycatClient> connect(Collection<Address> members) {
        return client.connect(members);
    }

    @Override
    public CompletableFuture<CopycatClient> recover() {
        return client.recover();
    }

    @Override
    public CompletableFuture<Void> close() {
        return client.close();
    }
}