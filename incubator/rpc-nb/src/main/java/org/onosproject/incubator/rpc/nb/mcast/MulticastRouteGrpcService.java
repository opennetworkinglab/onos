/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.incubator.rpc.nb.mcast;

import io.grpc.stub.StreamObserver;
import org.onlab.packet.IpAddress;
import org.onosproject.grpc.net.mcast.MulticastRouteServiceGrpc;
import org.onosproject.grpc.net.mcast.MulticastRouteServiceOuterClass;
import org.onosproject.net.mcast.McastRoute;
import org.onosproject.net.mcast.MulticastRouteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.Beta;

/**
 * Implementation of multicast gRPC service.
 */
@Beta
public class MulticastRouteGrpcService
    extends MulticastRouteServiceGrpc.MulticastRouteServiceImplBase {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final MulticastRouteService multicastRouteService;

    public MulticastRouteGrpcService(MulticastRouteService service) {
        this.multicastRouteService = service;
    }

    @Override
    public StreamObserver<MulticastRouteServiceOuterClass.MulticastRequest>
            operation(StreamObserver<MulticastRouteServiceOuterClass.MulticastReply> responseObserver) {

        return new MulticastServiceServerProxy(responseObserver);
    }

    private final class MulticastServiceServerProxy
            implements StreamObserver<MulticastRouteServiceOuterClass.MulticastRequest> {

        private final StreamObserver<MulticastRouteServiceOuterClass.MulticastReply> responseObserver;

        public MulticastServiceServerProxy(
                StreamObserver<MulticastRouteServiceOuterClass.MulticastReply> responseObserver) {
            this.responseObserver = responseObserver;
        }

        @Override
        public void onNext(MulticastRouteServiceOuterClass.MulticastRequest value) {
            MulticastRouteServiceOuterClass.MulticastRoute route = value.getRoute();

            switch (value.getOperation()) {
            case ADD_ROUTE:
                multicastRouteService.add(
                        new McastRoute(IpAddress.valueOf(route.getSource()),
                                IpAddress.valueOf(route.getGroup()),
                                McastRoute.Type.STATIC));
                break;
            case ADD_SOURCE:
                break;
            case ADD_SINK:
                break;
            case REMOVE_ROUTE:
                break;
            case REMOVE_SOURCE:
                break;
            case REMOVE_SINK:
                break;
            case UNRECOGNIZED:
            default:
                break;
            }

            responseObserver.onNext(MulticastRouteServiceOuterClass.MulticastReply.newBuilder().build());
        }

        @Override
        public void onError(Throwable t) {
            log.warn("Error receiving multicast route", t);
        }

        @Override
        public void onCompleted() {
            // When the client closes their stream, we'll close ours too
            responseObserver.onCompleted();
        }
    }
}
