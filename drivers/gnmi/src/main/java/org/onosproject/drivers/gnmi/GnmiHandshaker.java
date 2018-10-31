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

package org.onosproject.drivers.gnmi;

import org.onosproject.gnmi.api.GnmiClient;
import org.onosproject.gnmi.api.GnmiController;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.device.DeviceHandshaker;

import java.util.concurrent.CompletableFuture;

/**
 * Implementation of DeviceHandshaker for gNMI.
 */
public class GnmiHandshaker extends AbstractGnmiHandlerBehaviour implements DeviceHandshaker {

    @Override
    public CompletableFuture<Boolean> isReachable() {
        return CompletableFuture
                // gNMI requires a client to be created to
                // check for reachability.
                .supplyAsync(super::createClient)
                .thenApplyAsync(client -> {
                    if (client == null) {
                        return false;
                    }
                    return handler()
                            .get(GnmiController.class)
                            .isReachable(handler().data().deviceId());
                });
    }

    @Override
    public void roleChanged(MastershipRole newRole) {
        throw new UnsupportedOperationException("Mastership operation not supported");
    }

    @Override
    public MastershipRole getRole() {
        throw new UnsupportedOperationException("Mastership operation not supported");
    }

    @Override
    public CompletableFuture<Boolean> connect() {
        return CompletableFuture
                .supplyAsync(super::createClient)
                .thenComposeAsync(client -> {
                    if (client == null) {
                        return CompletableFuture.completedFuture(false);
                    }
                    return CompletableFuture.completedFuture(true);
                });
    }

    @Override
    public boolean isConnected() {
        final GnmiController controller = handler().get(GnmiController.class);
        final DeviceId deviceId = handler().data().deviceId();
        final GnmiClient client = controller.getClient(deviceId);

        if (client == null) {
            return false;
        }

        return getFutureWithDeadline(
                client.isServiceAvailable(),
                "checking if gNMI service is available", false);
    }

    @Override
    public CompletableFuture<Boolean> disconnect() {
        final GnmiController controller = handler().get(GnmiController.class);
        final DeviceId deviceId = handler().data().deviceId();
        final GnmiClient client = controller.getClient(deviceId);
        if (client == null) {
            return CompletableFuture.completedFuture(true);
        }
        return client.shutdown()
                .thenApplyAsync(v -> {
                    controller.removeClient(deviceId);
                    return true;
                });
    }
}
