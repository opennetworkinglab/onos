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

package org.onosproject.grpc.ctl;

import io.grpc.ConnectivityState;
import io.grpc.Context;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import org.onosproject.grpc.api.GrpcClient;
import org.onosproject.grpc.api.GrpcClientKey;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceAgentEvent;
import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Abstract client for gRPC service.
 */
public abstract class AbstractGrpcClient implements GrpcClient {

    protected final Logger log = getLogger(getClass());

    private final Context.CancellableContext cancellableContext =
            Context.current().withCancellation();

    protected final DeviceId deviceId;
    protected final ManagedChannel channel;
    private final boolean persistent;
    private final AbstractGrpcClientController controller;
    private final AtomicBoolean channelOpen = new AtomicBoolean(false);

    /**
     * Creates an new client for the given key and channel. Setting persistent
     * to true avoids the gRPC channel to stay IDLE. The controller instance is
     * needed to propagate channel events.
     *
     * @param clientKey  client key
     * @param channel    channel
     * @param persistent true if the gRPC should never stay IDLE
     * @param controller controller
     */
    protected AbstractGrpcClient(GrpcClientKey clientKey, ManagedChannel channel,
                                 boolean persistent, AbstractGrpcClientController controller) {
        checkNotNull(clientKey);
        checkNotNull(channel);
        this.deviceId = clientKey.deviceId();
        this.channel = channel;
        this.persistent = persistent;
        this.controller = controller;

        setChannelCallback(clientKey.deviceId(), channel, ConnectivityState.CONNECTING);
    }

    @Override
    public boolean isServerReachable() {
        final ConnectivityState state = channel.getState(false);
        switch (state) {
            case READY:
            case IDLE:
                return true;
            case CONNECTING:
            case TRANSIENT_FAILURE:
            case SHUTDOWN:
                return false;
            default:
                log.error("Unrecognized channel connectivity state {}", state);
                return false;
        }
    }

    @Override
    public void shutdown() {
        if (cancellableContext.isCancelled()) {
            log.warn("Context is already cancelled, " +
                             "ignoring request to shutdown for {}...", deviceId);
            return;
        }
        log.warn("Shutting down client for {}...", deviceId);
        cancellableContext.cancel(new InterruptedException(
                "Requested client shutdown"));
    }

    /**
     * Executes the given task in the cancellable context of this client.
     *
     * @param task task
     * @throws IllegalStateException if context has been cancelled
     */
    protected void runInCancellableContext(Runnable task) {
        if (this.cancellableContext.isCancelled()) {
            throw new IllegalStateException(
                    "Context is cancelled (client has been shut down)");
        }
        this.cancellableContext.run(task);
    }

    /**
     * Returns the context associated with this client.
     *
     * @return context
     */
    protected Context.CancellableContext context() {
        return cancellableContext;
    }

    protected void handleRpcError(Throwable throwable, String opDescription) {
        if (throwable instanceof StatusRuntimeException) {
            final StatusRuntimeException sre = (StatusRuntimeException) throwable;
            final String logMsg;
            if (sre.getCause() == null) {
                logMsg = sre.getMessage();
            } else {
                logMsg = format("%s (%s)", sre.getMessage(), sre.getCause().toString());
            }
            log.warn("Error while performing {} on {}: {}",
                     opDescription, deviceId, logMsg);
            log.debug("", throwable);
            return;
        }
        log.error(format("Exception while performing %s on %s",
                         opDescription, deviceId), throwable);
    }

    private void setChannelCallback(DeviceId deviceId, ManagedChannel channel,
                                    ConnectivityState sourceState) {
        if (log.isTraceEnabled()) {
            log.trace("Setting channel callback for {} with source state {}...",
                      deviceId, sourceState);
        }
        channel.notifyWhenStateChanged(
                sourceState, new ChannelConnectivityCallback(deviceId, channel));
    }

    /**
     * Runnable task invoked at each change of the channel connectivity state.
     * New callbacks are created as long as the channel is not shut down.
     */
    private final class ChannelConnectivityCallback implements Runnable {

        private final DeviceId deviceId;
        private final ManagedChannel channel;

        private ChannelConnectivityCallback(
                DeviceId deviceId, ManagedChannel channel) {
            this.deviceId = deviceId;
            this.channel = channel;
        }

        @Override
        public void run() {
            final ConnectivityState newState = channel.getState(false);
            final DeviceAgentEvent.Type eventType;
            switch (newState) {
                // On gRPC connectivity states:
                // https://github.com/grpc/grpc/blob/master/doc/connectivity-semantics-and-api.md
                case READY:
                    eventType = DeviceAgentEvent.Type.CHANNEL_OPEN;
                    break;
                case TRANSIENT_FAILURE:
                    eventType = DeviceAgentEvent.Type.CHANNEL_ERROR;
                    break;
                case SHUTDOWN:
                    eventType = DeviceAgentEvent.Type.CHANNEL_CLOSED;
                    break;
                case IDLE:
                    // IDLE and CONNECTING are transient states that will
                    // eventually move to READY or TRANSIENT_FAILURE. Do not
                    // generate an event for now.
                    if (persistent) {
                        log.debug("Forcing channel for {} to exist state IDLE...", deviceId);
                        channel.getState(true);
                    }
                    eventType = null;
                    break;
                case CONNECTING:
                    eventType = null;
                    break;
                default:
                    log.error("Unrecognized connectivity state {}", newState);
                    eventType = null;
            }

            if (log.isTraceEnabled()) {
                log.trace("Detected channel connectivity change for {}, new state is {}",
                          deviceId, newState);
            }

            if (eventType != null) {
                // Avoid sending consecutive duplicate events.
                final boolean present = eventType == DeviceAgentEvent.Type.CHANNEL_OPEN;
                final boolean past = channelOpen.getAndSet(present);
                if (present != past) {
                    log.debug("Notifying event {} for {}", eventType, deviceId);
                    controller.postEvent(new DeviceAgentEvent(eventType, deviceId));
                }
            }

            if (newState != ConnectivityState.SHUTDOWN) {
                // Channels never leave SHUTDOWN state, no need for a new callback.
                setChannelCallback(deviceId, channel, newState);
            }
        }
    }
}
