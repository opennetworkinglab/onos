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
import io.grpc.stub.StreamObserver;
import org.onosproject.net.pi.model.PiActionProfileId;
import org.onosproject.net.pi.model.PiCounterId;
import org.onosproject.net.pi.model.PiMeterId;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.model.PiTableId;
import org.onosproject.net.pi.runtime.PiHandle;
import org.onosproject.p4runtime.api.P4RuntimeReadClient;
import org.onosproject.p4runtime.ctl.codec.CodecException;
import org.onosproject.p4runtime.ctl.utils.P4InfoBrowser;
import org.onosproject.p4runtime.ctl.utils.PipeconfHelper;
import org.slf4j.Logger;
import p4.v1.P4RuntimeOuterClass;

import java.util.concurrent.CompletableFuture;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.onosproject.p4runtime.ctl.client.P4RuntimeClientImpl.SHORT_TIMEOUT_SECONDS;
import static org.onosproject.p4runtime.ctl.codec.Codecs.CODECS;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Handles the creation of P4Runtime ReadRequest and execution of the Read RPC
 * on the server.
 */
public final class ReadRequestImpl implements P4RuntimeReadClient.ReadRequest {

    private static final Logger log = getLogger(ReadRequestImpl.class);

    private final P4RuntimeClientImpl client;
    private final PiPipeconf pipeconf;
    private final P4RuntimeOuterClass.ReadRequest.Builder requestMsg;

    ReadRequestImpl(P4RuntimeClientImpl client, long p4DeviceId, PiPipeconf pipeconf) {
        this.client = client;
        this.pipeconf = pipeconf;
        this.requestMsg = P4RuntimeOuterClass.ReadRequest.newBuilder()
                .setDeviceId(p4DeviceId);
    }

    @Override
    public P4RuntimeReadClient.ReadRequest handles(Iterable<? extends PiHandle> handles) {
        checkNotNull(handles);
        handles.forEach(this::handle);
        return this;
    }

    @Override
    public P4RuntimeReadClient.ReadRequest tableEntries(Iterable<PiTableId> tableIds) {
        checkNotNull(tableIds);
        tableIds.forEach(this::tableEntries);
        return this;
    }

    @Override
    public P4RuntimeReadClient.ReadRequest defaultTableEntry(Iterable<PiTableId> tableIds) {
        checkNotNull(tableIds);
        tableIds.forEach(this::defaultTableEntry);
        return this;
    }

    @Override
    public P4RuntimeReadClient.ReadRequest actionProfileGroups(Iterable<PiActionProfileId> actionProfileIds) {
        checkNotNull(actionProfileIds);
        actionProfileIds.forEach(this::actionProfileGroups);
        return this;
    }

    @Override
    public P4RuntimeReadClient.ReadRequest actionProfileMembers(Iterable<PiActionProfileId> actionProfileIds) {
        checkNotNull(actionProfileIds);
        actionProfileIds.forEach(this::actionProfileMembers);
        return this;
    }

    @Override
    public P4RuntimeReadClient.ReadRequest counterCells(Iterable<PiCounterId> counterIds) {
        checkNotNull(counterIds);
        counterIds.forEach(this::counterCells);
        return this;
    }

    @Override
    public P4RuntimeReadClient.ReadRequest directCounterCells(Iterable<PiTableId> tableIds) {
        checkNotNull(tableIds);
        tableIds.forEach(this::directCounterCells);
        return this;
    }

    @Override
    public P4RuntimeReadClient.ReadRequest meterCells(Iterable<PiMeterId> meterIds) {
        checkNotNull(meterIds);
        meterIds.forEach(this::meterCells);
        return this;
    }

    @Override
    public P4RuntimeReadClient.ReadRequest directMeterCells(Iterable<PiTableId> tableIds) {
        checkNotNull(tableIds);
        tableIds.forEach(this::directMeterCells);
        return this;
    }

    @Override
    public P4RuntimeReadClient.ReadRequest handle(PiHandle handle) {
        checkNotNull(handle);
        try {
            requestMsg.addEntities(CODECS.handle().encode(handle, null, pipeconf));
        } catch (CodecException e) {
            log.warn("Unable to read {} from {}: {} [{}]",
                     handle.entityType(), client.deviceId(), e.getMessage(), handle);
        }
        return this;
    }

    @Override
    public P4RuntimeReadClient.ReadRequest tableEntries(PiTableId tableId) {
        try {
            doTableEntry(tableId, false);
        } catch (InternalRequestException e) {
            log.warn("Unable to read entries for table '{}' from {}: {}",
                     tableId, client.deviceId(), e.getMessage());
        }
        return this;
    }

    @Override
    public P4RuntimeReadClient.ReadRequest defaultTableEntry(PiTableId tableId) {
        try {
            doTableEntry(tableId, true);
        } catch (InternalRequestException e) {
            log.warn("Unable to read default entry for table '{}' from {}: {}",
                     tableId, client.deviceId(), e.getMessage());
        }
        return this;
    }

    @Override
    public P4RuntimeReadClient.ReadRequest allTableEntries() {
        try {
            doTableEntry(null, false);
        } catch (InternalRequestException e) {
            log.warn("Unable to read entries for all tables from {}: {}",
                     client.deviceId(), e.getMessage());
        }
        return this;
    }

    @Override
    public P4RuntimeReadClient.ReadRequest allDefaultTableEntries() {
        try {
            doTableEntry(null, true);
        } catch (InternalRequestException e) {
            log.warn("Unable to read default entries for all tables from {}: {}",
                     client.deviceId(), e.getMessage());
        }
        return this;
    }

    @Override
    public P4RuntimeReadClient.ReadRequest actionProfileGroups(PiActionProfileId actionProfileId) {
        try {
            requestMsg.addEntities(
                    P4RuntimeOuterClass.Entity.newBuilder()
                            .setActionProfileGroup(
                                    P4RuntimeOuterClass.ActionProfileGroup.newBuilder()
                                            .setActionProfileId(
                                                    p4ActionProfileId(actionProfileId))
                                            .build())
                            .build());
        } catch (InternalRequestException e) {
            log.warn("Unable to read groups for action profile '{}' from {}: {}",
                     actionProfileId, client.deviceId(), e.getMessage());
        }
        return this;
    }

    @Override
    public P4RuntimeReadClient.ReadRequest actionProfileMembers(PiActionProfileId actionProfileId) {
        try {
            requestMsg.addEntities(
                    P4RuntimeOuterClass.Entity.newBuilder()
                            .setActionProfileMember(
                                    P4RuntimeOuterClass.ActionProfileMember.newBuilder()
                                            .setActionProfileId(
                                                    p4ActionProfileId(actionProfileId))
                                            .build())
                            .build());
        } catch (InternalRequestException e) {
            log.warn("Unable to read members for action profile '{}' from {}: {}",
                     actionProfileId, client.deviceId(), e.getMessage());
        }
        return this;
    }

    @Override
    public P4RuntimeReadClient.ReadRequest counterCells(PiCounterId counterId) {
        try {
            requestMsg.addEntities(
                    P4RuntimeOuterClass.Entity.newBuilder()
                            .setCounterEntry(
                                    P4RuntimeOuterClass.CounterEntry.newBuilder()
                                            .setCounterId(p4CounterId(counterId))
                                            .build())
                            .build());
        } catch (InternalRequestException e) {
            log.warn("Unable to read cells for counter '{}' from {}: {}",
                     counterId, client.deviceId(), e.getMessage());
        }
        return this;
    }

    @Override
    public P4RuntimeReadClient.ReadRequest meterCells(PiMeterId meterId) {
        try {
            requestMsg.addEntities(
                    P4RuntimeOuterClass.Entity.newBuilder()
                            .setMeterEntry(
                                    P4RuntimeOuterClass.MeterEntry.newBuilder()
                                            .setMeterId(p4MeterId(meterId))
                                            .build())
                            .build());
        } catch (InternalRequestException e) {
            log.warn("Unable to read cells for meter '{}' from {}: {}",
                     meterId, client.deviceId(), e.getMessage());
        }
        return this;
    }

    @Override
    public P4RuntimeReadClient.ReadRequest directCounterCells(PiTableId tableId) {
        try {
            requestMsg.addEntities(
                    P4RuntimeOuterClass.Entity.newBuilder()
                            .setDirectCounterEntry(
                                    P4RuntimeOuterClass.DirectCounterEntry.newBuilder()
                                            .setTableEntry(
                                                    P4RuntimeOuterClass.TableEntry
                                                            .newBuilder()
                                                            .setTableId(p4TableId(tableId))
                                                            .build())
                                            .build())
                            .build());
        } catch (InternalRequestException e) {
            log.warn("Unable to read direct counter cells for table '{}' from {}: {}",
                     tableId, client.deviceId(), e.getMessage());
        }
        return this;
    }

    @Override
    public P4RuntimeReadClient.ReadRequest directMeterCells(PiTableId tableId) {
        try {
            requestMsg.addEntities(
                    P4RuntimeOuterClass.Entity.newBuilder()
                            .setDirectMeterEntry(
                                    P4RuntimeOuterClass.DirectMeterEntry.newBuilder()
                                            .setTableEntry(
                                                    P4RuntimeOuterClass.TableEntry
                                                            .newBuilder()
                                                            .setTableId(p4TableId(tableId))
                                                            .build())
                                            .build())
                            .build());
        } catch (InternalRequestException e) {
            log.warn("Unable to read direct meter cells for table '{}' from {}: {}",
                     tableId, client.deviceId(), e.getMessage());
        }
        return this;
    }

    private void doTableEntry(PiTableId piTableId, boolean defaultEntries)
            throws InternalRequestException {

        final var builder = P4RuntimeOuterClass.TableEntry.newBuilder();

        builder.setIsDefaultAction(defaultEntries);
        if (piTableId == null) {
            builder.setCounterData(P4RuntimeOuterClass.CounterData.getDefaultInstance());
            builder.setMeterConfig(P4RuntimeOuterClass.MeterConfig.getDefaultInstance());
        } else {
            builder.setTableId(p4TableId(piTableId));
            if (tableHasCounters(piTableId)) {
                builder.setCounterData(P4RuntimeOuterClass.CounterData.getDefaultInstance());
            }
        }
        final var entityMsg = P4RuntimeOuterClass.Entity
                .newBuilder().setTableEntry(builder.build()).build();
        requestMsg.addEntities(entityMsg);
    }

    @Override
    public CompletableFuture<P4RuntimeReadClient.ReadResponse> submit() {
        final P4RuntimeOuterClass.ReadRequest readRequest = requestMsg.build();
        log.debug("Sending read request to {} for {} entities...",
                  client.deviceId(), readRequest.getEntitiesCount());
        if (readRequest.getEntitiesCount() == 0) {
            // No need to ask the server.
            return completedFuture(ReadResponseImpl.EMPTY);
        }
        final CompletableFuture<P4RuntimeReadClient.ReadResponse> future =
                new CompletableFuture<>();
        // Instantiate response builder and let stream observer populate it.
        final ReadResponseImpl.Builder responseBuilder =
                ReadResponseImpl.builder(client.deviceId(), pipeconf);
        final StreamObserver<P4RuntimeOuterClass.ReadResponse> observer =
                new StreamObserver<P4RuntimeOuterClass.ReadResponse>() {
                    @Override
                    public void onNext(P4RuntimeOuterClass.ReadResponse value) {
                        log.debug("Received read response from {} with {} entities...",
                                  client.deviceId(), value.getEntitiesCount());
                        value.getEntitiesList().forEach(responseBuilder::addEntity);
                    }
                    @Override
                    public void onError(Throwable t) {
                        client.handleRpcError(t, "READ");
                        // TODO: implement parsing of trailer errors
                        future.complete(responseBuilder.fail(t));
                    }
                    @Override
                    public void onCompleted() {
                        future.complete(responseBuilder.build());
                    }
                };
        client.execRpc(s -> s.read(readRequest, observer), SHORT_TIMEOUT_SECONDS);
        return future;
    }

    @Override
    public P4RuntimeReadClient.ReadResponse submitSync() {
        return Futures.getUnchecked(submit());
    }

    private int p4TableId(PiTableId piTableId) throws InternalRequestException {
        try {
            return getBrowser().tables().getByName(piTableId.id())
                    .getPreamble().getId();
        } catch (P4InfoBrowser.NotFoundException e) {
            throw new InternalRequestException(e.getMessage());
        }
    }

    private boolean tableHasCounters(PiTableId piTableId) throws InternalRequestException {
        return pipeconf.pipelineModel().table(piTableId).orElseThrow(
                () -> new InternalRequestException(format(
                        "Not such a table in pipeline model: %s", piTableId)))
                .counters().size() > 0;
    }

    private int p4ActionProfileId(PiActionProfileId piActionProfileId)
            throws InternalRequestException {
        try {
            return getBrowser().actionProfiles().getByName(piActionProfileId.id())
                    .getPreamble().getId();
        } catch (P4InfoBrowser.NotFoundException e) {
            throw new InternalRequestException(e.getMessage());
        }
    }

    private int p4CounterId(PiCounterId counterId)
            throws InternalRequestException {
        try {
            return getBrowser().counters().getByName(counterId.id())
                    .getPreamble().getId();
        } catch (P4InfoBrowser.NotFoundException e) {
            throw new InternalRequestException(e.getMessage());
        }
    }

    private int p4MeterId(PiMeterId meterId)
            throws InternalRequestException {
        try {
            return getBrowser().meters().getByName(meterId.id())
                    .getPreamble().getId();
        } catch (P4InfoBrowser.NotFoundException e) {
            throw new InternalRequestException(e.getMessage());
        }
    }

    private P4InfoBrowser getBrowser() throws InternalRequestException {
        final P4InfoBrowser browser = PipeconfHelper.getP4InfoBrowser(pipeconf);
        if (browser == null) {
            throw new InternalRequestException(
                    "Unable to get a P4Info browser for pipeconf " + pipeconf.id());
        }
        return browser;
    }

    /**
     * Internal exception to signal that something went wrong when populating
     * the request.
     */
    private final class InternalRequestException extends Exception {

        private InternalRequestException(String message) {
            super(message);
        }
    }
}
