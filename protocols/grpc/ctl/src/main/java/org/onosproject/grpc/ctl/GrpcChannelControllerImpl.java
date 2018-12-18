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
import org.onosproject.grpc.api.GrpcChannelController;
import org.onosproject.grpc.api.GrpcChannelId;
import org.onosproject.grpc.proto.dummy.Dummy;
import org.onosproject.grpc.proto.dummy.DummyServiceGrpc;
import org.onosproject.net.DeviceId;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import static java.lang.String.format;

/**
 * Default implementation of the GrpcChannelController.
 */
@Component(immediate = true)
@Service
public class GrpcChannelControllerImpl implements GrpcChannelController {

    // FIXME: Should use message size to determine whether it needs to log the message or not.
    private static final String SET_FORWARDING_PIPELINE_CONFIG_METHOD = "p4.P4Runtime/SetForwardingPipelineConfig";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService componentConfigService;

    // Hint: set to true to log all gRPC messages received/sent on all channels
    // Does not enable log on existing channels
    private static final boolean ENABLE_MESSAGE_LOG_DEFAULT = false;
    private static final String ENABLE_MESSAGE_LOG = "enableMessageLog";
    @Property(name = ENABLE_MESSAGE_LOG, boolValue = ENABLE_MESSAGE_LOG_DEFAULT,
            label = "Indicates whether to log all gRPC messages on new channels")
    public static boolean enableMessageLog = ENABLE_MESSAGE_LOG_DEFAULT;

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
            enableMessageLog = Tools.isPropertyEnabled(
                    properties, ENABLE_MESSAGE_LOG, ENABLE_MESSAGE_LOG_DEFAULT);
            log.info("Configured. Log of gRPC messages is {} for new channels",
                     enableMessageLog ? "enabled" : "disabled");
        }
    }

    @Deactivate
    public void deactivate() {
        componentConfigService.unregisterProperties(getClass(), false);
        channels.values().forEach(ManagedChannel::shutdownNow);
        channels.clear();
        log.info("Stopped");
    }

    @Override
    public ManagedChannel connectChannel(GrpcChannelId channelId,
                                         ManagedChannelBuilder<?> channelBuilder) {
        checkNotNull(channelId);
        checkNotNull(channelBuilder);

        Lock lock = channelLocks.get(channelId);
        lock.lock();

        try {
            if (channels.containsKey(channelId)) {
                throw new IllegalArgumentException(format(
                        "A channel with ID '%s' already exists", channelId));
            }
            if (enableMessageLog) {
                channelBuilder.intercept(new InternalLogChannelInterceptor(channelId));
            }
            ManagedChannel channel = channelBuilder.build();
            // Forced connection API is still experimental. Use workaround...
            // channel.getState(true);
            try {
                doDummyMessage(channel);
            } catch (StatusRuntimeException e) {
                shutdownNowAndWait(channel, channelId);
                throw e;
            }
            // If here, channel is open.
            channels.put(channelId, channel);
            return channel;
        } finally {
            lock.unlock();
        }
    }

    private boolean doDummyMessage(ManagedChannel channel) throws StatusRuntimeException {
        DummyServiceGrpc.DummyServiceBlockingStub dummyStub = DummyServiceGrpc
                .newBlockingStub(channel)
                .withDeadlineAfter(CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        try {
            return dummyStub.sayHello(Dummy.DummyMessageThatNoOneWouldReallyUse
                                              .getDefaultInstance()) != null;
        } catch (StatusRuntimeException e) {
            if (e.getStatus().equals(Status.UNIMPLEMENTED)) {
                // UNIMPLEMENTED means that the server received our message but
                // doesn't know how to handle it. Hence, channel is open.
                return true;
            } else {
                throw e;
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
                log.warn("Unknown channel ID '{}', can't check if channel is open",
                         channelId);
                return false;
            }
            try {
                return doDummyMessage(channels.get(channelId));
            } catch (StatusRuntimeException e) {
                log.debug("Unable to send dummy message to {}: {}",
                          channelId, e.toString());
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
            final ManagedChannel channel = channels.remove(channelId);
            if (channel != null) {
                shutdownNowAndWait(channel, channelId);
            }
        } finally {
            lock.unlock();
        }
    }

    private void shutdownNowAndWait(ManagedChannel channel, GrpcChannelId channelId) {
        try {
            if (!channel.shutdownNow()
                    .awaitTermination(5, TimeUnit.SECONDS)) {
                log.error("Channel '{}' didn't terminate, although we " +
                                  "triggered a shutdown and waited",
                          channelId);
            }
        } catch (InterruptedException e) {
            log.warn("Channel {} didn't shutdown in time", channelId);
            Thread.currentThread().interrupt();
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
