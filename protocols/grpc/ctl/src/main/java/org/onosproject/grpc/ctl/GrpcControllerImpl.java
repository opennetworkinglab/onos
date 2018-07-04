/*
 * Copyright 2017-present Open Networking Foundation
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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Striped;
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
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.grpc.api.GrpcChannelId;
import org.onosproject.grpc.api.GrpcController;
import org.onosproject.grpc.ctl.dummy.Dummy;
import org.onosproject.grpc.ctl.dummy.DummyServiceGrpc;
import org.onosproject.net.DeviceId;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation of the GrpcController.
 */
@Component(immediate = true)
@Service
public class GrpcControllerImpl implements GrpcController {

    private  static final String SET_FORWARDING_PIPELINE_CONFIG_METHOD = "p4.P4Runtime/SetForwardingPipelineConfig";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService componentConfigService;

    // Hint: set to true to log all gRPC messages received/sent on all channels
    // Does not enable log on existing channels
    private static final boolean DEFAULT_LOG_LEVEL = false;
    @Property(name = "enableMessageLog", boolValue =  DEFAULT_LOG_LEVEL,
            label = "Indicates whether to log all gRPC messages sent and received on all channels")
    public static boolean enableMessageLog = DEFAULT_LOG_LEVEL;

    private final Logger log = LoggerFactory.getLogger(getClass());

    private Map<GrpcChannelId, ManagedChannel> channels;
    private final Striped<Lock> channelLocks = Striped.lock(30);

    @Activate
    public void activate() {
        componentConfigService.registerProperties(getClass());
        channels = new ConcurrentHashMap<>();
        log.info("Started");
    }

    @Modified
    public void modified(ComponentContext context) {
        if (context != null) {
            Dictionary<?, ?> properties = context.getProperties();
            enableMessageLog = Tools.isPropertyEnabled(properties, "enableMessageLog",
                    DEFAULT_LOG_LEVEL);
            log.info("Configured. Log of gRPC messages is {}", enableMessageLog ? "enabled" : "disabled");
        }
    }

    @Deactivate
    public void deactivate() {
        componentConfigService.unregisterProperties(getClass(), false);
        channels.values().forEach(ManagedChannel::shutdown);
        channels.clear();
        log.info("Stopped");
    }

    @Override
    public ManagedChannel connectChannel(GrpcChannelId channelId,
                                         ManagedChannelBuilder<?> channelBuilder)
            throws IOException {
        checkNotNull(channelId);
        checkNotNull(channelBuilder);

        Lock lock = channelLocks.get(channelId);
        lock.lock();

        try {
            channelBuilder.intercept(new InternalLogChannelInterceptor(channelId));
            ManagedChannel channel = channelBuilder.build();
            // Forced connection not yet implemented. Use workaround...
            // channel.getState(true);
            doDummyMessage(channel);
            channels.put(channelId, channel);
            return channel;
        } finally {
            lock.unlock();
        }
    }

    private void doDummyMessage(ManagedChannel channel) throws IOException {
        DummyServiceGrpc.DummyServiceBlockingStub dummyStub = DummyServiceGrpc
                .newBlockingStub(channel)
                .withDeadlineAfter(CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        try {
            dummyStub.sayHello(Dummy.DummyMessageThatNoOneWouldReallyUse
                                       .getDefaultInstance());
        } catch (StatusRuntimeException e) {
            if (e.getStatus() != Status.UNIMPLEMENTED) {
                // UNIMPLEMENTED means that the server received our message but
                // doesn't know how to handle it. Hence, channel is open.
                throw new IOException(e);
            }
        }
    }

    @Override
    public boolean isChannelOpen(GrpcChannelId channelId) {
        checkNotNull(channelId);

        Lock lock = channelLocks.get(channelId);
        lock.lock();

        try {
            if (!channels.containsKey(channelId)) {
                log.warn("Can't check if channel open for unknown channel ID {}",
                         channelId);
                return false;
            }
            try {
                doDummyMessage(channels.get(channelId));
                return true;
            } catch (IOException e) {
                log.debug("Unable to send dummy message to {}: {}",
                          channelId, e.getCause().getMessage());
                return false;
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void disconnectChannel(GrpcChannelId channelId) {
        checkNotNull(channelId);

        Lock lock = channelLocks.get(channelId);
        lock.lock();

        try {
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
                Thread.currentThread().interrupt();
            }

            channels.remove(channelId);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Map<GrpcChannelId, ManagedChannel> getChannels() {
        return ImmutableMap.copyOf(channels);
    }

    @Override
    public Collection<ManagedChannel> getChannels(final DeviceId deviceId) {
        checkNotNull(deviceId);
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
        checkNotNull(channelId);

        Lock lock = channelLocks.get(channelId);
        lock.lock();

        try {
            return Optional.ofNullable(channels.get(channelId));
        } finally {
            lock.unlock();
        }
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
        public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
                MethodDescriptor<ReqT, RespT> methodDescriptor,
                CallOptions callOptions, Channel channel) {
            return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
                    channel.newCall(methodDescriptor, callOptions.withoutWaitForReady())) {

                @Override
                public void sendMessage(ReqT message) {
                    if (enableMessageLog && !methodDescriptor.getFullMethodName()
                            .startsWith(SET_FORWARDING_PIPELINE_CONFIG_METHOD)) {
                        log.info("*** SENDING GRPC MESSAGE [{}]\n{}:\n{}",
                                channelId, methodDescriptor.getFullMethodName(),
                                message.toString());
                    }
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
                            if (enableMessageLog) {
                                log.info("*** RECEIVED GRPC MESSAGE [{}]\n{}:\n{}",
                                        channelId, methodDescriptor.getFullMethodName(),
                                        message.toString());
                            }
                            super.onMessage(message);
                        }
                    };
                    super.start(listener, headers);
                }
            };
        }
    }
}
