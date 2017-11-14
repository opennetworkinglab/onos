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

package org.onosproject.drivers.bmv2;

import org.apache.commons.io.IOUtils;
import org.onosproject.drivers.p4runtime.AbstractP4RuntimePipelineProgrammable;
import org.onosproject.net.behaviour.PiPipelineProgrammable;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.pipelines.basic.PipeconfLoader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Optional;

import static org.onosproject.net.pi.model.PiPipeconf.ExtensionType.BMV2_JSON;

/**
 * Implementation of the PiPipelineProgrammable behavior for BMv2.
 */
public class Bmv2PipelineProgrammable extends AbstractP4RuntimePipelineProgrammable
        implements PiPipelineProgrammable {

    @Override
    public ByteBuffer createDeviceDataBuffer(PiPipeconf pipeconf) {
        if (!pipeconf.extension(BMV2_JSON).isPresent()) {
            log.warn("Missing extension {} in pipeconf {}", BMV2_JSON, pipeconf.id());
            return null;
        }
        try {
            return ByteBuffer.wrap(IOUtils.toByteArray(pipeconf.extension(BMV2_JSON).get()));
        } catch (IOException e) {
            log.warn("Unable to read {} from pipeconf {}", BMV2_JSON, pipeconf.id());
            return null;
        }
    }

    @Override
    public Optional<PiPipeconf> getDefaultPipeconf() {
        return Optional.of(PipeconfLoader.BASIC_PIPECONF);
    }
}
