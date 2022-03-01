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
import org.onlab.util.SharedScheduledExecutors;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceAgentEvent;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.runtime.PiPacketOperation;
import org.onosproject.net.pi.service.PiPipeconfService;
import org.onosproject.p4runtime.api.P4RuntimeEvent;
import org.onosproject.p4runtime.api.P4RuntimeStreamClient;
import org.onosproject.p4runtime.ctl.codec.CodecException;
import org.onosproject.p4runtime.ctl.controller.MasterElectionIdStore;
import org.onosproject.p4runtime.ctl.controller.MasterElectionIdStore.MasterElectionIdListener;
import org.onosproject.p4runtime.ctl.controller.P4RuntimeControllerImpl;
import org.onosproject.p4runtime.ctl.controller.PacketInEvent;
import org.slf4j.Logger;
import p4.v1.P4RuntimeOuterClass;
import p4.v1.P4RuntimeOuterClass.StreamMessageRequest;
import p4.v1.P4RuntimeOuterClass.StreamMessageResponse;

import java.math.BigInteger;
import java.net.ConnectException;
import java.nio.ByteBuffer;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static org.onosproject.p4runtime.ctl.codec.Codecs.CODECS;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of P4RuntimeStreamClient. Handles P4Runtime StreamChannel RPC
 * operations, such as arbitration update and packet-in/out, for a given
 * P4Runtime-internal device ID.
 */
public final class StreamClientImpl implements P4RuntimeStreamClient {

    private static final Logger log = getLogger(StreamClientImpl.class);

    private static final int ARBITRATION_RETRY_SECONDS = 3;
    private static final int ARBITRATION_TIMEOUT_SECONDS = 15;

    private final P4RuntimeClientImpl client;
    private final DeviceId deviceId;
    private final long p4DeviceId;
    private final PiPipeconfService pipeconfService;
    private final MasterElectionIdStore masterElectionIdStore;
    private final P4RuntimeControllerImpl controller;

    private final StreamChannelManager streamChannelManager = new StreamChannelManager();
    private final MasterElectionIdListener masterElectionIdListener = new InternalMasterElectionIdListener();

    private final AtomicBoolean isMaster = new AtomicBoolean(false);
    private final AtomicBoolean requestedToBeMaster = new AtomicBoolean(false);

    private BigInteger pendingElectionId = null;
    private BigInteger lastUsedElectionId = null;

    private ScheduledFuture<?> pendingElectionIdRetryTask = null;
    private long pendingElectionIdTimestamp = 0;

    StreamClientImpl(
            PiPipeconfService pipeconfService,
            MasterElectionIdStore masterElectionIdStore,
            P4RuntimeClientImpl client,
            long p4DeviceId,
            P4RuntimeControllerImpl controller) {
        this.client = client;
        this.deviceId = client.deviceId();
        this.p4DeviceId = p4DeviceId;
        this.pipeconfService = pipeconfService;
        this.masterElectionIdStore = masterElectionIdStore;
        this.controller = controller;
    }

    @Override
    public boolean isSessionOpen(long p4DeviceId) {
        checkArgument(this.p4DeviceId == p4DeviceId);
        return streamChannelManager.isOpen();
    }

    @Override
    public void closeSession(long p4DeviceId) {
        checkArgument(this.p4DeviceId == p4DeviceId);
        synchronized (requestedToBeMaster) {
            this.masterElectionIdStore.unsetListener(deviceId, p4DeviceId);
            streamChannelManager.teardown();
            pendingElectionId = null;
            requestedToBeMaster.set(false);
            isMaster.set(false);
        }
    }

    @Override
    public void setMastership(long p4DeviceId, boolean master,
                              BigInteger newElectionId) {
        checkArgument(this.p4DeviceId == p4DeviceId);
        checkNotNull(newElectionId);
        checkArgument(newElectionId.compareTo(BigInteger.ZERO) > 0,
                      "newElectionId must be a non zero positive number");
        synchronized (requestedToBeMaster) {
            requestedToBeMaster.set(master);
            pendingElectionId = newElectionId;
            handlePendingElectionId(masterElectionIdStore.get(
                    deviceId, p4DeviceId));
        }
    }

    private void handlePendingElectionId(BigInteger masterElectionId) {
        synchronized (requestedToBeMaster) {
            if (pendingElectionId == null) {
                // No pending requests.
                return;
            }
            // Cancel any pending task. We'll reschedule if needed.
            if (pendingElectionIdRetryTask != null) {
                // Do not interrupt if running, as this might be executed by the
                // pending task itself.
                pendingElectionIdRetryTask.cancel(false);
                pendingElectionIdRetryTask = null;
            }
            // We implement a deferring mechanism to avoid becoming master when
            // we shouldn't, i.e. when the requested election ID is bigger than
            // the master one on the device, but we don't want to be master.
            // However, we make sure not to defer for more than
            // ARBITRATION_TIMEOUT_SECONDS.
            final boolean timeoutExpired;
            if (pendingElectionIdTimestamp == 0) {
                pendingElectionIdTimestamp = currentTimeMillis();
                timeoutExpired = false;
            } else {
                timeoutExpired = (currentTimeMillis() - pendingElectionIdTimestamp)
                        > ARBITRATION_TIMEOUT_SECONDS * 1000;
            }
            if (timeoutExpired) {
                log.warn("Arbitration timeout expired for {}! " +
                                 "Will send pending election ID now...",
                         deviceId);
            }
            if (!timeoutExpired &&
                    !requestedToBeMaster.get() && masterElectionId != null &&
                    pendingElectionId.compareTo(masterElectionId) > 0) {
                log.info("Deferring sending master arbitration update for {}, master " +
                                 "election ID of server ({}) is smaller than " +
                                 "requested one ({}), but we do NOT want to be master...",
                         deviceId, masterElectionId, pendingElectionId);
                // Will try again as soon as the master election ID store is
                // updated...
                masterElectionIdStore.setListener(
                        deviceId, p4DeviceId, masterElectionIdListener);
                // ..or in ARBITRATION_RETRY_SECONDS at the latest (if we missed
                // the store event).
                pendingElectionIdRetryTask = SharedScheduledExecutors.newTimeout(
                        () -> handlePendingElectionId(
                                masterElectionIdStore.get(deviceId, p4DeviceId)),
                        ARBITRATION_RETRY_SECONDS, TimeUnit.SECONDS);
            } else {
                // Send now.
                if (log.isDebugEnabled()) {
                    log.debug("Setting mastership on {}... " +
                                    "master={}, newElectionId={}, " +
                                    "masterElectionId={}, sessionOpen={}",
                            deviceId, requestedToBeMaster.get(),
                            pendingElectionId, masterElectionId,
                            streamChannelManager.isOpen());
                } else if (!pendingElectionId.equals(lastUsedElectionId)) {
                    log.info("Setting mastership on {}... " +
                                    "master={}, newElectionId={}, " +
                                    "masterElectionId={}, sessionOpen={}",
                            deviceId, requestedToBeMaster.get(),
                            pendingElectionId, masterElectionId,
                            streamChannelManager.isOpen());
                }
                // Optimistically set the reported master status, if wrong, it
                // will be updated by the arbitration response. This alleviates
                // race conditions when calling isMaster() right after setting
                // mastership.
                sendMasterArbitrationUpdate(pendingElectionId);
                isMaster.set(requestedToBeMaster.get());
                pendingElectionId = null;
                pendingElectionIdTimestamp = 0;
                // No need to listen for master election ID changes.
                masterElectionIdStore.unsetListener(deviceId, p4DeviceId);
            }
        }
    }

    @Override
    public boolean isMaster(long p4DeviceId) {
        checkArgument(this.p4DeviceId == p4DeviceId);
        synchronized (requestedToBeMaster) {
            return isMaster.get();
        }
    }

    @Override
    public void packetOut(long p4DeviceId, PiPacketOperation packet, PiPipeconf pipeconf) {
        checkArgument(this.p4DeviceId == p4DeviceId);
        if (!isSessionOpen(p4DeviceId)) {
            log.warn("Dropping packet-out request for {}, session is closed",
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
            streamChannelManager.send(packetOutRequest);
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
        lastUsedElectionId = electionId;
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
        // Is this client master?
        isMaster.set(msg.getStatus().getCode() == Status.OK.getCode().value());
        // Notify new master election IDs to all nodes via distributed store.
        // This is required for those nodes who do not have a Stream RPC open,
        // and that otherwise would not be aware of changes, keeping their
        // pending mastership operations forever.
        final BigInteger masterElectionId = uint128ToBigInteger(msg.getElectionId());
        masterElectionIdStore.set(deviceId, p4DeviceId, masterElectionId);

        log.debug("Received arbitration update from {}: isMaster={}, masterElectionId={}",
                  deviceId, isMaster.get(), masterElectionId);

        // Post mastership event via controller.
        controller.postEvent(new DeviceAgentEvent(
                isMaster.get() ? DeviceAgentEvent.Type.ROLE_MASTER
                        : DeviceAgentEvent.Type.ROLE_STANDBY, deviceId));
    }

    /**
     * Returns the election ID last used in a MasterArbitrationUpdate message
     * sent by the client to the server.
     *
     * @return election ID uint128 protobuf message
     */
    P4RuntimeOuterClass.Uint128 lastUsedElectionId() {
        return lastUsedElectionId == null
                ? P4RuntimeOuterClass.Uint128.getDefaultInstance()
                : bigIntegerToUint128(lastUsedElectionId);
    }

    /**
     * Handles updates of the master election ID by applying any pending
     * mastership operation.
     */
    private class InternalMasterElectionIdListener
            implements MasterElectionIdStore.MasterElectionIdListener {

        @Override
        public void updated(BigInteger masterElectionId) {
            log.debug("UPDATED master election ID of {}: {}",
                      deviceId, masterElectionId);
            handlePendingElectionId(masterElectionId);
        }
    }

    /**
     * A manager for the P4Runtime StreamChannel RPC that opportunistically creates
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
                requestObserver.onNext(value);
                // Optimistically set the session as open. In case of errors, it
                // will be closed by the response stream observer.
                streamChannelManager.signalOpen();
            }
        }

        private void initIfRequired() {
            if (requestObserver == null) {
                log.debug("Starting new StreamChannel RPC for {}...", deviceId);
                open.set(false);
                client.execRpcNoTimeout(
                        s -> requestObserver =
                                (ClientCallStreamObserver<StreamMessageRequest>)
                                        s.streamChannel(responseObserver)
                );
            }
        }

        void teardown() {
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
            open.set(true);
        }

        void signalClosed() {
            synchronized (this) {
                final boolean wasOpen = open.getAndSet(false);
                if (wasOpen) {
                    // We lost any valid mastership role.
                    controller.postEvent(new DeviceAgentEvent(
                            DeviceAgentEvent.Type.ROLE_NONE, deviceId));
                }
            }
        }

        boolean isOpen() {
            return open.get();
        }
    }

    /**
     * Handles messages received from the device on the StreamChannel RPC.
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
            try {
                if (log.isTraceEnabled()) {
                    log.trace("Received {} from {}: {}",
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
                    case ERROR:
                        P4RuntimeOuterClass.StreamError error = message.getError();
                        log.warn("Receive stream error {} from {} Canonical Code: {} Message: {} Space: {} Code: {}",
                                error.getDetailsCase(), deviceId, error.getCanonicalCode(), error.getMessage(),
                                error.getSpace(), error.getCode());
                        return;
                    default:
                        log.warn("Unrecognized StreamMessageResponse from {}: {}",
                                 deviceId, message.getUpdateCase());
                }
            } catch (Throwable ex) {
                log.error("Exception while processing StreamMessageResponse from {}",
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
                    log.warn("Error on StreamChannel RPC for {}: {}",
                             deviceId, throwable.getMessage());
                }
                log.debug("", throwable);
            } else {
                log.error(format("Exception on StreamChannel RPC for %s",
                                 deviceId), throwable);
            }
            streamChannelManager.teardown();
        }

        @Override
        public void onCompleted() {
            log.warn("StreamChannel RPC for {} has completed", deviceId);
            streamChannelManager.teardown();
        }
    }
}
