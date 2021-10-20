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

import com.google.common.util.concurrent.Futures;
import com.google.protobuf.TextFormat;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.runtime.PiEntity;
import org.onosproject.net.pi.runtime.PiHandle;
import org.onosproject.p4runtime.api.P4RuntimeWriteClient;
import org.onosproject.p4runtime.ctl.codec.CodecException;
import org.slf4j.Logger;
import p4.v1.P4RuntimeOuterClass;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.onosproject.p4runtime.api.P4RuntimeWriteClient.EntityUpdateStatus.PENDING;
import static org.onosproject.p4runtime.ctl.client.P4RuntimeClientImpl.SHORT_TIMEOUT_SECONDS;
import static org.onosproject.p4runtime.ctl.codec.Codecs.CODECS;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Handles the creation of P4Runtime WriteRequest and execution of the Write RPC
 * on the server.
 */
final class WriteRequestImpl implements P4RuntimeWriteClient.WriteRequest {

    private static final Logger log = getLogger(WriteRequestImpl.class);

    private static final P4RuntimeOuterClass.WriteResponse P4RT_DEFAULT_WRITE_RESPONSE_MSG =
            P4RuntimeOuterClass.WriteResponse.getDefaultInstance();

    private final P4RuntimeClientImpl client;
    private final PiPipeconf pipeconf;
    private final AtomicBoolean submitted = new AtomicBoolean(false);
    // The P4Runtime WriteRequest protobuf message we need to populate.
    private final P4RuntimeOuterClass.WriteRequest.Builder requestMsg;
    // WriteResponse instance builder. We populate entity responses as we add new
    // entities to this request. The status of each entity response will be
    // set once we receive a response from the device.
    private final WriteResponseImpl.Builder responseBuilder;

    WriteRequestImpl(P4RuntimeClientImpl client, long p4DeviceId, PiPipeconf pipeconf) {
        this.client = checkNotNull(client);
        this.pipeconf = checkNotNull(pipeconf);
        this.requestMsg = P4RuntimeOuterClass.WriteRequest.newBuilder()
                .setDeviceId(p4DeviceId);
        this.responseBuilder = WriteResponseImpl.builder(client.deviceId());
    }

    @Override
    public P4RuntimeWriteClient.WriteRequest withAtomicity(
            P4RuntimeWriteClient.Atomicity atomicity) {
        checkNotNull(atomicity);
        switch (atomicity) {
            case CONTINUE_ON_ERROR:
                requestMsg.setAtomicity(
                        P4RuntimeOuterClass.WriteRequest.Atomicity.CONTINUE_ON_ERROR);
                break;
            case ROLLBACK_ON_ERROR:
            case DATAPLANE_ATOMIC:
                // Supporting this while allowing codec exceptions to be
                // reported as write responses can be tricky. Assuming write on
                // device succeed but we have a codec exception and
                // atomicity is rollback on error.
            default:
                throw new UnsupportedOperationException(format(
                        "Atomicity mode %s not supported", atomicity));
        }
        return this;
    }

    @Override
    public P4RuntimeWriteClient.WriteRequest insert(PiEntity entity) {
        return entity(entity, P4RuntimeWriteClient.UpdateType.INSERT);
    }

    @Override
    public P4RuntimeWriteClient.WriteRequest insert(
            Iterable<? extends PiEntity> entities) {
        return entities(entities, P4RuntimeWriteClient.UpdateType.INSERT);
    }

    @Override
    public P4RuntimeWriteClient.WriteRequest modify(PiEntity entity) {
        return entity(entity, P4RuntimeWriteClient.UpdateType.MODIFY);
    }

    @Override
    public P4RuntimeWriteClient.WriteRequest modify(
            Iterable<? extends PiEntity> entities) {
        return entities(entities, P4RuntimeWriteClient.UpdateType.MODIFY);
    }

    @Override
    public P4RuntimeWriteClient.WriteRequest delete(
            Iterable<? extends PiHandle> handles) {
        checkNotNull(handles);
        handles.forEach(this::delete);
        return this;
    }

    @Override
    public P4RuntimeWriteClient.WriteRequest entities(
            Iterable<? extends PiEntity> entities,
            P4RuntimeWriteClient.UpdateType updateType) {
        checkNotNull(entities);
        entities.forEach(e -> this.entity(e, updateType));
        return this;
    }

    @Override
    public P4RuntimeWriteClient.WriteRequest entity(
            PiEntity entity, P4RuntimeWriteClient.UpdateType updateType) {
        checkNotNull(entity);
        checkNotNull(updateType);
        appendToRequestMsg(updateType, entity, entity.handle(client.deviceId()));
        return this;
    }

    @Override
    public P4RuntimeWriteClient.WriteRequest delete(PiHandle handle) {
        checkNotNull(handle);
        appendToRequestMsg(P4RuntimeWriteClient.UpdateType.DELETE, null, handle);
        return this;
    }

    @Override
    public P4RuntimeWriteClient.WriteResponse submitSync() {
        return Futures.getUnchecked(submit());
    }

    @Override
    public Collection<P4RuntimeWriteClient.EntityUpdateRequest> pendingUpdates() {
        return responseBuilder.pendingUpdates();
    }

    @Override
    public CompletableFuture<P4RuntimeWriteClient.WriteResponse> submit() {
        checkState(!submitted.getAndSet(true),
                   "Request has already been submitted, cannot submit again");
        final P4RuntimeOuterClass.WriteRequest writeRequest = requestMsg
                .setElectionId(client.lastUsedElectionId(
                        requestMsg.getDeviceId()))
                .build();
        log.debug("Sending write request to {} with {} updates...",
                  client.deviceId(), writeRequest.getUpdatesCount());
        if (writeRequest.getUpdatesCount() == 0) {
            // No need to ask the server.
            return completedFuture(responseBuilder.buildAsIs());
        }
        final CompletableFuture<P4RuntimeWriteClient.WriteResponse> future =
                new CompletableFuture<>();
        final StreamObserver<P4RuntimeOuterClass.WriteResponse> observer =
                new StreamObserver<P4RuntimeOuterClass.WriteResponse>() {
                    @Override
                    public void onNext(P4RuntimeOuterClass.WriteResponse value) {
                        if (!P4RT_DEFAULT_WRITE_RESPONSE_MSG.equals(value)) {
                            log.warn("Received invalid WriteResponse message from {}: {}",
                                     client.deviceId(), TextFormat.shortDebugString(value));
                            // Leave all entity responses in pending state.
                            future.complete(responseBuilder.buildAsIs());
                        } else {
                            log.debug("Received write response from {}...",
                                      client.deviceId());
                            // All good, all entities written successfully.
                            future.complete(responseBuilder.setSuccessAllAndBuild());
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        final WriteResponseImpl response = responseBuilder
                                .setErrorsAndBuild(t);
                        if (Status.fromThrowable(t).getCode() != Status.Code.UNKNOWN
                                || !response.status(PENDING).isEmpty()) {
                            // If UNKNOWN and no entities are in PENDING state,
                            // it means we have processed the response error
                            // details and a log message will be produced for
                            // each failed entity. No need to log the top level
                            // SRE. Otherwise, log a generic WRITE error.
                            client.handleRpcError(t, "WRITE");
                        }
                        future.complete(response);
                    }

                    @Override
                    public void onCompleted() {
                        // Nothing to do, unary call.
                    }
                };
        client.execRpc(s -> s.write(writeRequest, observer), SHORT_TIMEOUT_SECONDS);
        return future;
    }

    private void appendToRequestMsg(P4RuntimeWriteClient.UpdateType updateType,
                                    PiEntity piEntity, PiHandle handle) {
        checkState(!submitted.get(),
                   "Request has already been submitted, cannot add more entities");
        final P4RuntimeOuterClass.Update.Type p4UpdateType;
        final P4RuntimeOuterClass.Entity entityMsg;
        try {
            if (updateType.equals(P4RuntimeWriteClient.UpdateType.DELETE)) {
                p4UpdateType = P4RuntimeOuterClass.Update.Type.DELETE;
                entityMsg = CODECS.handle().encode(handle, null, pipeconf);
            } else {
                p4UpdateType = updateType == P4RuntimeWriteClient.UpdateType.INSERT
                        ? P4RuntimeOuterClass.Update.Type.INSERT
                        : P4RuntimeOuterClass.Update.Type.MODIFY;
                entityMsg = CODECS.entity().encode(piEntity, null, pipeconf);
            }
            final P4RuntimeOuterClass.Update updateMsg = P4RuntimeOuterClass.Update
                    .newBuilder()
                    .setEntity(entityMsg)
                    .setType(p4UpdateType)
                    .build();
            requestMsg.addUpdates(updateMsg);
            responseBuilder.addPendingResponse(handle, piEntity, updateType);
            if (log.isTraceEnabled()) {
                log.trace("Adding {} update to write request for {}: {}", updateType, handle.deviceId(),
                        piEntity == null ? handle : piEntity);
            }
        } catch (CodecException e) {
            log.error("Failed to add {} to write request for {}: {}", updateType, handle.deviceId(), e.getMessage());
            responseBuilder.addFailedResponse(
                    handle, piEntity, updateType, e.getMessage(),
                    P4RuntimeWriteClient.EntityUpdateStatus.CODEC_ERROR);
        }
    }
}
