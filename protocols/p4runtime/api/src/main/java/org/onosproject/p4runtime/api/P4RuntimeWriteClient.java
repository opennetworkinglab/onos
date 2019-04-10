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

package org.onosproject.p4runtime.api;

import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.runtime.PiEntity;
import org.onosproject.net.pi.runtime.PiEntityType;
import org.onosproject.net.pi.runtime.PiHandle;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * P4Runtime client interface for the Write RPC that allows inserting, modifying
 * and deleting PI entities. Allows batching of write requests and it returns a
 * detailed response for each PI entity in the request.
 */
public interface P4RuntimeWriteClient {

    /**
     * Returns a new {@link WriteRequest} instance that can be used to build a
     * batched write request, for the given P4Runtime-internal device ID and
     * pipeconf.
     *
     * @param p4DeviceId P4Runtime-internal device ID
     * @param pipeconf   pipeconf
     * @return new write request
     */
    WriteRequest write(long p4DeviceId, PiPipeconf pipeconf);

    /**
     * Signals the type of write operation for a given PI entity.
     */
    enum UpdateType {
        /**
         * Inserts an entity.
         */
        INSERT,
        /**
         * Modifies an existing entity.
         */
        MODIFY,
        /**
         * Deletes an existing entity.
         */
        DELETE
    }

    /**
     * Signals if the entity was written successfully or not.
     */
    enum EntityUpdateStatus {
        /**
         * The entity was written successfully, no errors occurred.
         */
        OK,
        /**
         * The server didn't return any status for the entity.
         */
        PENDING,
        /**
         * The entity was not added to the write request because it was not
         * possible to encode/decode it.
         */
        CODEC_ERROR,
        /**
         * Server responded that it was not possible to insert the entity as
         * another one with the same handle already exists.
         */
        ALREADY_EXIST,
        /**
         * Server responded that it was not possible to modify or delete the
         * entity as the same cannot be found on the server.
         */
        NOT_FOUND,
        /**
         * Other error. See {@link EntityUpdateResponse#explanation()} or {@link
         * EntityUpdateResponse#throwable()} for more details.
         */
        OTHER_ERROR,
    }

    /**
     * Signals the atomicity mode that the server should follow when executing a
     * write request. For more information on each atomicity mode please see the
     * P4Runtime spec.
     */
    enum Atomicity {
        /**
         * Continue on error. Default value for all write requests.
         */
        CONTINUE_ON_ERROR,
        /**
         * Rollback on error.
         */
        ROLLBACK_ON_ERROR,
        /**
         * Dataplane atomic.
         */
        DATAPLANE_ATOMIC,
    }

    /**
     * Abstraction of a batched P4Runtime write request. Multiple entities can
     * be added to the same request before submitting it. The implementation
     * should guarantee that entities are added in the final P4Runtime protobuf
     * message in the same order as added in this write request.
     */
    interface WriteRequest {

        /**
         * Sets the atomicity mode of this write request. Default value is
         * {@link Atomicity#CONTINUE_ON_ERROR}.
         *
         * @param atomicity atomicity mode
         * @return this
         */
        WriteRequest withAtomicity(Atomicity atomicity);

        /**
         * Requests to insert one PI entity.
         *
         * @param entity PI entity
         * @return this
         */
        WriteRequest insert(PiEntity entity);

        /**
         * Requests to insert multiple PI entities.
         *
         * @param entities iterable of PI entities
         * @return this
         */
        WriteRequest insert(Iterable<? extends PiEntity> entities);

        /**
         * Requests to modify one PI entity.
         *
         * @param entity PI entity
         * @return this
         */
        WriteRequest modify(PiEntity entity);

        /**
         * Requests to modify multiple PI entities.
         *
         * @param entities iterable of PI entities
         * @return this
         */
        WriteRequest modify(Iterable<? extends PiEntity> entities);

        /**
         * Requests to delete one PI entity identified by the given handle.
         *
         * @param handle PI handle
         * @return this
         */
        WriteRequest delete(PiHandle handle);

        /**
         * Requests to delete multiple PI entities identified by the given
         * handles.
         *
         * @param handles iterable of handles
         * @return this
         */
        WriteRequest delete(Iterable<? extends PiHandle> handles);

        /**
         * Requests to write the given PI entity with the given update type. If
         * {@code updateType} is {@link UpdateType#DELETE}, then only the handle
         * will be considered by the request.
         *
         * @param entity     PI entity
         * @param updateType update type
         * @return this
         */
        WriteRequest entity(PiEntity entity, UpdateType updateType);

        /**
         * Requests to write the given PI entities with the given update type.
         * If {@code updateType} is {@link UpdateType#DELETE}, then only the
         * handles will be considered by the request.
         *
         * @param entities   iterable of PI entity
         * @param updateType update type
         * @return this
         */
        WriteRequest entities(Iterable<? extends PiEntity> entities, UpdateType updateType);

        /**
         * Submits this write request to the server and returns a completable
         * future holding the response. The future is completed only after the
         * server signals that all entities are written.
         *
         * @return completable future of the write response
         */
        CompletableFuture<WriteResponse> submit();

        /**
         * Similar to {@link #submit()}, but blocks until the operation is
         * completed, after which, it returns a read response.
         *
         * @return read response
         */
        P4RuntimeWriteClient.WriteResponse submitSync();

        /**
         * Returns all entity update requests for which we are expecting a
         * responce from the device, in the same order they were added to this
         * batch.
         *
         * @return entity update requests
         */
        Collection<EntityUpdateRequest> pendingUpdates();
    }

    /**
     * Represents the update request for a specific entity.
     */
    interface EntityUpdateRequest {
        /**
         * Returns the handle of the PI entity subject of this update.
         *
         * @return handle
         */
        PiHandle handle();

        /**
         * Returns the PI entity subject of this update. Returns {@code null} if
         * the update type is {@link UpdateType#DELETE}, in which case only the
         * handle is used in the request.
         *
         * @return PI entity or null
         */
        PiEntity entity();

        /**
         * Returns the type of update requested for this entity.
         *
         * @return update type
         */
        UpdateType updateType();

        /**
         * Returns the type of entity subject of this update.
         *
         * @return PI entity type
         */
        PiEntityType entityType();
    }

    /**
     * Abstraction of a response obtained from a P4Runtime server after a write
     * request is submitted. It allows returning a detailed response ({@link
     * EntityUpdateResponse}) for each PI entity in the batched request. Entity
     * responses are guaranteed to be returned in the same order as the
     * corresponding PI entity in the request.
     */
    interface WriteResponse {

        /**
         * Returns true if all entities in the request were successfully
         * written. In other words, if no errors occurred. False otherwise.
         *
         * @return true if all entities were written successfully, false
         * otherwise
         */
        boolean isSuccess();

        /**
         * Returns a detailed response for each PI entity in the request. The
         * implementation of this method should guarantee that the returned
         * collection has size equal to the number of PI entities in the
         * original write request.
         *
         * @return collection of {@link EntityUpdateResponse}
         */
        Collection<EntityUpdateResponse> all();

        /**
         * Returns a detailed response for each PI entity that was successfully
         * written. If {@link #isSuccess()} is {@code true}, then this method is
         * expected to return the same values as {@link #all()}.
         *
         * @return collection of {@link EntityUpdateResponse}
         */
        Collection<EntityUpdateResponse> success();

        /**
         * Returns a detailed response for each PI entity for which the server
         * returned an error. If {@link #isSuccess()} is {@code true}, then this
         * method is expected to return an empty collection.
         *
         * @return collection of {@link EntityUpdateResponse}
         */
        Collection<EntityUpdateResponse> failed();

        /**
         * Returns a detailed response for each PI entity for which the server
         * returned the given status.
         *
         * @param status status
         * @return collection of {@link EntityUpdateResponse}
         */
        Collection<EntityUpdateResponse> status(EntityUpdateStatus status);
    }

    /**
     * Represents the response to an update request request for a specific PI
     * entity.
     */
    interface EntityUpdateResponse extends EntityUpdateRequest {

        /**
         * Returns true if this PI entity was written successfully, false
         * otherwise.
         *
         * @return true if this PI entity was written successfully, false
         * otherwise
         */
        boolean isSuccess();

        /**
         * Returns the status for this PI entity. If {@link #isSuccess()}
         * returns {@code true}, then this method is expected to return {@link
         * EntityUpdateStatus#OK}. If {@link EntityUpdateStatus#OTHER_ERROR} is
         * returned, further details might be provided in {@link #explanation()}
         * and {@link #throwable()}.
         *
         * @return status
         */
        EntityUpdateStatus status();

        /**
         * If the PI entity was NOT written successfully, this method returns a
         * message explaining the error occurred. Returns an empty string if
         * such message is not available, or {@code null} if no errors
         * occurred.
         *
         * @return error explanation or empty string or null
         */
        String explanation();

        /**
         * If the PI entity was NOT written successfully, this method returns
         * the internal throwable instance associated with the error (e.g. a
         * {@link io.grpc.StatusRuntimeException} instance). Returns null if
         * such throwable instance is not available or if no errors occurred.
         *
         * @return throwable instance associated with this PI entity
         */
        Throwable throwable();
    }
}
