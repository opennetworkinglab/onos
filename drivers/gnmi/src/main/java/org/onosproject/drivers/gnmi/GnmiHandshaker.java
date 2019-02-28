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
import org.onosproject.gnmi.api.GnmiClientKey;
import org.onosproject.gnmi.api.GnmiController;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.device.DeviceHandshaker;

import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * Implementation of DeviceHandshaker for gNMI.
 */
public class GnmiHandshaker extends AbstractGnmiHandlerBehaviour implements DeviceHandshaker {

    @Override
    public boolean isReachable() {
        final GnmiClient client = getClientByKey();
        return client != null && client.isServerReachable();
    }

    @Override
    public CompletableFuture<Boolean> probeReachability() {
        final GnmiClient client = getClientByKey();
        if (client == null) {
            return completedFuture(false);
        }
        return client.probeService();
    }

    @Override
    public boolean isAvailable() {
        return isReachable();
    }

    @Override
    public CompletableFuture<Boolean> probeAvailability() {
        return probeReachability();
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
        return CompletableFuture.supplyAsync(this::createClient);
    }

    private boolean createClient() {
        GnmiClientKey clientKey = clientKey();
        if (clientKey == null) {
            return false;
        }
        if (!handler().get(GnmiController.class).createClient(clientKey)) {
            log.warn("Unable to create client for {}",
                     handler().data().deviceId());
            return false;
        }
        return true;
    }

    @Override
    public boolean isConnected() {
        return getClientByKey() != null;
    }

    @Override
    public void disconnect() {
        handler().get(GnmiController.class)
                .removeClient(handler().data().deviceId());
    }
}
