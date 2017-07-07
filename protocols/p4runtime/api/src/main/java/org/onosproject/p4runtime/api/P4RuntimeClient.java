/*
 * Copyright 2017-present Open Networking Laboratory
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

import com.google.common.annotations.Beta;
import org.onosproject.net.pi.runtime.PiTableEntry;
import org.onosproject.net.pi.runtime.PiTableId;

import java.io.InputStream;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * Client to control a P4Runtime device.
 */
@Beta
public interface P4RuntimeClient {

    /**
     * Type of write operation.
     */
    enum WriteOperationType {
        UNSPECIFIED,
        INSERT,
        UPDATE,
        DELETE
    }

    /**
     * Sets the pipeline configuration. This method should be called before any other method of this client.
     *
     * @param p4Info       input stream of a P4Info message in text format
     * @param targetConfig input stream of the target-specific configuration (e.g. BMv2 JSON)
     * @return a completable future of a boolean, true if the operations was successful, false otherwise.
     */
    CompletableFuture<Boolean> setPipelineConfig(InputStream p4Info, InputStream targetConfig);

    /**
     * Initializes the stream channel, after which all messages received from the device will be notified using the
     * {@link P4RuntimeController} event listener.
     *
     * @return a completable future of a boolean, true if the operations was successful, false otherwise.
     */
    CompletableFuture<Boolean> initStreamChannel();

    /**
     * Performs the given write operation for the given table entries.
     *
     * @param entries table entries
     * @param opType  operation type.
     * @return true if the operation was successful, false otherwise.
     */
    boolean writeTableEntries(Collection<PiTableEntry> entries, WriteOperationType opType);

    /**
     * Dumps all entries currently installed in the given table.
     *
     * @param tableId table identifier
     * @return completable future of a collection of table entries
     */
    CompletableFuture<Collection<PiTableEntry>> dumpTable(PiTableId tableId);

    /**
     * Shutdown the client by terminating any active RPC such as the stream channel.
     */
    void shutdown();

    // TODO: work in progress.
}
