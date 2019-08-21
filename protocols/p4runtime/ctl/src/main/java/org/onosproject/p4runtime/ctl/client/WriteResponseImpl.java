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

import com.google.common.base.MoreObjects;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.TextFormat;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.lite.ProtoLiteUtils;
import org.onosproject.net.DeviceId;
import org.onosproject.net.pi.runtime.PiEntity;
import org.onosproject.net.pi.runtime.PiEntityType;
import org.onosproject.net.pi.runtime.PiHandle;
import org.onosproject.p4runtime.api.P4RuntimeWriteClient;
import org.onosproject.p4runtime.api.P4RuntimeWriteClient.EntityUpdateRequest;
import org.onosproject.p4runtime.api.P4RuntimeWriteClient.EntityUpdateResponse;
import org.onosproject.p4runtime.api.P4RuntimeWriteClient.EntityUpdateStatus;
import org.onosproject.p4runtime.api.P4RuntimeWriteClient.UpdateType;
import org.slf4j.Logger;
import p4.v1.P4RuntimeOuterClass;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Handles the creation of WriteResponse and parsing of P4Runtime errors
 * received from server, as well as logging of RPC errors.
 */
final class WriteResponseImpl implements P4RuntimeWriteClient.WriteResponse {

    private static final Metadata.Key<com.google.rpc.Status> STATUS_DETAILS_KEY =
            Metadata.Key.of(
                    "grpc-status-details-bin",
                    ProtoLiteUtils.metadataMarshaller(
                            com.google.rpc.Status.getDefaultInstance()));

    static final WriteResponseImpl EMPTY = new WriteResponseImpl(
            ImmutableList.of(), ImmutableListMultimap.of());

    private static final Logger log = getLogger(WriteResponseImpl.class);

    private final ImmutableList<EntityUpdateResponse> entityResponses;
    private final ImmutableListMultimap<EntityUpdateStatus, EntityUpdateResponse> statusMultimap;

    private WriteResponseImpl(
            ImmutableList<EntityUpdateResponse> allResponses,
            ImmutableListMultimap<EntityUpdateStatus, EntityUpdateResponse> statusMultimap) {
        this.entityResponses = allResponses;
        this.statusMultimap = statusMultimap;
    }

    @Override
    public boolean isSuccess() {
        return success().size() == all().size();
    }

    @Override
    public Collection<EntityUpdateResponse> all() {
        return entityResponses;
    }

    @Override
    public Collection<EntityUpdateResponse> success() {
        return statusMultimap.get(EntityUpdateStatus.OK);
    }

    @Override
    public Collection<EntityUpdateResponse> failed() {
        return isSuccess()
                ? Collections.emptyList()
                : entityResponses.stream().filter(r -> !r.isSuccess()).collect(toList());
    }

    @Override
    public Collection<EntityUpdateResponse> status(
            EntityUpdateStatus status) {
        checkNotNull(status);
        return statusMultimap.get(status);
    }

    /**
     * Returns a new response builder for the given device.
     *
     * @param deviceId device ID
     * @return response builder
     */
    static Builder builder(DeviceId deviceId) {
        return new Builder(deviceId);
    }

    /**
     * Builder of P4RuntimeWriteResponseImpl.
     */
    static final class Builder {

        private final DeviceId deviceId;
        private final Map<Integer, EntityUpdateResponseImpl> pendingResponses =
                Maps.newHashMap();
        private final List<EntityUpdateResponse> allResponses =
                Lists.newArrayList();
        private final ListMultimap<EntityUpdateStatus, EntityUpdateResponse> statusMap =
                ArrayListMultimap.create();

        private Builder(DeviceId deviceId) {
            this.deviceId = deviceId;
        }

        void addPendingResponse(PiHandle handle, PiEntity entity, UpdateType updateType) {
            synchronized (this) {
                final EntityUpdateResponseImpl resp = new EntityUpdateResponseImpl(
                        handle, entity, updateType);
                allResponses.add(resp);
                pendingResponses.put(pendingResponses.size(), resp);
            }
        }

        void addFailedResponse(PiHandle handle, PiEntity entity, UpdateType updateType,
                               String explanation, EntityUpdateStatus status) {
            synchronized (this) {
                final EntityUpdateResponseImpl resp = new EntityUpdateResponseImpl(
                        handle, entity, updateType)
                        .withFailure(explanation, status);
                allResponses.add(resp);
            }
        }

        Collection<EntityUpdateRequest> pendingUpdates() {
            return ImmutableList.copyOf(pendingResponses.values());
        }

        WriteResponseImpl buildAsIs() {
            synchronized (this) {
                final long pendingCount = pendingResponses.values().stream()
                        .filter(r -> r.status() == EntityUpdateStatus.PENDING)
                        .count();
                if (pendingCount > 0) {
                    log.warn("Partial response from {}, {} of {} total " +
                                     "updates are still in status PENDING",
                             deviceId, pendingCount, allResponses.size());
                }
                return new WriteResponseImpl(
                        ImmutableList.copyOf(allResponses),
                        ImmutableListMultimap.copyOf(statusMap));
            }
        }

        WriteResponseImpl setSuccessAllAndBuild() {
            synchronized (this) {
                pendingResponses.values().forEach(this::doSetSuccess);
                return buildAsIs();
            }
        }

        WriteResponseImpl setFailAllAndBuild(Throwable throwable) {
            synchronized (this) {
                pendingResponses.values().forEach(r -> r.setFailure(throwable));
                return buildAsIs();
            }
        }

        WriteResponseImpl setErrorsAndBuild(Throwable throwable) {
            synchronized (this) {
                return doSetErrorsAndBuild(throwable);
            }
        }

        private void setSuccess(int index) {
            synchronized (this) {
                final EntityUpdateResponseImpl resp = pendingResponses.get(index);
                if (resp != null && resp.status == EntityUpdateStatus.PENDING) {
                    doSetSuccess(resp);
                } else {
                    log.error("Missing pending response at index {}", index);
                }
            }
        }

        private void doSetSuccess(EntityUpdateResponseImpl resp) {
            resp.setSuccess();
            statusMap.put(EntityUpdateStatus.OK, resp);
        }

        private void setFailure(int index,
                                String explanation,
                                EntityUpdateStatus status) {
            synchronized (this) {
                final EntityUpdateResponseImpl resp = pendingResponses.get(index);
                if (resp != null) {
                    resp.withFailure(explanation, status);
                    statusMap.put(status, resp);
                    log.warn("Unable to {} {} on {}: {} {} [{}]",
                             resp.updateType(),
                             resp.entityType().humanReadableName(),
                             deviceId,
                             status, explanation,
                             resp.entity() != null ? resp.entity() : resp.handle());
                } else {
                    log.error("Missing pending response at index {}", index);
                }
            }
        }

        private WriteResponseImpl doSetErrorsAndBuild(Throwable throwable) {
            if (!(throwable instanceof StatusRuntimeException)) {
                // Leave all entity responses in pending state.
                return setFailAllAndBuild(throwable);
            }
            final StatusRuntimeException sre = (StatusRuntimeException) throwable;
            if (sre.getStatus().getCode() != Status.Code.UNKNOWN) {
                // Error trailers expected only if status is UNKNOWN.
                return setFailAllAndBuild(throwable);
            }
            // Extract error details.
            if (!sre.getTrailers().containsKey(STATUS_DETAILS_KEY)) {
                log.warn("Cannot parse write error details from {}, " +
                                 "missing status trailers in StatusRuntimeException",
                         deviceId);
                return setFailAllAndBuild(throwable);
            }
            com.google.rpc.Status status = sre.getTrailers().get(STATUS_DETAILS_KEY);
            if (status == null) {
                log.warn("Cannot parse write error details from {}, " +
                                 "found NULL status trailers in StatusRuntimeException",
                         deviceId);
                return setFailAllAndBuild(throwable);
            }
            final boolean reconcilable = status.getDetailsList().size() == pendingResponses.size();
            // We expect one error for each entity...
            if (!reconcilable) {
                log.warn("Unable to reconcile write error details from {}, " +
                                 "sent {} updates, but server returned {} errors",
                         deviceId, pendingResponses.size(), status.getDetailsList().size());
            }
            // ...in the same order as in the request.
            int index = 0;
            for (Any any : status.getDetailsList()) {
                // Set response entities only if reconcilable, otherwise log.
                unpackP4Error(index, any, reconcilable);
                index += 1;
            }
            return buildAsIs();
        }

        private void unpackP4Error(int index, Any any, boolean reconcilable) {
            final P4RuntimeOuterClass.Error p4Error;
            try {
                p4Error = any.unpack(P4RuntimeOuterClass.Error.class);
            } catch (InvalidProtocolBufferException e) {
                final String unpackErr = format(
                        "P4Runtime Error message format not recognized [%s]",
                        TextFormat.shortDebugString(any));
                if (reconcilable) {
                    setFailure(index, unpackErr, EntityUpdateStatus.OTHER_ERROR);
                } else {
                    log.warn(unpackErr);
                }
                return;
            }
            // Map gRPC status codes to our WriteResponseStatus codes.
            final Status.Code p4Code = Status.fromCodeValue(
                    p4Error.getCanonicalCode()).getCode();
            final EntityUpdateStatus ourCode;
            switch (p4Code) {
                case OK:
                    if (reconcilable) {
                        setSuccess(index);
                    }
                    return;
                case NOT_FOUND:
                    ourCode = EntityUpdateStatus.NOT_FOUND;
                    break;
                case ALREADY_EXISTS:
                    ourCode = EntityUpdateStatus.ALREADY_EXIST;
                    break;
                default:
                    ourCode = EntityUpdateStatus.OTHER_ERROR;
                    break;
            }
            // Put the p4Code in the explanation only if ourCode is OTHER_ERROR.
            final String explanationCode = ourCode == EntityUpdateStatus.OTHER_ERROR
                    ? p4Code.name() + " " : "";
            final String details = p4Error.hasDetails()
                    ? ", " + p4Error.getDetails().toString() : "";
            final String explanation = format(
                    "%s%s%s (%s:%d)", explanationCode, p4Error.getMessage(),
                    details, p4Error.getSpace(), p4Error.getCode());
            if (reconcilable) {
                setFailure(index, explanation, ourCode);
            } else {
                log.warn("P4Runtime write error: {}", explanation);
            }
        }
    }

    /**
     * Internal implementation of EntityUpdateResponse.
     */
    private static final class EntityUpdateResponseImpl implements EntityUpdateResponse {

        private final PiHandle handle;
        private final PiEntity entity;
        private final UpdateType updateType;

        private EntityUpdateStatus status = EntityUpdateStatus.PENDING;
        private String explanation;
        private Throwable throwable;

        private EntityUpdateResponseImpl(PiHandle handle, PiEntity entity, UpdateType updateType) {
            this.handle = handle;
            this.entity = entity;
            this.updateType = updateType;
        }

        private EntityUpdateResponseImpl withFailure(
                String explanation, EntityUpdateStatus status) {
            setStatus(status);
            this.explanation = explanation;
            this.throwable = null;
            return this;
        }

        private void setSuccess() {
            setStatus(EntityUpdateStatus.OK);
        }

        private void setFailure(Throwable throwable) {
            setStatus(EntityUpdateStatus.OTHER_ERROR);
            this.explanation = throwable.toString();
            this.throwable = throwable;
        }

        private void setStatus(EntityUpdateStatus newStatus) {
            checkState(this.status == EntityUpdateStatus.PENDING,
                       "Cannot set status for non-pending update");
            checkArgument(newStatus  != EntityUpdateStatus.PENDING,
                          "newStatus must be different than pending");
            this.status = newStatus;
        }

        @Override
        public PiHandle handle() {
            return handle;
        }

        @Override
        public PiEntity entity() {
            return entity;
        }

        @Override
        public UpdateType updateType() {
            return updateType;
        }

        @Override
        public PiEntityType entityType() {
            return handle.entityType();
        }

        @Override
        public boolean isSuccess() {
            return status().equals(EntityUpdateStatus.OK);
        }

        @Override
        public EntityUpdateStatus status() {
            return status;
        }

        @Override
        public String explanation() {
            return explanation;
        }

        @Override
        public Throwable throwable() {
            return throwable;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("handle", handle)
                    .add("entity", entity)
                    .add("updateType", updateType)
                    .add("status", status)
                    .add("explanation", explanation)
                    .add("throwable", throwable)
                    .toString();
        }
    }
}
