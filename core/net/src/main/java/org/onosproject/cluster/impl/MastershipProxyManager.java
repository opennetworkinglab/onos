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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.onlab.util.OrderedExecutor;
import org.onlab.util.Tools;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.mastership.MastershipProxyFactory;
import org.onosproject.mastership.MastershipProxyService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.DeviceId;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.serializers.KryoNamespaces;
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
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkArgument;
import static org.onlab.util.Tools.groupedThreads;

/**
 * Mastership proxy service implementation.
 * <p>
 * This implementation wraps both the proxy service and the generated proxy instance in additional proxies which check
 * mastership and route calls to the appropriate proxy instances.
 */
@Component(immediate = true, service = MastershipProxyService.class)
public class MastershipProxyManager extends AbstractProxyManager implements MastershipProxyService {

    private static final Serializer REQUEST_SERIALIZER =
        Serializer.using(KryoNamespaces.API, MastershipProxyRequest.class);
    private static final String MESSAGE_PREFIX = "mastership-proxy";

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterCommunicationService clusterCommunicator;

    private final ExecutorService proxyServiceExecutor =
        Executors.newFixedThreadPool(8, groupedThreads("onos/proxy", "service-executor", log));

    private final Map<Class, ProxyService> services = Maps.newConcurrentMap();
    private NodeId localNodeId;

    @Activate
    public void activate() {
        this.localNodeId = clusterService.getLocalNode().id();
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        proxyServiceExecutor.shutdownNow();
        log.info("Stopped");
    }

    @Override
    public <T> MastershipProxyFactory<T> getProxyFactory(Class<T> type, Serializer serializer) {
        checkArgument(type.isInterface(), "proxy type must be an interface");
        return new MastershipProxyManagerFactory<>(type, serializer);
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
    private class MastershipProxyManagerFactory<T> implements MastershipProxyFactory<T> {
        private final Class<T> type;
        private final Serializer serializer;
        private final Map<DeviceId, T> proxyInstances = Maps.newConcurrentMap();

        MastershipProxyManagerFactory(Class<T> type, Serializer serializer) {
            this.type = type;
            this.serializer = serializer;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T getProxyFor(DeviceId deviceId) {
            // Avoid unnecessary locking of computeIfAbsent if possible.
            T proxyInstance = proxyInstances.get(deviceId);
            if (proxyInstance != null) {
                return proxyInstance;
            }
            return proxyInstances.computeIfAbsent(deviceId, id -> (T) Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class[]{type},
                new ProxyInvocationHandler(type, MESSAGE_PREFIX,
                    o -> new SyncOperationHandler(o, deviceId, serializer),
                    o -> new AsyncOperationHandler(o, deviceId, serializer))));
        }
    }

    /**
     * Implementation of the operation service which handles synchronous method calls.
     */
    private class SyncOperationService
        extends OperationService
        implements Function<MastershipProxyRequest, Object> {

        private final Serializer serializer;

        SyncOperationService(
            Object instance,
            Method method,
            Operation operation,
            Serializer serializer,
            Executor executor) {
            super(instance, method, operation);
            this.serializer = serializer;
            clusterCommunicator.addSubscriber(
                operation.subject(), REQUEST_SERIALIZER::decode, this, REQUEST_SERIALIZER::encode, executor);
        }

        @Override
        public Object apply(MastershipProxyRequest request) {
            NodeId master = mastershipService.getMasterFor(request.deviceId());
            if (master == null) {
                throw new IllegalStateException("No master found for device " + request.deviceId());
            } else if (!Objects.equals(master, localNodeId)) {
                // If the given node has already been visited by this node, that indicates a cycle in the intra-cluster
                // communication. Reject the request.
                if (request.visited().contains(localNodeId)) {
                    throw new IllegalStateException("Ambiguous master for device " + request.deviceId());
                }

                // If this node is being visited for the first time, update the request with the local node ID
                // to prevent cyclic communication.
                return clusterCommunicator.sendAndReceive(
                    request.visit(),
                    operation.subject(),
                    REQUEST_SERIALIZER::encode,
                    REQUEST_SERIALIZER::decode,
                    master);
            } else {
                // Unwrap the raw arguments and apply the method call to the registered proxy service.
                return invoke(request.unwrap(serializer::decode));
            }
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
        implements Function<MastershipProxyRequest, CompletableFuture<Object>> {

        private final Serializer serializer;

        AsyncOperationService(Object instance, Method method, Operation operation, Serializer serializer) {
            super(instance, method, operation);
            this.serializer = serializer;
            clusterCommunicator.addSubscriber(
                operation.subject(), REQUEST_SERIALIZER::decode, this, REQUEST_SERIALIZER::encode);
        }

        @Override
        public CompletableFuture<Object> apply(MastershipProxyRequest request) {
            NodeId master = mastershipService.getMasterFor(request.deviceId());
            if (master == null) {
                return Tools.exceptionalFuture(
                    new IllegalStateException("No master found for device " + request.deviceId()));
            } else if (!Objects.equals(master, localNodeId)) {
                // If the given node has already been visited by this node, that indicates a cycle in the intra-cluster
                // communication. Reject the request.
                if (request.visited().contains(localNodeId)) {
                    return Tools.exceptionalFuture(
                        new IllegalStateException("Ambiguous master for device " + request.deviceId()));
                }

                // If this node is being visited for the first time, update the request with the local node ID
                // to prevent cyclic communication.
                return clusterCommunicator.sendAndReceive(
                    request.visit(),
                    operation.subject(),
                    REQUEST_SERIALIZER::encode,
                    REQUEST_SERIALIZER::decode,
                    master);
            } else {
                // Unwrap the raw arguments and apply the method call to the registered proxy service.
                return invoke(request.unwrap(serializer::decode));
            }
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
        private final DeviceId deviceId;
        private final Serializer serializer;

        SyncOperationHandler(Operation operation, DeviceId deviceId, Serializer serializer) {
            super(operation);
            this.deviceId = deviceId;
            this.serializer = serializer;
        }

        @Override
        public Object apply(Object[] args) {
            NodeId master = mastershipService.getMasterFor(deviceId);
            if (master == null) {
                throw new IllegalStateException("No master found for device " + deviceId);
            }

            MastershipProxyRequest request = new MastershipProxyRequest(deviceId, serializer.encode(args));
            try {
                return clusterCommunicator.sendAndReceive(
                    request, operation.subject(), REQUEST_SERIALIZER::encode, REQUEST_SERIALIZER::decode, master)
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
        private final DeviceId deviceId;
        private final Serializer serializer;

        AsyncOperationHandler(Operation operation, DeviceId deviceId, Serializer serializer) {
            super(operation);
            this.deviceId = deviceId;
            this.serializer = serializer;
        }

        @Override
        public Object apply(Object[] args) {
            NodeId master = mastershipService.getMasterFor(deviceId);
            if (master == null) {
                return Tools.exceptionalFuture(new IllegalStateException("No master found for device " + deviceId));
            }

            MastershipProxyRequest request = new MastershipProxyRequest(deviceId, serializer.encode(args));
            return clusterCommunicator.sendAndReceive(
                request, operation.subject(), REQUEST_SERIALIZER::encode, REQUEST_SERIALIZER::decode, master);
        }
    }

    /**
     * Internal arguments wrapper that contains the {@link DeviceId} for mastership checks.
     */
    private class MastershipProxyRequest {
        private final DeviceId deviceId;
        private final byte[] args;
        private final Set<NodeId> visited;

        MastershipProxyRequest(DeviceId deviceId, byte[] args) {
            this(deviceId, args, ImmutableSet.of(localNodeId));
        }

        MastershipProxyRequest(DeviceId deviceId, byte[] args, Set<NodeId> visited) {
            this.deviceId = deviceId;
            this.args = args;
            this.visited = visited;
        }

        /**
         * Returns the device identifier.
         *
         * @return the device identifier
         */
        DeviceId deviceId() {
            return deviceId;
        }

        /**
         * Returns the function arguments.
         *
         * @return the function arguments
         */
        byte[] args() {
            return args;
        }

        /**
         * Decodes and returns the function arguments.
         *
         * @param decoder the arguments decoder
         * @return the decoded function arguments
         */
        Object[] unwrap(Function<byte[], Object[]> decoder) {
            return decoder.apply(args());
        }

        /**
         * Returns the set of nodes visited by this request.
         *
         * @return the set of nodes visited by this request
         */
        Set<NodeId> visited() {
            return visited;
        }

        /**
         * Adds the local node to the set of visited nodes for this request, returning an updated request instance.
         *
         * @return a new request with the local node ID added to the visited set
         */
        MastershipProxyRequest visit() {
            return new MastershipProxyRequest(deviceId, args, ImmutableSet.<NodeId>builder()
                .addAll(visited)
                .add(localNodeId)
                .build());
        }
    }
}
