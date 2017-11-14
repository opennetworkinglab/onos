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
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.behaviour.PiPipelineProgrammable;
import org.onosproject.p4runtime.api.P4RuntimeClient;
import org.onosproject.p4runtime.api.P4RuntimeController;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Abstract implementation of the PiPipelineProgrammable behaviours for a P4Runtime device.
 */
public abstract class AbstractP4RuntimePipelineProgrammable extends AbstractHandlerBehaviour
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

        if (!controller.hasClient(deviceId)) {
            log.warn("Unable to find client for {}, aborting pipeconf deploy", deviceId);
            return false;
        }
        P4RuntimeClient client = controller.getClient(deviceId);

        ByteBuffer deviceDataBuffer = createDeviceDataBuffer(pipeconf);
        if (deviceDataBuffer == null) {
            // Hopefully the child class logged the problem.
            return false;
        }

        try {
            if (!client.initStreamChannel().get()) {
                log.warn("Unable to init stream channel to {}.", deviceId);
                return false;
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error("Exception while initializing stream channel on {}", deviceId, e);
            return false;
        }

        try {
            if (!client.setPipelineConfig(pipeconf, deviceDataBuffer).get()) {
                log.warn("Unable to deploy pipeconf {} to {}", pipeconf.id(), deviceId);
                return false;
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error("Exception while deploying pipeconf to {}", deviceId, e);
            return false;
        }
        return true;
    }

    @Override
    public abstract Optional<PiPipeconf> getDefaultPipeconf();
}
