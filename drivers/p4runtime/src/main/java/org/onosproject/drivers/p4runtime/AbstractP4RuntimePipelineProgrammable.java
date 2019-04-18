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

import org.onosproject.net.behaviour.PiPipelineProgrammable;
import org.onosproject.net.pi.model.PiPipeconf;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.completedFuture;
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
    public CompletableFuture<Boolean> setPipeconf(PiPipeconf pipeconf) {
        if (!setupBehaviour("setPipeconf()")) {
            return completedFuture(false);
        }

        final ByteBuffer deviceDataBuffer = createDeviceDataBuffer(pipeconf);
        if (deviceDataBuffer == null) {
            // Hopefully the child class logged the problem.
            return completedFuture(false);
        }

        return client.setPipelineConfig(p4DeviceId, pipeconf, deviceDataBuffer);
    }

    @Override
    public CompletableFuture<Boolean> isPipeconfSet(PiPipeconf pipeconf) {
        if (!setupBehaviour("isPipeconfSet()")) {
            return completedFuture(false);
        }

        return client.isPipelineConfigSet(p4DeviceId, pipeconf);
    }

    @Override
    public abstract Optional<PiPipeconf> getDefaultPipeconf();
}
