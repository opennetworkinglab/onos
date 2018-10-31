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

package org.onosproject.drivers.stratum.dummy;

import org.onosproject.drivers.p4runtime.AbstractP4RuntimePipelineProgrammable;
import org.onosproject.net.behaviour.PiPipelineProgrammable;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.pipelines.basic.PipeconfLoader;

import java.nio.ByteBuffer;
import java.util.Optional;

/**
 * Implementation of the PiPipelineProgrammable behavior for Dummy Stratum Switch.
 */
public class StratumDummyPipelineProgrammable
        extends AbstractP4RuntimePipelineProgrammable
        implements PiPipelineProgrammable {

    @Override
    public ByteBuffer createDeviceDataBuffer(PiPipeconf pipeconf) {
        // No pipeline binary needed
        return ByteBuffer.allocate(1);
    }

    @Override
    public Optional<PiPipeconf> getDefaultPipeconf() {
        return Optional.of(PipeconfLoader.BASIC_PIPECONF);
    }
}
