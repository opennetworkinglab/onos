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
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.cache.RemovalListeners.asynchronous;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.LinkKey.linkKey;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.Pair;
import org.onosproject.grpc.net.Link.ConnectPoint.ElementIdCase;
import org.onosproject.grpc.net.Link.LinkType;
import org.onosproject.grpc.net.link.LinkProviderServiceRpcGrpc.LinkProviderServiceRpcImplBase;
import org.onosproject.grpc.net.link.LinkService.LinkDetectedMsg;
import org.onosproject.grpc.net.link.LinkService.LinkVanishedMsg;
import org.onosproject.grpc.net.link.LinkService.Void;
import org.onosproject.incubator.protobuf.net.ProtobufUtils;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.LinkKey;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.link.DefaultLinkDescription;
import org.onosproject.net.link.LinkDescription;
import org.onosproject.net.link.LinkProviderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;
import com.google.common.annotations.Beta;

import io.grpc.stub.StreamObserver;

// Only single instance will be created and bound to gRPC LinkProviderService
/**
 * Server-side implementation of gRPC version of LinkProviderService.
 */
@Beta
final class LinkProviderServiceServerProxy
        extends LinkProviderServiceRpcImplBase {

    /**
     * Silence time in seconds, until link gets treated as vanished.
     */
    private static final int EVICT_LIMIT = 3 * 3;

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final GrpcRemoteServiceServer server;

    // TODO implement aging mechanism to automatically remove
    // stale links reported by dead client, etc.
    /**
     * Evicting Cache to track last seen time.
     */
    private final Cache<Pair<String, LinkKey>, LinkDescription> lastSeen;

    LinkProviderServiceServerProxy(GrpcRemoteServiceServer server) {
        this.server = checkNotNull(server);
        ScheduledExecutorService executor = server.getSharedExecutor();
        lastSeen = CacheBuilder.newBuilder()
                .expireAfterWrite(EVICT_LIMIT, TimeUnit.SECONDS)
                .removalListener(asynchronous(this::onRemove, executor))
                .build();

        executor.scheduleWithFixedDelay(lastSeen::cleanUp,
                                        EVICT_LIMIT, EVICT_LIMIT, TimeUnit.SECONDS);
    }

    private void onRemove(RemovalNotification<Pair<String, LinkKey>, LinkDescription> n) {
        if (n.wasEvicted()) {
            getLinkProviderServiceFor(n.getKey().getLeft())
                .linkVanished(n.getValue());
        }
    }

    /**
     * Gets or creates {@link LinkProviderService} registered for given ProviderId scheme.
     *
     * @param scheme ProviderId scheme.
     * @return {@link LinkProviderService}
     */
    private LinkProviderService getLinkProviderServiceFor(String scheme) {
        return server.getLinkProviderServiceFor(scheme);
    }

    @Override
    public void linkDetected(LinkDetectedMsg request,
                             StreamObserver<Void> responseObserver) {

        try {
            onLinkDetected(request, responseObserver);
            // If onNext call was not mandatory, it can be removed.
            responseObserver.onNext(Void.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Exception caught", e);
            responseObserver.onError(e);
        }
    }

    private void onLinkDetected(LinkDetectedMsg request,
                                StreamObserver<Void> responseObserver) {
        String scheme = request.getProviderId();

        LinkProviderService linkProviderService = getLinkProviderServiceFor(scheme);

        LinkDescription linkDescription = translate(request.getLinkDescription());
        linkProviderService.linkDetected(linkDescription);
        lastSeen.put(Pair.of(scheme, linkKey(linkDescription)), linkDescription);
    }

    @Override
    public void linkVanished(LinkVanishedMsg request,
                             StreamObserver<Void> responseObserver) {
        try {
            onLinksVanished(request, responseObserver);
            // If onNext call was not mandatory, it can be removed.
            responseObserver.onNext(Void.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Exception caught", e);
            responseObserver.onError(e);
        }
    }

    private void onLinksVanished(LinkVanishedMsg request,
                                 StreamObserver<Void> responseObserver) {
        String scheme = request.getProviderId();
        switch (request.getSubjectCase()) {
        case CONNECT_POINT:
            ConnectPoint cp = translate(request.getConnectPoint());
            getLinkProviderServiceFor(scheme).linksVanished(cp);
            break;
        case DEVICE_ID:
            DeviceId did = deviceId(request.getDeviceId());
            getLinkProviderServiceFor(scheme).linksVanished(did);
            break;
        case LINK_DESCRIPTION:
            LinkDescription desc = translate(request.getLinkDescription());
            getLinkProviderServiceFor(scheme).linkVanished(desc);
            lastSeen.invalidate(Pair.of(scheme, linkKey(desc)));
            break;
        case SUBJECT_NOT_SET:
        default:
            // do nothing
            break;
        }
    }

    /**
     * Translates gRPC message to corresponding ONOS object.
     *
     * @param connectPoint gRPC message.
     * @return {@link ConnectPoint}
     */
    private ConnectPoint translate(org.onosproject.grpc.net.Link.ConnectPoint connectPoint) {
        checkArgument(connectPoint.getElementIdCase() == ElementIdCase.DEVICE_ID,
                      "Only DeviceId supported.");
        return new ConnectPoint(deviceId(connectPoint.getDeviceId()),
                                PortNumber.fromString(connectPoint.getPortNumber()));
    }

    /**
     * Translates gRPC message to corresponding ONOS object.
     *
     * @param linkDescription gRPC message
     * @return {@link LinkDescription}
     */
    private LinkDescription translate(org.onosproject.grpc.net.Link.LinkDescription linkDescription) {
        ConnectPoint src = translate(linkDescription.getSrc());
        ConnectPoint dst = translate(linkDescription.getDst());
        Link.Type type = translate(linkDescription.getType());
        SparseAnnotations annotations = ProtobufUtils.asAnnotations(linkDescription.getAnnotations());
        return new DefaultLinkDescription(src, dst, type, annotations);
    }

    /**
     * Translates gRPC message to corresponding ONOS object.
     *
     * @param type gRPC message enum
     * @return {@link org.onosproject.net.Link.Type Link.Type}
     */
    private Link.Type translate(LinkType type) {
        switch (type) {
        case DIRECT:
            return Link.Type.DIRECT;
        case EDGE:
            return Link.Type.EDGE;
        case INDIRECT:
            return Link.Type.INDIRECT;
        case OPTICAL:
            return Link.Type.INDIRECT;
        case TUNNEL:
            return Link.Type.TUNNEL;
        case VIRTUAL:
            return Link.Type.VIRTUAL;

        case UNRECOGNIZED:
        default:
            return Link.Type.DIRECT;
        }
    }

}
