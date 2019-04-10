/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.gnmi.ctl;


import com.google.common.util.concurrent.Futures;
import gnmi.Gnmi;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.StreamObserver;
import org.onosproject.gnmi.api.GnmiEvent;
import org.onosproject.gnmi.api.GnmiUpdate;
import org.onosproject.net.DeviceId;
import org.slf4j.Logger;

import java.net.ConnectException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.String.format;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * A manager for the gNMI Subscribe RPC that opportunistically starts new RPC
 * (e.g. when one fails because of errors) and posts subscribe events via the
 * gNMI controller.
 */
final class GnmiSubscriptionManager {

    // FIXME: make this configurable
    private static final long DEFAULT_RECONNECT_DELAY = 5; // Seconds

    private static final Logger log = getLogger(GnmiSubscriptionManager.class);

    private final GnmiClientImpl client;
    private final DeviceId deviceId;
    private final GnmiControllerImpl controller;
    private final StreamObserver<Gnmi.SubscribeResponse> responseObserver;

    private final ScheduledExecutorService streamCheckerExecutor =
            newSingleThreadScheduledExecutor(groupedThreads("onos/gnmi-subscribe-check", "%d", log));
    private Future<?> checkTask;

    private ClientCallStreamObserver<Gnmi.SubscribeRequest> requestObserver;
    private Gnmi.SubscribeRequest existingSubscription;
    private AtomicBoolean active = new AtomicBoolean(false);

    GnmiSubscriptionManager(GnmiClientImpl client, DeviceId deviceId,
                            GnmiControllerImpl controller) {
        this.client = client;
        this.deviceId = deviceId;
        this.controller = controller;
        this.responseObserver = new InternalStreamResponseObserver();
    }

    void subscribe(Gnmi.SubscribeRequest request) {
        synchronized (this) {
            if (existingSubscription != null) {
                if (existingSubscription.equals(request)) {
                    // Nothing to do. We are already subscribed for the same
                    // request.
                    log.debug("Ignoring re-subscription to same request for {}",
                              deviceId);
                    return;
                }
                log.debug("Cancelling existing subscription for {} before " +
                                  "starting a new one", deviceId);
                complete();
            }
            existingSubscription = request;
            sendSubscribeRequest();
            if (checkTask != null) {
                checkTask = streamCheckerExecutor.scheduleAtFixedRate(
                        this::checkSubscription, 0,
                        DEFAULT_RECONNECT_DELAY,
                        TimeUnit.SECONDS);
            }
        }
    }

    void unsubscribe() {
        synchronized (this) {
            if (checkTask != null) {
                checkTask.cancel(false);
                checkTask = null;
            }
            existingSubscription = null;
            complete();
        }
    }

    public void shutdown() {
        log.debug("Shutting down gNMI subscription manager for {}", deviceId);
        unsubscribe();
        streamCheckerExecutor.shutdownNow();
    }

    private void checkSubscription() {
        synchronized (this) {
            if (existingSubscription != null && !active.get()) {
                if (client.isServerReachable() || Futures.getUnchecked(client.probeService())) {
                    log.info("Re-starting Subscribe RPC for {}...", deviceId);
                    sendSubscribeRequest();
                } else {
                    log.debug("Not restarting Subscribe RPC for {}, server is NOT reachable",
                              deviceId);
                }
            }
        }
    }

    private void sendSubscribeRequest() {
        if (requestObserver == null) {
            log.debug("Starting new Subscribe RPC for {}...", deviceId);
            client.execRpcNoTimeout(
                    s -> requestObserver =
                            (ClientCallStreamObserver<Gnmi.SubscribeRequest>)
                                    s.subscribe(responseObserver)
            );
        }
        requestObserver.onNext(existingSubscription);
        active.set(true);
    }

    public void complete() {
        synchronized (this) {
            active.set(false);
            if (requestObserver != null) {
                requestObserver.onCompleted();
                requestObserver.cancel("Terminated", null);
                requestObserver = null;
            }
        }
    }

    /**
     * Handles messages received from the device on the Subscribe RPC.
     */
    private final class InternalStreamResponseObserver
            implements StreamObserver<Gnmi.SubscribeResponse> {

        @Override
        public void onNext(Gnmi.SubscribeResponse message) {
            try {
                if (log.isTraceEnabled()) {
                    log.trace("Received SubscribeResponse from {}: {}",
                              deviceId, message.toString());
                }
                controller.postEvent(new GnmiEvent(GnmiEvent.Type.UPDATE, new GnmiUpdate(
                        deviceId, message.getUpdate(), message.getSyncResponse())));
            } catch (Throwable ex) {
                log.error("Exception processing SubscribeResponse from " + deviceId,
                          ex);
            }
        }

        @Override
        public void onError(Throwable throwable) {
            complete();
            if (throwable instanceof StatusRuntimeException) {
                StatusRuntimeException sre = (StatusRuntimeException) throwable;
                if (sre.getStatus().getCause() instanceof ConnectException) {
                    log.warn("{} is unreachable ({})",
                             deviceId, sre.getCause().getMessage());
                } else {
                    log.warn("Error on Subscribe RPC for {}: {}",
                             deviceId, throwable.getMessage());
                }
            } else {
                log.error(format("Exception on Subscribe RPC for %s",
                                 deviceId), throwable);
            }
        }

        @Override
        public void onCompleted() {
            complete();
            log.warn("Subscribe RPC for {} has completed", deviceId);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        if (!streamCheckerExecutor.isShutdown()) {
            log.error("Finalizing object but executor is still active! BUG? Shutting down...");
            shutdown();
        }
        super.finalize();
    }
}


