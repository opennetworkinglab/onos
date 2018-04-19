/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.drivers.mellanox;

import org.onosproject.drivers.p4runtime.AbstractP4RuntimePipelineProgrammable;
import org.onosproject.net.behaviour.PiPipelineProgrammable;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.model.PiPipeconfId;
import org.onosproject.net.pi.service.PiPipeconfService;

import java.nio.ByteBuffer;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;

/**
 * Implementation of the PiPipelineProgrammable behaviour for a Spectrum-based
 * switch with P4Runtime support.
 */
public class SpectrumPipelineProgrammable
        extends AbstractP4RuntimePipelineProgrammable
        implements PiPipelineProgrammable {

    private static final PiPipeconfId FABRIC_PIPECONF_ID =
            new PiPipeconfId("org.onosproject.pipelines.fabric");

    @Override
    public ByteBuffer createDeviceDataBuffer(PiPipeconf pipeconf) {
        checkArgument(pipeconf.id().equals(FABRIC_PIPECONF_ID),
                      format("Cannot program Spectrum device with a pipeconf " +
                                     "other than '%s' (found '%s')",
                             FABRIC_PIPECONF_ID, pipeconf.id()));
        // Dummy value.
        // We assume switch to be already configured with fabric.p4 profile.
        return ByteBuffer.allocate(1).put((byte) 1);
    }

    @Override
    public Optional<PiPipeconf> getDefaultPipeconf() {
        return handler().get(PiPipeconfService.class)
                .getPipeconf(FABRIC_PIPECONF_ID);
    }
}
