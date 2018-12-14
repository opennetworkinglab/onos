/*
 * Copyright 2018-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package protocols.gnmi.ctl.java.org.onosproject.gnmi.ctl;


import gnmi.Gnmi;
import gnmi.gNMIGrpc;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.StreamObserver;
import org.onosproject.gnmi.api.GnmiEvent;
import org.onosproject.gnmi.api.GnmiUpdate;
import org.onosproject.net.DeviceId;
import org.slf4j.Logger;

import java.net.ConnectException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.String.format;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * A manager for the gNMI stream channel that opportunistically creates
 * new stream RCP stubs (e.g. when one fails because of errors) and posts
 * subscribe events via the gNMI controller.
 */
final class GnmiSubscriptionManager {

    /**
     * The state of the subscription manager.
     */
    enum State {

        /**
         * Subscription not exists.
         */
        INIT,

        /**
         * Exists a subscription and channel opened.
         */
        SUBSCRIBED,

        /**
         * Exists a subscription, but the channel does not open.
         */
        RETRYING,
    }

    // FIXME: make this configurable
    private static final long DEFAULT_RECONNECT_DELAY = 5; // Seconds
    private static final Logger log = getLogger(GnmiSubscriptionManager.class);
    private final ManagedChannel channel;
    private final DeviceId deviceId;
    private final GnmiControllerImpl controller;

    private final StreamObserver<Gnmi.SubscribeResponse> responseObserver;
    private final AtomicReference<State> state = new AtomicReference<>(State.INIT);

    private ClientCallStreamObserver<Gnmi.SubscribeRequest> requestObserver;
    private Gnmi.SubscribeRequest existingSubscription;
    private final ScheduledExecutorService streamCheckerExecutor =
            newSingleThreadScheduledExecutor(groupedThreads("onos/gnmi-probe", "%d", log));

    GnmiSubscriptionManager(ManagedChannel channel, DeviceId deviceId,
                            GnmiControllerImpl controller) {
        this.channel = channel;
        this.deviceId = deviceId;
        this.controller = controller;
        this.responseObserver = new InternalStreamResponseObserver();
        streamCheckerExecutor.scheduleAtFixedRate(this::checkGnmiStream, 0,
                                                  DEFAULT_RECONNECT_DELAY,
                                                  TimeUnit.SECONDS);
    }

    public void shutdown() {
        log.info("gNMI subscription manager for device {} shutdown", deviceId);
        streamCheckerExecutor.shutdown();
        complete();
    }

    private void initIfRequired() {
        if (requestObserver == null) {
            log.debug("Creating new stream channel for {}...", deviceId);
            requestObserver = (ClientCallStreamObserver<Gnmi.SubscribeRequest>)
                    gNMIGrpc.newStub(channel).subscribe(responseObserver);

        }
    }

    boolean subscribe(Gnmi.SubscribeRequest request) {
        synchronized (state) {
            if (state.get() == State.SUBSCRIBED) {
                // Cancel subscription when we need to subscribe new thing
                complete();
            }

            existingSubscription = request;
            return send(request);
        }
    }

    private boolean send(Gnmi.SubscribeRequest value) {
        initIfRequired();
        try {
            requestObserver.onNext(value);
            state.set(State.SUBSCRIBED);
            return true;
        } catch (Throwable ex) {
            if (ex instanceof StatusRuntimeException) {
                log.warn("Unable to send subscribe request to {}: {}",
                        deviceId, ex.getMessage());
            } else {
                log.warn("Exception while sending subscribe request to {}",
                        deviceId, ex);
            }
            state.set(State.RETRYING);
            return false;
        }
    }

    public void complete() {
        synchronized (state) {
            state.set(State.INIT);
            if (requestObserver != null) {
                requestObserver.onCompleted();
                requestObserver.cancel("Terminated", null);
                requestObserver = null;
            }
        }
    }

    private void checkGnmiStream() {
        synchronized (state) {
            if (state.get() != State.RETRYING) {
                // No need to retry if the state is not RETRYING
                return;
            }
            log.info("Try reconnecting gNMI stream to device {}", deviceId);

            complete();
            send(existingSubscription);
        }
    }

    /**
     * Handles messages received from the device on the stream channel.
     */
    private final class InternalStreamResponseObserver
            implements StreamObserver<Gnmi.SubscribeResponse> {

        @Override
        public void onNext(Gnmi.SubscribeResponse message) {
            try {
                log.debug("Received message on stream channel from {}: {}",
                        deviceId, message.toString());
                GnmiUpdate update = new GnmiUpdate(deviceId, message.getUpdate(), message.getSyncResponse());
                GnmiEvent event = new GnmiEvent(GnmiEvent.Type.UPDATE, update);
                controller.postEvent(event);
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
            state.set(State.RETRYING);
        }

        @Override
        public void onCompleted() {
            log.warn("Stream channel for {} has completed", deviceId);
            state.set(State.RETRYING);
        }
    }
}


