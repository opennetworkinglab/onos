/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.gnmi.ctl;

import gnmi.Gnmi.CapabilityRequest;
import gnmi.Gnmi.CapabilityResponse;
import gnmi.Gnmi.GetRequest;
import gnmi.Gnmi.GetResponse;
import gnmi.Gnmi.Path;
import gnmi.Gnmi.PathElem;
import gnmi.Gnmi.SetRequest;
import gnmi.Gnmi.SetResponse;
import gnmi.Gnmi.SubscribeRequest;
import gnmi.gNMIGrpc;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.onosproject.gnmi.api.GnmiClient;
import org.onosproject.grpc.ctl.AbstractGrpcClient;
import org.onosproject.net.DeviceId;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Implementation of gNMI client.
 */
public class GnmiClientImpl extends AbstractGrpcClient implements GnmiClient {

    private static final int RPC_TIMEOUT_SECONDS = 10;

    private static final GetRequest PING_REQUEST = GetRequest.newBuilder().addPath(
            Path.newBuilder().addElem(
                    PathElem.newBuilder().setName("onos-gnmi-ping").build()
            ).build()).build();

    private GnmiSubscriptionManager subscribeManager;

    GnmiClientImpl(DeviceId deviceId, ManagedChannel managedChannel,
                   GnmiControllerImpl controller) {
        super(deviceId, managedChannel, false, controller);
        this.subscribeManager =
                new GnmiSubscriptionManager(this, deviceId, controller);
    }

    @Override
    public CompletableFuture<CapabilityResponse> capabilities() {
        final CompletableFuture<CapabilityResponse> future = new CompletableFuture<>();
        execRpc(s -> s.capabilities(
                CapabilityRequest.getDefaultInstance(),
                unaryObserver(future, CapabilityResponse.getDefaultInstance(),
                              "capabilities request"))
        );
        return future;
    }

    @Override
    public CompletableFuture<GetResponse> get(GetRequest request) {
        final CompletableFuture<GetResponse> future = new CompletableFuture<>();
        execRpc(s -> s.get(request, unaryObserver(
                future, GetResponse.getDefaultInstance(), "GET"))
        );
        return future;
    }

    @Override
    public CompletableFuture<SetResponse> set(SetRequest request) {
        final CompletableFuture<SetResponse> future = new CompletableFuture<>();
        execRpc(s -> s.set(request, unaryObserver(
                future, SetResponse.getDefaultInstance(), "SET"))
        );
        return future;
    }

    private <RES> StreamObserver<RES> unaryObserver(
            final CompletableFuture<RES> future,
            final RES defaultResponse,
            final String opDescription) {
        return new StreamObserver<RES>() {
            @Override
            public void onNext(RES value) {
                future.complete(value);
            }

            @Override
            public void onError(Throwable t) {
                handleRpcError(t, opDescription);
                future.complete(defaultResponse);
            }

            @Override
            public void onCompleted() {
                // Ignore. Unary call.
            }
        };
    }

    @Override
    public void subscribe(SubscribeRequest request) {
        subscribeManager.subscribe(request);
    }

    @Override
    public void unsubscribe() {
        subscribeManager.unsubscribe();
    }

    @Override
    public CompletableFuture<Boolean> probeService() {
        final CompletableFuture<Boolean> future = new CompletableFuture<>();
        final StreamObserver<GetResponse> responseObserver = new StreamObserver<GetResponse>() {
            @Override
            public void onNext(GetResponse value) {
                future.complete(true);
            }

            @Override
            public void onError(Throwable t) {
                // This gRPC call should throw INVALID_ARGUMENT status exception
                // since "/onos-gnmi-ping" path does not exists in any config
                // model For other status code such as UNIMPLEMENT, means the
                // gNMI service is not available on the device.
                future.complete(Status.fromThrowable(t).getCode()
                                        == Status.Code.INVALID_ARGUMENT);
            }

            @Override
            public void onCompleted() {
                // Ignore. Unary call.
            }
        };
        execRpc(s -> s.get(PING_REQUEST, responseObserver));
        return future;
    }

    @Override
    public void shutdown() {
        subscribeManager.shutdown();
        super.shutdown();
    }

    /**
     * Forces execution of an RPC in a cancellable context with a timeout.
     *
     * @param stubConsumer P4Runtime stub consumer
     */
    private void execRpc(Consumer<gNMIGrpc.gNMIStub> stubConsumer) {
        if (log.isTraceEnabled()) {
            log.trace("Executing RPC with timeout {} seconds (context deadline {})...",
                      RPC_TIMEOUT_SECONDS, context().getDeadline());
        }
        runInCancellableContext(() -> stubConsumer.accept(
                gNMIGrpc.newStub(channel)
                        .withDeadlineAfter(RPC_TIMEOUT_SECONDS, TimeUnit.SECONDS)));
    }

    /**
     * Forces execution of an RPC in a cancellable context with no timeout.
     *
     * @param stubConsumer P4Runtime stub consumer
     */
    void execRpcNoTimeout(Consumer<gNMIGrpc.gNMIStub> stubConsumer) {
        if (log.isTraceEnabled()) {
            log.trace("Executing RPC with no timeout (context deadline {})...",
                      context().getDeadline());
        }
        runInCancellableContext(() -> stubConsumer.accept(
                gNMIGrpc.newStub(channel)));
    }
}
