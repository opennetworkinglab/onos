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

import org.onlab.util.SharedExecutors;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.PiPipelineProgrammable;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.p4runtime.api.P4RuntimeClient;
import org.onosproject.p4runtime.api.P4RuntimeController;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Abstract implementation of the PiPipelineProgrammable behaviours for a P4Runtime device.
 */
public abstract class AbstractP4RuntimePipelineProgrammable
        extends AbstractP4RuntimeHandlerBehaviour
        implements PiPipelineProgrammable {

    protected final Logger log = getLogger(getClass());

    /**
     * Returns a byte buffer representing the target-specific device data to be used in the SetPipelineConfig message.
     *
     * @param pipeconf pipeconf
     * @return byte buffer
     */
    public abstract ByteBuffer createDeviceDataBuffer(PiPipeconf pipeconf);

    @Override
    public CompletableFuture<Boolean> deployPipeconf(PiPipeconf pipeconf) {
        return CompletableFuture.supplyAsync(
                () -> doDeployConfig(pipeconf),
                SharedExecutors.getPoolThreadExecutor());
    }

    private boolean doDeployConfig(PiPipeconf pipeconf) {

        DeviceId deviceId = handler().data().deviceId();
        P4RuntimeController controller = handler().get(P4RuntimeController.class);

        P4RuntimeClient client = controller.getClient(deviceId);
        if (client == null) {
            log.warn("Unable to find client for {}, aborting pipeconf deploy", deviceId);
            return false;
        }

        ByteBuffer deviceDataBuffer = createDeviceDataBuffer(pipeconf);
        if (deviceDataBuffer == null) {
            // Hopefully the child class logged the problem.
            return false;
        }

        // We need to be master to perform write RPC to the device.
        // FIXME: properly perform mastership handling in the device provider
        // This would probably mean deploying the pipeline after the device as
        // been notified to the core.
        final Boolean masterSuccess = getFutureWithDeadline(
                client.becomeMaster(),
                "becoming master ", null);
        if (masterSuccess == null) {
            // Error already logged by getFutureWithDeadline()
            return false;
        } else if (!masterSuccess) {
            log.warn("Unable to become master for {}, aborting pipeconf deploy", deviceId);
            return false;
        }

        final Boolean deploySuccess = getFutureWithDeadline(
                client.setPipelineConfig(pipeconf, deviceDataBuffer),
                "deploying pipeconf", null);
        if (deploySuccess == null) {
            return false;
        } else if (!deploySuccess) {
            log.warn("Unable to deploy pipeconf {} to {}", pipeconf.id(), deviceId);
            return false;
        }

        return true;
    }

    @Override
    public abstract Optional<PiPipeconf> getDefaultPipeconf();
}
