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

import org.onosproject.cluster.ClusterService;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.device.DeviceAgentListener;
import org.onosproject.net.device.DeviceHandshaker;
import org.onosproject.net.pi.service.PiPipeconfWatchdogService;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.p4runtime.api.P4RuntimeClient;
import org.onosproject.p4runtime.api.P4RuntimeClientKey;
import org.onosproject.p4runtime.api.P4RuntimeController;

import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * Implementation of DeviceHandshaker for P4Runtime.
 */
public class P4RuntimeHandshaker extends AbstractP4RuntimeHandlerBehaviour implements DeviceHandshaker {

    // This is needed to compute an election ID based on mastership term and
    // preference. At the time of writing the practical maximum cluster size is
    // 9. Since election IDs are 128bit numbers, we should'nt be too worried of
    // being conservative when setting a static max size here. Making the
    // cluster size dynamic would likely cause conflicts when generating
    // election IDs (e.g. two nodes seeing different cluster sizes).
    private static final int MAX_CLUSTER_SIZE = 20;

    @Override
    public CompletableFuture<Boolean> connect() {
        return CompletableFuture
                .supplyAsync(this::createClient);
    }

    private boolean createClient() {
        final P4RuntimeClientKey clientKey = clientKey();
        if (clientKey == null) {
            return false;
        }
        if (!handler().get(P4RuntimeController.class).createClient(clientKey)) {
            log.debug("Unable to create client for {}", data().deviceId());
            return false;
        }
        return true;
    }

    @Override
    public boolean isConnected() {
        // This is based on the client key obtained from the current netcfg. If
        // a client already exists for this device, but the netcfg with the
        // server endpoints has changed, this will return false.
        return getClientByKey() != null;
    }

    @Override
    public void disconnect() {
        // This removes clients associated with this device ID, even if the
        // netcfg has changed and so the client key for this device.
        handler().get(P4RuntimeController.class).removeClient(data().deviceId());
    }

    @Override
    public boolean isReachable() {
        final P4RuntimeClient client = getClientByKey();
        return client != null && client.isServerReachable();
    }

    @Override
    public CompletableFuture<Boolean> probeReachability() {
        final P4RuntimeClient client = getClientByKey();
        if (client == null) {
            return completedFuture(false);
        }
        return client.probeService();
    }

    @Override
    public boolean isAvailable() {
        // To be available, we require a session open (for packet in/out) and a
        // pipeline config set.
        final P4RuntimeClient client = getClientByKey();
        if (client == null || !client.isServerReachable() || !client.isSessionOpen()) {
            return false;
        }
        // Since we cannot probe the device, we rely on what's known by the
        // pipeconf watchdog service.
        return PiPipeconfWatchdogService.PipelineStatus.READY.equals(
                handler().get(PiPipeconfWatchdogService.class)
                        .getStatus(data().deviceId()));
    }

    @Override
    public CompletableFuture<Boolean> probeAvailability() {
        // To be available, we require a session open (for packet in/out) and a
        // pipeline config set.
        final P4RuntimeClient client = getClientByKey();
        if (client == null || !client.isServerReachable() || !client.isSessionOpen()) {
            return completedFuture(false);
        }
        return client.isAnyPipelineConfigSet();
    }

    @Override
    public void roleChanged(MastershipRole newRole) {
        if (!setupBehaviour("roleChanged()")) {
            return;
        }
        if (newRole.equals(MastershipRole.NONE)) {
            log.info("Notified role NONE, closing session...");
            client.closeSession();
        } else {
            throw new UnsupportedOperationException(
                    "Use preference-based way for setting MASTER or STANDBY roles");
        }
    }

    @Override
    public void roleChanged(int preference, long term) {
        if (!setupBehaviour("roleChanged()")) {
            return;
        }
        final int clusterSize = handler().get(ClusterService.class)
                .getNodes().size();
        if (clusterSize > MAX_CLUSTER_SIZE) {
            throw new IllegalStateException(
                    "Cluster too big! Maz size supported is " + MAX_CLUSTER_SIZE);
        }
        BigInteger electionId = BigInteger.valueOf(term)
                .multiply(BigInteger.valueOf(MAX_CLUSTER_SIZE))
                .subtract(BigInteger.valueOf(preference));
        client.setMastership(preference == 0, electionId);
    }

    @Override
    public MastershipRole getRole() {
        final P4RuntimeClient client = getClientByKey();
        if (client == null || !client.isServerReachable() || !client.isSessionOpen()) {
            return MastershipRole.NONE;
        }
        return client.isMaster() ? MastershipRole.MASTER : MastershipRole.STANDBY;
    }

    @Override
    public void addDeviceAgentListener(ProviderId providerId, DeviceAgentListener listener) {
        // Don't use controller/deviceId class variables as they might be uninitialized.
        handler().get(P4RuntimeController.class)
                .addDeviceAgentListener(data().deviceId(), providerId, listener);
    }

    @Override
    public void removeDeviceAgentListener(ProviderId providerId) {
        // Don't use controller/deviceId class variable as they might be uninitialized.
        handler().get(P4RuntimeController.class)
                .removeDeviceAgentListener(data().deviceId(), providerId);
    }
}
