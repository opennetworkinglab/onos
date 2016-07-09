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

import static org.onosproject.incubator.protobuf.net.ProtobufUtils.asMap;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.onosproject.grpc.net.Link.LinkType;
import org.onosproject.grpc.net.link.LinkProviderServiceRpcGrpc;
import org.onosproject.grpc.net.link.LinkProviderServiceRpcGrpc.LinkProviderServiceRpcFutureStub;
import org.onosproject.grpc.net.link.LinkService.LinkDetectedMsg;
import org.onosproject.grpc.net.link.LinkService.LinkVanishedMsg;
import org.onosproject.grpc.net.link.LinkService.Void;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link.Type;
import org.onosproject.net.link.LinkDescription;
import org.onosproject.net.link.LinkProvider;
import org.onosproject.net.link.LinkProviderService;
import org.onosproject.net.provider.AbstractProviderService;
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.Beta;
import com.google.common.util.concurrent.ListenableFuture;

import io.grpc.Channel;

/**
 * Proxy object to handle LinkProviderService calls.
 *
 * RPC wise, this will initiate a RPC call on each method invocation.
 */
@Beta
class LinkProviderServiceClientProxy
    extends AbstractProviderService<LinkProvider>
    implements LinkProviderService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Channel channel;

    /**
     * Constructs {@link LinkProviderServiceClientProxy}.
     *
     * @param provider {@link LinkProvider}. Only ProviderId scheme is used.
     * @param channel channel to use to call RPC
     */
    protected LinkProviderServiceClientProxy(LinkProvider provider, Channel channel) {
        super(provider);
        this.channel = channel;
    }

    @Override
    public void linkDetected(LinkDescription linkDescription) {
        checkValidity();

        LinkProviderServiceRpcFutureStub newStub = LinkProviderServiceRpcGrpc.newFutureStub(channel);
        ListenableFuture<Void> future = newStub.linkDetected(detectMsg(provider().id(), linkDescription));

        try {
            // There's no need to wait, but just checking server
            future.get(500, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.error("linkDetected({}) failed", linkDescription, e);
            invalidate();
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            log.error("linkDetected({}) failed", linkDescription, e);
            invalidate();
        } catch (TimeoutException e) {
            log.error("linkDetected({}) failed", linkDescription, e);
            invalidate();
        }
    }

    @Override
    public void linkVanished(LinkDescription linkDescription) {
        checkValidity();

        LinkProviderServiceRpcFutureStub newStub = LinkProviderServiceRpcGrpc.newFutureStub(channel);
        ListenableFuture<Void> future = newStub.linkVanished(vanishMsg(provider().id(), linkDescription));

        try {
            // There's no need to wait, but just checking server
            future.get(500, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.error("linkVanished({}) failed", linkDescription, e);
            invalidate();
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            log.error("linkVanished({}) failed", linkDescription, e);
            invalidate();
        } catch (TimeoutException e) {
            log.error("linkVanished({}) failed", linkDescription, e);
            invalidate();
        }
    }

    @Override
    public void linksVanished(ConnectPoint connectPoint) {
        checkValidity();

        LinkProviderServiceRpcFutureStub newStub = LinkProviderServiceRpcGrpc.newFutureStub(channel);
        ListenableFuture<Void> future = newStub.linkVanished(vanishMsg(provider().id(), connectPoint));

        try {
            // There's no need to wait, but just checking server
            future.get(500, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.error("linksVanished({}) failed", connectPoint, e);
            invalidate();
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            log.error("linksVanished({}) failed", connectPoint, e);
            invalidate();
        } catch (TimeoutException e) {
            log.error("linksVanished({}) failed", connectPoint, e);
            invalidate();
        }
    }

    @Override
    public void linksVanished(DeviceId deviceId) {
        checkValidity();

        LinkProviderServiceRpcFutureStub newStub = LinkProviderServiceRpcGrpc.newFutureStub(channel);
        ListenableFuture<Void> future = newStub.linkVanished(vanishMsg(provider().id(), deviceId));

        try {
            // There's no need to wait, but just checking server
            future.get(500, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.error("linksVanished({}) failed", deviceId, e);
            invalidate();
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            log.error("linksVanished({}) failed", deviceId, e);
            invalidate();
        } catch (TimeoutException e) {
            log.error("linksVanished({}) failed", deviceId, e);
            invalidate();
        }
    }

    /**
     * Builds {@link LinkDetectedMsg}.
     *
     * @param id ProviderId
     * @param linkDescription {@link LinkDescription}
     * @return {@link LinkDetectedMsg}
     */
    private LinkDetectedMsg detectMsg(ProviderId id,
                                      LinkDescription linkDescription) {
        LinkDetectedMsg.Builder builder = LinkDetectedMsg.newBuilder();
        builder.setProviderId(id.scheme())
               .setLinkDescription(builder.getLinkDescriptionBuilder()
                                    .setSrc(translate(linkDescription.src()))
                                    .setDst(translate(linkDescription.dst()))
                                    .setType(translate(linkDescription.type()))
                                    .putAllAnnotations(asMap(linkDescription.annotations()))
                                    .build()
                                    );
        return builder.build();
    }

    /**
     * Builds {@link LinkVanishedMsg}.
     *
     * @param id ProviderId
     * @param linkDescription {@link LinkDescription}
     * @return {@link LinkVanishedMsg}
     */
    private LinkVanishedMsg vanishMsg(ProviderId id,
                                      LinkDescription linkDescription) {

        LinkVanishedMsg.Builder builder = LinkVanishedMsg.newBuilder();
        builder.setProviderId(id.scheme())
            .setLinkDescription(builder.getLinkDescriptionBuilder()
                             .setSrc(translate(linkDescription.src()))
                             .setDst(translate(linkDescription.dst()))
                             .setType(translate(linkDescription.type()))
                             .putAllAnnotations(asMap(linkDescription.annotations()))
                             .build()
                             );
        return builder.build();
    }

    /**
     * Builds {@link LinkVanishedMsg}.
     *
     * @param id ProviderId
     * @param connectPoint {@link ConnectPoint}
     * @return {@link LinkVanishedMsg}
     */
    private LinkVanishedMsg vanishMsg(ProviderId id,
                                      ConnectPoint connectPoint) {

        LinkVanishedMsg.Builder builder = LinkVanishedMsg.newBuilder();
        builder.setProviderId(id.scheme())
            .setConnectPoint(translate(connectPoint));
        return builder.build();
    }

    /**
     * Builds {@link LinkVanishedMsg}.
     *
     * @param id ProviderId
     * @param deviceId {@link DeviceId}
     * @return {@link LinkVanishedMsg}
     */
    private LinkVanishedMsg vanishMsg(ProviderId id, DeviceId deviceId) {

        LinkVanishedMsg.Builder builder = LinkVanishedMsg.newBuilder();
        builder.setProviderId(id.scheme())
            .setDeviceId(deviceId.toString());
        return builder.build();
    }

    /**
     * Translates ONOS object to gRPC message.
     *
     * @param type {@link org.onosproject.net.Link.Type Link.Type}
     * @return gRPC LinkType
     */
    private LinkType translate(Type type) {
        switch (type) {
        case DIRECT:
            return LinkType.DIRECT;
        case EDGE:
            return LinkType.EDGE;
        case INDIRECT:
            return LinkType.INDIRECT;
        case OPTICAL:
            return LinkType.OPTICAL;
        case TUNNEL:
            return LinkType.TUNNEL;
        case VIRTUAL:
            return LinkType.VIRTUAL;

        default:
            return LinkType.DIRECT;

        }
    }

    /**
     * Translates ONOS object to gRPC message.
     *
     * @param cp {@link ConnectPoint}
     * @return gRPC ConnectPoint
     */
    private org.onosproject.grpc.net.Link.ConnectPoint translate(ConnectPoint cp) {
        return org.onosproject.grpc.net.Link.ConnectPoint.newBuilder()
                .setDeviceId(cp.deviceId().toString())
                .setPortNumber(cp.port().toString())
                .build();
    }

}
