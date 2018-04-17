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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.Context;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.util.Tools;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.pi.model.PiActionProfileId;
import org.onosproject.net.pi.model.PiCounterId;
import org.onosproject.net.pi.model.PiMeterId;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.model.PiTableId;
import org.onosproject.net.pi.runtime.PiActionGroup;
import org.onosproject.net.pi.runtime.PiActionGroupMember;
import org.onosproject.net.pi.runtime.PiCounterCellData;
import org.onosproject.net.pi.runtime.PiCounterCellId;
import org.onosproject.net.pi.runtime.PiEntity;
import org.onosproject.net.pi.runtime.PiMeterCellConfig;
import org.onosproject.net.pi.runtime.PiMeterCellId;
import org.onosproject.net.pi.runtime.PiPacketOperation;
import org.onosproject.net.pi.runtime.PiTableEntry;
import org.onosproject.net.pi.service.PiPipeconfService;
import org.onosproject.p4runtime.api.P4RuntimeClient;
import org.onosproject.p4runtime.api.P4RuntimeEvent;
import org.slf4j.Logger;
import p4.P4RuntimeGrpc;
import p4.P4RuntimeOuterClass;
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
import p4.P4RuntimeOuterClass.Uint128;
import p4.P4RuntimeOuterClass.Update;
import p4.P4RuntimeOuterClass.WriteRequest;
import p4.config.P4InfoOuterClass.P4Info;
import p4.tmp.P4Config;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.onlab.util.Tools.groupedThreads;
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

    private Map<Uint128, CompletableFuture<Boolean>> arbitrationUpdateMap = Maps.newConcurrentMap();
    protected Uint128 p4RuntimeElectionId;

    /**
     * Default constructor.
     *
     * @param deviceId   the ONOS device id
     * @param p4DeviceId the P4 device id
     * @param channel    gRPC channel
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
            } catch (StatusRuntimeException ex) {
                log.warn("Unable to execute {} on {}: {}", opDescription, deviceId, ex.toString());
                throw ex;
            } catch (Throwable ex) {
                log.error("Exception in client of {}, executing {}", deviceId, opDescription, ex);
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
    public CompletableFuture<Boolean> setPipelineConfig(PiPipeconf pipeconf, ByteBuffer deviceData) {
        return supplyInContext(() -> doSetPipelineConfig(pipeconf, deviceData), "setPipelineConfig");
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
        return supplyInContext(() -> doReadAllCounterCells(counterIds, pipeconf),
                               "readAllCounterCells-" + counterIds.hashCode());
    }

    @Override
    public CompletableFuture<Boolean> writeActionGroupMembers(PiActionGroup group,
                                                              WriteOperationType opType,
                                                              PiPipeconf pipeconf) {
        return supplyInContext(() -> doWriteActionGroupMembers(group, opType, pipeconf),
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

    @Override
    public CompletableFuture<Boolean> sendMasterArbitrationUpdate() {
        return supplyInContext(this::doArbitrationUpdate, "arbitrationUpdate");
    }

    @Override
    public CompletableFuture<Boolean> writeMeterCells(Collection<PiMeterCellConfig> cellIds, PiPipeconf pipeconf) {

        return supplyInContext(() -> doWriteMeterCells(cellIds, pipeconf),
                               "writeMeterCells");
    }

    @Override
    public CompletableFuture<Collection<PiMeterCellConfig>> readMeterCells(Set<PiMeterCellId> cellIds,
                                                                           PiPipeconf pipeconf) {
        return supplyInContext(() -> doReadMeterCells(cellIds, pipeconf),
                               "readMeterCells-" + cellIds.hashCode());
    }

    @Override
    public CompletableFuture<Collection<PiMeterCellConfig>> readAllMeterCells(Set<PiMeterId> meterIds,
                                                                                PiPipeconf pipeconf) {
        return supplyInContext(() -> doReadAllMeterCells(meterIds, pipeconf),
                               "readAllMeterCells-" + meterIds.hashCode());
    }

    /* Blocking method implementations below */

    private boolean doArbitrationUpdate() {
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        // TODO: currently we use 64-bit Long type for election id, should
        // we use 128-bit ?
        long nextElectId = controller.getNewMasterElectionId();
        Uint128 newElectionId = Uint128.newBuilder()
                .setLow(nextElectId)
                .build();
        MasterArbitrationUpdate arbitrationUpdate = MasterArbitrationUpdate.newBuilder()
                .setDeviceId(p4DeviceId)
                .setElectionId(newElectionId)
                .build();
        StreamMessageRequest requestMsg = StreamMessageRequest.newBuilder()
                .setArbitration(arbitrationUpdate)
                .build();
        log.debug("Sending arbitration update to {} with election id {}...",
                  deviceId, newElectionId);
        arbitrationUpdateMap.put(newElectionId, result);
        try {
            streamRequestObserver.onNext(requestMsg);
            return result.get();
        } catch (StatusRuntimeException e) {
            log.error("Unable to perform arbitration update on {}: {}", deviceId, e.getMessage());
            arbitrationUpdateMap.remove(newElectionId);
            return false;
        } catch (InterruptedException | ExecutionException e) {
            log.warn("Arbitration update failed for {} due to {}", deviceId, e);
            arbitrationUpdateMap.remove(newElectionId);
            return false;
        }
    }
    private boolean doInitStreamChannel() {
        // To listen for packets and other events, we need to start the RPC.
        // Here we do it by sending a master arbitration update.
        return doArbitrationUpdate();
    }

    private boolean doSetPipelineConfig(PiPipeconf pipeconf, ByteBuffer deviceData) {

        log.info("Setting pipeline config for {} to {}...", deviceId, pipeconf.id());

        checkNotNull(deviceData, "deviceData cannot be null");

        P4Info p4Info = PipeconfHelper.getP4Info(pipeconf);
        if (p4Info == null) {
            // Problem logged by PipeconfHelper.
            return false;
        }

        P4Config.P4DeviceConfig p4DeviceConfigMsg = P4Config.P4DeviceConfig
                .newBuilder()
                .setExtras(P4Config.P4DeviceConfig.Extras.getDefaultInstance())
                .setReassign(true)
                .setDeviceData(ByteString.copyFrom(deviceData))
                .build();

        ForwardingPipelineConfig pipelineConfig = ForwardingPipelineConfig
                .newBuilder()
                .setP4Info(p4Info)
                .setP4DeviceConfig(p4DeviceConfigMsg.toByteString())
                .build();

        SetForwardingPipelineConfigRequest request = SetForwardingPipelineConfigRequest
                .newBuilder()
                .setDeviceId(p4DeviceId)
                .setElectionId(p4RuntimeElectionId)
                .setAction(VERIFY_AND_COMMIT)
                .setConfig(pipelineConfig)
                .build();

        try {
            this.blockingStub.setForwardingPipelineConfig(request);
            return true;
        } catch (StatusRuntimeException ex) {
            log.warn("Unable to set pipeline config on {}: {}", deviceId, ex.getMessage());
            return false;
        }
    }

    private boolean doWriteTableEntries(Collection<PiTableEntry> piTableEntries, WriteOperationType opType,
                                        PiPipeconf pipeconf) {
        WriteRequest.Builder writeRequestBuilder = WriteRequest.newBuilder();

        if (piTableEntries.size() == 0) {
            return true;
        }

        Collection<Update> updateMsgs;
        try {
            updateMsgs = TableEntryEncoder.encode(piTableEntries, pipeconf)
                    .stream()
                    .map(tableEntryMsg ->
                                 Update.newBuilder()
                                         .setEntity(Entity.newBuilder()
                                                            .setTableEntry(tableEntryMsg)
                                                            .build())
                                         .setType(UPDATE_TYPES.get(opType))
                                         .build())
                    .collect(Collectors.toList());
        } catch (EncodeException e) {
            log.error("Unable to encode table entries, aborting {} operation: {}",
                      opType.name(), e.getMessage());
            return false;
        }

        writeRequestBuilder
                .setDeviceId(p4DeviceId)
                .setElectionId(p4RuntimeElectionId)
                .addAllUpdates(updateMsgs)
                .build();

        try {
            blockingStub.write(writeRequestBuilder.build());
            return true;
        } catch (StatusRuntimeException e) {
            logWriteErrors(piTableEntries, e, opType, "table entry");
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
            log.warn("Unable to dump table {} from {}: {}", piTableId, deviceId, e.getMessage());
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
        PiPacketOperation packetOperation = PacketIOCodec.decodePacketIn(packetInMsg, pipeconf, deviceId);
        DefaultPacketIn packetInEventSubject = new DefaultPacketIn(deviceId, packetOperation);
        P4RuntimeEvent event = new P4RuntimeEvent(P4RuntimeEvent.Type.PACKET_IN, packetInEventSubject);
        log.debug("Received packet in: {}", event);
        controller.postEvent(event);
    }

    private void doArbitrationUpdateFromDevice(MasterArbitrationUpdate arbitrationMsg) {
        log.debug("Received arbitration update from {}: {}", deviceId, arbitrationMsg);

        Uint128 electionId = arbitrationMsg.getElectionId();
        CompletableFuture<Boolean> mastershipFeature = arbitrationUpdateMap.remove(electionId);

        if (mastershipFeature == null) {
            log.warn("Can't find completable future of election id {}", electionId);
            return;
        }

        this.p4RuntimeElectionId = electionId;
        int statusCode = arbitrationMsg.getStatus().getCode();
        MastershipRole arbitrationRole;
        // arbitration update success

        if (statusCode == Status.OK.getCode().value()) {
            mastershipFeature.complete(true);
            arbitrationRole = MastershipRole.MASTER;
        } else {
            mastershipFeature.complete(false);
            arbitrationRole = MastershipRole.STANDBY;
        }

        DefaultArbitration arbitrationEventSubject = new DefaultArbitration(arbitrationRole, electionId);
        P4RuntimeEvent event = new P4RuntimeEvent(P4RuntimeEvent.Type.ARBITRATION,
                                                  arbitrationEventSubject);
        controller.postEvent(event);
    }

    private Collection<PiCounterCellData> doReadAllCounterCells(
            Collection<PiCounterId> counterIds, PiPipeconf pipeconf) {
        return doReadCounterEntities(
                CounterEntryCodec.readAllCellsEntities(counterIds, pipeconf),
                pipeconf);
    }

    private Collection<PiCounterCellData> doReadCounterCells(
            Collection<PiCounterCellId> cellIds, PiPipeconf pipeconf) {
        return doReadCounterEntities(
                CounterEntryCodec.encodePiCounterCellIds(cellIds, pipeconf),
                pipeconf);
    }

    private Collection<PiCounterCellData> doReadCounterEntities(
            Collection<Entity> counterEntities, PiPipeconf pipeconf) {

        if (counterEntities.size() == 0) {
            return Collections.emptyList();
        }

        final ReadRequest request = ReadRequest.newBuilder()
                .setDeviceId(p4DeviceId)
                .addAllEntities(counterEntities)
                .build();

        final Iterable<ReadResponse> responses;
        try {
            responses = () -> blockingStub.read(request);
        } catch (StatusRuntimeException e) {
            log.warn("Unable to read counter cells from {}: {}", deviceId, e.getMessage());
            return Collections.emptyList();
        }

        List<Entity> entities = StreamSupport.stream(responses.spliterator(), false)
                .map(ReadResponse::getEntitiesList)
                .flatMap(List::stream)
                .collect(Collectors.toList());

        return CounterEntryCodec.decodeCounterEntities(entities, pipeconf);
    }

    private boolean doWriteActionGroupMembers(PiActionGroup group, WriteOperationType opType, PiPipeconf pipeconf) {
        final Collection<ActionProfileMember> actionProfileMembers = Lists.newArrayList();

        for (PiActionGroupMember member : group.members()) {
            try {
                actionProfileMembers.add(ActionProfileMemberEncoder.encode(group, member, pipeconf));
            } catch (EncodeException | P4InfoBrowser.NotFoundException e) {
                log.warn("Unable to encode group member, aborting {} operation: {} [{}]",
                         opType.name(), e.getMessage(), member.toString());
                return false;
            }
        }

        final Collection<Update> updateMsgs = actionProfileMembers.stream()
                .map(actionProfileMember ->
                             Update.newBuilder()
                                     .setEntity(Entity.newBuilder()
                                                        .setActionProfileMember(actionProfileMember)
                                                        .build())
                                     .setType(UPDATE_TYPES.get(opType))
                                     .build())
                .collect(Collectors.toList());

        if (updateMsgs.size() == 0) {
            // Nothing to update.
            return true;
        }

        WriteRequest writeRequestMsg = WriteRequest.newBuilder()
                .setDeviceId(p4DeviceId)
                .setElectionId(p4RuntimeElectionId)
                .addAllUpdates(updateMsgs)
                .build();
        try {
            blockingStub.write(writeRequestMsg);
            return true;
        } catch (StatusRuntimeException e) {
            logWriteErrors(group.members(), e, opType, "group member");
            return false;
        }
    }

    private Collection<PiActionGroup> doDumpGroups(PiActionProfileId piActionProfileId, PiPipeconf pipeconf) {
        log.debug("Dumping groups from action profile {} from {} (pipeconf {})...",
                  piActionProfileId.id(), deviceId, pipeconf.id());

        final P4InfoBrowser browser = PipeconfHelper.getP4InfoBrowser(pipeconf);
        if (browser == null) {
            log.warn("Unable to get a P4Info browser for pipeconf {}, aborting dump action profile", pipeconf);
            return Collections.emptySet();
        }

        final int actionProfileId;
        try {
            actionProfileId = browser
                    .actionProfiles()
                    .getByName(piActionProfileId.id())
                    .getPreamble()
                    .getId();
        } catch (P4InfoBrowser.NotFoundException e) {
            log.warn("Unable to dump groups: {}", e.getMessage());
            return Collections.emptySet();
        }

        // Prepare read request to read all groups from the given action profile.
        final ReadRequest groupRequestMsg = ReadRequest.newBuilder()
                .setDeviceId(p4DeviceId)
                .addEntities(Entity.newBuilder()
                                     .setActionProfileGroup(
                                             ActionProfileGroup.newBuilder()
                                                     .setActionProfileId(actionProfileId)
                                                     .build())
                                     .build())
                .build();

        // Read groups.
        final Iterator<ReadResponse> groupResponses;
        try {
            groupResponses = blockingStub.read(groupRequestMsg);
        } catch (StatusRuntimeException e) {
            log.warn("Unable to dump action profile {} from {}: {}", piActionProfileId, deviceId, e.getMessage());
            return Collections.emptySet();
        }

        final List<ActionProfileGroup> groupMsgs = Tools.stream(() -> groupResponses)
                .map(ReadResponse::getEntitiesList)
                .flatMap(List::stream)
                .filter(entity -> entity.getEntityCase() == ACTION_PROFILE_GROUP)
                .map(Entity::getActionProfileGroup)
                .collect(Collectors.toList());

        log.debug("Retrieved {} groups from action profile {} on {}...",
                  groupMsgs.size(), piActionProfileId.id(), deviceId);

        // Returned groups contain only a minimal description of their members.
        // We need to issue a new request to get the full description of each member.

        // Keep a map of all member IDs for each group ID, will need it later.
        final Multimap<Integer, Integer> groupIdToMemberIdsMap = HashMultimap.create();
        groupMsgs.forEach(g -> groupIdToMemberIdsMap.putAll(
                g.getGroupId(),
                g.getMembersList().stream()
                        .map(ActionProfileGroup.Member::getMemberId)
                        .collect(Collectors.toList())));

        // Prepare one big read request to read all members in one shot.
        final Set<Entity> entityMsgs = groupMsgs.stream()
                .flatMap(g -> g.getMembersList().stream())
                .map(ActionProfileGroup.Member::getMemberId)
                // Prevent issuing many read requests for the same member.
                .distinct()
                .map(id -> ActionProfileMember.newBuilder()
                        .setActionProfileId(actionProfileId)
                        .setMemberId(id)
                        .build())
                .map(m -> Entity.newBuilder()
                        .setActionProfileMember(m)
                        .build())
                .collect(Collectors.toSet());
        final ReadRequest memberRequestMsg = ReadRequest.newBuilder().setDeviceId(p4DeviceId)
                .addAllEntities(entityMsgs)
                .build();

        // Read members.
        final Iterator<ReadResponse> memberResponses;
        try {
            memberResponses = blockingStub.read(memberRequestMsg);
        } catch (StatusRuntimeException e) {
            log.warn("Unable to read members of action profile {} from {}: {}",
                     piActionProfileId, deviceId, e.getMessage());
            return Collections.emptyList();
        }

        final Multimap<Integer, ActionProfileMember> groupIdToMembersMap = HashMultimap.create();
        Tools.stream(() -> memberResponses)
                .map(ReadResponse::getEntitiesList)
                .flatMap(List::stream)
                .filter(e -> e.getEntityCase() == ACTION_PROFILE_MEMBER)
                .map(Entity::getActionProfileMember)
                .forEach(member -> groupIdToMemberIdsMap.asMap()
                        // Get all group IDs that contain this member.
                        .entrySet()
                        .stream()
                        .filter(entry -> entry.getValue().contains(member.getMemberId()))
                        .map(Map.Entry::getKey)
                        .forEach(gid -> groupIdToMembersMap.put(gid, member)));

        log.debug("Retrieved {} group members from action profile {} on {}...",
                  groupIdToMembersMap.size(), piActionProfileId.id(), deviceId);

        return groupMsgs.stream()
                .map(groupMsg -> {
                    try {
                        return ActionProfileGroupEncoder.decode(groupMsg,
                                                                groupIdToMembersMap.get(groupMsg.getGroupId()),
                                                                pipeconf);
                    } catch (P4InfoBrowser.NotFoundException | EncodeException e) {
                        log.warn("Unable to decode group: {}\n {}", e.getMessage(), groupMsg);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private boolean doWriteActionGroup(PiActionGroup group, WriteOperationType opType, PiPipeconf pipeconf) {
        final ActionProfileGroup actionProfileGroup;
        try {
            actionProfileGroup = ActionProfileGroupEncoder.encode(group, pipeconf);
        } catch (EncodeException | P4InfoBrowser.NotFoundException e) {
            log.warn("Unable to encode group, aborting {} operation: {}", e.getMessage(), opType.name());
            return false;
        }

        final WriteRequest writeRequestMsg = WriteRequest.newBuilder()
                .setDeviceId(p4DeviceId)
                .setElectionId(p4RuntimeElectionId)
                .addUpdates(Update.newBuilder()
                                    .setEntity(Entity.newBuilder()
                                                       .setActionProfileGroup(actionProfileGroup)
                                                       .build())
                                    .setType(UPDATE_TYPES.get(opType))
                                    .build())
                .build();
        try {
            blockingStub.write(writeRequestMsg);
            return true;
        } catch (StatusRuntimeException e) {
            logWriteErrors(Collections.singleton(group), e, opType, "group");
            return false;
        }
    }

    private Collection<PiMeterCellConfig> doReadAllMeterCells(
            Collection<PiMeterId> meterIds, PiPipeconf pipeconf) {
        return doReadMeterEntities(MeterEntryCodec.readAllCellsEntities(
                meterIds, pipeconf), pipeconf);
    }

    private Collection<PiMeterCellConfig> doReadMeterCells(
            Collection<PiMeterCellId> cellIds, PiPipeconf pipeconf) {

        final Collection<PiMeterCellConfig> piMeterCellConfigs = cellIds.stream()
                .map(cellId -> PiMeterCellConfig.builder()
                        .withMeterCellId(cellId)
                        .build())
                .collect(Collectors.toList());

        return doReadMeterEntities(MeterEntryCodec.encodePiMeterCellConfigs(
                piMeterCellConfigs, pipeconf), pipeconf);
    }

    private Collection<PiMeterCellConfig> doReadMeterEntities(
            Collection<Entity> entitiesToRead, PiPipeconf pipeconf) {

        if (entitiesToRead.size() == 0) {
            return Collections.emptyList();
        }

        final ReadRequest request = ReadRequest.newBuilder()
                .setDeviceId(p4DeviceId)
                .addAllEntities(entitiesToRead)
                .build();

        final Iterable<ReadResponse> responses;
        try {
            responses = () -> blockingStub.read(request);
        } catch (StatusRuntimeException e) {
            log.warn("Unable to read meter cells: {}", e.getMessage());
            log.debug("exception", e);
            return Collections.emptyList();
        }

        List<Entity> responseEntities = StreamSupport
                .stream(responses.spliterator(), false)
                .map(ReadResponse::getEntitiesList)
                .flatMap(List::stream)
                .collect(Collectors.toList());

        return MeterEntryCodec.decodeMeterEntities(responseEntities, pipeconf);
    }

    private boolean doWriteMeterCells(Collection<PiMeterCellConfig> cellIds, PiPipeconf pipeconf) {

        WriteRequest.Builder writeRequestBuilder = WriteRequest.newBuilder();

        Collection<Update> updateMsgs = MeterEntryCodec.encodePiMeterCellConfigs(cellIds, pipeconf)
                .stream()
                .map(meterEntryMsg ->
                             Update.newBuilder()
                                     .setEntity(meterEntryMsg)
                                     .setType(UPDATE_TYPES.get(WriteOperationType.MODIFY))
                                     .build())
                .collect(Collectors.toList());

        if (updateMsgs.size() == 0) {
            return true;
        }

        writeRequestBuilder
                .setDeviceId(p4DeviceId)
                .setElectionId(p4RuntimeElectionId)
                .addAllUpdates(updateMsgs)
                .build();
        try {
            blockingStub.write(writeRequestBuilder.build());
            return true;
        } catch (StatusRuntimeException e) {
            log.warn("Unable to write meter entries : {}", e.getMessage());
            log.debug("exception", e);
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
                Thread.currentThread().interrupt();
            }
        } finally {
            writeLock.unlock();
        }
    }

    private <E extends PiEntity> void logWriteErrors(Collection<E> writeEntities,
                                                     StatusRuntimeException ex,
                                                     WriteOperationType opType,
                                                     String entryType) {
        List<P4RuntimeOuterClass.Error> errors = null;
        String description = null;
        try {
            errors = extractWriteErrorDetails(ex);
        } catch (InvalidProtocolBufferException e) {
            description = ex.getStatus().getDescription();
        }

        log.warn("Unable to {} {} {}(s) on {}: {}{} (detailed errors might be logged below)",
                 opType.name(), writeEntities.size(), entryType, deviceId,
                 ex.getStatus().getCode().name(),
                 description == null ? "" : " - " + description);

        if (errors == null || errors.isEmpty()) {
            return;
        }

        // FIXME: we are assuming entities is an ordered collection, e.g. a list,
        // and that errors are reported in the same order as the corresponding
        // written entity. Write RPC methods should be refactored to accept an
        // order list of entities, instead of a collection.
        if (errors.size() == writeEntities.size()) {
            Iterator<E> entityIterator = writeEntities.iterator();
            errors.stream()
                    .map(e -> ImmutablePair.of(e, entityIterator.next()))
                    .filter(p -> p.left.getCanonicalCode() != Status.OK.getCode().value())
                    .forEach(p -> log.warn("Unable to {} {}: {} [{}]",
                                           opType.name(), entryType, parseP4Error(p.getLeft()),
                                           p.getRight().toString()));
        } else {
            log.error("Unable to reconcile error details to updates " +
                              "(sent {} updates, but device returned {} errors)",
                      entryType, writeEntities.size(), errors.size());
            errors.stream()
                    .filter(err -> err.getCanonicalCode() != Status.OK.getCode().value())
                    .forEach(err -> log.warn("Unable to {} {} (unknown): {}",
                                 opType.name(), entryType, parseP4Error(err)));
        }
    }

    private List<P4RuntimeOuterClass.Error> extractWriteErrorDetails(
            StatusRuntimeException ex) throws InvalidProtocolBufferException {
        String statusString = ex.getStatus().getDescription();
        if (statusString == null) {
            return Collections.emptyList();
        }
        com.google.rpc.Status status = com.google.rpc.Status
                .parseFrom(statusString.getBytes());
        return status.getDetailsList().stream()
                .map(any -> {
                    try {
                        return any.unpack(P4RuntimeOuterClass.Error.class);
                    } catch (InvalidProtocolBufferException e) {
                        log.warn("Unable to unpack P4Runtime Error: {}",
                                 any.toString());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

    }

    private String parseP4Error(P4RuntimeOuterClass.Error err) {
        return format("%s %s (%s code %d)%s",
                      Status.fromCodeValue(err.getCanonicalCode()),
                      err.getMessage(),
                      err.getSpace(),
                      err.getCode(),
                      err.hasDetails() ? "\n" + err.getDetails().toString() : "");
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
