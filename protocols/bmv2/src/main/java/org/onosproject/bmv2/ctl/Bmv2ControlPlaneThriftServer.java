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

import com.google.common.collect.Maps;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.apache.thrift.TException;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransportException;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.bmv2.api.runtime.Bmv2ControlPlaneServer;
import org.onosproject.bmv2.api.runtime.Bmv2Device;
import org.onosproject.core.CoreService;
import org.p4.bmv2.thrift.ControlPlaneService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.onlab.util.Tools.groupedThreads;
import static org.p4.bmv2.thrift.ControlPlaneService.Processor;

@Component(immediate = true)
@Service
public class Bmv2ControlPlaneThriftServer implements Bmv2ControlPlaneServer {

    private static final String APP_ID = "org.onosproject.bmv2";
    private static final Logger LOG = LoggerFactory.getLogger(Bmv2ControlPlaneThriftServer.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    private final InternalTrackingProcessor trackingProcessor = new InternalTrackingProcessor();
    private final ExecutorService executorService = Executors
            .newFixedThreadPool(16, groupedThreads("onos/bmv2", "control-plane-server", LOG));

    private final Set<HelloListener> helloListeners = new CopyOnWriteArraySet<>();
    private final Set<PacketListener> packetListeners = new CopyOnWriteArraySet<>();

    private TThreadPoolServer thriftServer;
    private int serverPort = DEFAULT_PORT;

    @Activate
    public void activate() {
        coreService.registerApplication(APP_ID);
        try {
            TServerTransport transport = new TServerSocket(serverPort);
            LOG.info("Starting server on port {}...", serverPort);
            this.thriftServer = new TThreadPoolServer(new TThreadPoolServer.Args(transport)
                                                              .processor(trackingProcessor)
                                                              .executorService(executorService));
            executorService.execute(thriftServer::serve);
        } catch (TTransportException e) {
            LOG.error("Unable to start server", e);
        }
        LOG.info("Activated");
    }

    @Deactivate
    public void deactivate() {
        // Stop the server if running...
        if (thriftServer != null && !thriftServer.isServing()) {
            thriftServer.stop();
        }
        try {
            executorService.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOG.error("Server threads did not terminate");
        }
        executorService.shutdownNow();
        LOG.info("Deactivated");
    }

    @Override
    public void addHelloListener(HelloListener listener) {
        if (!helloListeners.contains(listener)) {
            helloListeners.add(listener);
        }
    }

    @Override
    public void removeHelloListener(HelloListener listener) {
        helloListeners.remove(listener);
    }

    @Override
    public void addPacketListener(PacketListener listener) {
        if (!packetListeners.contains(listener)) {
            packetListeners.add(listener);
        }
    }

    @Override
    public void removePacketListener(PacketListener listener) {
        packetListeners.remove(listener);
    }

    /**
     * Handles service calls using registered listeners.
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
        public void hello(int thriftServerPort, int deviceId) {
            // Locally note the remote device for future uses.
            String host = socket.getSocket().getInetAddress().getHostAddress();
            remoteDevice = new Bmv2Device(host, thriftServerPort, deviceId);

            if (helloListeners.size() == 0) {
                LOG.debug("Received hello, but there's no listener registered.");
            } else {
                helloListeners.forEach(listener -> listener.handleHello(remoteDevice));
            }
        }

        @Override
        public void packetIn(int port, long reason, int tableId, int contextId, ByteBuffer packet) {
            if (remoteDevice == null) {
                LOG.debug("Received packet-in, but the remote device is still unknown. Need a hello first...");
                return;
            }

            if (packetListeners.size() == 0) {
                LOG.debug("Received packet-in, but there's no listener registered.");
            } else {
                packetListeners.forEach(listener -> listener.handlePacketIn(remoteDevice,
                                                                            port,
                                                                            reason,
                                                                            tableId,
                                                                            contextId,
                                                                            ImmutableByteSequence.copyFrom(packet)));
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
        private final ConcurrentMap<TSocket, Processor<InternalServiceHandler>> processors = Maps.newConcurrentMap();

        @Override
        public boolean process(final TProtocol in, final TProtocol out) throws TException {
            // Get the socket for this request.
            TSocket socket = (TSocket) in.getTransport();
            // Get or create a processor for this socket
            Processor<InternalServiceHandler> processor = processors.computeIfAbsent(socket, s -> {
                InternalServiceHandler handler = new InternalServiceHandler(s);
                return new Processor<>(handler);
            });
            // Delegate to the processor we are decorating.
            return processor.process(in, out);
        }
    }
}
