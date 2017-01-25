/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.incubator.rpc.grpc;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;
import static org.onosproject.incubator.protobuf.net.ProtobufUtils.translate;
import static org.onosproject.net.DeviceId.deviceId;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.onosproject.grpc.net.device.DeviceProviderRegistryRpcGrpc;
import org.onosproject.grpc.net.device.DeviceProviderRegistryRpcGrpc.DeviceProviderRegistryRpcStub;
import org.onosproject.grpc.net.device.DeviceService.DeviceProviderMsg;
import org.onosproject.grpc.net.device.DeviceService.DeviceProviderServiceMsg;
import org.onosproject.grpc.net.device.DeviceService.IsReachableRequest;
import org.onosproject.grpc.net.device.DeviceService.RoleChanged;
import org.onosproject.grpc.net.device.DeviceService.TriggerProbe;
import org.onosproject.incubator.protobuf.net.ProtobufUtils;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.device.PortStatistics;
import org.onosproject.net.provider.AbstractProviderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

import io.grpc.Channel;
import io.grpc.stub.StreamObserver;

// gRPC Client side
// gRPC wise, this object represents bidirectional streaming service session
// and deals with outgoing message stream
/**
 * DeviceProviderService instance associated with given DeviceProvider.
 */
final class DeviceProviderServiceClientProxy
        extends AbstractProviderService<DeviceProvider>
        implements DeviceProviderService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final StreamObserver<DeviceProviderServiceMsg> devProvService;
    private final AtomicBoolean hasShutdown = new AtomicBoolean(false);

    private final Channel channel;

    private Throwable error;

    DeviceProviderServiceClientProxy(DeviceProvider provider, Channel channel) {
        super(provider);
        this.channel = channel;

        DeviceProviderRegistryRpcStub stub = DeviceProviderRegistryRpcGrpc.newStub(channel);
        log.debug("Calling RPC register({}) against {}", provider.id(), channel.authority());
        devProvService = stub.register(new DeviceProviderClientProxy(provider));

        // send initialize message
        DeviceProviderServiceMsg.Builder builder = DeviceProviderServiceMsg.newBuilder();
        builder.setRegisterProvider(builder.getRegisterProviderBuilder()
                                    .setProviderScheme(provider.id().scheme())
                                    .build());
        devProvService.onNext(builder.build());
    }

    @Override
    public void deviceConnected(DeviceId deviceId,
                                DeviceDescription deviceDescription) {
        checkValidity();

        DeviceProviderServiceMsg.Builder builder = DeviceProviderServiceMsg.newBuilder();
        builder.setDeviceConnected(builder.getDeviceConnectedBuilder()
                                      .setDeviceId(deviceId.toString())
                                      .setDeviceDescription(translate(deviceDescription))
                                      .build());

        devProvService.onNext(builder.build());
    }

    @Override
    public void deviceDisconnected(DeviceId deviceId) {
        checkValidity();

        DeviceProviderServiceMsg.Builder builder = DeviceProviderServiceMsg.newBuilder();
        builder.setDeviceDisconnected(builder.getDeviceDisconnectedBuilder()
                                      .setDeviceId(deviceId.toString())
                                      .build());

        devProvService.onNext(builder.build());
    }

    @Override
    public void updatePorts(DeviceId deviceId,
                            List<PortDescription> portDescriptions) {
        checkValidity();

        DeviceProviderServiceMsg.Builder builder = DeviceProviderServiceMsg.newBuilder();
        List<org.onosproject.grpc.net.Port.PortDescription> portDescs =
                portDescriptions.stream()
                    .map(ProtobufUtils::translate)
                    .collect(toList());

        builder.setUpdatePorts(builder.getUpdatePortsBuilder()
                               .setDeviceId(deviceId.toString())
                               .addAllPortDescriptions(portDescs)
                               .build());

        devProvService.onNext(builder.build());
    }

    @Override
    public void deletePort(DeviceId deviceId, PortDescription portDescription) {

    }

    @Override
    public void portStatusChanged(DeviceId deviceId,
                                  PortDescription portDescription) {
        checkValidity();

        DeviceProviderServiceMsg.Builder builder = DeviceProviderServiceMsg.newBuilder();
        builder.setPortStatusChanged(builder.getPortStatusChangedBuilder()
                                      .setDeviceId(deviceId.toString())
                                      .setPortDescription(translate(portDescription))
                                      .build());

        devProvService.onNext(builder.build());
    }

    @Override
    public void receivedRoleReply(DeviceId deviceId, MastershipRole requested,
                                  MastershipRole response) {
        checkValidity();

        DeviceProviderServiceMsg.Builder builder = DeviceProviderServiceMsg.newBuilder();
        builder.setReceivedRoleReply(builder.getReceivedRoleReplyBuilder()
                                      .setDeviceId(deviceId.toString())
                                      .setRequested(translate(requested))
                                      .setResponse(translate(response))
                                      .build());

        devProvService.onNext(builder.build());
    }

    @Override
    public void updatePortStatistics(DeviceId deviceId,
                                     Collection<PortStatistics> portStatistics) {
        checkValidity();

        DeviceProviderServiceMsg.Builder builder = DeviceProviderServiceMsg.newBuilder();
        List<org.onosproject.grpc.net.Port.PortStatistics> portStats =
                portStatistics.stream()
                    .map(ProtobufUtils::translate)
                    .collect(toList());
        builder.setUpdatePortStatistics(builder.getUpdatePortStatisticsBuilder()
                                      .setDeviceId(deviceId.toString())
                                      .addAllPortStatistics(portStats)
                                      .build());

        devProvService.onNext(builder.build());
    }

    /**
     * Shutdown this session.
     */
    public void shutdown() {
        if (hasShutdown.compareAndSet(false, true)) {
            log.info("Shutting down session over {}", channel.authority());
            // initiate clean shutdown from client
            devProvService.onCompleted();
            invalidate();
        }
    }

    /**
     * Abnormally terminate this session.
     * @param t error details
     */
    public void shutdown(Throwable t) {
        if (hasShutdown.compareAndSet(false, true)) {
            log.error("Shutting down session over {}", channel.authority());
            // initiate abnormal termination from client
            devProvService.onError(t);
            invalidate(t);
        }
    }

    /**
     * Invalidates the ProviderService indicating Failure.
     * @param t {@link Throwable} describing last failure
     */
    private void invalidate(Throwable t) {
        this.error = t;
        invalidate();
    }

    @Override
    public void checkValidity() {
        if (error != null) {
            throw new IllegalStateException("DeviceProviderService no longer valid",
                                            error);
        }
        super.checkValidity();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("channel", channel.authority())
            .add("hasShutdown", hasShutdown.get())
            .toString();
    }

    // gRPC wise, this object handles incoming message stream
    /**
     * Translates DeviceProvider instructions received from RPC to Java calls.
     */
    private final class DeviceProviderClientProxy
            implements StreamObserver<DeviceProviderMsg> {

        private final DeviceProvider provider;

        DeviceProviderClientProxy(DeviceProvider provider) {
            this.provider = checkNotNull(provider);
        }

        @Override
        public void onNext(DeviceProviderMsg msg) {
            try {
                log.trace("DeviceProviderClientProxy received: {}", msg);
                onMethod(msg);
            } catch (Exception e) {
                log.error("Exception caught handling {} at DeviceProviderClientProxy", msg, e);
                // initiate shutdown from client
                shutdown(e);
            }
        }

        /**
         * Translates received RPC message to {@link DeviceProvider} method calls.
         * @param msg DeviceProvider message
         */
        private void onMethod(DeviceProviderMsg msg) {
            switch (msg.getMethodCase()) {
            case TRIGGER_PROBE:
                TriggerProbe triggerProbe = msg.getTriggerProbe();
                provider.triggerProbe(deviceId(triggerProbe.getDeviceId()));
                break;
            case ROLE_CHANGED:
                RoleChanged roleChanged = msg.getRoleChanged();
                provider.roleChanged(deviceId(roleChanged.getDeviceId()),
                                     translate(roleChanged.getNewRole()));
                break;
            case IS_REACHABLE_REQUEST:
                IsReachableRequest isReachableRequest = msg.getIsReachableRequest();
                // check if reachable
                boolean reachable = provider.isReachable(deviceId(isReachableRequest.getDeviceId()));

                int xid = isReachableRequest.getXid();
                // send response back DeviceProviderService channel
                DeviceProviderServiceMsg.Builder builder = DeviceProviderServiceMsg.newBuilder();
                builder.setIsReachableResponse(builder.getIsReachableResponseBuilder()
                                               .setXid(xid)
                                               .setIsReachable(reachable)
                                               .build());
                devProvService.onNext(builder.build());
                break;

            case METHOD_NOT_SET:
            default:
                log.warn("Unexpected method, ignoring", msg);
                break;
            }
        }

        @Override
        public void onCompleted() {
            log.info("DeviceProviderClientProxy completed");
            // session terminated from remote
            invalidate();
        }

        @Override
        public void onError(Throwable t) {
            log.error("DeviceProviderClientProxy#onError", t);
            // session terminated from remote
            invalidate(t);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("channel", channel.authority())
                    .toString();
        }
    }
}

