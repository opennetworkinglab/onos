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
package org.onosproject.cluster.impl;

import com.google.common.collect.Maps;
import org.onlab.util.OrderedExecutor;
import org.onosproject.cluster.NodeId;
import org.onosproject.cluster.ProxyFactory;
import org.onosproject.cluster.ProxyService;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.service.Serializer;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkArgument;
import static org.onlab.util.Tools.groupedThreads;

/**
 * Implementation of the proxy service.
 */
@Component(immediate = true, service = ProxyService.class)
public class ProxyManager extends AbstractProxyManager implements ProxyService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String MESSAGE_PREFIX = "proxy";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterCommunicationService clusterCommunicator;

    private final ExecutorService proxyServiceExecutor =
        Executors.newFixedThreadPool(
            Math.min(Math.max(Runtime.getRuntime().availableProcessors(), 4), 16),
            groupedThreads("onos/proxy", "service-executor", log));

    private final Map<Class, ProxyService> services = Maps.newConcurrentMap();

    @Activate
    public void activate() {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        proxyServiceExecutor.shutdownNow();
        log.info("Stopped");
    }

    @Override
    public <T> ProxyFactory<T> getProxyFactory(Class<T> type, Serializer serializer) {
        checkArgument(type.isInterface(), "proxy type must be an interface");
        return new ProxyManagerFactory<>(type, serializer);
    }

    @Override
    public <T> void registerProxyService(Class<? super T> type, T instance, Serializer serializer) {
        checkArgument(type.isInterface(), "proxy type must be an interface");
        Executor executor = new OrderedExecutor(proxyServiceExecutor);
        services.computeIfAbsent(type, t -> new ProxyService(instance, t, MESSAGE_PREFIX,
            (i, m, o) -> new SyncOperationService(i, m, o, serializer, executor),
            (i, m, o) -> new AsyncOperationService(i, m, o, serializer)));
    }

    @Override
    public void unregisterProxyService(Class<?> type) {
        ProxyService service = services.remove(type);
        if (service != null) {
            service.close();
        }
    }

    /**
     * Internal proxy factory.
     */
    private class ProxyManagerFactory<T> implements ProxyFactory<T> {
        private final Class<T> type;
        private final Serializer serializer;
        private final Map<NodeId, T> proxyInstances = Maps.newConcurrentMap();

        ProxyManagerFactory(Class<T> type, Serializer serializer) {
            this.type = type;
            this.serializer = serializer;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T getProxyFor(NodeId nodeId) {
            // Avoid unnecessary locking of computeIfAbsent if possible.
            T proxyInstance = proxyInstances.get(nodeId);
            if (proxyInstance != null) {
                return proxyInstance;
            }
            return proxyInstances.computeIfAbsent(nodeId, id -> (T) Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class[]{type},
                new ProxyInvocationHandler(type, MESSAGE_PREFIX,
                    o -> new SyncOperationHandler(o, nodeId, serializer),
                    o -> new AsyncOperationHandler(o, nodeId, serializer))));
        }
    }

    /**
     * Implementation of the operation service which handles synchronous method calls.
     */
    private class SyncOperationService
        extends OperationService
        implements Function<Object[], Object> {
        SyncOperationService(
            Object instance,
            Method method,
            Operation operation,
            Serializer serializer,
            Executor executor) {
            super(instance, method, operation);
            clusterCommunicator.addSubscriber(
                operation.subject(), serializer::decode, this, serializer::encode, executor);
        }

        @Override
        public Object apply(Object[] args) {
            return invoke(args);
        }

        @Override
        void close() {
            clusterCommunicator.removeSubscriber(operation.subject());
        }
    }

    /**
     * Implementation of the operation service which handles asynchronous method calls.
     */
    private class AsyncOperationService
        extends OperationService
        implements Function<Object[], CompletableFuture<Object>> {
        AsyncOperationService(Object instance, Method method, Operation operation, Serializer serializer) {
            super(instance, method, operation);
            clusterCommunicator.addSubscriber(
                operation.subject(), serializer::decode, this, serializer::encode);
        }

        @Override
        public CompletableFuture<Object> apply(Object[] args) {
            return invoke(args);
        }

        @Override
        void close() {
            clusterCommunicator.removeSubscriber(operation.subject());
        }
    }

    /**
     * Handler for synchronous proxy operations which blocks on {@code ClusterCommunicationService} requests.
     */
    private class SyncOperationHandler extends OperationHandler {
        private final NodeId nodeId;
        private final Serializer serializer;

        SyncOperationHandler(Operation operation, NodeId nodeId, Serializer serializer) {
            super(operation);
            this.nodeId = nodeId;
            this.serializer = serializer;
        }

        @Override
        public Object apply(Object[] args) {
            try {
                return clusterCommunicator.sendAndReceive(
                    args, operation.subject(), serializer::encode, serializer::decode, nodeId)
                    .join();
            } catch (CompletionException e) {
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                } else {
                    throw new IllegalStateException(e.getCause());
                }
            }
        }
    }

    /**
     * Handler for asynchronous proxy operations which uses async {@code ClusterCommunicationService} requests.
     */
    private class AsyncOperationHandler extends OperationHandler {
        private final NodeId nodeId;
        private final Serializer serializer;

        AsyncOperationHandler(Operation operation, NodeId nodeId, Serializer serializer) {
            super(operation);
            this.nodeId = nodeId;
            this.serializer = serializer;
        }

        @Override
        public Object apply(Object[] args) {
            return clusterCommunicator.sendAndReceive(
                args, operation.subject(), serializer::encode, serializer::decode, nodeId);
        }
    }
}
