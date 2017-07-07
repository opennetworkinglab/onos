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
import com.google.protobuf.ByteString;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.TextFormat;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.net.DeviceId;
import org.onosproject.net.pi.runtime.PiTableEntry;
import org.onosproject.net.pi.runtime.PiTableId;
import org.onosproject.p4runtime.api.P4RuntimeClient;
import org.onosproject.p4runtime.api.P4RuntimeEvent;
import org.slf4j.Logger;
import p4.P4RuntimeGrpc;
import p4.config.P4InfoOuterClass;
import p4.tmp.P4Config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.onlab.util.ImmutableByteSequence.copyFrom;
import static org.slf4j.LoggerFactory.getLogger;
import static p4.P4RuntimeOuterClass.*;
import static p4.P4RuntimeOuterClass.SetForwardingPipelineConfigRequest.Action.VERIFY_AND_COMMIT;

/**
 * Implementation of a P4Runtime client.
 */
public class P4RuntimeClientImpl implements P4RuntimeClient {

    private static final int DEADLINE_SECONDS = 15;

    private final Logger log = getLogger(getClass());

    private final DeviceId deviceId;
    private final int p4DeviceId;
    private final P4RuntimeControllerImpl controller;
    private final P4RuntimeGrpc.P4RuntimeBlockingStub blockingStub;
    private final P4RuntimeGrpc.P4RuntimeStub asyncStub;
    private ExecutorService executorService;
    private StreamObserver<StreamMessageRequest> streamRequestObserver;


    P4RuntimeClientImpl(DeviceId deviceId, int p4DeviceId, ManagedChannel channel, P4RuntimeControllerImpl controller,
                        ExecutorService executorService) {
        this.deviceId = deviceId;
        this.p4DeviceId = p4DeviceId;
        this.controller = controller;
        this.executorService = executorService;
        this.blockingStub = P4RuntimeGrpc.newBlockingStub(channel)
                .withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS);
        this.asyncStub = P4RuntimeGrpc.newStub(channel)
                .withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public CompletableFuture<Boolean> initStreamChannel() {
        return CompletableFuture.supplyAsync(this::doInitStreamChannel, executorService);
    }

    private boolean doInitStreamChannel() {
        if (this.streamRequestObserver == null) {
            this.streamRequestObserver = this.asyncStub.streamChannel(new StreamChannelResponseObserver());
            // To listen for packets and other events, we need to start the RPC.
            // Here we do it by sending an empty packet out.
            try {
                this.streamRequestObserver.onNext(StreamMessageRequest.newBuilder()
                                                          .setPacket(PacketOut.getDefaultInstance())
                                                          .build());
            } catch (StatusRuntimeException e) {
                log.warn("Unable to initialize stream channel for {}: {}", deviceId, e);
                return false;
            }
        }
        return true;
    }

    @Override
    public CompletableFuture<Boolean> setPipelineConfig(InputStream p4info, InputStream targetConfig) {
        return CompletableFuture.supplyAsync(() -> doSetPipelineConfig(p4info, targetConfig), executorService);
    }

    private boolean doSetPipelineConfig(InputStream p4info, InputStream targetConfig) {

        log.debug("Setting pipeline config for {}", deviceId);

        P4InfoOuterClass.P4Info.Builder p4iInfoBuilder = P4InfoOuterClass.P4Info.newBuilder();

        try {
            TextFormat.getParser().merge(new InputStreamReader(p4info),
                                         ExtensionRegistry.getEmptyRegistry(),
                                         p4iInfoBuilder);
        } catch (IOException ex) {
            log.warn("Unable to load p4info for {}: {}", deviceId, ex.getMessage());
            return false;
        }

        P4Config.P4DeviceConfig deviceIdConfig;
        try {
            deviceIdConfig = P4Config.P4DeviceConfig
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
                                    .setP4Info(p4iInfoBuilder.build())
                                    .setP4DeviceConfig(deviceIdConfig.toByteString())
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

    @Override
    public boolean writeTableEntries(Collection<PiTableEntry> entries, WriteOperationType opType) {

        throw new UnsupportedOperationException("writeTableEntries not implemented.");
    }

    @Override
    public CompletableFuture<Collection<PiTableEntry>> dumpTable(PiTableId tableId) {

        throw new UnsupportedOperationException("dumpTable not implemented.");
    }

    @Override
    public void shutdown() {

        if (this.streamRequestObserver != null) {
            this.streamRequestObserver.onError(new StatusRuntimeException(Status.CANCELLED));
            this.streamRequestObserver.onCompleted();
        }

        this.executorService.shutdownNow();
        try {
            executorService.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("Executor service didn't shutdown in time.");
        }

        // Prevent the execution of other tasks.
        executorService = null;
    }

    private class StreamChannelResponseObserver implements StreamObserver<StreamMessageResponse> {

        @Override
        public void onNext(StreamMessageResponse message) {

            P4RuntimeEvent event;

            if (message.getPacket().isInitialized()) {
                // Packet-in
                PacketIn packetIn = message.getPacket();
                ImmutableByteSequence data = copyFrom(packetIn.getPayload().asReadOnlyByteBuffer());
                ImmutableList.Builder<ImmutableByteSequence> metadataBuilder = ImmutableList.builder();
                packetIn.getMetadataList().stream()
                        .map(m -> m.getValue().asReadOnlyByteBuffer())
                        .map(ImmutableByteSequence::copyFrom)
                        .forEach(metadataBuilder::add);
                event = new DefaultPacketInEvent(deviceId, data, metadataBuilder.build());

            } else if (message.getArbitration().isInitialized()) {
                // Arbitration.
                throw new UnsupportedOperationException("Arbitration not implemented.");

            } else {
                log.warn("Unrecognized stream message from {}: {}", deviceId, message);
                return;
            }

            controller.postEvent(event);
        }

        @Override
        public void onError(Throwable throwable) {
            log.warn("Error on stream channel for {}: {}", deviceId, throwable);
        }

        @Override
        public void onCompleted() {
            // TODO: declare the device as disconnected?
        }
    }

}
