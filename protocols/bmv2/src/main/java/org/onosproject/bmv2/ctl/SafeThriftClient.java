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

/*
 * Most of the code of this class was copied from:
 * http://liveramp.com/engineering/reconnecting-thrift-client/
 */

package org.onosproject.bmv2.ctl;

import com.google.common.collect.ImmutableSet;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Set;

/**
 * Thrift client wrapper that attempts a few reconnects before giving up a method call execution. It al provides
 * synchronization between calls (automatically serialize multiple calls to the same client from different threads).
 */
public final class SafeThriftClient {

    private static final Logger LOG = LoggerFactory.getLogger(SafeThriftClient.class);

    /**
     * List of causes which suggest a restart might fix things (defined as constants in {@link TTransportException}).
     */
    private static final Set<Integer> RESTARTABLE_CAUSES = ImmutableSet.of(TTransportException.NOT_OPEN,
                                                                           TTransportException.END_OF_FILE,
                                                                           TTransportException.TIMED_OUT,
                                                                           TTransportException.UNKNOWN);

    private SafeThriftClient() {
        // ban constructor.
    }

    /**
     * Reflectively wraps an already existing Thrift client.
     *
     * @param baseClient      the client to wrap
     * @param clientInterface the interface that the client implements
     * @param options         options that control behavior of the reconnecting client
     * @param <T>
     * @param <C>
     * @return
     */
    public static <T extends TServiceClient, C> C wrap(T baseClient, Class<C> clientInterface, Options options) {
        Object proxyObject = Proxy.newProxyInstance(clientInterface.getClassLoader(),
                                                    new Class<?>[]{clientInterface},
                                                    new ReconnectingClientProxy<T>(baseClient,
                                                                                   options.getNumRetries(),
                                                                                   options.getTimeBetweenRetries()));

        return (C) proxyObject;
    }

    /**
     * Reflectively wraps an already existing Thrift client.
     *
     * @param baseClient the client to wrap
     * @param options    options that control behavior of the reconnecting client
     * @param <T>
     * @param <C>
     * @return
     */
    public static <T extends TServiceClient, C> C wrap(T baseClient, Options options) {
        Class<?>[] interfaces = baseClient.getClass().getInterfaces();

        for (Class<?> iface : interfaces) {
            if (iface.getSimpleName().equals("Iface")
                    && iface.getEnclosingClass().equals(baseClient.getClass().getEnclosingClass())) {
                return (C) wrap(baseClient, iface, options);
            }
        }

        throw new RuntimeException("Class needs to implement Iface directly. Use wrap(TServiceClient, Class) instead.");
    }

    /**
     * Reflectively wraps an already existing Thrift client.
     *
     * @param baseClient      the client to wrap
     * @param clientInterface the interface that the client implements
     * @param <T>
     * @param <C>
     * @return
     */
    public static <T extends TServiceClient, C> C wrap(T baseClient, Class<C> clientInterface) {
        return wrap(baseClient, clientInterface, Options.defaults());
    }

    /**
     * Reflectively wraps an already existing Thrift client.
     *
     * @param baseClient the client to wrap
     * @param <T>
     * @param <C>
     * @return
     */
    public static <T extends TServiceClient, C> C wrap(T baseClient) {
        return wrap(baseClient, Options.defaults());
    }

    /**
     * Reconnection options for {@link SafeThriftClient}.
     */
    public static class Options {
        private int numRetries;
        private long timeBetweenRetries;

        /**
         * Creates new options with the given parameters.
         *
         * @param numRetries         the maximum number of times to try reconnecting before giving up and throwing an
         *                           exception
         * @param timeBetweenRetries the number of milliseconds to wait in between reconnection attempts.
         */
        public Options(int numRetries, long timeBetweenRetries) {
            this.numRetries = numRetries;
            this.timeBetweenRetries = timeBetweenRetries;
        }

        private static Options defaults() {
            return new Options(5, 10000L);
        }

        private int getNumRetries() {
            return numRetries;
        }

        private long getTimeBetweenRetries() {
            return timeBetweenRetries;
        }
    }

    /**
     * Helper proxy class. Attempts to call method on proxy object wrapped in try/catch. If it fails, it attempts a
     * reconnect and tries the method again.
     *
     * @param <T>
     */
    private static class ReconnectingClientProxy<T extends TServiceClient> implements InvocationHandler {
        private final T baseClient;
        private final int maxRetries;
        private final long timeBetweenRetries;

        public ReconnectingClientProxy(T baseClient, int maxRetries, long timeBetweenRetries) {
            this.baseClient = baseClient;
            this.maxRetries = maxRetries;
            this.timeBetweenRetries = timeBetweenRetries;
        }

        private static void reconnectOrThrowException(TTransport transport, int maxRetries, long timeBetweenRetries)
                throws TTransportException {
            int errors = 0;
            transport.close();

            while (errors < maxRetries) {
                try {
                    LOG.debug("Attempting to reconnect...");
                    transport.open();
                    LOG.debug("Reconnection successful");
                    break;
                } catch (TTransportException e) {
                    LOG.error("Error while reconnecting:", e);
                    errors++;

                    if (errors < maxRetries) {
                        try {
                            LOG.debug("Sleeping for {} milliseconds before retrying", timeBetweenRetries);
                            Thread.sleep(timeBetweenRetries);
                        } catch (InterruptedException e2) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException(e);
                        }
                    }
                }
            }

            if (errors >= maxRetries) {
                throw new TTransportException("Failed to reconnect");
            }
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

            // With Thrift clients must be instantiated for each different transport session, i.e. server instance.
            // Hence, using baseClient as lock, only calls towards the same server will be synchronized.

            synchronized (baseClient) {

                LOG.debug("Invoking client method... > method={}, fromThread={}",
                          method.getName(), Thread.currentThread().getId());

                Object result = null;

                try {
                    result = method.invoke(baseClient, args);

                } catch (InvocationTargetException e) {
                    if (e.getTargetException() instanceof TTransportException) {
                        TTransportException cause = (TTransportException) e.getTargetException();

                        if (RESTARTABLE_CAUSES.contains(cause.getType())) {
                            reconnectOrThrowException(baseClient.getInputProtocol().getTransport(),
                                                      maxRetries,
                                                      timeBetweenRetries);
                            result = method.invoke(baseClient, args);
                        }
                    }

                    if (result == null) {
                        LOG.debug("Exception while invoking client method: {} > method={}, fromThread={}",
                                  e, method.getName(), Thread.currentThread().getId());
                        throw e.getTargetException();
                    }
                }

                LOG.debug("Method invoke complete! > method={}, fromThread={}",
                          method.getName(), Thread.currentThread().getId());

                return result;
            }
        }
    }
}
