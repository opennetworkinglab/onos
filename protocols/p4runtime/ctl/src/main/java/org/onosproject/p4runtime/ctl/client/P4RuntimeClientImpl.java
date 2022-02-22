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

import com.google.common.collect.Maps;
import io.grpc.ConnectivityState;
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
import org.onosproject.p4runtime.ctl.controller.MasterElectionIdStore;
import org.onosproject.p4runtime.ctl.controller.P4RuntimeControllerImpl;
import p4.v1.P4RuntimeGrpc;
import p4.v1.P4RuntimeOuterClass;
import p4.v1.P4RuntimeOuterClass.GetForwardingPipelineConfigRequest;
import p4.v1.P4RuntimeOuterClass.GetForwardingPipelineConfigResponse;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;
import static p4.v1.P4RuntimeOuterClass.GetForwardingPipelineConfigRequest.ResponseType.COOKIE_ONLY;

/**
 * Implementation of P4RuntimeClient.
 */
public final class P4RuntimeClientImpl
        extends AbstractGrpcClient implements P4RuntimeClient {

    private static final long DEFAULT_P4_DEVICE_ID = 1;

    // TODO: consider making timeouts configurable per-device via netcfg
    // We have measured that some devices can take up to 15s to push a pipeline
    // which can block potentially other READ done against the target.
    /**
     * Timeout in seconds for short/fast RPCs.
     */
    static final int SHORT_TIMEOUT_SECONDS = 15;
    /**
     * Timeout in seconds for RPCs that involve transfer of potentially large
     * amount of data. This shoulld be long enough to allow for network delay
     * (e.g. to transfer large pipeline binaries over slow network).
     */
    static final int LONG_TIMEOUT_SECONDS = 60;

    private final P4RuntimeControllerImpl controller;
    private final PipelineConfigClientImpl pipelineConfigClient;
    private final PiPipeconfService pipeconfService;
    private final MasterElectionIdStore masterElectionIdStore;
    private final ConcurrentMap<Long, StreamClientImpl> streamClients = Maps.newConcurrentMap();

    /**
     * Instantiates a new client with the given arguments.
     *
     * @param deviceId              device ID
     * @param channel               gRPC managed channel
     * @param controller            P4Runtime controller instance
     * @param pipeconfService       pipeconf service instance
     * @param masterElectionIdStore master election ID store
     */
    public P4RuntimeClientImpl(DeviceId deviceId,
                               ManagedChannel channel,
                               P4RuntimeControllerImpl controller,
                               PiPipeconfService pipeconfService,
                               MasterElectionIdStore masterElectionIdStore) {
        super(deviceId, channel, true, controller);
        checkNotNull(channel);
        checkNotNull(controller);
        checkNotNull(pipeconfService);
        checkNotNull(masterElectionIdStore);

        this.controller = controller;
        this.pipeconfService = pipeconfService;
        this.masterElectionIdStore = masterElectionIdStore;
        this.pipelineConfigClient = new PipelineConfigClientImpl(this);
    }

    @Override
    public void shutdown() {
        streamClients.forEach((p4DeviceId, streamClient) ->
                                      streamClient.closeSession(p4DeviceId));
        super.shutdown();
    }

    @Override
    public CompletableFuture<Boolean> setPipelineConfig(
            long p4DeviceId, PiPipeconf pipeconf, ByteBuffer deviceData) {
        return pipelineConfigClient.setPipelineConfig(p4DeviceId, pipeconf, deviceData);
    }

    @Override
    public CompletableFuture<Boolean> isPipelineConfigSet(
            long p4DeviceId, PiPipeconf pipeconf) {
        return pipelineConfigClient.isPipelineConfigSet(p4DeviceId, pipeconf);
    }

    @Override
    public CompletableFuture<Boolean> isAnyPipelineConfigSet(long p4DeviceId) {
        return pipelineConfigClient.isAnyPipelineConfigSet(p4DeviceId);
    }

    @Override
    public ReadRequest read(long p4DeviceId, PiPipeconf pipeconf) {
        return new ReadRequestImpl(this, p4DeviceId, pipeconf);
    }

    @Override
    public boolean isSessionOpen(long p4DeviceId) {
        return streamClients.containsKey(p4DeviceId) &&
                streamClients.get(p4DeviceId).isSessionOpen(p4DeviceId);
    }

    @Override
    public void closeSession(long p4DeviceId) {
        if (streamClients.containsKey(p4DeviceId)) {
            streamClients.get(p4DeviceId).closeSession(p4DeviceId);
        }
    }

    @Override
    public void setMastership(long p4DeviceId, boolean master, BigInteger newElectionId) {
        streamClients.putIfAbsent(p4DeviceId, new StreamClientImpl(
                pipeconfService, masterElectionIdStore, this, p4DeviceId, controller));
        streamClients.get(p4DeviceId).setMastership(p4DeviceId, master, newElectionId);
    }

    @Override
    public boolean isMaster(long p4DeviceId) {
        return streamClients.containsKey(p4DeviceId) &&
                streamClients.get(p4DeviceId).isMaster(p4DeviceId);
    }

    @Override
    public void packetOut(long p4DeviceId, PiPacketOperation packet, PiPipeconf pipeconf) {
        if (streamClients.containsKey(p4DeviceId)) {
            streamClients.get(p4DeviceId).packetOut(p4DeviceId, packet, pipeconf);
        }
    }

    @Override
    public WriteRequest write(long p4DeviceId, PiPipeconf pipeconf) {
        return new WriteRequestImpl(this, p4DeviceId, pipeconf);
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
                        log.debug("", t);
                        // FIXME: The P4Runtime spec is not explicit about error
                        //  codes when a pipeline config is not set, which would
                        //  be useful here as it's an indication that the
                        //  service is available. As a workaround, we simply
                        //  check the channel state.
                        future.complete(ConnectivityState.READY.equals(
                                channel.getState(false)));
                    }

                    @Override
                    public void onCompleted() {
                        // Ignore, unary call.
                    }
                };
        // Get any p4DeviceId under the control of this client or a default one.
        final long p4DeviceId = streamClients.isEmpty() ? DEFAULT_P4_DEVICE_ID
                : streamClients.keySet().iterator().next();
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

    @Override
    protected void handleRpcError(Throwable throwable, String opDescription) {
        if (throwable instanceof StatusRuntimeException) {
            checkGrpcException((StatusRuntimeException) throwable);
        }
        super.handleRpcError(throwable, opDescription);
    }

    private void checkGrpcException(StatusRuntimeException sre) {
        if (sre.getStatus().getCode() == Status.Code.PERMISSION_DENIED) {
            // Notify upper layers that this node is not master.
            controller.postEvent(new DeviceAgentEvent(
                    DeviceAgentEvent.Type.NOT_MASTER, deviceId));
        }
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
     * sent by the client to the server for the given P4Runtime-internal device
     * ID. No guarantees are given that this is the current election ID
     * associated to the session, nor that the server has acknowledged this
     * value as valid.
     *
     * @param p4DeviceId P4Runtime-internal device ID
     * @return election ID uint128 protobuf message
     */
    P4RuntimeOuterClass.Uint128 lastUsedElectionId(long p4DeviceId) {
        if (streamClients.containsKey(p4DeviceId)) {
            return streamClients.get(p4DeviceId).lastUsedElectionId();
        } else {
            return P4RuntimeOuterClass.Uint128.getDefaultInstance();
        }
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
}
