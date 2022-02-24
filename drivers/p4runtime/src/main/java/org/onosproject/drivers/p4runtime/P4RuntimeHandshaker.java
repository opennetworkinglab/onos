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
import org.onosproject.grpc.utils.AbstractGrpcHandshaker;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.device.DeviceAgentListener;
import org.onosproject.net.device.DeviceHandshaker;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.service.PiPipeconfService;
import org.onosproject.net.pi.service.PiPipeconfWatchdogService;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.p4runtime.api.P4RuntimeClient;
import org.onosproject.p4runtime.api.P4RuntimeController;

import java.math.BigInteger;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.onosproject.drivers.p4runtime.P4RuntimeDriverUtils.extractP4DeviceId;

/**
 * Implementation of DeviceHandshaker for P4Runtime.
 */
public class P4RuntimeHandshaker
        extends AbstractGrpcHandshaker<P4RuntimeClient, P4RuntimeController>
        implements DeviceHandshaker {

    // This is needed to compute an election ID based on mastership term and
    // preference. At the time of writing the practical maximum cluster size is
    // 9. Since election IDs are 128bit numbers, we should'nt be too worried of
    // being conservative when setting a static max size here. Making the
    // cluster size dynamic would likely cause conflicts when generating
    // election IDs (e.g. two nodes seeing different cluster sizes).
    private static final int MAX_CLUSTER_SIZE = 20;

    private Long p4DeviceId;

    public P4RuntimeHandshaker() {
        super(P4RuntimeController.class);
    }

    @Override
    protected boolean setupBehaviour(String opName) {
        if (!super.setupBehaviour(opName)) {
            return false;
        }

        p4DeviceId = extractP4DeviceId(mgmtUriFromNetcfg());
        if (p4DeviceId == null) {
            log.warn("Unable to obtain the P4Runtime-internal device_id from " +
                             "config of {}, cannot perform {}",
                     deviceId, opName);
            return false;
        }
        return true;
    }

    @Override
    public boolean isAvailable() {
        // To be available, we require a session open (for packet in/out) and a
        // pipeline config set.
        if (!setupBehaviour("isAvailable()") ||
                !client.isServerReachable() ||
                !client.isSessionOpen(p4DeviceId)) {
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
        if (!setupBehaviour("probeAvailability()") ||
                !client.isServerReachable() ||
                !client.isSessionOpen(p4DeviceId)) {
            return completedFuture(false);
        }

        PiPipeconfService piPipeconfService = handler().get(PiPipeconfService.class);
        final Optional<PiPipeconf> optionalPiPipeconf = piPipeconfService.getPipeconf(deviceId);
        if (optionalPiPipeconf.isEmpty()) {
            return completedFuture(false);
        }

        if (!PiPipeconfWatchdogService.PipelineStatus.READY.equals(
                handler().get(PiPipeconfWatchdogService.class)
                        .getStatus(data().deviceId()))) {
            return completedFuture(false);
        }

        return client.isPipelineConfigSet(p4DeviceId, optionalPiPipeconf.get());
    }

    @Override
    public void roleChanged(MastershipRole newRole) {
        if (!setupBehaviour("roleChanged()")) {
            return;
        }
        if (newRole.equals(MastershipRole.NONE)) {
            log.info("Notified role NONE, closing session...");
            client.closeSession(p4DeviceId);
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
        final BigInteger electionId = BigInteger.valueOf(term)
                .multiply(BigInteger.valueOf(MAX_CLUSTER_SIZE))
                .subtract(BigInteger.valueOf(preference));
        client.setMastership(p4DeviceId, preference == 0, electionId);
    }

    @Override
    public MastershipRole getRole() {
        if (!setupBehaviour("getRole()") ||
                !client.isServerReachable() ||
                !client.isSessionOpen(p4DeviceId)) {
            return MastershipRole.NONE;
        }
        return client.isMaster(p4DeviceId)
                ? MastershipRole.MASTER : MastershipRole.STANDBY;
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
