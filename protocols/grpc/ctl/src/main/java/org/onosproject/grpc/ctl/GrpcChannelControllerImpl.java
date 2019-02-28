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
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.grpc.api.GrpcChannelController;
import org.onosproject.grpc.api.GrpcChannelId;
import org.onosproject.grpc.proto.dummy.Dummy;
import org.onosproject.grpc.proto.dummy.DummyServiceGrpc;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.onosproject.grpc.ctl.OsgiPropertyConstants.ENABLE_MESSAGE_LOG;
import static org.onosproject.grpc.ctl.OsgiPropertyConstants.ENABLE_MESSAGE_LOG_DEFAULT;

/**
 * Default implementation of the GrpcChannelController.
 */
@Component(immediate = true, service = GrpcChannelController.class,
        property = {
                ENABLE_MESSAGE_LOG + ":Boolean=" + ENABLE_MESSAGE_LOG_DEFAULT,
        })
public class GrpcChannelControllerImpl implements GrpcChannelController {

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService componentConfigService;

    /**
     * Indicates whether to log gRPC messages.
     */
    private final AtomicBoolean enableMessageLog = new AtomicBoolean(
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

    private void doDummyMessage(ManagedChannel channel) throws StatusRuntimeException {
        DummyServiceGrpc.DummyServiceBlockingStub dummyStub = DummyServiceGrpc
                .newBlockingStub(channel)
                .withDeadlineAfter(CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        try {
            //noinspection ResultOfMethodCallIgnored
            dummyStub.sayHello(Dummy.DummyMessageThatNoOneWouldReallyUse
                                       .getDefaultInstance());
        } catch (StatusRuntimeException e) {
            if (!e.getStatus().equals(Status.UNIMPLEMENTED)) {
                // UNIMPLEMENTED means that the server received our message but
                // doesn't know how to handle it. Hence, channel is open.
                throw e;
            }
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

    @Override
    public CompletableFuture<Boolean> probeChannel(GrpcChannelId channelId) {
        final ManagedChannel channel = channels.get(channelId);
        if (channel == null) {
            log.warn("Unable to find any channel with ID {}, cannot send probe",
                     channelId);
            return CompletableFuture.completedFuture(false);
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                doDummyMessage(channel);
                return true;
            } catch (StatusRuntimeException e) {
                log.debug("Probe for {} failed", channelId);
                log.debug("", e);
                return false;
            }
        });
    }
}
