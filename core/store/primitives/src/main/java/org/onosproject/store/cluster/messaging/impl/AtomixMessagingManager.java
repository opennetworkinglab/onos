/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.store.cluster.messaging.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import io.atomix.utils.net.Address;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.IpAddress;
import org.onosproject.store.cluster.messaging.Endpoint;
import org.onosproject.store.cluster.messaging.MessagingService;
import org.onosproject.store.impl.AtomixManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Atomix messaging manager.
 */
@Component(immediate = true)
@Service
public class AtomixMessagingManager implements MessagingService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected AtomixManager atomixManager;

    private io.atomix.cluster.messaging.MessagingService messagingService;

    @Activate
    public void activate() {
        messagingService = atomixManager.getAtomix().getMessagingService();
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    private Address toAddress(Endpoint ep) {
        return new Address(ep.host().toString(), ep.port(), ep.host().toInetAddress());
    }

    private Endpoint toEndpoint(Address address) {
        return new Endpoint(IpAddress.valueOf(address.address()), address.port());
    }

    @Override
    public CompletableFuture<Void> sendAsync(Endpoint ep, String type, byte[] payload) {
        return messagingService.sendAsync(toAddress(ep), type, payload);
    }

    @Override
    public CompletableFuture<byte[]> sendAndReceive(Endpoint ep, String type, byte[] payload) {
        return messagingService.sendAndReceive(toAddress(ep), type, payload);
    }

    @Override
    public CompletableFuture<byte[]> sendAndReceive(Endpoint ep, String type, byte[] payload, Executor executor) {
        return messagingService.sendAndReceive(toAddress(ep), type, payload, executor);
    }

    @Override
    public void registerHandler(String type, BiConsumer<Endpoint, byte[]> handler, Executor executor) {
        BiConsumer<Address, byte[]> consumer = (address, payload) -> handler.accept(toEndpoint(address), payload);
        messagingService.registerHandler(type, consumer, executor);
    }

    @Override
    public void registerHandler(String type, BiFunction<Endpoint, byte[], byte[]> handler, Executor executor) {
        BiFunction<Address, byte[], byte[]> function = (address, payload) ->
            handler.apply(toEndpoint(address), payload);
        messagingService.registerHandler(type, function, executor);
    }

    @Override
    public void registerHandler(String type, BiFunction<Endpoint, byte[], CompletableFuture<byte[]>> handler) {
        BiFunction<Address, byte[], CompletableFuture<byte[]>> function = (address, payload) ->
            handler.apply(toEndpoint(address), payload);
        messagingService.registerHandler(type, function);
    }

    @Override
    public void unregisterHandler(String type) {
        messagingService.unregisterHandler(type);
    }
}
