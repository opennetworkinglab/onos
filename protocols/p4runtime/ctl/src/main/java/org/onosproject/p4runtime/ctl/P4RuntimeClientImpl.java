/*
 * Copyright 2017-present Open Networking Foundation
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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.protobuf.ByteString;
import io.grpc.Context;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.util.Tools;
import org.onosproject.net.DeviceId;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.runtime.PiActionProfileId;
import org.onosproject.net.pi.runtime.PiCounterCellData;
import org.onosproject.net.pi.runtime.PiCounterCellId;
import org.onosproject.net.pi.runtime.PiCounterId;
import org.onosproject.net.pi.runtime.PiDirectCounterCellId;
import org.onosproject.net.pi.runtime.PiIndirectCounterCellId;
import org.onosproject.net.pi.runtime.PiActionGroup;
import org.onosproject.net.pi.runtime.PiActionGroupMember;
import org.onosproject.net.pi.runtime.PiPacketOperation;
import org.onosproject.net.pi.runtime.PiPipeconfService;
import org.onosproject.net.pi.runtime.PiTableEntry;
import org.onosproject.net.pi.runtime.PiTableId;
import org.onosproject.p4runtime.api.P4RuntimeClient;
import org.onosproject.p4runtime.api.P4RuntimeEvent;
import org.slf4j.Logger;
import p4.P4RuntimeGrpc;
import p4.P4RuntimeOuterClass.ActionProfileGroup;
import p4.P4RuntimeOuterClass.ActionProfileMember;
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
import p4.P4RuntimeOuterClass.Update;
import p4.P4RuntimeOuterClass.WriteRequest;
import p4.config.P4InfoOuterClass;
import p4.config.P4InfoOuterClass.P4Info;
import p4.tmp.P4Config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.pi.model.PiPipeconf.ExtensionType;
import static org.slf4j.LoggerFactory.getLogger;
import static p4.P4RuntimeOuterClass.Entity.EntityCase.ACTION_PROFILE_GROUP;
import static p4.P4RuntimeOuterClass.Entity.EntityCase.ACTION_PROFILE_MEMBER;
import static p4.P4RuntimeOuterClass.Entity.EntityCase.TABLE_ENTRY;
import static p4.P4RuntimeOuterClass.PacketOut;
import static p4.P4RuntimeOuterClass.SetForwardingPipelineConfigRequest.Action.VERIFY_AND_COMMIT;

/**
 * Implementation of a P4Runtime client.
 */
public final class P4RuntimeClientImpl implements P4RuntimeClient {

    private static final Map<WriteOperationType, Update.Type> UPDATE_TYPES = ImmutableMap.of(
            WriteOperationType.UNSPECIFIED, Update.Type.UNSPECIFIED,
            WriteOperationType.INSERT, Update.Type.INSERT,
            WriteOperationType.MODIFY, Update.Type.MODIFY,
            WriteOperationType.DELETE, Update.Type.DELETE
    );

    private final Logger log = getLogger(getClass());

    private final DeviceId deviceId;
    private final long p4DeviceId;
    private final P4RuntimeControllerImpl controller;
    private final P4RuntimeGrpc.P4RuntimeBlockingStub blockingStub;
    private final Context.CancellableContext cancellableContext;
    private final ExecutorService executorService;
    private final Executor contextExecutor;
    private final Lock writeLock = new ReentrantLock();
    private final StreamObserver<StreamMessageRequest> streamRequestObserver;

    /**
     * Default constructor.
     *
     * @param deviceId the ONOS device id
     * @param p4DeviceId the P4 device id
     * @param channel gRPC channel
     * @param controller runtime client controller
     */
    P4RuntimeClientImpl(DeviceId deviceId, long p4DeviceId, ManagedChannel channel,
                        P4RuntimeControllerImpl controller) {
        this.deviceId = deviceId;
        this.p4DeviceId = p4DeviceId;
        this.controller = controller;
        this.cancellableContext = Context.current().withCancellation();
        this.executorService = Executors.newFixedThreadPool(15, groupedThreads(
                "onos/p4runtime-client-" + deviceId.toString(),
                deviceId.toString() + "-%d"));
        this.contextExecutor = this.cancellableContext.fixedContextExecutor(executorService);
        //TODO Investigate deadline or timeout in supplyInContext Method
        this.blockingStub = P4RuntimeGrpc.newBlockingStub(channel);
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
    private <U> CompletableFuture<U> supplyInContext(Supplier<U> supplier, String opDescription) {
        return CompletableFuture.supplyAsync(() -> {
            // TODO: explore a more relaxed locking strategy.
            writeLock.lock();
            try {
                return supplier.get();
            } catch (Throwable ex) {
                if (ex instanceof StatusRuntimeException) {
                    log.warn("Unable to execute {} on {}: {}", opDescription, deviceId, ex.toString());
                } else {
                    log.error("Exception in client of {}, executing {}", deviceId, opDescription, ex);
                }
                throw ex;
            } finally {
                writeLock.unlock();
            }
        }, contextExecutor);
    }

    @Override
    public CompletableFuture<Boolean> initStreamChannel() {
        return supplyInContext(this::doInitStreamChannel, "initStreamChannel");
    }

    @Override
    public CompletableFuture<Boolean> setPipelineConfig(PiPipeconf pipeconf, ExtensionType targetConfigExtType) {
        return supplyInContext(() -> doSetPipelineConfig(pipeconf, targetConfigExtType), "setPipelineConfig");
    }

    @Override
    public CompletableFuture<Boolean> writeTableEntries(Collection<PiTableEntry> piTableEntries,
                                                        WriteOperationType opType, PiPipeconf pipeconf) {
        return supplyInContext(() -> doWriteTableEntries(piTableEntries, opType, pipeconf),
                               "writeTableEntries-" + opType.name());
    }

    @Override
    public CompletableFuture<Collection<PiTableEntry>> dumpTable(PiTableId piTableId, PiPipeconf pipeconf) {
        return supplyInContext(() -> doDumpTable(piTableId, pipeconf), "dumpTable-" + piTableId);
    }

    @Override
    public CompletableFuture<Boolean> packetOut(PiPacketOperation packet, PiPipeconf pipeconf) {
        return supplyInContext(() -> doPacketOut(packet, pipeconf), "packetOut");
    }

    @Override
    public CompletableFuture<Collection<PiCounterCellData>> readCounterCells(Set<PiCounterCellId> cellIds,
                                                                             PiPipeconf pipeconf) {
        return supplyInContext(() -> doReadCounterCells(cellIds, pipeconf),
                               "readCounterCells-" + cellIds.hashCode());
    }

    @Override
    public CompletableFuture<Collection<PiCounterCellData>> readAllCounterCells(Set<PiCounterId> counterIds,
                                                                                PiPipeconf pipeconf) {

        /*
        From p4runtime.proto, the scope of a ReadRequest is defined as follows:
        CounterEntry:
            - All counter cells for all meters if counter_id = 0 (default).
            - All counter cells for given counter_id if index = 0 (default).
        DirectCounterEntry:
            - All counter cells for all meters if counter_id = 0 (default).
            - All counter cells for given counter_id if table_entry.match is empty.
         */

        Set<PiCounterCellId> cellIds = Sets.newHashSet();

        for (PiCounterId counterId : counterIds) {
            switch (counterId.type()) {
                case INDIRECT:
                    cellIds.add(PiIndirectCounterCellId.of(counterId, 0));
                    break;
                case DIRECT:
                    cellIds.add(PiDirectCounterCellId.of(counterId, PiTableEntry.EMTPY));
                    break;
                default:
                    log.warn("Unrecognized PI counter ID '{}'", counterId.type());
            }
        }

        return supplyInContext(() -> doReadCounterCells(cellIds, pipeconf),
                               "readAllCounterCells-" + cellIds.hashCode());
    }

    @Override
    public CompletableFuture<Boolean> writeActionGroupMembers(PiActionGroup group,
                                                              Collection<PiActionGroupMember> members,
                                                              WriteOperationType opType,
                                                              PiPipeconf pipeconf) {
        return supplyInContext(() -> doWriteActionGroupMembers(group, members, opType, pipeconf),
                               "writeActionGroupMembers-" + opType.name());
    }

    @Override
    public CompletableFuture<Boolean> writeActionGroup(PiActionGroup group,
                                                       WriteOperationType opType,
                                                       PiPipeconf pipeconf) {
        return supplyInContext(() -> doWriteActionGroup(group, opType, pipeconf),
                               "writeActionGroup-" + opType.name());
    }

    @Override
    public CompletableFuture<Collection<PiActionGroup>> dumpGroups(PiActionProfileId actionProfileId,
                                                                   PiPipeconf pipeconf) {
        return supplyInContext(() -> doDumpGroups(actionProfileId, pipeconf),
                               "dumpGroups-" + actionProfileId.id());
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


        ForwardingPipelineConfig.Builder pipelineConfigBuilder = ForwardingPipelineConfig
                .newBuilder()
                .setDeviceId(p4DeviceId)
                .setP4Info(p4Info);

        //if the target config extension is null we don't want to add the config.
        if (targetConfigExtType != null) {
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

                pipelineConfigBuilder.setP4DeviceConfig(p4DeviceConfigMsg.toByteString());

            } catch (IOException ex) {
                log.warn("Unable to load target-specific config for {}: {}", deviceId, ex.getMessage());
                return false;
            }
        }

        SetForwardingPipelineConfigRequest request = SetForwardingPipelineConfigRequest
                .newBuilder()
                .setAction(VERIFY_AND_COMMIT)
                .addConfigs(pipelineConfigBuilder.build())
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
                /* PI ignores this ElectionId, commenting out for now.
                .setElectionId(Uint128.newBuilder()
                                       .setHigh(0)
                                       .setLow(ELECTION_ID)
                                       .build()) */
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

        log.debug("Dumping table {} from {} (pipeconf {})...", piTableId, deviceId, pipeconf.id());

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

        log.debug("Retrieved {} entries from table {} on {}...", tableEntryMsgs.size(), piTableId, deviceId);

        return TableEntryEncoder.decode(tableEntryMsgs, pipeconf);
    }

    private boolean doPacketOut(PiPacketOperation packet, PiPipeconf pipeconf) {
        try {
            //encode the PiPacketOperation into a PacketOut
            PacketOut packetOut = PacketIOCodec.encodePacketOut(packet, pipeconf);

            //Build the request
            StreamMessageRequest packetOutRequest = StreamMessageRequest
                    .newBuilder().setPacket(packetOut).build();

            //Send the request
            streamRequestObserver.onNext(packetOutRequest);

        } catch (P4InfoBrowser.NotFoundException e) {
            log.error("Cant find expected metadata in p4Info file. {}", e.getMessage());
            log.debug("Exception", e);
            return false;
        }
        return true;
    }

    private void doPacketIn(PacketIn packetInMsg) {

        // Retrieve the pipeconf for this client's device.
        PiPipeconfService pipeconfService = DefaultServiceDirectory.getService(PiPipeconfService.class);
        if (pipeconfService == null) {
            throw new IllegalStateException("PiPipeconfService is null. Can't handle packet in.");
        }
        final PiPipeconf pipeconf;
        if (pipeconfService.ofDevice(deviceId).isPresent() &&
                pipeconfService.getPipeconf(pipeconfService.ofDevice(deviceId).get()).isPresent()) {
            pipeconf = pipeconfService.getPipeconf(pipeconfService.ofDevice(deviceId).get()).get();
        } else {
            log.warn("Unable to get pipeconf of {}. Can't handle packet in", deviceId);
            return;
        }
        // Decode packet message and post event.
        PiPacketOperation packetOperation = PacketIOCodec.decodePacketIn(packetInMsg, pipeconf);
        DefaultPacketIn packetInEventSubject = new DefaultPacketIn(deviceId, packetOperation);
        P4RuntimeEvent event = new P4RuntimeEvent(P4RuntimeEvent.Type.PACKET_IN, packetInEventSubject);
        log.debug("Received packet in: {}", event);
        controller.postEvent(event);
    }

    private void doArbitrationUpdateFromDevice(MasterArbitrationUpdate arbitrationMsg) {

        log.warn("Received arbitration update from {} (NOT IMPLEMENTED YET): {}", deviceId, arbitrationMsg);
    }

    private Collection<PiCounterCellData> doReadCounterCells(Collection<PiCounterCellId> cellIds, PiPipeconf pipeconf) {

        // We use this map to remember the original PI counter IDs of the returned response.
        final Map<Integer, PiCounterId> counterIdMap = Maps.newHashMap();

        final ReadRequest request = ReadRequest.newBuilder()
                .setDeviceId(p4DeviceId)
                .addAllEntities(CounterEntryCodec.encodePiCounterCellIds(cellIds, counterIdMap, pipeconf))
                .build();

        if (request.getEntitiesList().size() == 0) {
            return Collections.emptyList();
        }

        final Iterable<ReadResponse> responses;
        try {
            responses = () -> blockingStub.read(request);
        } catch (StatusRuntimeException e) {
            log.warn("Unable to read counters: {}", e.getMessage());
            return Collections.emptyList();
        }

        List<Entity> entities = StreamSupport.stream(responses.spliterator(), false)
                .map(ReadResponse::getEntitiesList)
                .flatMap(List::stream)
                .collect(Collectors.toList());

        return CounterEntryCodec.decodeCounterEntities(entities, counterIdMap, pipeconf);
    }

    private boolean doWriteActionGroupMembers(PiActionGroup group, Collection<PiActionGroupMember> members,
                                              WriteOperationType opType, PiPipeconf pipeconf) {
        WriteRequest.Builder writeRequestBuilder = WriteRequest.newBuilder();

        Collection<ActionProfileMember> actionProfileMembers = Lists.newArrayList();
        try {
            for (PiActionGroupMember member : members) {
                actionProfileMembers.add(
                        ActionProfileMemberEncoder.encode(group, member, pipeconf)
                );
            }
        } catch (EncodeException | P4InfoBrowser.NotFoundException e) {
            log.warn("Can't encode group member {} due to {}", members, e.getMessage());
            return false;
        }

        Collection<Update> updateMsgs = actionProfileMembers.stream()
                .map(actionProfileMember ->
                             Update.newBuilder()
                                     .setEntity(Entity.newBuilder()
                                                        .setActionProfileMember(actionProfileMember)
                                                        .build())
                                     .setType(UPDATE_TYPES.get(opType))
                                     .build())
                .collect(Collectors.toList());

        if (updateMsgs.size() == 0) {
            // Nothing to update
            return true;
        }

        writeRequestBuilder
                .setDeviceId(p4DeviceId)
                .addAllUpdates(updateMsgs);
        try {
            blockingStub.write(writeRequestBuilder.build());
            return true;
        } catch (StatusRuntimeException e) {
            log.warn("Unable to write table entries ({}): {}", opType, e.getMessage());
            return false;
        }
    }

    private Collection<PiActionGroup> doDumpGroups(PiActionProfileId piActionProfileId, PiPipeconf pipeconf) {
        log.debug("Dumping groups from action profile {} from {} (pipeconf {})...",
                  piActionProfileId.id(), deviceId, pipeconf.id());
        P4InfoBrowser browser = PipeconfHelper.getP4InfoBrowser(pipeconf);
        if (browser == null) {
            log.warn("Unable to get a P4Info browser for pipeconf {}, skipping dump action profile {}",
                     pipeconf, piActionProfileId);
            return Collections.emptySet();
        }

        int actionProfileId;
        try {
            P4InfoOuterClass.ActionProfile actionProfile =
                    browser.actionProfiles().getByName(piActionProfileId.id());
            actionProfileId = actionProfile.getPreamble().getId();
        } catch (P4InfoBrowser.NotFoundException e) {
            log.warn("Can't find action profile {} from p4info", piActionProfileId);
            return Collections.emptySet();
        }

        ActionProfileGroup actionProfileGroup =
                ActionProfileGroup.newBuilder()
                        .setActionProfileId(actionProfileId)
                        .build();

        ReadRequest requestMsg = ReadRequest.newBuilder()
                .setDeviceId(p4DeviceId)
                .addEntities(Entity.newBuilder()
                                     .setActionProfileGroup(actionProfileGroup)
                                     .build())
                .build();

        Iterator<ReadResponse> responses;
        try {
            responses = blockingStub.read(requestMsg);
        } catch (StatusRuntimeException e) {
            log.warn("Unable to read action profile {} due to {}", piActionProfileId, e.getMessage());
            return Collections.emptySet();
        }

        List<ActionProfileGroup> actionProfileGroups =
                Tools.stream(() -> responses)
                        .map(ReadResponse::getEntitiesList)
                        .flatMap(List::stream)
                        .filter(entity -> entity.getEntityCase() == ACTION_PROFILE_GROUP)
                        .map(Entity::getActionProfileGroup)
                        .collect(Collectors.toList());

        log.debug("Retrieved {} groups from action profile {} on {}...",
                  actionProfileGroups.size(), piActionProfileId.id(), deviceId);

        // group id -> members
        Multimap<Integer, ActionProfileMember> actionProfileMemberMap = HashMultimap.create();
        AtomicLong memberCount = new AtomicLong(0);
        AtomicBoolean success = new AtomicBoolean(true);
        actionProfileGroups.forEach(actProfGrp -> {
            actProfGrp.getMembersList().forEach(member -> {
                ActionProfileMember actProfMember =
                        ActionProfileMember.newBuilder()
                                .setActionProfileId(actProfGrp.getActionProfileId())
                                .setMemberId(member.getMemberId())
                                .build();
                Entity entity = Entity.newBuilder()
                        .setActionProfileMember(actProfMember)
                        .build();

                ReadRequest reqMsg = ReadRequest.newBuilder().setDeviceId(p4DeviceId)
                        .addEntities(entity)
                        .build();

                Iterator<ReadResponse> resps;
                try {
                    resps = blockingStub.read(reqMsg);
                } catch (StatusRuntimeException e) {
                    log.warn("Unable to read member {} from action profile {} due to {}",
                             member, piActionProfileId, e.getMessage());
                    success.set(false);
                    return;
                }
                Tools.stream(() -> resps)
                        .map(ReadResponse::getEntitiesList)
                        .flatMap(List::stream)
                        .filter(e -> e.getEntityCase() == ACTION_PROFILE_MEMBER)
                        .map(Entity::getActionProfileMember)
                        .forEach(m -> {
                            actionProfileMemberMap.put(actProfGrp.getGroupId(), m);
                            memberCount.incrementAndGet();
                        });
            });
        });

        if (!success.get()) {
            // Can't read members
            return Collections.emptySet();
        }
        log.info("Retrieved {} group members from action profile {} on {}...",
                 memberCount.get(), piActionProfileId.id(), deviceId);

        Collection<PiActionGroup> piActionGroups = Sets.newHashSet();

        for (ActionProfileGroup apg : actionProfileGroups) {
            try {
                Collection<ActionProfileMember> members = actionProfileMemberMap.get(apg.getGroupId());
                PiActionGroup decodedGroup =
                        ActionProfileGroupEncoder.decode(apg, members, pipeconf);
                piActionGroups.add(decodedGroup);
            } catch (EncodeException | P4InfoBrowser.NotFoundException e) {
                log.warn("Can't decode group {} due to {}", apg, e.getMessage());
                return Collections.emptySet();
            }
        }

        return piActionGroups;
    }

    private boolean doWriteActionGroup(PiActionGroup group, WriteOperationType opType, PiPipeconf pipeconf) {
        WriteRequest.Builder writeRequestBuilder = WriteRequest.newBuilder();
        ActionProfileGroup actionProfileGroup;
        try {
            actionProfileGroup = ActionProfileGroupEncoder.encode(group, pipeconf);
        } catch (EncodeException | P4InfoBrowser.NotFoundException e) {
            log.warn("Can't encode group {} due to {}", e.getMessage());
            return false;
        }
        Update updateMessage = Update.newBuilder()
                .setEntity(Entity.newBuilder()
                                   .setActionProfileGroup(actionProfileGroup)
                                   .build())
                .setType(UPDATE_TYPES.get(opType))
                .build();
        writeRequestBuilder
                .setDeviceId(p4DeviceId)
                .addUpdates(updateMessage);
        try {
            blockingStub.write(writeRequestBuilder.build());
            return true;
        } catch (StatusRuntimeException e) {
            log.warn("Unable to write table entries ({}): {}", opType, e.getMessage());
            return false;
        }
    }

    /**
     * Returns the internal P4 device ID associated with this client.
     *
     * @return P4 device ID
     */
    public long p4DeviceId() {
        return p4DeviceId;
    }

    /**
     * For testing purpose only. TODO: remove before release.
     *
     * @return blocking stub
     */
    public P4RuntimeGrpc.P4RuntimeBlockingStub blockingStub() {
        return this.blockingStub;
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
            try {
                log.debug("Received message on stream channel from {}: {}", deviceId, message.getUpdateCase());
                switch (message.getUpdateCase()) {
                    case PACKET:
                        // Packet-in
                        doPacketIn(message.getPacket());
                        return;
                    case ARBITRATION:
                        doArbitrationUpdateFromDevice(message.getArbitration());
                        return;
                    default:
                        log.warn("Unrecognized stream message from {}: {}", deviceId, message.getUpdateCase());
                }
            } catch (Throwable ex) {
                log.error("Exception while processing stream channel message from {}", deviceId, ex);
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