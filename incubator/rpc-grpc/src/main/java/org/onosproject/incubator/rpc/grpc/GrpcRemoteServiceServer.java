/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.incubator.rpc.grpc;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;
import static org.onosproject.incubator.rpc.grpc.GrpcDeviceUtils.translate;
import static org.onosproject.net.DeviceId.deviceId;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.grpc.Device.DeviceConnected;
import org.onosproject.grpc.Device.DeviceDisconnected;
import org.onosproject.grpc.Device.DeviceProviderMsg;
import org.onosproject.grpc.Device.DeviceProviderServiceMsg;
import org.onosproject.grpc.Device.IsReachableResponse;
import org.onosproject.grpc.Device.PortStatusChanged;
import org.onosproject.grpc.Device.ReceivedRoleReply;
import org.onosproject.grpc.Device.RegisterProvider;
import org.onosproject.grpc.Device.UpdatePortStatistics;
import org.onosproject.grpc.Device.UpdatePorts;
import org.onosproject.grpc.DeviceProviderRegistryRpcGrpc;
import org.onosproject.grpc.DeviceProviderRegistryRpcGrpc.DeviceProviderRegistryRpc;
import org.onosproject.grpc.LinkProviderServiceRpcGrpc;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.link.LinkProvider;
import org.onosproject.net.link.LinkProviderRegistry;
import org.onosproject.net.link.LinkProviderService;
import org.onosproject.net.provider.ProviderId;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;

// gRPC Server on Metro-side
// Translates request received on RPC channel, and calls corresponding Service on
// Metro-ONOS cluster.

// Currently supports DeviceProviderRegistry, LinkProviderService
/**
 * Server side implementation of gRPC based RemoteService.
 */
@Component(immediate = true)
public class GrpcRemoteServiceServer {

    static final String RPC_PROVIDER_NAME = "org.onosproject.rpc.provider.grpc";

    // TODO pick a number
    public static final int DEFAULT_LISTEN_PORT = 11984;

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceProviderRegistry deviceProviderRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkProviderRegistry linkProviderRegistry;


    @Property(name = "listenPort", intValue = DEFAULT_LISTEN_PORT,
            label = "Port to listen on")
    protected int listenPort = DEFAULT_LISTEN_PORT;

    private Server server;
    private final Set<DeviceProviderServerProxy> registeredProviders = Sets.newConcurrentHashSet();

    // scheme -> ...
    // updates must be guarded by synchronizing `this`
    private final Map<String, LinkProviderService> linkProviderServices = Maps.newConcurrentMap();
    private final Map<String, LinkProvider> linkProviders = Maps.newConcurrentMap();

    @Activate
    protected void activate(ComponentContext context) throws IOException {
        modified(context);

        log.debug("Server starting on {}", listenPort);
        try {
            server  = NettyServerBuilder.forPort(listenPort)
                    .addService(DeviceProviderRegistryRpcGrpc.bindService(new DeviceProviderRegistryServerProxy()))
                    .addService(LinkProviderServiceRpcGrpc.bindService(new LinkProviderServiceServerProxy(this)))
                    .build().start();
        } catch (IOException e) {
            log.error("Failed to start gRPC server", e);
            throw e;
        }

        log.info("Started on {}", listenPort);
    }

    @Deactivate
    protected void deactivate() {

        registeredProviders.stream()
            .forEach(deviceProviderRegistry::unregister);

        server.shutdown();
        // Should we wait for shutdown?

        unregisterLinkProviders();

        log.info("Stopped");
    }

    @Modified
    public void modified(ComponentContext context) {
        // TODO support dynamic reconfiguration and restarting server?
    }

    /**
     * Registers {@link StubLinkProvider} for given ProviderId scheme.
     *
     * DO NOT DIRECTLY CALL THIS METHOD.
     * Only expected to be called from {@link #getLinkProviderServiceFor(String)}.
     *
     * @param scheme ProviderId scheme.
     * @return {@link LinkProviderService} registered.
     */
    private synchronized LinkProviderService registerStubLinkProvider(String scheme) {
        StubLinkProvider provider = new StubLinkProvider(scheme);
        linkProviders.put(scheme, provider);
        return linkProviderRegistry.register(provider);
    }

    /**
     * Unregisters all registered LinkProviders.
     */
    private synchronized void unregisterLinkProviders() {
        linkProviders.values().forEach(linkProviderRegistry::unregister);
        linkProviders.clear();
        linkProviderServices.clear();
    }

    /**
     * Gets or creates {@link LinkProviderService} registered for given ProviderId scheme.
     *
     * @param scheme ProviderId scheme.
     * @return {@link LinkProviderService}
     */
    protected LinkProviderService getLinkProviderServiceFor(String scheme) {
        return linkProviderServices.computeIfAbsent(scheme, this::registerStubLinkProvider);
    }

    // RPC Server-side code
    // RPC session Factory
    /**
     * Relays DeviceProviderRegistry calls from RPC client.
     */
    class DeviceProviderRegistryServerProxy implements DeviceProviderRegistryRpc {

        @Override
        public StreamObserver<DeviceProviderServiceMsg> register(StreamObserver<DeviceProviderMsg> toDeviceProvider) {
            log.trace("DeviceProviderRegistryServerProxy#register called!");

            DeviceProviderServerProxy provider = new DeviceProviderServerProxy(toDeviceProvider);

            return new DeviceProviderServiceServerProxy(provider, toDeviceProvider);
        }
    }

    // Lower -> Upper Controller message
    // RPC Server-side code
    // RPC session handler
    private final class DeviceProviderServiceServerProxy
            implements StreamObserver<DeviceProviderServiceMsg> {

        // intentionally shadowing
        private final Logger log = LoggerFactory.getLogger(getClass());

        private final DeviceProviderServerProxy pairedProvider;
        private final StreamObserver<DeviceProviderMsg> toDeviceProvider;

        private final Cache<Integer, CompletableFuture<Boolean>> outstandingIsReachable;

        // wrapped providerService
        private DeviceProviderService deviceProviderService;


        DeviceProviderServiceServerProxy(DeviceProviderServerProxy provider,
                                         StreamObserver<DeviceProviderMsg> toDeviceProvider) {
            this.pairedProvider = provider;
            this.toDeviceProvider = toDeviceProvider;
            outstandingIsReachable = CacheBuilder.newBuilder()
                        .expireAfterWrite(1, TimeUnit.MINUTES)
                        .build();

            // pair RPC session in other direction
            provider.pair(this);
        }

        @Override
        public void onNext(DeviceProviderServiceMsg msg) {
            try {
                log.trace("DeviceProviderServiceServerProxy received: {}", msg);
                onMethod(msg);
            } catch (Exception e) {
                log.error("Exception thrown handling {}", msg, e);
                onError(e);
                throw e;
            }
        }

        /**
         * Translates received RPC message to {@link DeviceProviderService} method calls.
         * @param msg DeviceProviderService message
         */
        private void onMethod(DeviceProviderServiceMsg msg) {
            switch (msg.getMethodCase()) {
            case REGISTER_PROVIDER:
                RegisterProvider registerProvider = msg.getRegisterProvider();
                // TODO Do we care about provider name?
                pairedProvider.setProviderId(new ProviderId(registerProvider.getProviderScheme(), RPC_PROVIDER_NAME));
                registeredProviders.add(pairedProvider);
                deviceProviderService = deviceProviderRegistry.register(pairedProvider);
                break;

            case DEVICE_CONNECTED:
                DeviceConnected deviceConnected = msg.getDeviceConnected();
                deviceProviderService.deviceConnected(deviceId(deviceConnected.getDeviceId()),
                                                      translate(deviceConnected.getDeviceDescription()));
                break;
            case DEVICE_DISCONNECTED:
                DeviceDisconnected deviceDisconnected = msg.getDeviceDisconnected();
                deviceProviderService.deviceDisconnected(deviceId(deviceDisconnected.getDeviceId()));
                break;
            case UPDATE_PORTS:
                UpdatePorts updatePorts = msg.getUpdatePorts();
                deviceProviderService.updatePorts(deviceId(updatePorts.getDeviceId()),
                                                  updatePorts.getPortDescriptionsList()
                                                      .stream()
                                                          .map(GrpcDeviceUtils::translate)
                                                          .collect(toList()));
                break;
            case PORT_STATUS_CHANGED:
                PortStatusChanged portStatusChanged = msg.getPortStatusChanged();
                deviceProviderService.portStatusChanged(deviceId(portStatusChanged.getDeviceId()),
                                                        translate(portStatusChanged.getPortDescription()));
                break;
            case RECEIVED_ROLE_REPLY:
                ReceivedRoleReply receivedRoleReply = msg.getReceivedRoleReply();
                deviceProviderService.receivedRoleReply(deviceId(receivedRoleReply.getDeviceId()),
                                                        translate(receivedRoleReply.getRequested()),
                                                        translate(receivedRoleReply.getResponse()));
                break;
            case UPDATE_PORT_STATISTICS:
                UpdatePortStatistics updatePortStatistics = msg.getUpdatePortStatistics();
                deviceProviderService.updatePortStatistics(deviceId(updatePortStatistics.getDeviceId()),
                                                           updatePortStatistics.getPortStatisticsList()
                                                             .stream()
                                                                .map(GrpcDeviceUtils::translate)
                                                                .collect(toList()));
                break;

            // return value of DeviceProvider#isReachable
            case IS_REACHABLE_RESPONSE:
                IsReachableResponse isReachableResponse = msg.getIsReachableResponse();
                int xid = isReachableResponse.getXid();
                boolean isReachable = isReachableResponse.getIsReachable();
                CompletableFuture<Boolean> result = outstandingIsReachable.asMap().remove(xid);
                if (result != null) {
                    result.complete(isReachable);
                }
                break;

            case METHOD_NOT_SET:
            default:
                log.warn("Unexpected message received {}", msg);
                break;
            }
        }

        @Override
        public void onCompleted() {
            log.info("DeviceProviderServiceServerProxy completed");
            deviceProviderRegistry.unregister(pairedProvider);
            registeredProviders.remove(pairedProvider);
            toDeviceProvider.onCompleted();
        }

        @Override
        public void onError(Throwable e) {
            log.error("DeviceProviderServiceServerProxy#onError", e);
            deviceProviderRegistry.unregister(pairedProvider);
            registeredProviders.remove(pairedProvider);
            // TODO What is the proper clean up for bi-di stream on error?
            // sample suggests no-op
            toDeviceProvider.onError(e);
        }


        /**
         * Registers Future for {@link DeviceProvider#isReachable(DeviceId)} return value.
         * @param xid   IsReachable call ID.
         * @param reply Future to
         */
        void register(int xid, CompletableFuture<Boolean> reply) {
            outstandingIsReachable.put(xid, reply);
        }

    }

    // Upper -> Lower Controller message
    /**
     * Relay DeviceProvider calls to RPC client.
     */
    private final class DeviceProviderServerProxy
            implements DeviceProvider {

        private final Logger log = LoggerFactory.getLogger(getClass());

        // xid for isReachable calls
        private final AtomicInteger xidPool = new AtomicInteger();
        private final StreamObserver<DeviceProviderMsg> toDeviceProvider;

        private DeviceProviderServiceServerProxy deviceProviderServiceProxy = null;
        private ProviderId providerId;

        DeviceProviderServerProxy(StreamObserver<DeviceProviderMsg> toDeviceProvider) {
            this.toDeviceProvider = toDeviceProvider;
        }

        void setProviderId(ProviderId pid) {
            this.providerId = pid;
        }

        /**
         * Registers RPC stream in other direction.
         * @param deviceProviderServiceProxy {@link DeviceProviderServiceServerProxy}
         */
        void pair(DeviceProviderServiceServerProxy deviceProviderServiceProxy) {
            this.deviceProviderServiceProxy  = deviceProviderServiceProxy;
        }

        @Override
        public void triggerProbe(DeviceId deviceId) {
            log.trace("triggerProbe({})", deviceId);
            DeviceProviderMsg.Builder msgBuilder = DeviceProviderMsg.newBuilder();
            msgBuilder.setTriggerProbe(msgBuilder.getTriggerProbeBuilder()
                                       .setDeviceId(deviceId.toString())
                                       .build());
            DeviceProviderMsg triggerProbeMsg = msgBuilder.build();
            toDeviceProvider.onNext(triggerProbeMsg);
            // TODO Catch Exceptions and call onError()
        }

        @Override
        public void roleChanged(DeviceId deviceId, MastershipRole newRole) {
            log.trace("roleChanged({}, {})", deviceId, newRole);
            DeviceProviderMsg.Builder msgBuilder = DeviceProviderMsg.newBuilder();
            msgBuilder.setRoleChanged(msgBuilder.getRoleChangedBuilder()
                                          .setDeviceId(deviceId.toString())
                                          .setNewRole(translate(newRole))
                                          .build());
            toDeviceProvider.onNext(msgBuilder.build());
            // TODO Catch Exceptions and call onError()
        }

        @Override
        public boolean isReachable(DeviceId deviceId) {
            log.trace("isReachable({})", deviceId);
            CompletableFuture<Boolean> result = new CompletableFuture<>();
            final int xid = xidPool.incrementAndGet();

            DeviceProviderMsg.Builder msgBuilder = DeviceProviderMsg.newBuilder();
            msgBuilder.setIsReachableRequest(msgBuilder.getIsReachableRequestBuilder()
                                                 .setXid(xid)
                                                 .setDeviceId(deviceId.toString())
                                                 .build());

            // Associate xid and register above future some where
            // in DeviceProviderService channel to receive reply
            if (deviceProviderServiceProxy != null) {
                deviceProviderServiceProxy.register(xid, result);
            }

            // send message down RPC
            toDeviceProvider.onNext(msgBuilder.build());

            // wait for reply
            try {
                return result.get(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                log.debug("isReachable({}) was Interrupted", deviceId, e);
                Thread.currentThread().interrupt();
            } catch (TimeoutException e) {
                log.warn("isReachable({}) Timed out", deviceId, e);
            } catch (ExecutionException e) {
                log.error("isReachable({}) Execution failed", deviceId, e);
                // close session?
            }
            return false;
            // TODO Catch Exceptions and call onError()
        }

        @Override
        public void enablePort(DeviceId deviceId, PortNumber portNumber) {
            //TODO
        }

        @Override
        public void disablePort(DeviceId deviceId, PortNumber portNumber) {
            //TODO
        }

        @Override
        public ProviderId id() {
            return checkNotNull(providerId, "not initialized yet");
        }

    }
}
