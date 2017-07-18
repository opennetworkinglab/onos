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

import org.onlab.util.SharedExecutors;
import org.onosproject.net.DeviceId;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.model.PiPipelineProgrammable;
import org.onosproject.p4runtime.api.P4RuntimeClient;
import org.onosproject.p4runtime.api.P4RuntimeController;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.onosproject.net.pi.model.PiPipeconf.ExtensionType.BMV2_JSON;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of the PiPipelineProgrammable for BMv2.
 */
public class Bmv2PipelineProgrammable extends AbstractHandlerBehaviour implements PiPipelineProgrammable {

    private static final PiPipeconf DEFAULT_PIPECONF = Bmv2DefaultPipeconfFactory.get();

    private final Logger log = getLogger(getClass());

    @Override
    public CompletableFuture<Boolean> deployPipeconf(PiPipeconf pipeconf) {

        CompletableFuture<Boolean> result = new CompletableFuture<>();

        SharedExecutors.getPoolThreadExecutor().submit(() -> result.complete(doDeployConfig(pipeconf)));

        return result;
    }

    private boolean doDeployConfig(PiPipeconf pipeconf) {

        P4RuntimeController controller = handler().get(P4RuntimeController.class);

        DeviceId deviceId = handler().data().deviceId();

        if (!controller.hasClient(deviceId)) {
            log.warn("Unable to find client for {}, aborting pipeconf deploy", deviceId);
            return false;

        }

        P4RuntimeClient client = controller.getClient(deviceId);

        try {
            if (!client.setPipelineConfig(pipeconf, BMV2_JSON).get()) {
                log.warn("Unable to deploy pipeconf {} to {}", pipeconf.id(), deviceId);
                return false;
            }

            // It would be more logical to have this performed at device handshake, but P4runtime would reject any
            // command if a P4info has not been set first.
            if (!client.initStreamChannel().get()) {
                log.warn("Unable to init stream channel to {}.", deviceId);
                return false;
            }

        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        return true;
    }

    @Override
    public Optional<PiPipeconf> getDefaultPipeconf() {
        return Optional.of(DEFAULT_PIPECONF);
    }
}
