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

import org.onosproject.net.pi.model.PiActionProfileId;
import org.onosproject.net.pi.model.PiCounterId;
import org.onosproject.net.pi.model.PiMeterId;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.model.PiTableId;
import org.onosproject.net.pi.runtime.PiEntity;
import org.onosproject.net.pi.runtime.PiHandle;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * P4Runtime client interface for the Read RPC that allows reading multiple
 * entities with one request.
 */
public interface P4RuntimeReadClient {

    /**
     * Returns a new {@link ReadRequest} instance that can bed used to build a
     * batched read request, for the given P4Runtime-internal device ID and
     * pipeconf.
     *
     * @param p4DeviceId P4Runtime-internal device ID
     * @param pipeconf   pipeconf
     * @return new read request
     */
    ReadRequest read(long p4DeviceId, PiPipeconf pipeconf);

    /**
     * Abstraction of a batched P4Runtime read request. Multiple entities can be
     * added to the same request before submitting it.
     */
    interface ReadRequest {

        /**
         * Requests to read one entity identified by the given handle.
         *
         * @param handle handle
         * @return this
         */
        ReadRequest handle(PiHandle handle);

        /**
         * Requests to read multiple entities identified by the given handles.
         *
         * @param handles iterable of handles
         * @return this
         */
        ReadRequest handles(Iterable<? extends PiHandle> handles);

        /**
         * Requests to read all table entries from the given table ID.
         *
         * @param tableId table ID
         * @return this
         */
        ReadRequest tableEntries(PiTableId tableId);

        /**
         * Requests to read all table entries from the given table IDs.
         *
         * @param tableIds table IDs
         * @return this
         */
        ReadRequest tableEntries(Iterable<PiTableId> tableIds);

        /**
         * Requests to read the default table entry from the given table.
         *
         * @param tableId table ID
         * @return this
         */
        ReadRequest defaultTableEntry(PiTableId tableId);

        /**
         * Requests to read the default table entry from the given tables.
         *
         * @param tableIds table IDs
         * @return this
         */
        ReadRequest defaultTableEntry(Iterable<PiTableId> tableIds);

        /**
         * Requests to read all table entries from all tables.
         *
         * @return this
         */
        ReadRequest allTableEntries();

        /**
         * Requests to read all default table entries from all tables.
         *
         * @return this
         */
        ReadRequest allDefaultTableEntries();

        /**
         * Requests to read all action profile groups from the given action
         * profile.
         *
         * @param actionProfileId action profile ID
         * @return this
         */
        ReadRequest actionProfileGroups(PiActionProfileId actionProfileId);

        /**
         * Requests to read all action profile groups from the given action
         * profiles.
         *
         * @param actionProfileIds action profile IDs
         * @return this
         */
        ReadRequest actionProfileGroups(Iterable<PiActionProfileId> actionProfileIds);

        /**
         * Requests to read all action profile members from the given action
         * profile.
         *
         * @param actionProfileId action profile ID
         * @return this
         */
        ReadRequest actionProfileMembers(PiActionProfileId actionProfileId);

        /**
         * Requests to read all action profile members from the given action
         * profiles.
         *
         * @param actionProfileIds action profile IDs
         * @return this
         */
        ReadRequest actionProfileMembers(Iterable<PiActionProfileId> actionProfileIds);

        /**
         * Requests to read all counter cells from the given counter.
         *
         * @param counterId counter ID
         * @return this
         */
        ReadRequest counterCells(PiCounterId counterId);

        /**
         * Requests to read all counter cells from the given counters.
         *
         * @param counterIds counter IDs
         * @return this
         */
        ReadRequest counterCells(Iterable<PiCounterId> counterIds);

        /**
         * Requests to read all direct counter cells from the given table.
         *
         * @param tableId table ID
         * @return this
         */
        ReadRequest directCounterCells(PiTableId tableId);

        /**
         * Requests to read all direct counter cells from the given tables.
         *
         * @param tableIds table IDs
         * @return this
         */
        ReadRequest directCounterCells(Iterable<PiTableId> tableIds);

        /**
         * Requests to read all meter cell configs from the given meter ID.
         *
         * @param meterId meter ID
         * @return this
         */
        ReadRequest meterCells(PiMeterId meterId);

        /**
         * Requests to read all meter cell configs from the given meter IDs.
         *
         * @param meterIds meter IDs
         * @return this
         */
        ReadRequest meterCells(Iterable<PiMeterId> meterIds);

        /**
         * Requests to read all direct meter cell configs from the given table.
         *
         * @param tableId table ID
         * @return this
         */
        ReadRequest directMeterCells(PiTableId tableId);

        /**
         * Requests to read all direct meter cell configs from the given
         * tables.
         *
         * @param tableIds table IDs
         * @return this
         */
        ReadRequest directMeterCells(Iterable<PiTableId> tableIds);

        /**
         * Submits the read request and returns a read response wrapped in a
         * completable future. The future is completed once all entities have
         * been received by the P4Runtime client.
         *
         * @return completable future of a read response
         */
        CompletableFuture<ReadResponse> submit();

        /**
         * Similar to {@link #submit()}, but blocks until the operation is
         * completed, after which, it returns a read response.
         *
         * @return read response
         */
        ReadResponse submitSync();

        //TODO: implement per-entity asynchronous reads. This would allow a user
        // of this client to process read entities as they arrive, instead of
        // waiting for the client to receive them all. Java 9 Reactive Streams
        // seems a good way of doing it.
    }

    /**
     * Response to a P4Runtime read request.
     */
    interface ReadResponse {

        /**
         * Returns true if the request was successful with no errors, otherwise
         * returns false. In case of errors, further details can be obtained
         * with {@link #explanation()} and {@link #throwable()}.
         *
         * @return true if the request was successful with no errors, false
         * otherwise
         */
        boolean isSuccess();

        /**
         * Returns a collection of all PI entities returned by the server.
         *
         * @return collection of all PI entities returned by the server
         */
        Collection<PiEntity> all();

        /**
         * Returns a collection of all PI entities of a given class returned by
         * the server.
         *
         * @param clazz PI entity class
         * @param <E>   PI entity class
         * @return collection of all PI entities returned by the server for the
         * given PI entity class
         */
        <E extends PiEntity> Collection<E> all(Class<E> clazz);

        /**
         * If the read request was not successful, this method returns a message
         * explaining the error occurred. Returns an empty string if such
         * message is not available, or {@code null} if no errors occurred.
         *
         * @return error explanation or empty string or null
         */
        String explanation();

        /**
         * If the read request was not successful, this method returns the
         * internal throwable instance associated with the error (e.g. a {@link
         * io.grpc.StatusRuntimeException} instance). Returns null if such
         * throwable instance is not available or if no errors occurred.
         *
         * @return throwable instance
         */
        Throwable throwable();
    }
}
