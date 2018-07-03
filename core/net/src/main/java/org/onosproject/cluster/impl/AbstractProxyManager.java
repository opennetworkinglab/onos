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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.Maps;
import org.onosproject.store.cluster.messaging.MessageSubject;

/**
 * Implementation of the proxy service.
 */
public abstract class AbstractProxyManager {

    /**
     * Wrapper for a proxy service which handles registration of proxy methods as {@code ClusterCommunicationService}
     * subscribers.
     */
    class ProxyService {
        private final Map<Method, OperationService> operations = Maps.newConcurrentMap();

        ProxyService(
            Object instance,
            Class<?> type,
            String prefix,
            OperationServiceFactory syncServiceFactory,
            OperationServiceFactory asyncServiceFactory) {
            operations.putAll(getMethodMap(type, prefix).entrySet().stream()
                .map(entry -> {
                    if (entry.getValue().type() == Operation.Type.SYNC) {
                        return Maps.immutableEntry(
                            entry.getKey(),
                            syncServiceFactory.create(instance, entry.getKey(), entry.getValue()));
                    } else {
                        return Maps.immutableEntry(
                            entry.getKey(),
                            asyncServiceFactory.create(instance, entry.getKey(), entry.getValue()));
                    }
                }).collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue())));
        }

        /**
         * Closes the proxy service.
         */
        void close() {
            operations.values().forEach(operation -> operation.close());
        }
    }

    /**
     * Operation service factory.
     */
    @FunctionalInterface
    interface OperationServiceFactory {
        OperationService create(Object instance, Method method, Operation operation);
    }

    /**
     * Wrapper for a single proxy service operation which handles registration of subscribers and invocation
     * of service instance methods.
     */
    abstract class OperationService {
        protected final Object instance;
        protected final Method method;
        protected final Operation operation;

        OperationService(Object instance, Method method, Operation operation) {
            this.instance = instance;
            this.method = method;
            this.operation = operation;
        }

        /**
         * Invokes the method with the given arguments.
         *
         * @param args the arguments with which to invoke the operation
         * @param <T>  the operation return type
         * @return the operation return value
         */
        @SuppressWarnings("unchecked")
        <T> T invoke(Object[] args) {
            try {
                return (T) method.invoke(instance, args);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new IllegalStateException(e);
            }
        }

        /**
         * Closes the operation service.
         */
        abstract void close();
    }

    /**
     * Proxy invocation handler which routes proxy method calls to the correct node and subscriber via
     * {@code ClusterCommunicationService}.
     */
    class ProxyInvocationHandler implements InvocationHandler {
        private final Map<Method, OperationHandler> handlers = Maps.newConcurrentMap();

        ProxyInvocationHandler(
            Class<?> type,
            String prefix,
            OperationHandlerFactory syncHandlerFactory,
            OperationHandlerFactory asyncHandlerFactory) {
            handlers.putAll(getMethodMap(type, prefix).entrySet().stream()
                .map(entry -> {
                    if (entry.getValue().type() == Operation.Type.SYNC) {
                        return Maps.immutableEntry(entry.getKey(), syncHandlerFactory.create(entry.getValue()));
                    } else {
                        return Maps.immutableEntry(entry.getKey(), asyncHandlerFactory.create(entry.getValue()));
                    }
                }).collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue())));
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            OperationHandler handler = handlers.get(method);
            if (handler == null) {
                throw new IllegalStateException("Unknown proxy operation " + method.getName());
            }
            return handler.apply(args);
        }
    }

    /**
     * Operation handler factory.
     */
    @FunctionalInterface
    interface OperationHandlerFactory {
        OperationHandler create(Operation operation);
    }

    /**
     * Invocation handler for an individual proxy operation.
     */
    abstract class OperationHandler implements Function<Object[], Object> {
        protected final Operation operation;

        OperationHandler(Operation operation) {
            this.operation = operation;
        }
    }

    /**
     * Recursively finds operations defined by the given type and its implemented interfaces.
     *
     * @param type   the type for which to find operations
     * @param prefix the prefix with which to generate message subjects
     * @return the operations defined by the given type and its parent interfaces
     */
    Map<Method, Operation> getMethodMap(Class<?> type, String prefix) {
        String service = type.getCanonicalName().replace(".", "-");
        Map<Method, Operation> methods = new HashMap<>();
        for (Method method : type.getDeclaredMethods()) {
            String name = method.getName();
            if (methods.values().stream().anyMatch(op -> op.name.equals(name))) {
                throw new IllegalArgumentException("Method " + name + " is ambiguous");
            }

            Class<?> returnType = method.getReturnType();
            if (CompletableFuture.class.isAssignableFrom(returnType)) {
                methods.put(method, new Operation(Operation.Type.ASYNC, prefix, service, name, method));
            } else {
                methods.put(method, new Operation(Operation.Type.SYNC, prefix, service, name, method));
            }
        }
        for (Class<?> iface : type.getInterfaces()) {
            methods.putAll(getMethodMap(iface, prefix));
        }
        return methods;
    }

    /**
     * Simple data class for proxy operation metadata.
     */
    static class Operation {

        /**
         * Operation type.
         */
        enum Type {
            SYNC,
            ASYNC,
        }

        private final Type type;
        private final String service;
        private final String name;
        private final Method method;
        private final MessageSubject subject;

        Operation(Type type, String prefix, String service, String name, Method method) {
            this.type = type;
            this.service = service;
            this.name = name;
            this.method = method;
            this.subject = new MessageSubject(String.format("%s-%s-%s", prefix, service, name));
        }

        /**
         * Returns the operation type.
         *
         * @return the operation type
         */
        Type type() {
            return type;
        }

        /**
         * Returns the service name of the service to which this operation belongs.
         *
         * @return the service name of the service to which this operation belongs
         */
        String service() {
            return service;
        }

        /**
         * Returns the operation name.
         *
         * @return the operation name
         */
        String name() {
            return name;
        }

        /**
         * Returns the operation method.
         *
         * @return the operation method
         */
        Method method() {
            return method;
        }

        /**
         * Returns the operation message subject.
         *
         * @return the operation message subject
         */
        MessageSubject subject() {
            return subject;
        }
    }
}
