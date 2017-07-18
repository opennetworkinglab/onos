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

package org.onosproject.p4runtime.ctl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.ByteString;
import io.grpc.Context;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.net.DeviceId;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.runtime.PiPacketOperation;
import org.onosproject.net.pi.runtime.PiTableEntry;
import org.onosproject.net.pi.runtime.PiTableId;
import org.onosproject.p4runtime.api.P4RuntimeClient;
import org.onosproject.p4runtime.api.P4RuntimeEvent;
import org.slf4j.Logger;
import p4.P4RuntimeGrpc;
import p4.P4RuntimeOuterClass.Entity;
import p4.P4RuntimeOuterClass.ForwardingPipelineConfig;
import p4.P4RuntimeOuterClass.MasterArbitrationUpdate;
import p4.P4RuntimeOuterClass.PacketIn;
import p4.P4RuntimeOuterClass.ReadRequest;
import p4.P4RuntimeOuterClass.ReadResponse;
import p4.P4RuntimeOuterClass.SetForwardingPipelineConfigRequest;
import p4.P4RuntimeOuterClass.StreamMessageRequest;
import p4.P4RuntimeOuterClass.StreamMessageResponse;
import p4.P4RuntimeOuterClass.TableEntry;
import p4.P4RuntimeOuterClass.Uint128;
import p4.P4RuntimeOuterClass.Update;
import p4.P4RuntimeOuterClass.WriteRequest;
import p4.tmp.P4Config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.onlab.util.ImmutableByteSequence.copyFrom;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.pi.model.PiPipeconf.ExtensionType;
import static org.slf4j.LoggerFactory.getLogger;
import static p4.P4RuntimeOuterClass.Entity.EntityCase.TABLE_ENTRY;
import static p4.P4RuntimeOuterClass.SetForwardingPipelineConfigRequest.Action.VERIFY_AND_COMMIT;
import static p4.config.P4InfoOuterClass.P4Info;

/**
 * Implementation of a P4Runtime client.
 */
public final class P4RuntimeClientImpl implements P4RuntimeClient {

    private static final int DEADLINE_SECONDS = 15;

    // FIXME: use static election ID, since mastership arbitration is not yet support on BMv2 or Tofino.
    private static final int ELECTION_ID = 1;

    private static final Map<WriteOperationType, Update.Type> UPDATE_TYPES = ImmutableMap.of(
            WriteOperationType.UNSPECIFIED, Update.Type.UNSPECIFIED,
            WriteOperationType.INSERT, Update.Type.INSERT,
            WriteOperationType.MODIFY, Update.Type.MODIFY,
            WriteOperationType.DELETE, Update.Type.DELETE
    );

    private final Logger log = getLogger(getClass());

    private final DeviceId deviceId;
    private final int p4DeviceId;
    private final P4RuntimeControllerImpl controller;
    private final P4RuntimeGrpc.P4RuntimeBlockingStub blockingStub;
    private final Context.CancellableContext cancellableContext;
    private final ExecutorService executorService;
    private final Executor contextExecutor;
    private final Lock writeLock = new ReentrantLock();
    private final StreamObserver<StreamMessageRequest> streamRequestObserver;


    P4RuntimeClientImpl(DeviceId deviceId, int p4DeviceId, ManagedChannel channel, P4RuntimeControllerImpl controller) {
        this.deviceId = deviceId;
        this.p4DeviceId = p4DeviceId;
        this.controller = controller;
        this.cancellableContext = Context.current().withCancellation();
        this.executorService = Executors.newFixedThreadPool(5, groupedThreads(
                "onos/p4runtime-client-" + deviceId.toString(),
                deviceId.toString() + "-%d"));
        this.contextExecutor = this.cancellableContext.fixedContextExecutor(executorService);
        this.blockingStub = P4RuntimeGrpc.newBlockingStub(channel)
                .withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS);
        P4RuntimeGrpc.P4RuntimeStub asyncStub = P4RuntimeGrpc.newStub(channel);
        this.streamRequestObserver = asyncStub.streamChannel(new StreamChannelResponseObserver());
    }

    /**
     * Executes the given task (supplier) in the gRPC context executor of this client, such that if the context is
     * cancelled (e.g. client shutdown) the RPC is automatically cancelled.
     * <p>
     * Important: Tasks submitted in parallel by different threads are forced executed sequentially.
     * <p>
     */
    private <U> CompletableFuture<U> supplyInContext(Supplier<U> supplier) {
        return CompletableFuture.supplyAsync(() -> {
            // TODO: explore a more relaxed locking strategy.
            writeLock.lock();
            try {
                return supplier.get();
            } finally {
                writeLock.unlock();
            }
        }, contextExecutor);
    }

    @Override
    public CompletableFuture<Boolean> initStreamChannel() {
        return supplyInContext(this::doInitStreamChannel);
    }

    @Override
    public CompletableFuture<Boolean> setPipelineConfig(PiPipeconf pipeconf, ExtensionType targetConfigExtType) {
        return supplyInContext(() -> doSetPipelineConfig(pipeconf, targetConfigExtType));
    }

    @Override
    public CompletableFuture<Boolean> writeTableEntries(Collection<PiTableEntry> piTableEntries,
                                                        WriteOperationType opType, PiPipeconf pipeconf) {
        return supplyInContext(() -> doWriteTableEntries(piTableEntries, opType, pipeconf));
    }

    @Override
    public CompletableFuture<Collection<PiTableEntry>> dumpTable(PiTableId piTableId, PiPipeconf pipeconf) {
        return supplyInContext(() -> doDumpTable(piTableId, pipeconf));
    }

    @Override
    public CompletableFuture<Boolean> packetOut(PiPacketOperation packet, PiPipeconf pipeconf) {
        CompletableFuture<Boolean> result = new CompletableFuture<>();
//        P4InfoBrowser browser = null; //PipeconfHelper.getP4InfoBrowser(pipeconf);
//        try {
//            ControllerPacketMetadata controllerPacketMetadata =
//                    browser.controllerPacketMetadatas().getByName("packet_out");
//            PacketOut.Builder packetOutBuilder = PacketOut.newBuilder();
//            packetOutBuilder.addAllMetadata(packet.metadatas().stream().map(metadata -> {
//                //FIXME we are assuming that there is no more than one metadata per name.
//                int metadataId = controllerPacketMetadata.getMetadataList().stream().filter(metadataInfo -> {
//                   return  metadataInfo.getName().equals(metadata.id().name());
//                }).findFirst().get().getId();
//                return PacketMetadata.newBuilder()
//                        .setMetadataId(metadataId)
//                        .setValue(ByteString.copyFrom(metadata.value().asReadOnlyBuffer()))
//                        .build();
//            }).filter(Objects::nonNull).collect(Collectors.toList()));
//            packetOutBuilder.setPayload(ByteString.copyFrom(packet.data().asReadOnlyBuffer()));
//            PacketOut packetOut = packetOutBuilder.build();
//            StreamMessageRequest packetOutRequest = StreamMessageRequest
//                    .newBuilder().setPacket(packetOut).build();
//            streamRequestObserver.onNext(packetOutRequest);
//            result.complete(true);
//        } catch (P4InfoBrowser.NotFoundException e) {
//            log.error("Cant find metadata with name \"packet_out\" in p4Info file.");
//            result.complete(false);
//        }
        return result;
    }

    /* Blocking method implementations below */

    private boolean doInitStreamChannel() {
        // To listen for packets and other events, we need to start the RPC.
        // Here we do it by sending a master arbitration update.
        log.info("initializing stream chanel on {}...", deviceId);
        if (!doArbitrationUpdate()) {
            log.warn("Unable to initialize stream channel for {}", deviceId);
            return false;
        } else {
            return true;
        }
    }

    private boolean doArbitrationUpdate() {
        log.info("Sending arbitration update to {}...", deviceId);
        StreamMessageRequest requestMsg = StreamMessageRequest.newBuilder()
                .setArbitration(MasterArbitrationUpdate.newBuilder()
                                        .setDeviceId(p4DeviceId)
                                        .build())
                .build();
        try {
            streamRequestObserver.onNext(requestMsg);
            return true;
        } catch (StatusRuntimeException e) {
            log.warn("Arbitration update failed for {}: {}", deviceId, e);
            return false;
        }
    }

    private boolean doSetPipelineConfig(PiPipeconf pipeconf, ExtensionType targetConfigExtType) {

        log.info("Setting pipeline config for {} to {} using {}...", deviceId, pipeconf.id(), targetConfigExtType);

        P4Info p4Info = PipeconfHelper.getP4Info(pipeconf);
        if (p4Info == null) {
            // Problem logged by PipeconfHelper.
            return false;
        }

        if (!pipeconf.extension(targetConfigExtType).isPresent()) {
            log.warn("Missing extension {} in pipeconf {}", targetConfigExtType, pipeconf.id());
            return false;
        }

        InputStream targetConfig = pipeconf.extension(targetConfigExtType).get();
        P4Config.P4DeviceConfig p4DeviceConfigMsg;
        try {
            p4DeviceConfigMsg = P4Config.P4DeviceConfig
                    .newBuilder()
                    .setExtras(P4Config.P4DeviceConfig.Extras.getDefaultInstance())
                    .setReassign(true)
                    .setDeviceData(ByteString.readFrom(targetConfig))
                    .build();
        } catch (IOException ex) {
            log.warn("Unable to load target-specific config for {}: {}", deviceId, ex.getMessage());
            return false;
        }

        SetForwardingPipelineConfigRequest request = SetForwardingPipelineConfigRequest
                .newBuilder()
                .setAction(VERIFY_AND_COMMIT)
                .addConfigs(ForwardingPipelineConfig
                                    .newBuilder()
                                    .setDeviceId(p4DeviceId)
                                    .setP4Info(p4Info)
                                    .setP4DeviceConfig(p4DeviceConfigMsg.toByteString())
                                    .build())
                .build();

        try {
            this.blockingStub.setForwardingPipelineConfig(request);

        } catch (StatusRuntimeException ex) {
            log.warn("Unable to set pipeline config for {}: {}", deviceId, ex.getMessage());
            return false;
        }

        return true;
    }

    private boolean doWriteTableEntries(Collection<PiTableEntry> piTableEntries, WriteOperationType opType,
                                        PiPipeconf pipeconf) {

        WriteRequest.Builder writeRequestBuilder = WriteRequest.newBuilder();

        Collection<Update> updateMsgs = TableEntryEncoder.encode(piTableEntries, pipeconf)
                .stream()
                .map(tableEntryMsg ->
                             Update.newBuilder()
                                     .setEntity(Entity.newBuilder()
                                                        .setTableEntry(tableEntryMsg)
                                                        .build())
                                     .setType(UPDATE_TYPES.get(opType))
                                     .build())
                .collect(Collectors.toList());

        if (updateMsgs.size() == 0) {
            return true;
        }

        writeRequestBuilder
                .setDeviceId(p4DeviceId)
                .setElectionId(Uint128.newBuilder()
                                       .setHigh(0)
                                       .setLow(ELECTION_ID)
                                       .build())
                .addAllUpdates(updateMsgs)
                .build();

        try {
            blockingStub.write(writeRequestBuilder.build());
            return true;
        } catch (StatusRuntimeException e) {
            log.warn("Unable to write table entries ({}): {}", opType, e.getMessage());
            return false;
        }
    }

    private Collection<PiTableEntry> doDumpTable(PiTableId piTableId, PiPipeconf pipeconf) {

        log.info("Dumping table {} from {} (pipeconf {})...", piTableId, deviceId, pipeconf.id());

        P4InfoBrowser browser = PipeconfHelper.getP4InfoBrowser(pipeconf);
        int tableId;
        try {
            tableId = browser.tables().getByName(piTableId.id()).getPreamble().getId();
        } catch (P4InfoBrowser.NotFoundException e) {
            log.warn("Unable to dump table: {}", e.getMessage());
            return Collections.emptyList();
        }

        ReadRequest requestMsg = ReadRequest.newBuilder()
                .setDeviceId(p4DeviceId)
                .addEntities(Entity.newBuilder()
                                     .setTableEntry(TableEntry.newBuilder()
                                                            .setTableId(tableId)
                                                            .build())
                                     .build())
                .build();

        Iterator<ReadResponse> responses;
        try {
            responses = blockingStub.read(requestMsg);
        } catch (StatusRuntimeException e) {
            log.warn("Unable to dump table: {}", e.getMessage());
            return Collections.emptyList();
        }

        Iterable<ReadResponse> responseIterable = () -> responses;
        List<TableEntry> tableEntryMsgs = StreamSupport
                .stream(responseIterable.spliterator(), false)
                .map(ReadResponse::getEntitiesList)
                .flatMap(List::stream)
                .filter(entity -> entity.getEntityCase() == TABLE_ENTRY)
                .map(Entity::getTableEntry)
                .collect(Collectors.toList());

        log.info("Retrieved {} entries from table {} on {}...", tableEntryMsgs.size(), piTableId, deviceId);

        return TableEntryEncoder.decode(tableEntryMsgs, pipeconf);
    }

    @Override
    public void shutdown() {

        log.info("Shutting down client for {}...", deviceId);

        writeLock.lock();
        try {
            if (streamRequestObserver != null) {
                streamRequestObserver.onCompleted();
                cancellableContext.cancel(new InterruptedException("Requested client shutdown"));
            }

            this.executorService.shutdown();
            try {
                executorService.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                log.warn("Executor service didn't shutdown in time.");
            }
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Handles messages received from the device on the stream channel.
     */
    private class StreamChannelResponseObserver implements StreamObserver<StreamMessageResponse> {

        @Override
        public void onNext(StreamMessageResponse message) {
            executorService.submit(() -> doNext(message));
        }

        private void doNext(StreamMessageResponse message) {
            log.info("Received message on stream channel from {}: {}", deviceId, message.getUpdateCase());
            switch (message.getUpdateCase()) {
                case PACKET:
                    // Packet-in
                    PacketIn packetIn = message.getPacket();
                    ImmutableByteSequence data = copyFrom(packetIn.getPayload().asReadOnlyByteBuffer());
                    ImmutableList.Builder<ImmutableByteSequence> metadataBuilder = ImmutableList.builder();
                    packetIn.getMetadataList().stream()
                            .map(m -> m.getValue().asReadOnlyByteBuffer())
                            .map(ImmutableByteSequence::copyFrom)
                            .forEach(metadataBuilder::add);
                    P4RuntimeEvent event = new DefaultPacketInEvent(deviceId, data, metadataBuilder.build());
                    controller.postEvent(event);
                    return;

                case ARBITRATION:
                    throw new UnsupportedOperationException("Arbitration not implemented.");

                default:
                    log.warn("Unrecognized stream message from {}: {}", deviceId, message.getUpdateCase());
            }
        }

        @Override
        public void onError(Throwable throwable) {
            log.warn("Error on stream channel for {}: {}", deviceId, Status.fromThrowable(throwable));
            // FIXME: we might want to recreate the channel.
            // In general, we want to be robust against any transient error and, if the channel is open, make sure the
            // stream channel is always on.
        }

        @Override
        public void onCompleted() {
            log.warn("Stream channel for {} has completed", deviceId);
            // FIXME: same concern as before.
        }
    }
}