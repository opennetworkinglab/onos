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

import com.google.protobuf.TextFormat;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.StreamObserver;
import org.onosproject.net.DeviceId;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.runtime.PiPacketOperation;
import org.onosproject.net.pi.service.PiPipeconfService;
import org.onosproject.p4runtime.api.P4RuntimeEvent;
import org.onosproject.p4runtime.api.P4RuntimeStreamClient;
import org.onosproject.p4runtime.ctl.codec.CodecException;
import org.onosproject.p4runtime.ctl.controller.ArbitrationUpdateEvent;
import org.onosproject.p4runtime.ctl.controller.ChannelEvent;
import org.onosproject.p4runtime.ctl.controller.P4RuntimeControllerImpl;
import org.onosproject.p4runtime.ctl.controller.PacketInEvent;
import org.slf4j.Logger;
import p4.v1.P4RuntimeOuterClass;
import p4.v1.P4RuntimeOuterClass.StreamMessageRequest;
import p4.v1.P4RuntimeOuterClass.StreamMessageResponse;

import java.math.BigInteger;
import java.net.ConnectException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.String.format;
import static org.onosproject.p4runtime.ctl.codec.Codecs.CODECS;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of P4RuntimeStreamClient. Handles P4Runtime StreamChannel RPC
 * operations, such as arbitration update and packet-in/out.
 */
public final class StreamClientImpl implements P4RuntimeStreamClient {

    private static final Logger log = getLogger(StreamClientImpl.class);

    private static final BigInteger ONE_THOUSAND = BigInteger.valueOf(1000);

    private final P4RuntimeClientImpl client;
    private final DeviceId deviceId;
    private final long p4DeviceId;
    private final PiPipeconfService pipeconfService;
    private final P4RuntimeControllerImpl controller;
    private final StreamChannelManager streamChannelManager = new StreamChannelManager();

    private P4RuntimeOuterClass.Uint128 lastUsedElectionId = P4RuntimeOuterClass.Uint128
            .newBuilder().setLow(1).build();

    private final AtomicBoolean isClientMaster = new AtomicBoolean(false);

    StreamClientImpl(
            PiPipeconfService pipeconfService,
            P4RuntimeClientImpl client,
            P4RuntimeControllerImpl controller) {
        this.client = client;
        this.deviceId = client.deviceId();
        this.p4DeviceId = client.p4DeviceId();
        this.pipeconfService = pipeconfService;
        this.controller = controller;
    }

    @Override
    public void openSession() {
        if (isSessionOpen()) {
            log.debug("Dropping request to open session for {}, session is already open",
                      deviceId);
            return;
        }
        log.debug("Opening session for {}...", deviceId);
        sendMasterArbitrationUpdate(controller.newMasterElectionId(deviceId));

    }

    @Override
    public boolean isSessionOpen() {
        return streamChannelManager.isOpen();
    }

    @Override
    public void closeSession() {
        streamChannelManager.complete();
    }

    @Override
    public void runForMastership() {
        if (!isSessionOpen()) {
            log.debug("Dropping mastership request for {}, session is closed",
                      deviceId);
            return;
        }
        // Becoming master is a race. Here we increase our chances of win, i.e.
        // using the highest election ID, against other ONOS nodes in the
        // cluster that are calling openSession() (which is used to start the
        // stream RPC session, not to become master).
        log.debug("Running for mastership on {}...", deviceId);
        final BigInteger masterId = controller.newMasterElectionId(deviceId)
                .add(ONE_THOUSAND);
        sendMasterArbitrationUpdate(masterId);
    }

    @Override
    public boolean isMaster() {
        return streamChannelManager.isOpen() && isClientMaster.get();
    }

    @Override
    public void packetOut(PiPacketOperation packet, PiPipeconf pipeconf) {
        if (!isSessionOpen()) {
            log.debug("Dropping packet-out request for {}, session is closed",
                      deviceId);
            return;
        }
        if (log.isTraceEnabled()) {
            log.trace("Sending packet-out to {}: {}", deviceId, packet);
        }
        try {
            // Encode the PiPacketOperation into a PacketOut
            final P4RuntimeOuterClass.PacketOut packetOut =
                    CODECS.packetOut().encode(packet, null, pipeconf);
            // Build the request
            final StreamMessageRequest packetOutRequest = StreamMessageRequest
                    .newBuilder().setPacket(packetOut).build();
            // Send.
            streamChannelManager.sendIfOpen(packetOutRequest);
        } catch (CodecException e) {
            log.error("Unable to send packet-out: {}", e.getMessage());
        }
    }

    private void sendMasterArbitrationUpdate(BigInteger electionId) {
        log.debug("Sending arbitration update to {}... electionId={}",
                  deviceId, electionId);
        final P4RuntimeOuterClass.Uint128 idMsg = bigIntegerToUint128(electionId);
        streamChannelManager.send(
                StreamMessageRequest.newBuilder()
                        .setArbitration(
                                P4RuntimeOuterClass.MasterArbitrationUpdate
                                        .newBuilder()
                                        .setDeviceId(p4DeviceId)
                                        .setElectionId(idMsg)
                                        .build())
                        .build());
        lastUsedElectionId = idMsg;
    }

    private P4RuntimeOuterClass.Uint128 bigIntegerToUint128(BigInteger value) {
        final byte[] arr = value.toByteArray();
        final ByteBuffer bb = ByteBuffer.allocate(Long.BYTES * 2)
                .put(new byte[Long.BYTES * 2 - arr.length])
                .put(arr);
        bb.rewind();
        return P4RuntimeOuterClass.Uint128.newBuilder()
                .setHigh(bb.getLong())
                .setLow(bb.getLong())
                .build();
    }

    private BigInteger uint128ToBigInteger(P4RuntimeOuterClass.Uint128 value) {
        return new BigInteger(
                ByteBuffer.allocate(Long.BYTES * 2)
                        .putLong(value.getHigh())
                        .putLong(value.getLow())
                        .array());
    }

    private void handlePacketIn(P4RuntimeOuterClass.PacketIn packetInMsg) {
        if (log.isTraceEnabled()) {
            log.trace("Received packet-in from {}: {}", deviceId, packetInMsg);
        }
        if (!pipeconfService.getPipeconf(deviceId).isPresent()) {
            log.warn("Unable to handle packet-in from {}, missing pipeconf: {}",
                     deviceId, TextFormat.shortDebugString(packetInMsg));
            return;
        }
        // Decode packet message and post event.
        // TODO: consider implementing a cache to speed up
        //  encoding/deconding of packet-in/out (e.g. LLDP, ARP)
        final PiPipeconf pipeconf = pipeconfService.getPipeconf(deviceId).get();
        final PiPacketOperation pktOperation;
        try {
            pktOperation = CODECS.packetIn().decode(
                    packetInMsg, null, pipeconf);
        } catch (CodecException e) {
            log.warn("Unable to process packet-int: {}", e.getMessage());
            return;
        }
        controller.postEvent(new P4RuntimeEvent(
                P4RuntimeEvent.Type.PACKET_IN,
                new PacketInEvent(deviceId, pktOperation)));
    }

    private void handleArbitrationUpdate(P4RuntimeOuterClass.MasterArbitrationUpdate msg) {
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
        final boolean isMaster = msg.getStatus().getCode() == Status.OK.getCode().value();
        log.debug("Received arbitration update from {}: isMaster={}, electionId={}",
                  deviceId, isMaster, uint128ToBigInteger(msg.getElectionId()));
        controller.postEvent(new P4RuntimeEvent(
                P4RuntimeEvent.Type.ARBITRATION_RESPONSE,
                new ArbitrationUpdateEvent(deviceId, isMaster)));
        isClientMaster.set(isMaster);
    }

    /**
     * Returns the election ID last used in a MasterArbitrationUpdate message
     * sent by the client to the server.
     *
     * @return election ID uint128 protobuf message
     */
    P4RuntimeOuterClass.Uint128 lastUsedElectionId() {
        return lastUsedElectionId;
    }

    /**
     * A manager for the P4Runtime stream channel that opportunistically creates
     * new stream RCP stubs (e.g. when one fails because of errors) and posts
     * channel events via the P4Runtime controller.
     */
    private final class StreamChannelManager {

        private final AtomicBoolean open = new AtomicBoolean(false);
        private final StreamObserver<StreamMessageResponse> responseObserver =
                new InternalStreamResponseObserver(this);
        private ClientCallStreamObserver<StreamMessageRequest> requestObserver;

        void send(StreamMessageRequest value) {
            synchronized (this) {
                initIfRequired();
                doSend(value);
            }
        }

        void sendIfOpen(StreamMessageRequest value) {
            // We do not lock here, but we ignore NPEs due to stream RPC not
            // being active (null requestObserver). Good for frequent
            // packet-outs.
            try {
                doSend(value);
            } catch (NullPointerException e) {
                if (requestObserver != null) {
                    // Must be something else.
                    throw e;
                }
            }
        }

        private void doSend(StreamMessageRequest value) {
            try {
                requestObserver.onNext(value);
            } catch (Throwable ex) {
                if (ex instanceof StatusRuntimeException) {
                    log.warn("Unable to send {} to {}: {}",
                             value.getUpdateCase().toString(), deviceId, ex.getMessage());
                } else {
                    log.error("Exception while sending {} to {}: {}",
                              value.getUpdateCase().toString(), deviceId, ex);
                }
                complete();
            }
        }

        private void initIfRequired() {
            if (requestObserver == null) {
                log.debug("Creating new stream channel for {}...", deviceId);
                open.set(false);
                client.execRpcNoTimeout(
                        s -> requestObserver =
                                (ClientCallStreamObserver<StreamMessageRequest>)
                                        s.streamChannel(responseObserver)
                );
            }
        }

        void complete() {
            synchronized (this) {
                signalClosed();
                if (requestObserver != null) {
                    requestObserver.onCompleted();
                    requestObserver.cancel("Completed", null);
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

        boolean isOpen() {
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
            try {
                if (log.isTraceEnabled()) {
                    log.trace(
                            "Received {} from {}: {}",
                            message.getUpdateCase(), deviceId,
                            TextFormat.shortDebugString(message));
                }
                switch (message.getUpdateCase()) {
                    case PACKET:
                        handlePacketIn(message.getPacket());
                        return;
                    case ARBITRATION:
                        handleArbitrationUpdate(message.getArbitration());
                        return;
                    default:
                        log.warn("Unrecognized StreamMessageResponse from {}: {}",
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
                final StatusRuntimeException sre = (StatusRuntimeException) throwable;
                if (sre.getStatus().getCause() instanceof ConnectException) {
                    log.warn("{} is unreachable ({})",
                             deviceId, sre.getCause().getMessage());
                } else {
                    log.warn("Error on stream channel for {}: {}",
                             deviceId, throwable.getMessage());
                }
            } else {
                log.error(format("Exception on stream channel for %s",
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
