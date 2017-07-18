/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.grpc.ctl;

import com.google.common.collect.ImmutableSet;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.grpc.api.GrpcChannelId;
import org.onosproject.grpc.api.GrpcController;
import org.onosproject.grpc.api.GrpcObserverHandler;
import org.onosproject.grpc.api.GrpcStreamObserverId;
import org.onosproject.grpc.ctl.dummy.Dummy;
import org.onosproject.grpc.ctl.dummy.DummyServiceGrpc;
import org.onosproject.net.DeviceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Default implementation of the GrpcController.
 */
@Component(immediate = true)
@Service
public class GrpcControllerImpl implements GrpcController {

    // Hint: set to true to log all gRPC messages received/sent on all channels.
    // TODO: make configurable at runtime
    public static boolean enableMessageLog = false;

    private static final int CONNECTION_TIMEOUT_SECONDS = 20;

    public static final Logger log = LoggerFactory
            .getLogger(GrpcControllerImpl.class);

    private Map<GrpcStreamObserverId, GrpcObserverHandler> observers;
    private Map<GrpcChannelId, ManagedChannel> channels;
    private Map<GrpcChannelId, ManagedChannelBuilder<?>> channelBuilders;

    @Activate
    public void activate() {
        observers = new ConcurrentHashMap<>();
        channels = new ConcurrentHashMap<>();
        channelBuilders = new ConcurrentHashMap<>();
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        channels.values().forEach(ManagedChannel::shutdown);
        observers.clear();
        channels.clear();
        channelBuilders.clear();
        log.info("Stopped");
    }

    @Override
    public void addObserver(GrpcStreamObserverId observerId, GrpcObserverHandler grpcObserverHandler) {
        grpcObserverHandler.bindObserver(channels.get(observerId.serviceId().channelId()));
        observers.put(observerId, grpcObserverHandler);
    }

    @Override
    public void removeObserver(GrpcStreamObserverId observerId) {
        observers.get(observerId).removeObserver();
        observers.remove(observerId);
    }

    @Override
    public Optional<GrpcObserverHandler> getObserverManager(GrpcStreamObserverId observerId) {
        return Optional.ofNullable(observers.get(observerId));
    }

    @Override
    public ManagedChannel connectChannel(GrpcChannelId channelId, ManagedChannelBuilder<?> channelBuilder)
            throws IOException {

        if (enableMessageLog) {
            channelBuilder.intercept(new InternalLogChannelInterceptor(channelId));
        }

        ManagedChannel channel = channelBuilder.build();

        // Forced connection not yet implemented. Use workaround...
        // channel.getState(true);
        doDummyMessage(channel);

        channelBuilders.put(channelId, channelBuilder);
        channels.put(channelId, channel);
        return channel;
    }

    private void doDummyMessage(ManagedChannel channel) throws IOException {
        DummyServiceGrpc.DummyServiceBlockingStub dummyStub = DummyServiceGrpc.newBlockingStub(channel)
                .withDeadlineAfter(CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        try {
            dummyStub.sayHello(Dummy.DummyMessageThatNoOneWouldReallyUse.getDefaultInstance());
        } catch (StatusRuntimeException e) {
            if (e.getStatus() != Status.UNIMPLEMENTED) {
                // UNIMPLEMENTED means that server received our message but doesn't know how to handle it.
                // Hence, channel is open.
                throw new IOException(e);
            }
        }
    }

    @Override
    public boolean isChannelOpen(GrpcChannelId channelId) {
        if (!channels.containsKey(channelId)) {
            log.warn("Can't check if channel open for unknown channel id {}", channelId);
            return false;
        }

        try {
            doDummyMessage(channels.get(channelId));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public void disconnectChannel(GrpcChannelId channelId) {
        if (!channels.containsKey(channelId)) {
            // Nothing to do.
            return;
        }
        ManagedChannel channel = channels.get(channelId);

        try {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("Channel {} didn't shut down in time.");
            channel.shutdownNow();
        }

        channels.remove(channelId);
        channelBuilders.remove(channelId);
    }

    @Override
    public Map<GrpcChannelId, ManagedChannel> getChannels() {
        return channels;
    }

    @Override
    public Collection<ManagedChannel> getChannels(final DeviceId deviceId) {
        final Set<ManagedChannel> deviceChannels = new HashSet<>();
        channels.forEach((k, v) -> {
            if (k.deviceId().equals(deviceId)) {
                deviceChannels.add(v);
            }
        });

        return ImmutableSet.copyOf(deviceChannels);
    }

    @Override
    public Optional<ManagedChannel> getChannel(GrpcChannelId channelId) {
        return Optional.ofNullable(channels.get(channelId));
    }

    /**
     * gRPC client interceptor that logs all messages sent and received.
     */
    private final class InternalLogChannelInterceptor implements ClientInterceptor {

        private final GrpcChannelId channelId;

        private InternalLogChannelInterceptor(GrpcChannelId channelId) {
            this.channelId = channelId;
        }

        @Override
        public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> methodDescriptor,
                                                                   CallOptions callOptions, Channel channel) {
            return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(channel.newCall(
                    methodDescriptor, callOptions.withoutWaitForReady())) {

                @Override
                public void sendMessage(ReqT message) {
                    log.info("*** SENDING GRPC MESSAGE [{}]\n{}:\n{}", channelId, methodDescriptor.getFullMethodName(),
                             message.toString());
                    super.sendMessage(message);
                }

                @Override
                public void start(Listener<RespT> responseListener, Metadata headers) {

                    ClientCall.Listener<RespT> listener = new ForwardingClientCallListener<RespT>() {
                        @Override
                        protected Listener<RespT> delegate() {
                            return responseListener;
                        }

                        @Override
                        public void onMessage(RespT message) {
                            log.info("*** RECEIVED GRPC MESSAGE [{}]\n{}:\n{}", channelId,
                                     methodDescriptor.getFullMethodName(),
                                     message.toString());
                            super.onMessage(message);
                        }
                    };
                    super.start(listener, headers);
                }
            };
        }
    }
}
