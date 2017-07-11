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

package org.onosproject.drivers.bmv2;

import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.NettyChannelBuilder;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.device.DeviceHandshaker;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.driver.DriverData;
import org.onosproject.p4runtime.api.P4RuntimeController;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of DeviceHandshaker for BMv2.
 */
public class Bmv2Handshaker extends AbstractHandlerBehaviour
        implements DeviceHandshaker {

    private final Logger log = getLogger(getClass());

    // TODO: consider abstract class with empty connect method and  implementation into a protected one for reusability.

    @Override
    public CompletableFuture<Boolean> connect() {
        return CompletableFuture.supplyAsync(this::doConnect);
    }

    private boolean doConnect() {

        P4RuntimeController controller = handler().get(P4RuntimeController.class);

        DeviceId deviceId = handler().data().deviceId();
        // DeviceKeyService deviceKeyService = handler().get(DeviceKeyService.class);
        DriverData data = data();

        String serverAddr = data.value("p4runtime_ip");
        int serverPort = Integer.valueOf(data.value("p4runtime_port"));
        int p4DeviceId = Integer.valueOf(data.value("p4runtime_deviceId"));

        ManagedChannelBuilder channelBuilder = NettyChannelBuilder
                .forAddress(serverAddr, serverPort)
                .usePlaintext(true);

        if (!controller.createClient(deviceId, p4DeviceId, channelBuilder)) {
            log.warn("Unable to create P4runtime client for {}", deviceId);
            return false;
        }

        // TODO: gNMI handling

        return true;
    }

    @Override
    public CompletableFuture<Boolean> disconnect() {
        return CompletableFuture.supplyAsync(() -> {
            P4RuntimeController controller = handler().get(P4RuntimeController.class);
            DeviceId deviceId = handler().data().deviceId();
            controller.removeClient(deviceId);
            return true;
        });
    }

    @Override
    public CompletableFuture<Boolean> isReachable() {
        return CompletableFuture.supplyAsync(() -> {
            P4RuntimeController controller = handler().get(P4RuntimeController.class);
            DeviceId deviceId = handler().data().deviceId();
            return controller.isReacheable(deviceId);
        });
    }

    @Override
    public CompletableFuture<MastershipRole> roleChanged(MastershipRole newRole) {
        CompletableFuture<MastershipRole> result = new CompletableFuture<>();
        log.warn("roleChanged not implemented");
        result.complete(MastershipRole.MASTER);
        // TODO.
        return result;
    }
}
