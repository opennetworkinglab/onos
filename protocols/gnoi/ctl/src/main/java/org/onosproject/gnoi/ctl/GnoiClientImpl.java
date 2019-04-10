/*
 * Copyright 2019-present Open Networking Foundation
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

package org.onosproject.gnoi.ctl;

import gnoi.system.SystemGrpc;
import gnoi.system.SystemOuterClass.RebootRequest;
import gnoi.system.SystemOuterClass.RebootResponse;
import gnoi.system.SystemOuterClass.TimeRequest;
import gnoi.system.SystemOuterClass.TimeResponse;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import org.onosproject.gnoi.api.GnoiClient;
import org.onosproject.grpc.ctl.AbstractGrpcClient;
import org.onosproject.net.DeviceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Implementation of gNOI client.
 */
public class GnoiClientImpl extends AbstractGrpcClient implements GnoiClient {

    private static final int RPC_TIMEOUT_SECONDS = 10;
    private static final Logger log = LoggerFactory.getLogger(GnoiClientImpl.class);

    GnoiClientImpl(DeviceId deviceId, ManagedChannel managedChannel, GnoiControllerImpl controller) {
        super(deviceId, managedChannel, false, controller);
    }

    @Override
    public CompletableFuture<Boolean> probeService() {
        return this.time().handle((response, t) -> {
            if (t == null) {
                log.debug("gNOI probeService succeed");
                return true;
            } else {
                log.debug("gNOI probeService failed", t);
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<TimeResponse> time() {
        // The TimeRequest message is empty one so just form it
        final TimeRequest requestMsg = TimeRequest.getDefaultInstance();
        final CompletableFuture<TimeResponse> future = new CompletableFuture<>();

        final StreamObserver<TimeResponse> observer =
                new StreamObserver<TimeResponse>() {
                    @Override
                    public void onNext(TimeResponse value) {
                        future.complete(value);
                    }
                    @Override
                    public void onError(Throwable t) {
                        handleRpcError(t, "gNOI time request");
                        future.completeExceptionally(t);
                    }
                    @Override
                    public void onCompleted() {
                        // ignore
                    }
                };

        execRpc(s -> s.time(requestMsg, observer));
        return future;
    }

    @Override
    public CompletableFuture<RebootResponse> reboot(RebootRequest request) {
        final CompletableFuture<RebootResponse> future = new CompletableFuture<>();

        final StreamObserver<RebootResponse> observer =
                new StreamObserver<RebootResponse>() {
                    @Override
                    public void onNext(RebootResponse value) {
                        future.complete(value);
                    }
                    @Override
                    public void onError(Throwable t) {
                        handleRpcError(t, "gNOI reboot request");
                        future.completeExceptionally(t);
                    }
                    @Override
                    public void onCompleted() {
                        // ignore
                    }
                };

        execRpc(s -> s.reboot(request, observer));
        return future;
    }

    /**
     * Forces execution of an RPC in a cancellable context with a timeout.
     *
     * @param stubConsumer SystemStub stub consumer
     */
    private void execRpc(Consumer<SystemGrpc.SystemStub> stubConsumer) {
        if (log.isTraceEnabled()) {
            log.trace("Executing RPC with timeout {} seconds (context deadline {})...",
                    RPC_TIMEOUT_SECONDS, context().getDeadline());
        }
        runInCancellableContext(() -> stubConsumer.accept(
                SystemGrpc.newStub(channel)
                        .withDeadlineAfter(RPC_TIMEOUT_SECONDS, TimeUnit.SECONDS)));
    }
}
