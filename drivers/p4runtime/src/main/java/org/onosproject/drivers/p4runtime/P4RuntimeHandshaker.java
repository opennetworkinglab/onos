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

package org.onosproject.drivers.p4runtime;

import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.device.DeviceAgentListener;
import org.onosproject.net.device.DeviceHandshaker;
import org.onosproject.p4runtime.api.P4RuntimeClient;
import org.onosproject.p4runtime.api.P4RuntimeController;

import java.util.concurrent.CompletableFuture;

/**
 * Implementation of DeviceHandshaker for P4Runtime.
 */
public class P4RuntimeHandshaker extends AbstractP4RuntimeHandlerBehaviour implements DeviceHandshaker {

    @Override
    public CompletableFuture<Boolean> connect() {
        return CompletableFuture
                .supplyAsync(super::createClient)
                .thenComposeAsync(client -> {
                    if (client == null) {
                        return CompletableFuture.completedFuture(false);
                    }
                    return client.start();
                });
    }

    @Override
    public CompletableFuture<Boolean> disconnect() {
        final P4RuntimeController controller = handler().get(P4RuntimeController.class);
        final DeviceId deviceId = handler().data().deviceId();
        final P4RuntimeClient client = controller.getClient(deviceId);
        if (client == null) {
            return CompletableFuture.completedFuture(true);
        }
        return client.shutdown()
                .thenApplyAsync(v -> {
                    controller.removeClient(deviceId);
                    return true;
                });
    }

    @Override
    public CompletableFuture<Boolean> isReachable() {
        return CompletableFuture.supplyAsync(() -> handler()
                .get(P4RuntimeController.class)
                .isReachable(handler().data().deviceId())
        );
    }

    @Override
    public void roleChanged(MastershipRole newRole) {
        if (setupBehaviour() && newRole.equals(MastershipRole.MASTER)) {
            client.becomeMaster().thenAcceptAsync(result -> {
                if (!result) {
                    log.error("Unable to notify mastership role {} to {}",
                              newRole, deviceId);
                }
            });
        }
    }

    @Override
    public void addDeviceAgentListener(DeviceAgentListener listener) {
        // Don't use controller/deviceId class variables as they might be uninitialized.
        handler().get(P4RuntimeController.class)
                .addDeviceAgentListener(data().deviceId(), listener);
    }

    @Override
    public void removeDeviceAgentListener(DeviceAgentListener listener) {
        // Don't use controller/deviceId class variable as they might be uninitialized.
        handler().get(P4RuntimeController.class)
                .removeDeviceAgentListener(data().deviceId(), listener);
    }
}
