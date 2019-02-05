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
import com.google.common.util.concurrent.Striped;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
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
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * Default implementation of the GrpcChannelController.
 */
@Component(immediate = true)
@Service
public class GrpcChannelControllerImpl implements GrpcChannelController {

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService componentConfigService;

    private static final boolean ENABLE_MESSAGE_LOG_DEFAULT = false;
    private static final String ENABLE_MESSAGE_LOG = "enableMessageLog";
    @Property(name = ENABLE_MESSAGE_LOG, boolValue = ENABLE_MESSAGE_LOG_DEFAULT,
            label = "Indicates whether to log gRPC messages")
    private static AtomicBoolean enableMessageLog = new AtomicBoolean(
            ENABLE_MESSAGE_LOG_DEFAULT);

    private final Logger log = LoggerFactory.getLogger(getClass());

    private Map<GrpcChannelId, ManagedChannel> channels;
    private Map<GrpcChannelId, GrpcLoggingInterceptor> interceptors;

    private final Striped<Lock> channelLocks = Striped.lock(30);

    @Activate
    public void activate() {
        componentConfigService.registerProperties(getClass());
        channels = new ConcurrentHashMap<>();
        interceptors = new ConcurrentHashMap<>();
        log.info("Started");
    }

    @Modified
    public void modified(ComponentContext context) {
        if (context != null) {
            Dictionary<?, ?> properties = context.getProperties();
            enableMessageLog.set(Tools.isPropertyEnabled(
                    properties, ENABLE_MESSAGE_LOG, ENABLE_MESSAGE_LOG_DEFAULT));
            log.info("Configured. Logging of gRPC messages is {}",
                     enableMessageLog.get()
                             ? "ENABLED for new channels"
                             : "DISABLED for new and existing channels");
        }
    }

    @Deactivate
    public void deactivate() {
        componentConfigService.unregisterProperties(getClass(), false);
        channels.values().forEach(ManagedChannel::shutdownNow);
        channels.clear();
        channels = null;
        interceptors.values().forEach(GrpcLoggingInterceptor::close);
        interceptors.clear();
        interceptors = null;
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

            GrpcLoggingInterceptor interceptor = null;
            if (enableMessageLog.get()) {
                interceptor = new GrpcLoggingInterceptor(channelId, enableMessageLog);
                channelBuilder.intercept(interceptor);
            }
            ManagedChannel channel = channelBuilder.build();
            // Forced connection API is still experimental. Use workaround...
            // channel.getState(true);
            try {
                doDummyMessage(channel);
            } catch (StatusRuntimeException e) {
                if (interceptor != null) {
                    interceptor.close();
                }
                shutdownNowAndWait(channel, channelId);
                throw e;
            }
            // If here, channel is open.
            channels.put(channelId, channel);
            if (interceptor != null) {
                interceptors.put(channelId, interceptor);
            }
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
            final GrpcLoggingInterceptor interceptor = interceptors.remove(channelId);
            if (interceptor != null) {
                interceptor.close();
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

}
