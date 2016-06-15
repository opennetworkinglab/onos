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

package org.onosproject.bmv2.ctl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.apache.thrift.TException;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TMultiplexedProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.bmv2.api.service.Bmv2Controller;
import org.onosproject.bmv2.api.runtime.Bmv2Device;
import org.onosproject.bmv2.api.runtime.Bmv2DeviceAgent;
import org.onosproject.bmv2.api.service.Bmv2DeviceListener;
import org.onosproject.bmv2.api.service.Bmv2PacketListener;
import org.onosproject.bmv2.api.runtime.Bmv2RuntimeException;
import org.onosproject.bmv2.thriftapi.ControlPlaneService;
import org.onosproject.bmv2.thriftapi.SimpleSwitch;
import org.onosproject.bmv2.thriftapi.Standard;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.groupedThreads;

/**
 * Default implementation of a BMv2 controller.
 */
@Component(immediate = true)
@Service
public class Bmv2ControllerImpl implements Bmv2Controller {

    private static final String APP_ID = "org.onosproject.bmv2";

    // Seconds after a client is expired (and connection closed) in the cache.
    private static final int CLIENT_CACHE_TIMEOUT = 60;
    // Number of connection retries after a network error.
    private static final int NUM_CONNECTION_RETRIES = 2;
    // Time between retries in milliseconds.
    private static final int TIME_BETWEEN_RETRIES = 10;

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    // Cache where clients are removed after a predefined timeout.
    private final LoadingCache<DeviceId, Pair<TTransport, Bmv2DeviceThriftClient>> agentCache =
            CacheBuilder.newBuilder()
                    .expireAfterAccess(CLIENT_CACHE_TIMEOUT, TimeUnit.SECONDS)
                    .removalListener(new ClientRemovalListener())
                    .build(new ClientLoader());

    private final InternalTrackingProcessor trackingProcessor = new InternalTrackingProcessor();

    private final ExecutorService executorService = Executors
            .newFixedThreadPool(16, groupedThreads("onos/bmv2", "controller", log));

    private final Set<Bmv2DeviceListener> deviceListeners = new CopyOnWriteArraySet<>();
    private final Set<Bmv2PacketListener> packetListeners = new CopyOnWriteArraySet<>();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    private TThreadPoolServer thriftServer;

    // TODO: make it configurable trough component config
    private int serverPort = DEFAULT_PORT;

    @Activate
    public void activate() {
        coreService.registerApplication(APP_ID);
        startServer(serverPort);
        log.info("Activated");
    }

    @Deactivate
    public void deactivate() {
        stopServer();
        log.info("Deactivated");
    }

    private void startServer(int port) {
        try {
            TServerTransport transport = new TServerSocket(port);
            log.info("Starting server on port {}...", port);
            this.thriftServer = new TThreadPoolServer(new TThreadPoolServer.Args(transport)
                                                              .processor(trackingProcessor)
                                                              .executorService(executorService));
            executorService.execute(thriftServer::serve);
        } catch (TTransportException e) {
            log.error("Unable to start server", e);
        }
    }

    private void stopServer() {
        // Stop the server if running...
        if (thriftServer != null && !thriftServer.isServing()) {
            thriftServer.stop();
        }
        try {
            executorService.shutdown();
            executorService.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            List<Runnable> runningTasks = executorService.shutdownNow();
            log.warn("Unable to stop server threads: {}", runningTasks);
        }
    }

    @Override
    public Bmv2DeviceAgent getAgent(DeviceId deviceId) throws Bmv2RuntimeException {
        try {
            checkNotNull(deviceId, "deviceId cannot be null");
            return agentCache.get(deviceId).getRight();
        } catch (ExecutionException e) {
            throw new Bmv2RuntimeException(e);
        }
    }

    @Override
    public boolean isReacheable(DeviceId deviceId) {
        try {
            return getAgent(deviceId).ping();
        } catch (Bmv2RuntimeException e) {
            return false;
        }
    }

    @Override
    public void addDeviceListener(Bmv2DeviceListener listener) {
        if (!deviceListeners.contains(listener)) {
            deviceListeners.add(listener);
        }
    }

    @Override
    public void removeDeviceListener(Bmv2DeviceListener listener) {
        deviceListeners.remove(listener);
    }

    @Override
    public void addPacketListener(Bmv2PacketListener listener) {
        if (!packetListeners.contains(listener)) {
            packetListeners.add(listener);
        }
    }

    @Override
    public void removePacketListener(Bmv2PacketListener listener) {
        packetListeners.remove(listener);
    }

    /**
     * Client cache removal listener. Close the connection on cache removal.
     */
    private static class ClientRemovalListener implements
            RemovalListener<DeviceId, Pair<TTransport, Bmv2DeviceThriftClient>> {

        @Override
        public void onRemoval(RemovalNotification<DeviceId, Pair<TTransport, Bmv2DeviceThriftClient>> notification) {
            // close the transport connection
            Bmv2DeviceThriftClient client = notification.getValue().getRight();
            TTransport transport = notification.getValue().getLeft();
            // Locking here is ugly, but needed (see SafeThriftClient).
            synchronized (transport) {
                if (transport.isOpen()) {
                    transport.close();
                }
            }
        }
    }

    /**
     * Handles Thrift calls from BMv2 devices using registered listeners.
     */
    private final class InternalServiceHandler implements ControlPlaneService.Iface {

        private final TSocket socket;
        private Bmv2Device remoteDevice;

        private InternalServiceHandler(TSocket socket) {
            this.socket = socket;
        }

        @Override
        public boolean ping() {
            return true;
        }

        @Override
        public void hello(int thriftServerPort, int deviceId, int instanceId, String jsonConfigMd5) {
            // Locally note the remote device for future uses.
            String host = socket.getSocket().getInetAddress().getHostAddress();
            remoteDevice = new Bmv2Device(host, thriftServerPort, deviceId);

            if (deviceListeners.size() == 0) {
                log.debug("Received hello, but there's no listener registered.");
            } else {
                deviceListeners.forEach(
                        l -> executorService.execute(() -> l.handleHello(remoteDevice, instanceId, jsonConfigMd5)));
            }
        }

        @Override
        public void packet_in(int port, ByteBuffer packet) {
            if (remoteDevice == null) {
                log.debug("Received packet-in, but the remote device is still unknown. Need a hello first...");
                return;
            }

            if (packetListeners.size() == 0) {
                log.debug("Received packet-in, but there's no listener registered.");
            } else {
                packetListeners.forEach(
                        l -> executorService.execute(() -> l.handlePacketIn(remoteDevice,
                                                                            port,
                                                                            ImmutableByteSequence.copyFrom(packet))));
            }
        }
    }

    /**
     * Thrift Processor decorator. This class is needed in order to have access to the socket when handling a call.
     * Socket is needed to get the IP address of the client originating the call (see InternalServiceHandler.hello())
     */
    private final class InternalTrackingProcessor implements TProcessor {

        // Map sockets to processors.
        // TODO: implement it as a cache so unused sockets are expired automatically
        private final ConcurrentMap<TSocket, ControlPlaneService.Processor<InternalServiceHandler>> processors =
                Maps.newConcurrentMap();

        @Override
        public boolean process(final TProtocol in, final TProtocol out) throws TException {
            // Get the socket for this request.
            TSocket socket = (TSocket) in.getTransport();
            // Get or create a processor for this socket
            ControlPlaneService.Processor<InternalServiceHandler> processor = processors.computeIfAbsent(socket, s -> {
                InternalServiceHandler handler = new InternalServiceHandler(s);
                return new ControlPlaneService.Processor<>(handler);
            });
            // Delegate to the processor we are decorating.
            return processor.process(in, out);
        }
    }

    /**
     * Transport/client cache loader.
     */
    private class ClientLoader extends CacheLoader<DeviceId, Pair<TTransport, Bmv2DeviceThriftClient>> {

        private final SafeThriftClient.Options options = new SafeThriftClient.Options(NUM_CONNECTION_RETRIES,
                                                                                      TIME_BETWEEN_RETRIES);

        @Override
        public Pair<TTransport, Bmv2DeviceThriftClient> load(DeviceId deviceId)
                throws TTransportException {
            log.debug("Instantiating new client... > deviceId={}", deviceId);
            // Make the expensive call
            Bmv2Device device = Bmv2Device.of(deviceId);
            TTransport transport = new TSocket(device.thriftServerHost(), device.thriftServerPort());
            TProtocol protocol = new TBinaryProtocol(transport);
            // Our BMv2 device implements multiple Thrift services, create a client for each one on the same transport.
            Standard.Client standardClient = new Standard.Client(
                    new TMultiplexedProtocol(protocol, "standard"));
            SimpleSwitch.Client simpleSwitch = new SimpleSwitch.Client(
                    new TMultiplexedProtocol(protocol, "simple_switch"));
            // Wrap clients so to automatically have synchronization and resiliency to connectivity errors
            Standard.Iface safeStandardClient = SafeThriftClient.wrap(standardClient,
                                                                      Standard.Iface.class,
                                                                      options);
            SimpleSwitch.Iface safeSimpleSwitchClient = SafeThriftClient.wrap(simpleSwitch,
                                                                              SimpleSwitch.Iface.class,
                                                                              options);
            Bmv2DeviceThriftClient client = new Bmv2DeviceThriftClient(deviceId,
                                                                       transport,
                                                                       safeStandardClient,
                                                                       safeSimpleSwitchClient);
            return Pair.of(transport, client);
        }
    }
}
