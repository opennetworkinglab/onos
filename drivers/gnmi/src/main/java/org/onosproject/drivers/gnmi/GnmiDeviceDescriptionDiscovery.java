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

package org.onosproject.drivers.gnmi;

import com.google.common.collect.ImmutableList;
import gnmi.gNMIGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.internal.DnsNameResolverProvider;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.onosproject.grpc.api.GrpcChannelId;
import org.onosproject.grpc.api.GrpcController;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static gnmi.Gnmi.Path;
import static gnmi.Gnmi.PathElem;
import static gnmi.Gnmi.SubscribeRequest;
import static gnmi.Gnmi.SubscribeResponse;
import static gnmi.Gnmi.Subscription;
import static gnmi.Gnmi.SubscriptionList;
import static gnmi.Gnmi.Update;

/**
 * Class that discovers the device description and ports of a device that
 * supports the gNMI protocol and Openconfig models.
 */
public class GnmiDeviceDescriptionDiscovery
        extends AbstractHandlerBehaviour
        implements DeviceDescriptionDiscovery {

    private static final int REQUEST_TIMEOUT_SECONDS = 5;

    private static final Logger log = LoggerFactory
            .getLogger(GnmiDeviceDescriptionDiscovery.class);

    private static final String GNMI_SERVER_ADDR_KEY = "gnmi_ip";
    private static final String GNMI_SERVER_PORT_KEY = "gnmi_port";

    @Override
    public DeviceDescription discoverDeviceDetails() {
        return null;
    }

    @Override
    public List<PortDescription> discoverPortDetails() {
        log.info("Discovering port details on device {}", handler().data().deviceId());

        String serverAddr = this.data().value(GNMI_SERVER_ADDR_KEY);
        String serverPortString = this.data().value(GNMI_SERVER_PORT_KEY);

        if (serverAddr == null || serverPortString == null ||
                serverAddr.isEmpty() || serverPortString.isEmpty()) {
            log.warn("gNMI server information not provided, can't discover ports");
            return ImmutableList.of();
        }

        // Get the channel
        ManagedChannel channel = getChannel(serverAddr, serverPortString);

        if (channel == null) {
            return ImmutableList.of();
        }

        // Build the subscribe request
        SubscribeRequest request = subscribeRequest();

        // New stub
        gNMIGrpc.gNMIStub gnmiStub = gNMIGrpc.newStub(channel);

        final CompletableFuture<List<PortDescription>>
                reply = new CompletableFuture<>();

        // Subscribe to the replies
        StreamObserver<SubscribeRequest> subscribeRequest = gnmiStub
                .subscribe(new SubscribeResponseObserver(reply));
        log.debug("Interfaces request {}", request);

        List<PortDescription> ports;
        try {
            // Issue the request
            subscribeRequest.onNext(request);
            ports = reply.get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException
                | StatusRuntimeException e) {
            log.warn("Unable to discover ports from {}: {}",
                    data().deviceId(), e.getMessage());
            log.debug("{}", e);
            return ImmutableList.of();
        } finally {
            subscribeRequest.onCompleted();
        }

        return ports;
    }

    /**
     * Obtains the ManagedChannel to be used for the communication.
     *
     * @return the managed channel
     */
    private ManagedChannel getChannel(String serverAddr, String serverPortString) {

        DeviceId deviceId = handler().data().deviceId();

        GrpcController controller = handler().get(GrpcController.class);
        ManagedChannel channel = null;

        //FIXME can be optimized
        //getting a channel if exists.
        ManagedChannel managedChannel = controller
                .getChannels(handler().data().deviceId()).stream().filter(c -> {
                    String[] authority = c.authority().split(":");
                    String host = authority[0];
                    String port = authority[1];
                    return host.equals(serverAddr) && port.equals(serverPortString);
                }).findAny().orElse(null);

        if (managedChannel != null) {
            log.debug("Reusing Channel");
            channel = managedChannel;
        } else {
            log.debug("Creating Channel");
            GrpcChannelId newChannelId = GrpcChannelId.of(deviceId, "gnmi");

            ManagedChannelBuilder channelBuilder = NettyChannelBuilder
                    .forAddress(serverAddr, Integer.valueOf(serverPortString))
                    .usePlaintext(true)
                    .nameResolverFactory(new DnsNameResolverProvider());

            try {
                channel = controller.connectChannel(newChannelId, channelBuilder);
            } catch (IOException e) {
                log.warn("Unable to connect to gRPC server of {}: {}",
                        deviceId, e.getMessage());
            }
        }
        return channel;
    }

    /**
     * Creates the subscribe request for the interfaces.
     *
     * @return subscribe request
     */
    private SubscribeRequest subscribeRequest() {
        Path path = Path.newBuilder()
                .addElem(PathElem.newBuilder().setName("interfaces").build())
                .addElem(PathElem.newBuilder().setName("interface").build())
                .addElem(PathElem.newBuilder().setName("...").build())
                .build();
        Subscription subscription = Subscription.newBuilder().setPath(path).build();
        SubscriptionList list = SubscriptionList.newBuilder().setMode(SubscriptionList.Mode.ONCE)
                .addSubscription(subscription).build();
        return SubscribeRequest.newBuilder().setSubscribe(list).build();
    }

    /**
     * Handles messages received from the device on the stream channel.
     */
    private final class SubscribeResponseObserver
            implements StreamObserver<SubscribeResponse> {

        private final CompletableFuture<List<PortDescription>> reply;

        private SubscribeResponseObserver(CompletableFuture<List<PortDescription>> reply) {
            this.reply = reply;
        }

        @Override
        public void onNext(SubscribeResponse message) {
            Map<String, DefaultPortDescription.Builder> ports = new HashMap<>();
            Map<String, DefaultAnnotations.Builder> portsAnnotations = new HashMap<>();
            log.debug("Response {} ", message.getUpdate().toString());
            message.getUpdate().getUpdateList().forEach(update -> {
                parseUpdate(ports, portsAnnotations, update);
            });

            List<PortDescription> portDescriptionList = new ArrayList<>();
            ports.forEach((k, v) -> {
//                v.portSpeed(1000L);
                v.type(Port.Type.COPPER);
                v.annotations(portsAnnotations.get(k).set("name", k).build());
                portDescriptionList.add(v.build());
            });

            reply.complete(portDescriptionList);
        }


        @Override
        public void onError(Throwable throwable) {
            log.warn("Error on stream channel for {}: {}",
                    data().deviceId(), Status.fromThrowable(throwable));
            log.debug("{}", throwable);
        }

        @Override
        public void onCompleted() {
            log.debug("SubscribeResponseObserver completed");
        }
    }

    /**
     * Parses the update received from the device.
     *
     * @param ports            the ports description to build
     * @param portsAnnotations the ports annotations list to populate
     * @param update           the update received
     */
    private void parseUpdate(Map<String, DefaultPortDescription.Builder> ports,
                             Map<String, DefaultAnnotations.Builder> portsAnnotations,
                             Update update) {

        //FIXME crude parsing, can be done via object (de)serialization
        if (update.getPath().getElemList().size() > 3) {
            String name = update.getPath().getElem(3).getName();
            String portId = update.getPath().getElem(1).getKeyMap().get("name");
            if (!ports.containsKey(portId)) {
                int number = Character.getNumericValue(portId.charAt(portId.length() - 1));
                PortNumber portNumber = PortNumber.portNumber(number, portId);
                ports.put(portId, DefaultPortDescription.builder()
                        .withPortNumber(portNumber));
            }
            if (name.equals("enabled")) {
                DefaultPortDescription.Builder builder = ports.get(portId);
                builder = builder.isEnabled(update.getVal().getBoolVal());
                ports.put(portId, builder);
            } else if (name.equals("state")) {
                String speedName = update.getPath().getElem(4).getName();
                if (speedName.equals("negotiated-port-speed")) {
                    DefaultPortDescription.Builder builder = ports.get(portId);
                    long speed = parsePortSpeed(update.getVal().getStringVal());
                    builder = builder.portSpeed(speed);
                    ports.put(portId, builder);
                }
            } else if (!name.equals("ifindex")) {
                if (!portsAnnotations.containsKey(portId)) {
                    portsAnnotations.put(portId, DefaultAnnotations.builder()
                            .set(name, update.getVal().toByteString()
                                    .toString(Charset.defaultCharset()).trim()));
                } else {
                    DefaultAnnotations.Builder builder = portsAnnotations.get(portId);
                    builder = builder.set(name, update.getVal().toByteString().
                            toString(Charset.defaultCharset()).trim());
                    portsAnnotations.put(portId, builder);
                }
            }
        }
    }

    private long parsePortSpeed(String speed) {
        log.debug("Speed from config {}", speed);
        switch (speed) {
            case "SPEED_10MB":
                return 10;
            case "SPEED_100MB":
                return 10;
            case "SPEED_1GB":
                return 1000;
            case "SPEED_10GB":
                return 10000;
            case "SPEED_25GB":
                return 25000;
            case "SPEED_40GB":
                return 40000;
            case "SPEED_50GB":
                return 50000;
            case "SPEED_100GB":
                return 100000;
            default:
                return 1000;
        }
    }
}
