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
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.bmv2.api.runtime.Bmv2Device;
import org.onosproject.bmv2.api.runtime.Bmv2DeviceAgent;
import org.onosproject.bmv2.api.runtime.Bmv2RuntimeException;
import org.onosproject.bmv2.api.service.Bmv2Controller;
import org.onosproject.bmv2.api.service.Bmv2DeviceListener;
import org.onosproject.bmv2.api.service.Bmv2PacketListener;
import org.onosproject.bmv2.thriftapi.BmConfig;
import org.onosproject.bmv2.thriftapi.ControlPlaneService;
import org.onosproject.bmv2.thriftapi.SimpleSwitch;
import org.onosproject.bmv2.thriftapi.Standard;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
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
import static org.onosproject.bmv2.thriftapi.ControlPlaneService.Processor;

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

    private final TProcessor trackingProcessor = new TrackingProcessor();

    private final ExecutorService executorService = Executors
            .newFixedThreadPool(32, groupedThreads("onos/bmv2", "controller", log));

    private final Set<Bmv2DeviceListener> deviceListeners = new CopyOnWriteArraySet<>();
    private final Set<Bmv2PacketListener> packetListeners = new CopyOnWriteArraySet<>();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    private Bmv2ControlPlaneThriftServer server;

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
            log.info("Starting server on port {}...", port);
            this.server = new Bmv2ControlPlaneThriftServer(port, trackingProcessor, executorService);
            executorService.execute(server::serve);
        } catch (TTransportException e) {
            log.error("Unable to start server", e);
        }
    }

    private void stopServer() {
        // Stop the server if running...
        if (server != null && !server.isServing()) {
            server.setShouldStop(true);
            server.stop();
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
            Bmv2DeviceThriftClient client = (Bmv2DeviceThriftClient) getAgent(deviceId);
            BmConfig config = client.standardClient.bm_mgmt_get_info();
            // The BMv2 instance running at this thrift IP and port might have a different BMv2 internal ID.
            return config.getDevice_id() == Integer.valueOf(deviceId.uri().getFragment());
        } catch (Bmv2RuntimeException | TException e) {
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
     * Handles requests from BMv2 devices using the registered listeners.
     */
    private final class ServiceHandler implements ControlPlaneService.Iface {

        private final InetAddress clientAddress;
        private Bmv2Device remoteDevice;

        ServiceHandler(InetAddress clientAddress) {
            this.clientAddress = clientAddress;
        }

        @Override
        public boolean ping() {
            return true;
        }

        @Override
        public void hello(int thriftServerPort, int deviceId, int instanceId, String jsonConfigMd5) {
            // Store a reference to the remote device for future uses.
            String host = clientAddress.getHostAddress();
            remoteDevice = new Bmv2Device(host, thriftServerPort, deviceId);

            if (deviceListeners.size() == 0) {
                log.debug("Received hello, but there's no listener registered.");
            } else {
                deviceListeners.forEach(l -> l.handleHello(remoteDevice, instanceId, jsonConfigMd5));
            }
        }

        @Override
        public void packet_in(int port, ByteBuffer data, int dataLength) {
            if (remoteDevice == null) {
                log.debug("Received packet-in, but the remote device is still unknown. Need a hello first...");
                return;
            }

            if (packetListeners.size() == 0) {
                log.debug("Received packet-in, but there's no listener registered.");
            } else {
                byte[] bytes = new byte[dataLength];
                data.get(bytes);
                ImmutableByteSequence pkt = ImmutableByteSequence.copyFrom(bytes);
                packetListeners.forEach(l -> l.handlePacketIn(remoteDevice, port, pkt));
            }
        }
    }

    /**
     * Decorator of a Thrift processor needed in order to keep track of the client's IP address that originated the
     * request.
     */
    private final class TrackingProcessor implements TProcessor {

        // Map transports to processors.
        private final ConcurrentMap<TTransport, Processor<ServiceHandler>> processors = Maps.newConcurrentMap();

        @Override
        public boolean process(final TProtocol in, final TProtocol out) throws TException {
            // Get the client address for this request.
            InetAddress clientAddress = server.getClientAddress((TFramedTransport) in.getTransport());
            if (clientAddress != null) {
                // Get or create a processor for this input transport, i.e. the client on the other side.
                Processor<ServiceHandler> processor = processors.computeIfAbsent(
                        in.getTransport(), t -> new Processor<>(new ServiceHandler(clientAddress)));
                // Delegate to the processor we are decorating.
                return processor.process(in, out);
            } else {
                log.warn("Unable to retrieve client IP address of incoming request");
                return false;
            }
        }
    }

    /**
     * Cache loader of BMv2 Thrift clients.
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
