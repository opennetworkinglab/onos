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
package org.onosproject.cluster;

import org.onosproject.store.service.Serializer;

/**
 * Manages remote proxy services and instances.
 * <p>
 * This service can be used to make arbitrary method calls on remote objects in ONOS. The objects on which the methods
 * are called are referred to as <em>services</em> and are made available to the proxy service by
 * {@link #registerProxyService(Class, Object, Serializer) registering} a service instance. Services must implement
 * an <em>interface</em> which can be used to construct a Java proxy. Proxy interfaces may define either synchronous
 * or asynchronous methods. Synchronous proxy methods will be blocked, and asynchronous proxy methods (which must
 * return {@link java.util.concurrent.CompletableFuture}) will be proxied using asynchronous intra-cluster
 * communication. To make a remote call on a proxy service, get an instance of the {@link ProxyFactory} for the service
 * type using {@link #getProxyFactory(Class, Serializer)}. The proxy factory is responsible for constructing proxy
 * instances for each node in the cluster. Once a proxy instance is constructed, calls on the proxy instance will be
 * transparently serialized and sent to the associated peer and be executed on the proxy service registered by that
 * peer.
 */
public interface ProxyService {

    /**
     * Returns a new proxy factory for the given type.
     * <p>
     * The proxy {@code type} passed to this method must be an interface. The proxy factory can be used to construct
     * proxy instances for different nodes in the cluster.
     *
     * @param type       the proxy type
     * @param serializer the proxy serializer
     * @param <T>        the proxy type
     * @return the proxy factory
     * @throws IllegalArgumentException if the {@code type} is not an interface
     */
    <T> ProxyFactory<T> getProxyFactory(Class<T> type, Serializer serializer);

    /**
     * Registers a proxy service.
     * <p>
     * The proxy {@code type} passed to this method must be an interface. The {@code proxy} should be an implementation
     * of that interface on which methods will be called when proxy calls from other nodes are received.
     *
     * @param type       the proxy type
     * @param proxy      the proxy service
     * @param serializer the proxy serializer
     * @param <T>        the proxy type
     * @throws IllegalArgumentException if the {@code type} is not an interface
     */
    <T> void registerProxyService(Class<? super T> type, T proxy, Serializer serializer);

    /**
     * Unregisters the proxy service of the given type.
     * <p>
     * Once the proxy service has been unregistered, calls to the proxy instance on this node will fail.
     *
     * @param type the proxy service type to unregister
     */
    void unregisterProxyService(Class<?> type);

}
