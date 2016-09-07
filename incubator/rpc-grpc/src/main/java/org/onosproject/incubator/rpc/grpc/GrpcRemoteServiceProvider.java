/*
 * Copyright 2015-present Open Networking Laboratory
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

import static com.google.common.base.Preconditions.checkArgument;

import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.incubator.rpc.RemoteServiceContext;
import org.onosproject.incubator.rpc.RemoteServiceContextProvider;
import org.onosproject.incubator.rpc.RemoteServiceContextProviderService;
import org.onosproject.incubator.rpc.RemoteServiceProviderRegistry;
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.ManagedChannel;
import io.grpc.internal.DnsNameResolverProvider;
import io.grpc.netty.NegotiationType;
import io.grpc.netty.NettyChannelBuilder;


// gRPC Client side
/**
 * RemoteServiceContextProvider based on gRPC.
 */
@Component(immediate = true)
public class GrpcRemoteServiceProvider implements RemoteServiceContextProvider {

    public static final String GRPC_SCHEME = "grpc";

    public static final String RPC_PROVIDER_NAME = "org.onosproject.rpc.provider.grpc";

    private static final ProviderId PID = new ProviderId(GRPC_SCHEME, RPC_PROVIDER_NAME);

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected RemoteServiceProviderRegistry rpcRegistry;

    private final Map<URI, ManagedChannel> channels = new ConcurrentHashMap<>();

    private RemoteServiceContextProviderService providerService;

    @Activate
    protected void activate() {
        providerService = rpcRegistry.register(this);

        // Uncomment to test if gRPC can be loaded in karaf
        //getChannel(URI.create("grpc://localhost:11984"));

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        rpcRegistry.unregister(this);

        // shutdown all channels
        channels.values().forEach(ManagedChannel::shutdown);
        // Should we wait for shutdown? How?
        channels.clear();
        log.info("Stopped");
    }

    @Override
    public ProviderId id() {
        return PID;
    }

    @Override
    public RemoteServiceContext get(URI uri) {
        // Create gRPC client
        return new GrpcRemoteServiceContext(getChannel(uri));
    }

    private ManagedChannel getChannel(URI uri) {
        checkArgument(Objects.equals(GRPC_SCHEME, uri.getScheme()),
                      "Invalid URI scheme: %s", uri.getScheme());

        return channels.compute(uri, (u, ch) -> {
            if (ch != null && !ch.isShutdown()) {
                return ch;
            } else {
                return createChannel(u);
            }
        });
    }

    private ManagedChannel createChannel(URI uri) {
        log.debug("Creating channel for {}", uri);
        int port = GrpcRemoteServiceServer.DEFAULT_LISTEN_PORT;
        if (uri.getPort() != -1) {
            port = uri.getPort();
        }
        return NettyChannelBuilder.forAddress(uri.getHost(), port)
                .negotiationType(NegotiationType.PLAINTEXT)
                // TODO Not ideal fix, gRPC discovers name resolvers
                // in the class path, but OSGi was preventing it.
                // Manually specifying the default dns resolver for now.
                .nameResolverFactory(new DnsNameResolverProvider())
                .build();
    }

}
