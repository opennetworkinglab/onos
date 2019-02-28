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

package org.onosproject.p4runtime.ctl.client;

import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.onosproject.grpc.ctl.AbstractGrpcClient;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceAgentEvent;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.runtime.PiPacketOperation;
import org.onosproject.net.pi.service.PiPipeconfService;
import org.onosproject.p4runtime.api.P4RuntimeClient;
import org.onosproject.p4runtime.api.P4RuntimeClientKey;
import org.onosproject.p4runtime.ctl.controller.MasterElectionIdStore;
import org.onosproject.p4runtime.ctl.controller.P4RuntimeControllerImpl;
import p4.v1.P4RuntimeGrpc;
import p4.v1.P4RuntimeOuterClass;
import p4.v1.P4RuntimeOuterClass.GetForwardingPipelineConfigRequest;
import p4.v1.P4RuntimeOuterClass.GetForwardingPipelineConfigResponse;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static p4.v1.P4RuntimeOuterClass.GetForwardingPipelineConfigRequest.ResponseType.COOKIE_ONLY;

/**
 * Implementation of P4RuntimeClient.
 */
public final class P4RuntimeClientImpl
        extends AbstractGrpcClient implements P4RuntimeClient {

    // TODO: consider making timeouts configurable per-device via netcfg
    /**
     * Timeout in seconds for short/fast RPCs.
     */
    static final int SHORT_TIMEOUT_SECONDS = 10;
    /**
     * Timeout in seconds for RPCs that involve transfer of potentially large
     * amount of data. This shoulld be long enough to allow for network delay
     * (e.g. to transfer large pipeline binaries over slow network).
     */
    static final int LONG_TIMEOUT_SECONDS = 60;

    private final long p4DeviceId;
    private final P4RuntimeControllerImpl controller;
    private final StreamClientImpl streamClient;
    private final PipelineConfigClientImpl pipelineConfigClient;

    /**
     * Instantiates a new client with the given arguments.
     *
     * @param clientKey             client key
     * @param channel               gRPC managed channel
     * @param controller            P$Runtime controller instance
     * @param pipeconfService       pipeconf service instance
     * @param masterElectionIdStore master election ID store
     */
    public P4RuntimeClientImpl(P4RuntimeClientKey clientKey,
                               ManagedChannel channel,
                               P4RuntimeControllerImpl controller,
                               PiPipeconfService pipeconfService,
                               MasterElectionIdStore masterElectionIdStore) {
        super(clientKey, channel, true, controller);
        checkNotNull(channel);
        checkNotNull(controller);
        checkNotNull(pipeconfService);
        checkNotNull(masterElectionIdStore);

        this.p4DeviceId = clientKey.p4DeviceId();
        this.controller = controller;
        this.streamClient = new StreamClientImpl(
                pipeconfService, masterElectionIdStore, this, controller);
        this.pipelineConfigClient = new PipelineConfigClientImpl(this);
    }

    @Override
    protected Void doShutdown() {
        streamClient.closeSession();
        return super.doShutdown();
    }

    @Override
    public CompletableFuture<Boolean> setPipelineConfig(
            PiPipeconf pipeconf, ByteBuffer deviceData) {
        return pipelineConfigClient.setPipelineConfig(pipeconf, deviceData);
    }

    @Override
    public CompletableFuture<Boolean> isPipelineConfigSet(
            PiPipeconf pipeconf, ByteBuffer deviceData) {
        return pipelineConfigClient.isPipelineConfigSet(pipeconf, deviceData);
    }

    @Override
    public CompletableFuture<Boolean> isAnyPipelineConfigSet() {
        return pipelineConfigClient.isAnyPipelineConfigSet();
    }

    @Override
    public ReadRequest read(PiPipeconf pipeconf) {
        return new ReadRequestImpl(this, pipeconf);
    }

    @Override
    public boolean isSessionOpen() {
        return streamClient.isSessionOpen();
    }

    @Override
    public void closeSession() {
        streamClient.closeSession();
    }

    @Override
    public void setMastership(boolean master, BigInteger newElectionId) {
        streamClient.setMastership(master, newElectionId);
    }

    @Override
    public boolean isMaster() {
        return streamClient.isMaster();
    }

    @Override
    public void packetOut(PiPacketOperation packet, PiPipeconf pipeconf) {
        streamClient.packetOut(packet, pipeconf);
    }

    @Override
    public WriteRequest write(PiPipeconf pipeconf) {
        return new WriteRequestImpl(this, pipeconf);
    }

    @Override
    public CompletableFuture<Boolean> probeService() {
        final CompletableFuture<Boolean> future = new CompletableFuture<>();
        final StreamObserver<GetForwardingPipelineConfigResponse> responseObserver =
                new StreamObserver<GetForwardingPipelineConfigResponse>() {
                    @Override
                    public void onNext(GetForwardingPipelineConfigResponse value) {
                        future.complete(true);
                    }

                    @Override
                    public void onError(Throwable t) {
                        if (Status.fromThrowable(t).getCode() ==
                                Status.Code.FAILED_PRECONDITION) {
                            // Pipeline not set but service is available.
                            future.complete(true);
                        } else {
                            log.debug("", t);
                        }
                        future.complete(false);
                    }

                    @Override
                    public void onCompleted() {
                        // Ignore, unary call.
                    }
                };
        // Use long timeout as the device might return the full P4 blob
        // (e.g. server does not support cookie), over a slow network.
        execRpc(s -> s.getForwardingPipelineConfig(
                GetForwardingPipelineConfigRequest.newBuilder()
                        .setDeviceId(p4DeviceId)
                        .setResponseType(COOKIE_ONLY)
                        .build(), responseObserver),
                SHORT_TIMEOUT_SECONDS);
        return future;
    }

    /**
     * Returns the P4Runtime-internal device ID associated with this client.
     *
     * @return P4Runtime-internal device ID
     */
    long p4DeviceId() {
        return this.p4DeviceId;
    }

    /**
     * Returns the ONOS device ID associated with this client.
     *
     * @return ONOS device ID
     */
    DeviceId deviceId() {
        return this.deviceId;
    }

    /**
     * Returns the election ID last used in a MasterArbitrationUpdate message
     * sent by the client to the server. No guarantees are given that this is
     * the current election ID associated to the session, nor that the server
     * has acknowledged this value as valid.
     *
     * @return election ID uint128 protobuf message
     */
    P4RuntimeOuterClass.Uint128 lastUsedElectionId() {
        return streamClient.lastUsedElectionId();
    }

    /**
     * Forces execution of an RPC in a cancellable context with the given
     * timeout (in seconds).
     *
     * @param stubConsumer P4Runtime stub consumer
     * @param timeout      timeout in seconds
     */
    void execRpc(Consumer<P4RuntimeGrpc.P4RuntimeStub> stubConsumer,
                 int timeout) {
        if (log.isTraceEnabled()) {
            log.trace("Executing RPC with timeout {} seconds (context deadline {})...",
                      timeout, context().getDeadline());
        }
        runInCancellableContext(() -> stubConsumer.accept(
                P4RuntimeGrpc.newStub(channel)
                        .withDeadlineAfter(timeout, TimeUnit.SECONDS)));
    }

    /**
     * Forces execution of an RPC in a cancellable context with no timeout.
     *
     * @param stubConsumer P4Runtime stub consumer
     */
    void execRpcNoTimeout(Consumer<P4RuntimeGrpc.P4RuntimeStub> stubConsumer) {
        if (log.isTraceEnabled()) {
            log.trace("Executing RPC with no timeout (context deadline {})...",
                      context().getDeadline());
        }
        runInCancellableContext(() -> stubConsumer.accept(
                P4RuntimeGrpc.newStub(channel)));
    }

    /**
     * Logs the error and checks it for any condition that might be of interest
     * for the controller.
     *
     * @param throwable     throwable
     * @param opDescription operation description for logging
     */
    void handleRpcError(Throwable throwable, String opDescription) {
        if (throwable instanceof StatusRuntimeException) {
            final StatusRuntimeException sre = (StatusRuntimeException) throwable;
            checkGrpcException(sre);
            final String logMsg;
            if (sre.getCause() == null) {
                logMsg = sre.getMessage();
            } else {
                logMsg = format("%s (%s)", sre.getMessage(), sre.getCause().toString());
            }
            log.warn("Error while performing {} on {}: {}",
                     opDescription, deviceId, logMsg);
            log.debug("", throwable);
            return;
        }
        log.error(format("Exception while performing %s on %s",
                         opDescription, deviceId), throwable);
    }

    private void checkGrpcException(StatusRuntimeException sre) {
        if (sre.getStatus().getCode() == Status.Code.PERMISSION_DENIED) {
            // Notify upper layers that this node is not master.
            controller.postEvent(new DeviceAgentEvent(
                    DeviceAgentEvent.Type.NOT_MASTER, deviceId));
        }
    }
}
