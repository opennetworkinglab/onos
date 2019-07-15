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

package org.onosproject.drivers.stratum;

import org.apache.commons.io.IOUtils;
import org.onosproject.drivers.p4runtime.AbstractP4RuntimePipelineProgrammable;
import org.onosproject.net.behaviour.PiPipelineProgrammable;
import org.onosproject.net.pi.model.PiPipeconf;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Optional;

import static org.onosproject.net.pi.model.PiPipeconf.ExtensionType.STRATUM_FPM_BIN;

/**
 * Implementation of the PiPipelineProgrammable behaviour for a Stratum
 * Fixed-Pipeline Model (FPM) target.
 */
public class FpmPipelineProgrammable
        extends AbstractP4RuntimePipelineProgrammable
        implements PiPipelineProgrammable {

    @Override
    public Optional<PiPipeconf> getDefaultPipeconf() {
        return Optional.empty();
    }

    @Override
    public ByteBuffer createDeviceDataBuffer(PiPipeconf pipeconf) {
        if (pipeconf.extension(STRATUM_FPM_BIN).isEmpty()) {
            log.warn("Missing extension {} in pipeconf {}",
                     STRATUM_FPM_BIN, pipeconf.id());
            return null;
        }
        try {
            return ByteBuffer.wrap(IOUtils.toByteArray(
                    pipeconf.extension(STRATUM_FPM_BIN).get()));
        } catch (IOException e) {
            log.warn("Unable to read {} from pipeconf {}",
                     STRATUM_FPM_BIN, pipeconf.id());
            return null;
        }
    }
}
