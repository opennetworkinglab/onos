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
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.lite.ProtoLiteUtils;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.StreamObserver;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.util.Tools;
import org.onosproject.grpc.ctl.AbstractGrpcClient;
import org.onosproject.net.pi.model.PiActionProfileId;
import org.onosproject.net.pi.model.PiCounterId;
import org.onosproject.net.pi.model.PiMeterId;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.model.PiTableId;
import org.onosproject.net.pi.runtime.PiActionProfileGroup;
import org.onosproject.net.pi.runtime.PiActionProfileMember;
import org.onosproject.net.pi.runtime.PiActionProfileMemberId;
import org.onosproject.net.pi.runtime.PiCounterCell;
import org.onosproject.net.pi.runtime.PiCounterCellId;
import org.onosproject.net.pi.runtime.PiMeterCellConfig;
import org.onosproject.net.pi.runtime.PiMeterCellId;
import org.onosproject.net.pi.runtime.PiMulticastGroupEntry;
import org.onosproject.net.pi.runtime.PiPacketOperation;
import org.onosproject.net.pi.runtime.PiTableEntry;
import org.onosproject.net.pi.service.PiPipeconfService;
import org.onosproject.p4runtime.api.P4RuntimeClient;
import org.onosproject.p4runtime.api.P4RuntimeClientKey;
import org.onosproject.p4runtime.api.P4RuntimeEvent;
import p4.config.v1.P4InfoOuterClass.P4Info;
import p4.tmp.P4Config;
import p4.v1.P4RuntimeGrpc;
import p4.v1.P4RuntimeOuterClass;
import p4.v1.P4RuntimeOuterClass.ActionProfileGroup;
import p4.v1.P4RuntimeOuterClass.ActionProfileMember;
import p4.v1.P4RuntimeOuterClass.Entity;
import p4.v1.P4RuntimeOuterClass.ForwardingPipelineConfig;
import p4.v1.P4RuntimeOuterClass.GetForwardingPipelineConfigRequest;
import p4.v1.P4RuntimeOuterClass.GetForwardingPipelineConfigResponse;
import p4.v1.P4RuntimeOuterClass.MasterArbitrationUpdate;
import p4.v1.P4RuntimeOuterClass.MulticastGroupEntry;
import p4.v1.P4RuntimeOuterClass.PacketReplicationEngineEntry;
import p4.v1.P4RuntimeOuterClass.ReadRequest;
import p4.v1.P4RuntimeOuterClass.ReadResponse;
import p4.v1.P4RuntimeOuterClass.SetForwardingPipelineConfigRequest;
import p4.v1.P4RuntimeOuterClass.StreamMessageRequest;
import p4.v1.P4RuntimeOuterClass.StreamMessageResponse;
import p4.v1.P4RuntimeOuterClass.TableEntry;
import p4.v1.P4RuntimeOuterClass.Uint128;
import p4.v1.P4RuntimeOuterClass.Update;
import p4.v1.P4RuntimeOuterClass.WriteRequest;

import java.math.BigInteger;
import java.net.ConnectException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static p4.v1.P4RuntimeOuterClass.Entity.EntityCase.ACTION_PROFILE_GROUP;
import static p4.v1.P4RuntimeOuterClass.Entity.EntityCase.ACTION_PROFILE_MEMBER;
import static p4.v1.P4RuntimeOuterClass.Entity.EntityCase.PACKET_REPLICATION_ENGINE_ENTRY;
import static p4.v1.P4RuntimeOuterClass.Entity.EntityCase.TABLE_ENTRY;
import static p4.v1.P4RuntimeOuterClass.PacketIn;
import static p4.v1.P4RuntimeOuterClass.PacketOut;
import static p4.v1.P4RuntimeOuterClass.PacketReplicationEngineEntry.TypeCase.MULTICAST_GROUP_ENTRY;
import static p4.v1.P4RuntimeOuterClass.SetForwardingPipelineConfigRequest.Action.VERIFY_AND_COMMIT;

/**
 * Implementation of a P4Runtime client.
 */
final class P4RuntimeClientImpl extends AbstractGrpcClient implements P4RuntimeClient {

    private static final Metadata.Key<com.google.rpc.Status> STATUS_DETAILS_KEY =
            Metadata.Key.of("grpc-status-details-bin",
                            ProtoLiteUtils.metadataMarshaller(
                                    com.google.rpc.Status.getDefaultInstance()));

    private static final Map<WriteOperationType, Update.Type> UPDATE_TYPES = ImmutableMap.of(
            WriteOperationType.UNSPECIFIED, Update.Type.UNSPECIFIED,
            WriteOperationType.INSERT, Update.Type.INSERT,
            WriteOperationType.MODIFY, Update.Type.MODIFY,
            WriteOperationType.DELETE, Update.Type.DELETE
    );

    private final long p4DeviceId;
    private final P4RuntimeControllerImpl controller;
    private final P4RuntimeGrpc.P4RuntimeBlockingStub blockingStub;
    private StreamChannelManager streamChannelManager;

    // Used by this client for write requests.
    private Uint128 clientElectionId = Uint128.newBuilder().setLow(1).build();

    private final AtomicBoolean isClientMaster = new AtomicBoolean(false);

    /**
     * Default constructor.
     *
     * @param clientKey  the client key of this client
     * @param channel    gRPC channel
     * @param controller runtime client controller
     */
    P4RuntimeClientImpl(P4RuntimeClientKey clientKey, ManagedChannel channel,
                        P4RuntimeControllerImpl controller) {

        super(clientKey);
        this.p4DeviceId = clientKey.p4DeviceId();
        this.controller = controller;

        //TODO Investigate use of stub deadlines instead of timeout in supplyInContext
        this.blockingStub = P4RuntimeGrpc.newBlockingStub(channel);
        this.streamChannelManager = new StreamChannelManager(channel);
    }

    @Override
    public CompletableFuture<Boolean> startStreamChannel() {
        return supplyInContext(() -> sendMasterArbitrationUpdate(false),
                               "start-initStreamChannel");
    }

    @Override
    public CompletableFuture<Boolean> becomeMaster() {
        return supplyInContext(() -> sendMasterArbitrationUpdate(true),
                               "becomeMaster");
    }

    @Override
    public boolean isMaster() {
        return streamChannelManager.isOpen() && isClientMaster.get();
    }

    @Override
    public boolean isStreamChannelOpen() {
        return streamChannelManager.isOpen();
    }

    @Override
    public CompletableFuture<Boolean> setPipelineConfig(PiPipeconf pipeconf, ByteBuffer deviceData) {
        return supplyInContext(() -> doSetPipelineConfig(pipeconf, deviceData), "setPipelineConfig");
    }

    @Override
    public boolean isPipelineConfigSet(PiPipeconf pipeconf, ByteBuffer deviceData) {
        return doIsPipelineConfigSet(pipeconf, deviceData);
    }

    @Override
    public CompletableFuture<Boolean> writeTableEntries(List<PiTableEntry> piTableEntries,
                                                        WriteOperationType opType, PiPipeconf pipeconf) {
        return supplyInContext(() -> doWriteTableEntries(piTableEntries, opType, pipeconf),
                               "writeTableEntries-" + opType.name());
    }

    @Override
    public CompletableFuture<List<PiTableEntry>> dumpTables(
            Set<PiTableId> piTableIds, boolean defaultEntries, PiPipeconf pipeconf) {
        return supplyInContext(() -> doDumpTables(piTableIds, defaultEntries, pipeconf),
                               "dumpTables-" + piTableIds.hashCode());
    }

    @Override
    public CompletableFuture<List<PiTableEntry>> dumpAllTables(PiPipeconf pipeconf) {
        return supplyInContext(() -> doDumpTables(null, false, pipeconf), "dumpAllTables");
    }

    @Override
    public CompletableFuture<Boolean> packetOut(PiPacketOperation packet, PiPipeconf pipeconf) {
        return supplyInContext(() -> doPacketOut(packet, pipeconf), "packetOut");
    }

    @Override
    public CompletableFuture<List<PiCounterCell>> readCounterCells(Set<PiCounterCellId> cellIds,
                                                                   PiPipeconf pipeconf) {
        return supplyInContext(() -> doReadCounterCells(Lists.newArrayList(cellIds), pipeconf),
                               "readCounterCells-" + cellIds.hashCode());
    }

    @Override
    public CompletableFuture<List<PiCounterCell>> readAllCounterCells(Set<PiCounterId> counterIds,
                                                                      PiPipeconf pipeconf) {
        return supplyInContext(() -> doReadAllCounterCells(Lists.newArrayList(counterIds), pipeconf),
                               "readAllCounterCells-" + counterIds.hashCode());
    }

    @Override
    public CompletableFuture<Boolean> writeActionProfileMembers(List<PiActionProfileMember> members,
                                                                WriteOperationType opType,
                                                                PiPipeconf pipeconf) {
        return supplyInContext(() -> doWriteActionProfileMembers(members, opType, pipeconf),
                               "writeActionProfileMembers-" + opType.name());
    }


    @Override
    public CompletableFuture<Boolean> writeActionProfileGroup(PiActionProfileGroup group,
                                                              WriteOperationType opType,
                                                              PiPipeconf pipeconf,
                                                       int maxMemberSize) {
        return supplyInContext(() -> doWriteActionProfileGroup(group, opType, pipeconf, maxMemberSize),
                               "writeActionProfileGroup-" + opType.name());
    }

    @Override
    public CompletableFuture<List<PiActionProfileGroup>> dumpActionProfileGroups(
            PiActionProfileId actionProfileId, PiPipeconf pipeconf) {
        return supplyInContext(() -> doDumpGroups(actionProfileId, pipeconf),
                               "dumpActionProfileGroups-" + actionProfileId.id());
    }

    @Override
    public CompletableFuture<List<PiActionProfileMemberId>> dumpActionProfileMemberIds(
            PiActionProfileId actionProfileId, PiPipeconf pipeconf) {
        return supplyInContext(() -> doDumpActionProfileMemberIds(actionProfileId, pipeconf),
                               "dumpActionProfileMemberIds-" + actionProfileId.id());
    }

    @Override
    public CompletableFuture<List<PiActionProfileMemberId>> removeActionProfileMembers(
            PiActionProfileId actionProfileId,
            List<PiActionProfileMemberId> memberIds,
            PiPipeconf pipeconf) {
        return supplyInContext(
                () -> doRemoveActionProfileMembers(actionProfileId, memberIds, pipeconf),
                "cleanupActionProfileMembers-" + actionProfileId.id());
    }

    @Override
    public CompletableFuture<Boolean> writeMeterCells(List<PiMeterCellConfig> cellIds, PiPipeconf pipeconf) {

        return supplyInContext(() -> doWriteMeterCells(cellIds, pipeconf),
                               "writeMeterCells");
    }

    @Override
    public CompletableFuture<Boolean> writePreMulticastGroupEntries(
            List<PiMulticastGroupEntry> entries,
            WriteOperationType opType) {
        return supplyInContext(() -> doWriteMulticastGroupEntries(entries, opType),
                               "writePreMulticastGroupEntries");
    }

    @Override
    public CompletableFuture<List<PiMulticastGroupEntry>> readAllMulticastGroupEntries() {
        return supplyInContext(this::doReadAllMulticastGroupEntries,
                               "readAllMulticastGroupEntries");
    }

    @Override
    public CompletableFuture<List<PiMeterCellConfig>> readMeterCells(Set<PiMeterCellId> cellIds,
                                                                     PiPipeconf pipeconf) {
        return supplyInContext(() -> doReadMeterCells(Lists.newArrayList(cellIds), pipeconf),
                               "readMeterCells-" + cellIds.hashCode());
    }

    @Override
    public CompletableFuture<List<PiMeterCellConfig>> readAllMeterCells(Set<PiMeterId> meterIds,
                                                                        PiPipeconf pipeconf) {
        return supplyInContext(() -> doReadAllMeterCells(Lists.newArrayList(meterIds), pipeconf),
                               "readAllMeterCells-" + meterIds.hashCode());
    }

    /* Blocking method implementations below */

    private boolean sendMasterArbitrationUpdate(boolean asMaster) {
        BigInteger newId = controller.newMasterElectionId(deviceId);
        if (asMaster) {
            // Becoming master is a race. Here we increase our chances of win
            // against other ONOS nodes in the cluster that are calling start()
            // (which is used to start the stream RPC session, not to become
            // master).
            newId = newId.add(BigInteger.valueOf(1000));
        }
        final Uint128 idMsg = bigIntegerToUint128(
                controller.newMasterElectionId(deviceId));

        log.debug("Sending arbitration update to {}... electionId={}",
                  deviceId, newId);

        streamChannelManager.send(
                StreamMessageRequest.newBuilder()
                        .setArbitration(
                                MasterArbitrationUpdate
                                        .newBuilder()
                                        .setDeviceId(p4DeviceId)
                                        .setElectionId(idMsg)
                                        .build())
                        .build());
        clientElectionId = idMsg;
        return true;
    }

    private ForwardingPipelineConfig getPipelineConfig(
            PiPipeconf pipeconf, ByteBuffer deviceData) {
        P4Info p4Info = PipeconfHelper.getP4Info(pipeconf);
        if (p4Info == null) {
            // Problem logged by PipeconfHelper.
            return null;
        }

        ForwardingPipelineConfig.Cookie pipeconfCookie = ForwardingPipelineConfig.Cookie
                .newBuilder()
                .setCookie(pipeconf.fingerprint())
                .build();

        // FIXME: This is specific to PI P4Runtime implementation.
        P4Config.P4DeviceConfig p4DeviceConfigMsg = P4Config.P4DeviceConfig
                .newBuilder()
                .setExtras(P4Config.P4DeviceConfig.Extras.getDefaultInstance())
                .setReassign(true)
                .setDeviceData(ByteString.copyFrom(deviceData))
                .build();

        return ForwardingPipelineConfig
                .newBuilder()
                .setP4Info(p4Info)
                .setP4DeviceConfig(p4DeviceConfigMsg.toByteString())
                .setCookie(pipeconfCookie)
                .build();
    }

    private boolean doIsPipelineConfigSet(PiPipeconf pipeconf, ByteBuffer deviceData) {

        GetForwardingPipelineConfigRequest request = GetForwardingPipelineConfigRequest
                .newBuilder()
                .setDeviceId(p4DeviceId)
                .setResponseType(GetForwardingPipelineConfigRequest
                                         .ResponseType.COOKIE_ONLY)
                .build();

        GetForwardingPipelineConfigResponse resp;
        try {
            resp = this.blockingStub
                    .getForwardingPipelineConfig(request);
        } catch (StatusRuntimeException ex) {
            checkGrpcException(ex);
            // FAILED_PRECONDITION means that a pipeline config was not set in
            // the first place. Don't bother logging.
            if (!ex.getStatus().getCode()
                    .equals(Status.FAILED_PRECONDITION.getCode())) {
                log.warn("Unable to get pipeline config from {}: {}",
                         deviceId, ex.getMessage());
            }
            return false;
        }
        if (!resp.getConfig().hasCookie()) {
            log.warn("{} returned GetForwardingPipelineConfigResponse " +
                             "with 'cookie' field unset. " +
                             "Will try by comparing 'device_data'...",
                     deviceId);
            return doIsPipelineConfigSetWithData(pipeconf, deviceData);
        }

        return resp.getConfig().getCookie().getCookie() == pipeconf.fingerprint();
    }

    private boolean doIsPipelineConfigSetWithData(PiPipeconf pipeconf, ByteBuffer deviceData) {

        GetForwardingPipelineConfigRequest request = GetForwardingPipelineConfigRequest
                .newBuilder()
                .setDeviceId(p4DeviceId)
                .build();

        GetForwardingPipelineConfigResponse resp;
        try {
            resp = this.blockingStub
                    .getForwardingPipelineConfig(request);
        } catch (StatusRuntimeException ex) {
            checkGrpcException(ex);
            return false;
        }

        ForwardingPipelineConfig expectedConfig = getPipelineConfig(
                pipeconf, deviceData);

        if (expectedConfig == null) {
            return false;
        }
        if (!resp.hasConfig()) {
            log.warn("{} returned GetForwardingPipelineConfigResponse " +
                             "with 'config' field unset",
                     deviceId);
            return false;
        }
        if (resp.getConfig().getP4DeviceConfig().isEmpty()
                && !expectedConfig.getP4DeviceConfig().isEmpty()) {
            // Don't bother with a warn or error since we don't really allow
            // updating the pipeline to a different one. So the P4Info should be
            // enough for us.
            log.debug("{} returned GetForwardingPipelineConfigResponse " +
                              "with empty 'p4_device_config' field, " +
                              "equality will be based only on P4Info",
                      deviceId);
            return resp.getConfig().getP4Info().equals(
                    expectedConfig.getP4Info());
        } else {
            return resp.getConfig().getP4DeviceConfig()
                    .equals(expectedConfig.getP4DeviceConfig())
                    && resp.getConfig().getP4Info()
                    .equals(expectedConfig.getP4Info());
        }
    }

    private boolean doSetPipelineConfig(PiPipeconf pipeconf, ByteBuffer deviceData) {

        log.info("Setting pipeline config for {} to {}...", deviceId, pipeconf.id());

        checkNotNull(deviceData, "deviceData cannot be null");

        ForwardingPipelineConfig pipelineConfig = getPipelineConfig(pipeconf, deviceData);

        if (pipelineConfig == null) {
            // Error logged in getPipelineConfig()
            return false;
        }

        SetForwardingPipelineConfigRequest request = SetForwardingPipelineConfigRequest
                .newBuilder()
                .setDeviceId(p4DeviceId)
                .setElectionId(clientElectionId)
                .setAction(VERIFY_AND_COMMIT)
                .setConfig(pipelineConfig)
                .build();

        try {
            //noinspection ResultOfMethodCallIgnored
            this.blockingStub.setForwardingPipelineConfig(request);
            return true;
        } catch (StatusRuntimeException ex) {
            checkGrpcException(ex);
            log.warn("Unable to set pipeline config on {}: {}", deviceId, ex.getMessage());
            return false;
        }
    }

    private boolean doWriteTableEntries(List<PiTableEntry> piTableEntries, WriteOperationType opType,
                                        PiPipeconf pipeconf) {
        if (piTableEntries.size() == 0) {
            return true;
        }

        List<Update> updateMsgs;
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

        return write(updateMsgs, piTableEntries, opType, "table entry");
    }

    private List<PiTableEntry> doDumpTables(
            Set<PiTableId> piTableIds, boolean defaultEntries, PiPipeconf pipeconf) {

        log.debug("Dumping tables {} from {} (pipeconf {})...",
                  piTableIds, deviceId, pipeconf.id());

        Set<Integer> tableIds = Sets.newHashSet();
        if (piTableIds == null) {
            // Dump all tables.
            tableIds.add(0);
        } else {
            P4InfoBrowser browser = PipeconfHelper.getP4InfoBrowser(pipeconf);
            if (browser == null) {
                log.warn("Unable to get a P4Info browser for pipeconf {}", pipeconf);
                return Collections.emptyList();
            }
            piTableIds.forEach(piTableId -> {
                try {
                    tableIds.add(browser.tables().getByName(piTableId.id()).getPreamble().getId());
                } catch (P4InfoBrowser.NotFoundException e) {
                    log.warn("Unable to dump table {}: {}", piTableId, e.getMessage());
                }
            });
        }

        if (tableIds.isEmpty()) {
            return Collections.emptyList();
        }

        ReadRequest.Builder requestMsgBuilder = ReadRequest.newBuilder()
                .setDeviceId(p4DeviceId);
        tableIds.forEach(tableId -> requestMsgBuilder.addEntities(
                Entity.newBuilder()
                        .setTableEntry(
                                TableEntry.newBuilder()
                                        .setTableId(tableId)
                                        .setIsDefaultAction(defaultEntries)
                                        .setCounterData(P4RuntimeOuterClass.CounterData.getDefaultInstance())
                                        .build())
                        .build())
                .build());

        Iterator<ReadResponse> responses;
        try {
            responses = blockingStub.read(requestMsgBuilder.build());
        } catch (StatusRuntimeException e) {
            checkGrpcException(e);
            log.warn("Unable to dump tables from {}: {}", deviceId, e.getMessage());
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

        log.debug("Retrieved {} entries from {} tables on {}...",
                  tableEntryMsgs.size(), tableIds.size(), deviceId);

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
            streamChannelManager.send(packetOutRequest);

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
        PacketInEvent packetInEventSubject = new PacketInEvent(deviceId, packetOperation);
        P4RuntimeEvent event = new P4RuntimeEvent(P4RuntimeEvent.Type.PACKET_IN, packetInEventSubject);
        log.debug("Received packet in: {}", event);
        controller.postEvent(event);
    }

    private void doArbitrationResponse(MasterArbitrationUpdate msg) {
        // From the spec...
        // - Election_id: The stream RPC with the highest election_id is the
        // master. Switch populates with the highest election ID it
        // has received from all connected controllers.
        // - Status: Switch populates this with OK for the client that is the
        // master, and with an error status for all other connected clients (at
        // every mastership change).
        if (!msg.hasElectionId() || !msg.hasStatus()) {
            return;
        }
        final boolean isMaster =
                msg.getStatus().getCode() == Status.OK.getCode().value();
        log.debug("Received arbitration update from {}: isMaster={}, electionId={}",
                  deviceId, isMaster, uint128ToBigInteger(msg.getElectionId()));
        controller.postEvent(new P4RuntimeEvent(
                P4RuntimeEvent.Type.ARBITRATION_RESPONSE,
                new ArbitrationResponse(deviceId, isMaster)));
        isClientMaster.set(isMaster);
    }

    private List<PiCounterCell> doReadAllCounterCells(
            List<PiCounterId> counterIds, PiPipeconf pipeconf) {
        return doReadCounterEntities(
                CounterEntryCodec.readAllCellsEntities(counterIds, pipeconf),
                pipeconf);
    }

    private List<PiCounterCell> doReadCounterCells(
            List<PiCounterCellId> cellIds, PiPipeconf pipeconf) {
        return doReadCounterEntities(
                CounterEntryCodec.encodePiCounterCellIds(cellIds, pipeconf),
                pipeconf);
    }

    private List<PiCounterCell> doReadCounterEntities(
            List<Entity> counterEntities, PiPipeconf pipeconf) {

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
            checkGrpcException(e);
            log.warn("Unable to read counter cells from {}: {}", deviceId, e.getMessage());
            return Collections.emptyList();
        }

        List<Entity> entities = StreamSupport.stream(responses.spliterator(), false)
                .map(ReadResponse::getEntitiesList)
                .flatMap(List::stream)
                .collect(Collectors.toList());

        return CounterEntryCodec.decodeCounterEntities(entities, pipeconf);
    }

    private boolean doWriteActionProfileMembers(List<PiActionProfileMember> members,
                                                WriteOperationType opType, PiPipeconf pipeconf) {
        final List<ActionProfileMember> actionProfileMembers = Lists.newArrayList();

        for (PiActionProfileMember member : members) {
            try {
                actionProfileMembers.add(ActionProfileMemberEncoder.encode(member, pipeconf));
            } catch (EncodeException | P4InfoBrowser.NotFoundException e) {
                log.warn("Unable to encode action profile member, aborting {} operation: {} [{}]",
                         opType.name(), e.getMessage(), member.toString());
                return false;
            }
        }

        final List<Update> updateMsgs = actionProfileMembers.stream()
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

        return write(updateMsgs, members, opType, "action profile member");
    }

    private List<PiActionProfileGroup> doDumpGroups(PiActionProfileId piActionProfileId, PiPipeconf pipeconf) {
        log.debug("Dumping groups from action profile {} from {} (pipeconf {})...",
                  piActionProfileId.id(), deviceId, pipeconf.id());

        final P4InfoBrowser browser = PipeconfHelper.getP4InfoBrowser(pipeconf);
        if (browser == null) {
            log.warn("Unable to get a P4Info browser for pipeconf {}, aborting dump action profile", pipeconf);
            return Collections.emptyList();
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
            return Collections.emptyList();
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
            checkGrpcException(e);
            log.warn("Unable to dump action profile {} from {}: {}", piActionProfileId, deviceId, e.getMessage());
            return Collections.emptyList();
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
            checkGrpcException(e);
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

        log.debug("Retrieved {} members from action profile {} on {}...",
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

    private List<PiActionProfileMemberId> doDumpActionProfileMemberIds(
            PiActionProfileId actionProfileId, PiPipeconf pipeconf) {

        final P4InfoBrowser browser = PipeconfHelper.getP4InfoBrowser(pipeconf);
        if (browser == null) {
            log.warn("Unable to get a P4Info browser for pipeconf {}, " +
                             "aborting cleanup of action profile members",
                     pipeconf);
            return Collections.emptyList();
        }

        final int p4ActProfId;
        try {
            p4ActProfId = browser
                    .actionProfiles()
                    .getByName(actionProfileId.id())
                    .getPreamble()
                    .getId();
        } catch (P4InfoBrowser.NotFoundException e) {
            log.warn("Unable to cleanup action profile members: {}", e.getMessage());
            return Collections.emptyList();
        }

        final ReadRequest memberRequestMsg = ReadRequest.newBuilder()
                .setDeviceId(p4DeviceId)
                .addEntities(Entity.newBuilder().setActionProfileMember(
                        ActionProfileMember.newBuilder()
                                .setActionProfileId(p4ActProfId)
                                .build()).build())
                .build();

        // Read members.
        final Iterator<ReadResponse> memberResponses;
        try {
            memberResponses = blockingStub.read(memberRequestMsg);
        } catch (StatusRuntimeException e) {
            checkGrpcException(e);
            log.warn("Unable to read members of action profile {} from {}: {}",
                     actionProfileId, deviceId, e.getMessage());
            return Collections.emptyList();
        }

        return Tools.stream(() -> memberResponses)
                .map(ReadResponse::getEntitiesList)
                .flatMap(List::stream)
                .filter(e -> e.getEntityCase() == ACTION_PROFILE_MEMBER)
                .map(Entity::getActionProfileMember)
                // Perhaps not needed, but better to double check to avoid
                // removing members of other groups.
                .filter(m -> m.getActionProfileId() == p4ActProfId)
                .map(ActionProfileMember::getMemberId)
                .map(PiActionProfileMemberId::of)
                .collect(Collectors.toList());
    }

    private List<PiActionProfileMemberId> doRemoveActionProfileMembers(
            PiActionProfileId actionProfileId,
            List<PiActionProfileMemberId> memberIds,
            PiPipeconf pipeconf) {

        if (memberIds.isEmpty()) {
            return Collections.emptyList();
        }

        final P4InfoBrowser browser = PipeconfHelper.getP4InfoBrowser(pipeconf);
        if (browser == null) {
            log.warn("Unable to get a P4Info browser for pipeconf {}, " +
                             "aborting cleanup of action profile members",
                     pipeconf);
            return Collections.emptyList();
        }

        final int p4ActProfId;
        try {
            p4ActProfId = browser.actionProfiles()
                    .getByName(actionProfileId.id()).getPreamble().getId();
        } catch (P4InfoBrowser.NotFoundException e) {
            log.warn("Unable to cleanup action profile members: {}", e.getMessage());
            return Collections.emptyList();
        }

        final List<Update> updateMsgs = memberIds.stream()
                .map(m -> ActionProfileMember.newBuilder()
                        .setActionProfileId(p4ActProfId)
                        .setMemberId(m.id()).build())
                .map(m -> Entity.newBuilder().setActionProfileMember(m).build())
                .map(e -> Update.newBuilder().setEntity(e)
                        .setType(Update.Type.DELETE).build())
                .collect(Collectors.toList());

        log.debug("Removing {} members of action profile '{}'...",
                  memberIds.size(), actionProfileId);

        return writeAndReturnSuccessEntities(
                updateMsgs, memberIds, WriteOperationType.DELETE,
                "action profile members");
    }

    private boolean doWriteActionProfileGroup(
            PiActionProfileGroup group, WriteOperationType opType, PiPipeconf pipeconf,
                                       int maxMemberSize) {
        final ActionProfileGroup actionProfileGroup;
        if (opType == P4RuntimeClient.WriteOperationType.INSERT && maxMemberSize < group.members().size()) {
            log.warn("Unable to encode group, since group member larger than maximum member size");
            return false;
        }
        try {
            actionProfileGroup = ActionProfileGroupEncoder.encode(group, pipeconf, maxMemberSize);
        } catch (EncodeException | P4InfoBrowser.NotFoundException e) {
            log.warn("Unable to encode group, aborting {} operation: {}", e.getMessage(), opType.name());
            return false;
        }

        final Update updateMsg = Update.newBuilder()
                .setEntity(Entity.newBuilder()
                                   .setActionProfileGroup(actionProfileGroup)
                                   .build())
                .setType(UPDATE_TYPES.get(opType))
                .build();

        return write(singletonList(updateMsg), singletonList(group),
                     opType, "group");
    }

    private List<PiMeterCellConfig> doReadAllMeterCells(
            List<PiMeterId> meterIds, PiPipeconf pipeconf) {
        return doReadMeterEntities(MeterEntryCodec.readAllCellsEntities(
                meterIds, pipeconf), pipeconf);
    }

    private List<PiMeterCellConfig> doReadMeterCells(
            List<PiMeterCellId> cellIds, PiPipeconf pipeconf) {

        final List<PiMeterCellConfig> piMeterCellConfigs = cellIds.stream()
                .map(cellId -> PiMeterCellConfig.builder()
                        .withMeterCellId(cellId)
                        .build())
                .collect(Collectors.toList());

        return doReadMeterEntities(MeterEntryCodec.encodePiMeterCellConfigs(
                piMeterCellConfigs, pipeconf), pipeconf);
    }

    private List<PiMeterCellConfig> doReadMeterEntities(
            List<Entity> entitiesToRead, PiPipeconf pipeconf) {

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
            checkGrpcException(e);
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

    private boolean doWriteMeterCells(List<PiMeterCellConfig> cellConfigs, PiPipeconf pipeconf) {

        List<Update> updateMsgs = MeterEntryCodec.encodePiMeterCellConfigs(cellConfigs, pipeconf)
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

        return write(updateMsgs, cellConfigs, WriteOperationType.MODIFY, "meter cell config");
    }

    private boolean doWriteMulticastGroupEntries(
            List<PiMulticastGroupEntry> entries,
            WriteOperationType opType) {

        final List<Update> updateMsgs = entries.stream()
                .map(piEntry -> {
                    try {
                        return MulticastGroupEntryCodec.encode(piEntry);
                    } catch (EncodeException e) {
                        log.warn("Unable to encode PiMulticastGroupEntry: {}", e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .map(mcMsg -> PacketReplicationEngineEntry.newBuilder()
                        .setMulticastGroupEntry(mcMsg)
                        .build())
                .map(preMsg -> Entity.newBuilder()
                        .setPacketReplicationEngineEntry(preMsg)
                        .build())
                .map(entityMsg -> Update.newBuilder()
                        .setEntity(entityMsg)
                        .setType(UPDATE_TYPES.get(opType))
                        .build())
                .collect(Collectors.toList());
        return write(updateMsgs, entries, opType, "multicast group entry");
    }

    private List<PiMulticastGroupEntry> doReadAllMulticastGroupEntries() {

        final Entity entity = Entity.newBuilder()
                .setPacketReplicationEngineEntry(
                        PacketReplicationEngineEntry.newBuilder()
                                .setMulticastGroupEntry(
                                        MulticastGroupEntry.newBuilder()
                                                .build())
                                .build())
                .build();

        final ReadRequest req = ReadRequest.newBuilder()
                .setDeviceId(p4DeviceId)
                .addEntities(entity)
                .build();

        Iterator<ReadResponse> responses;
        try {
            responses = blockingStub.read(req);
        } catch (StatusRuntimeException e) {
            checkGrpcException(e);
            log.warn("Unable to read multicast group entries from {}: {}", deviceId, e.getMessage());
            return Collections.emptyList();
        }

        Iterable<ReadResponse> responseIterable = () -> responses;
        final List<PiMulticastGroupEntry> mcEntries = StreamSupport
                .stream(responseIterable.spliterator(), false)
                .map(ReadResponse::getEntitiesList)
                .flatMap(List::stream)
                .filter(e -> e.getEntityCase()
                        .equals(PACKET_REPLICATION_ENGINE_ENTRY))
                .map(Entity::getPacketReplicationEngineEntry)
                .filter(e -> e.getTypeCase().equals(MULTICAST_GROUP_ENTRY))
                .map(PacketReplicationEngineEntry::getMulticastGroupEntry)
                .map(MulticastGroupEntryCodec::decode)
                .collect(Collectors.toList());

        log.debug("Retrieved {} multicast group entries from {}...",
                  mcEntries.size(), deviceId);

        return mcEntries;
    }

    private <T> boolean write(List<Update> updates,
                              List<T> writeEntities,
                              WriteOperationType opType,
                              String entryType) {
        // True if all entities were successfully written.
        return writeAndReturnSuccessEntities(updates, writeEntities, opType, entryType)
                .size() == writeEntities.size();
    }

    private <T> List<T> writeAndReturnSuccessEntities(
            List<Update> updates, List<T> writeEntities,
            WriteOperationType opType, String entryType) {
        if (updates.isEmpty()) {
            return Collections.emptyList();
        }
        if (updates.size() != writeEntities.size()) {
            log.error("Cannot perform {} operation, provided {} " +
                              "update messages for {} {} - BUG?",
                      opType, updates.size(), writeEntities.size(), entryType);
            return Collections.emptyList();
        }
        try {
            //noinspection ResultOfMethodCallIgnored
            blockingStub.write(writeRequest(updates));
            return writeEntities;
        } catch (StatusRuntimeException e) {
            return checkAndLogWriteErrors(writeEntities, e, opType, entryType);
        }
    }

    private WriteRequest writeRequest(Iterable<Update> updateMsgs) {
        return WriteRequest.newBuilder()
                .setDeviceId(p4DeviceId)
                .setElectionId(clientElectionId)
                .addAllUpdates(updateMsgs)
                .build();
    }

    protected Void doShutdown() {
        streamChannelManager.complete();
        return super.doShutdown();
    }

    // Returns the collection of succesfully write entities.
    private <T> List<T> checkAndLogWriteErrors(
            List<T> writeEntities, StatusRuntimeException ex,
            WriteOperationType opType, String entryType) {

        checkGrpcException(ex);

        final List<P4RuntimeOuterClass.Error> errors = extractWriteErrorDetails(ex);

        if (errors.isEmpty()) {
            final String description = ex.getStatus().getDescription();
            log.warn("Unable to {} {} {}(s) on {}: {}",
                     opType.name(), writeEntities.size(), entryType, deviceId,
                     ex.getStatus().getCode().name(),
                     description == null ? "" : " - " + description);
            return Collections.emptyList();
        }

        if (errors.size() == writeEntities.size()) {
            List<T> okEntities = Lists.newArrayList();
            Iterator<T> entityIterator = writeEntities.iterator();
            for (P4RuntimeOuterClass.Error error : errors) {
                T entity = entityIterator.next();
                if (error.getCanonicalCode() != Status.OK.getCode().value()) {
                    log.warn("Unable to {} {} on {}: {} [{}]",
                             opType.name(), entryType, deviceId,
                             parseP4Error(error), entity.toString());
                } else {
                    okEntities.add(entity);
                }
            }
            return okEntities;
        } else {
            log.warn("Unable to reconcile error details to {} updates " +
                             "(sent {} updates, but device returned {} errors)",
                     entryType, writeEntities.size(), errors.size());
            errors.stream()
                    .filter(err -> err.getCanonicalCode() != Status.OK.getCode().value())
                    .forEach(err -> log.warn("Unable to {} {} (unknown): {}",
                                             opType.name(), entryType, parseP4Error(err)));
            return Collections.emptyList();
        }
    }

    private List<P4RuntimeOuterClass.Error> extractWriteErrorDetails(
            StatusRuntimeException ex) {
        if (!ex.getTrailers().containsKey(STATUS_DETAILS_KEY)) {
            return Collections.emptyList();
        }
        com.google.rpc.Status status = ex.getTrailers().get(STATUS_DETAILS_KEY);
        if (status == null) {
            return Collections.emptyList();
        }
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
        return format("%s %s%s (%s:%d)",
                      Status.fromCodeValue(err.getCanonicalCode()).getCode(),
                      err.getMessage(),
                      err.hasDetails() ? ", " + err.getDetails().toString() : "",
                      err.getSpace(),
                      err.getCode());
    }

    private void checkGrpcException(StatusRuntimeException ex) {
        switch (ex.getStatus().getCode()) {
            case OK:
                break;
            case CANCELLED:
                break;
            case UNKNOWN:
                break;
            case INVALID_ARGUMENT:
                break;
            case DEADLINE_EXCEEDED:
                break;
            case NOT_FOUND:
                break;
            case ALREADY_EXISTS:
                break;
            case PERMISSION_DENIED:
                // Notify upper layers that this node is not master.
                controller.postEvent(new P4RuntimeEvent(
                        P4RuntimeEvent.Type.PERMISSION_DENIED,
                        new BaseP4RuntimeEventSubject(deviceId)));
                break;
            case RESOURCE_EXHAUSTED:
                break;
            case FAILED_PRECONDITION:
                break;
            case ABORTED:
                break;
            case OUT_OF_RANGE:
                break;
            case UNIMPLEMENTED:
                break;
            case INTERNAL:
                break;
            case UNAVAILABLE:
                // Channel might be closed.
                controller.postEvent(new P4RuntimeEvent(
                        P4RuntimeEvent.Type.CHANNEL_EVENT,
                        new ChannelEvent(deviceId, ChannelEvent.Type.ERROR)));
                break;
            case DATA_LOSS:
                break;
            case UNAUTHENTICATED:
                break;
            default:
                break;
        }
    }

    private Uint128 bigIntegerToUint128(BigInteger value) {
        final byte[] arr = value.toByteArray();
        final ByteBuffer bb = ByteBuffer.allocate(Long.BYTES * 2)
                .put(new byte[Long.BYTES * 2 - arr.length])
                .put(arr);
        bb.rewind();
        return Uint128.newBuilder()
                .setHigh(bb.getLong())
                .setLow(bb.getLong())
                .build();
    }

    private BigInteger uint128ToBigInteger(Uint128 value) {
        return new BigInteger(
                ByteBuffer.allocate(Long.BYTES * 2)
                        .putLong(value.getHigh())
                        .putLong(value.getLow())
                        .array());
    }

    /**
     * A manager for the P4Runtime stream channel that opportunistically creates
     * new stream RCP stubs (e.g. when one fails because of errors) and posts
     * channel events via the P4Runtime controller.
     */
    private final class StreamChannelManager {

        private final ManagedChannel channel;
        private final AtomicBoolean open;
        private final StreamObserver<StreamMessageResponse> responseObserver;
        private ClientCallStreamObserver<StreamMessageRequest> requestObserver;

        private StreamChannelManager(ManagedChannel channel) {
            this.channel = channel;
            this.responseObserver = new InternalStreamResponseObserver(this);
            this.open = new AtomicBoolean(false);
        }

        private void initIfRequired() {
            if (requestObserver == null) {
                log.debug("Creating new stream channel for {}...", deviceId);
                requestObserver =
                        (ClientCallStreamObserver<StreamMessageRequest>)
                                P4RuntimeGrpc.newStub(channel)
                                        .streamChannel(responseObserver);
                open.set(false);
            }
        }

        public boolean send(StreamMessageRequest value) {
            synchronized (this) {
                initIfRequired();
                try {
                    requestObserver.onNext(value);
                    // FIXME
                    // signalOpen();
                    return true;
                } catch (Throwable ex) {
                    if (ex instanceof StatusRuntimeException) {
                        log.warn("Unable to send {} to {}: {}",
                                 value.getUpdateCase().toString(), deviceId, ex.getMessage());
                    } else {
                        log.warn(format(
                                "Exception while sending %s to %s",
                                value.getUpdateCase().toString(), deviceId), ex);
                    }
                    complete();
                    return false;
                }
            }
        }

        public void complete() {
            synchronized (this) {
                signalClosed();
                if (requestObserver != null) {
                    requestObserver.onCompleted();
                    requestObserver.cancel("Terminated", null);
                    requestObserver = null;
                }
            }
        }

        void signalOpen() {
            synchronized (this) {
                final boolean wasOpen = open.getAndSet(true);
                if (!wasOpen) {
                    controller.postEvent(new P4RuntimeEvent(
                            P4RuntimeEvent.Type.CHANNEL_EVENT,
                            new ChannelEvent(deviceId, ChannelEvent.Type.OPEN)));
                }
            }
        }

        void signalClosed() {
            synchronized (this) {
                final boolean wasOpen = open.getAndSet(false);
                if (wasOpen) {
                    controller.postEvent(new P4RuntimeEvent(
                            P4RuntimeEvent.Type.CHANNEL_EVENT,
                            new ChannelEvent(deviceId, ChannelEvent.Type.CLOSED)));
                }
            }
        }

        public boolean isOpen() {
            return open.get();
        }
    }

    /**
     * Handles messages received from the device on the stream channel.
     */
    private final class InternalStreamResponseObserver
            implements StreamObserver<StreamMessageResponse> {

        private final StreamChannelManager streamChannelManager;

        private InternalStreamResponseObserver(
                StreamChannelManager streamChannelManager) {
            this.streamChannelManager = streamChannelManager;
        }

        @Override
        public void onNext(StreamMessageResponse message) {
            streamChannelManager.signalOpen();
            executorService.submit(() -> doNext(message));
        }

        private void doNext(StreamMessageResponse message) {
            try {
                log.debug("Received message on stream channel from {}: {}",
                          deviceId, message.getUpdateCase());
                switch (message.getUpdateCase()) {
                    case PACKET:
                        doPacketIn(message.getPacket());
                        return;
                    case ARBITRATION:
                        doArbitrationResponse(message.getArbitration());
                        return;
                    default:
                        log.warn("Unrecognized stream message from {}: {}",
                                 deviceId, message.getUpdateCase());
                }
            } catch (Throwable ex) {
                log.error("Exception while processing stream message from {}",
                          deviceId, ex);
            }
        }

        @Override
        public void onError(Throwable throwable) {
            if (throwable instanceof StatusRuntimeException) {
                StatusRuntimeException sre = (StatusRuntimeException) throwable;
                if (sre.getStatus().getCause() instanceof ConnectException) {
                    log.warn("Device {} is unreachable ({})",
                             deviceId, sre.getCause().getMessage());
                } else {
                    log.warn("Received error on stream channel for {}: {}",
                             deviceId, throwable.getMessage());
                }
            } else {
                log.warn(format("Received exception on stream channel for %s",
                                deviceId), throwable);
            }
            streamChannelManager.complete();
        }

        @Override
        public void onCompleted() {
            log.warn("Stream channel for {} has completed", deviceId);
            streamChannelManager.complete();
        }
    }
}
