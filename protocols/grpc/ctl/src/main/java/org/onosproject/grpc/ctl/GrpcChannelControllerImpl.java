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

import com.google.common.util.concurrent.Striped;
import io.grpc.LoadBalancerRegistry;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.NameResolverRegistry;
import io.grpc.internal.DnsNameResolverProvider;
import io.grpc.internal.PickFirstLoadBalancerProvider;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.grpc.api.GrpcChannelController;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.net.URI;
import java.util.Dictionary;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
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

    private static final String GRPC = "grpc";
    private static final String GRPCS = "grpcs";

    private static final int DEFAULT_MAX_INBOUND_MSG_SIZE = 256; // Megabytes.
    // The maximum metadata size in Megabytes that a P4Runtime client should accept.
    // This is necessary, because the P4Runtime protocol returns individual errors to
    // requests in a batch all wrapped in a single status, which counts towards the
    // metadata size limit.  For large batches, this easily exceeds the default of
    // 8KB. According to the tests done with Stratum, 4MB will support batches of
    // around 40000 entries, assuming 100 bytes per error, without exceeding the
    // maximum metadata size. Setting here 10 times higher.
    private static final int DEFAULT_MAX_INBOUND_META_SIZE = 40;
    private static final int MEGABYTES = 1024 * 1024;

    private static final PickFirstLoadBalancerProvider PICK_FIRST_LOAD_BALANCER_PROVIDER =
            new PickFirstLoadBalancerProvider();
    private static final DnsNameResolverProvider DNS_NAME_RESOLVER_PROVIDER =
            new DnsNameResolverProvider();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService componentConfigService;

    /**
     * Indicates whether to log gRPC messages.
     */
    private final AtomicBoolean enableMessageLog = new AtomicBoolean(
            ENABLE_MESSAGE_LOG_DEFAULT);

    private final Logger log = LoggerFactory.getLogger(getClass());

    private Map<URI, ManagedChannel> channels;
    private Map<URI, GrpcLoggingInterceptor> interceptors;

    private final Striped<Lock> channelLocks = Striped.lock(30);

    @Activate
    public void activate() {
        componentConfigService.registerProperties(getClass());
        channels = new ConcurrentHashMap<>();
        interceptors = new ConcurrentHashMap<>();
        LoadBalancerRegistry.getDefaultRegistry()
                .register(PICK_FIRST_LOAD_BALANCER_PROVIDER);
        NameResolverRegistry.getDefaultRegistry()
                .register(DNS_NAME_RESOLVER_PROVIDER);
        log.info("Started");
    }

    @Modified
    public void modified(ComponentContext context) {
        if (context != null) {
            Dictionary<?, ?> properties = context.getProperties();
            enableMessageLog.set(Tools.isPropertyEnabled(
                    properties, ENABLE_MESSAGE_LOG, ENABLE_MESSAGE_LOG_DEFAULT));
            log.info("Configured. Logging of gRPC messages is {}",
                     enableMessageLog.get() ? "ENABLED" : "DISABLED");
        }
    }

    @Deactivate
    public void deactivate() {
        LoadBalancerRegistry.getDefaultRegistry()
                .deregister(PICK_FIRST_LOAD_BALANCER_PROVIDER);
        NameResolverRegistry.getDefaultRegistry()
                .register(DNS_NAME_RESOLVER_PROVIDER);
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
    public ManagedChannel create(URI channelUri) {
        return create(channelUri, makeChannelBuilder(channelUri));
    }

    @Override
    public ManagedChannel create(URI channelUri, ManagedChannelBuilder<?> channelBuilder) {
        checkNotNull(channelUri);
        checkNotNull(channelBuilder);

        channelLocks.get(channelUri).lock();
        try {
            if (channels.containsKey(channelUri)) {
                throw new IllegalArgumentException(format(
                        "A channel with ID '%s' already exists", channelUri));
            }

            log.info("Creating new gRPC channel {}...", channelUri);

            final GrpcLoggingInterceptor interceptor = new GrpcLoggingInterceptor(
                    channelUri, enableMessageLog);
            channelBuilder.intercept(interceptor);

            final ManagedChannel channel = channelBuilder.build();

            channels.put(channelUri, channelBuilder.build());
            interceptors.put(channelUri, interceptor);

            return channel;
        } finally {
            channelLocks.get(channelUri).unlock();
        }
    }

    private NettyChannelBuilder makeChannelBuilder(URI channelUri) {

        checkArgument(channelUri.getScheme().equals(GRPC)
                              || channelUri.getScheme().equals(GRPCS),
                      format("Server URI scheme must be %s or %s", GRPC, GRPCS));
        checkArgument(!isNullOrEmpty(channelUri.getHost()),
                      "Server host address should not be empty");
        checkArgument(channelUri.getPort() > 0 && channelUri.getPort() <= 65535,
                      "Invalid server port");

        final boolean useTls = channelUri.getScheme().equals(GRPCS);

        final NettyChannelBuilder channelBuilder = NettyChannelBuilder
                .forAddress(channelUri.getHost(), channelUri.getPort())
                .nameResolverFactory(DNS_NAME_RESOLVER_PROVIDER)
                .defaultLoadBalancingPolicy(
                        PICK_FIRST_LOAD_BALANCER_PROVIDER.getPolicyName())
                .maxInboundMessageSize(
                        DEFAULT_MAX_INBOUND_MSG_SIZE * MEGABYTES)
                .maxInboundMetadataSize(
                        DEFAULT_MAX_INBOUND_META_SIZE * MEGABYTES);

        if (useTls) {
            try {
                // Accept any server certificate; this is insecure and
                // should not be used in production.
                final SslContext sslContext = GrpcSslContexts.forClient().trustManager(
                        InsecureTrustManagerFactory.INSTANCE).build();
                channelBuilder.sslContext(sslContext).useTransportSecurity();
            } catch (SSLException e) {
                log.error("Failed to build SSL context", e);
                return null;
            }
        } else {
            channelBuilder.usePlaintext();
        }

        return channelBuilder;
    }

    @Override
    public void destroy(URI channelUri) {
        checkNotNull(channelUri);

        channelLocks.get(channelUri).lock();
        try {
            log.info("Destroying gRPC channel {}...", channelUri);
            final ManagedChannel channel = channels.remove(channelUri);
            if (channel != null) {
                shutdownNowAndWait(channel, channelUri);
            }
            final GrpcLoggingInterceptor interceptor = interceptors.remove(channelUri);
            if (interceptor != null) {
                interceptor.close();
            }
        } finally {
            channelLocks.get(channelUri).unlock();
        }
    }

    private void shutdownNowAndWait(ManagedChannel channel, URI channelUri) {
        try {
            if (!channel.shutdownNow()
                    .awaitTermination(5, TimeUnit.SECONDS)) {
                log.error("Channel {} did not terminate properly",
                          channelUri);
            }
        } catch (InterruptedException e) {
            log.warn("Channel {} didn't shutdown in time", channelUri);
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public Optional<ManagedChannel> get(URI channelUri) {
        checkNotNull(channelUri);
        return Optional.ofNullable(channels.get(channelUri));
    }
}
