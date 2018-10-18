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

import org.apache.commons.io.IOUtils;
import org.onosproject.drivers.p4runtime.AbstractP4RuntimePipelineProgrammable;
import org.onosproject.net.behaviour.PiPipelineProgrammable;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.model.PiPipeconfId;
import org.onosproject.net.pi.model.PiPipeconf.ExtensionType;
import org.onosproject.net.pi.service.PiPipeconfService;

import java.io.IOException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Optional;

import static org.onosproject.net.pi.model.PiPipeconf.ExtensionType.SPECTRUM_BIN;

/**
 * Implementation of the PiPipelineProgrammable behaviour for a Spectrum-based
 * switch with P4Runtime support.
 */
public class SpectrumPipelineProgrammable
        extends AbstractP4RuntimePipelineProgrammable
        implements PiPipelineProgrammable {

    private static final PiPipeconfId MLNX_FABRIC_PIPECONF_ID =
            new PiPipeconfId("org.onosproject.pipelines.fabric-mlnx");

    @Override
    public ByteBuffer createDeviceDataBuffer(PiPipeconf pipeconf) {
        log.debug("Creating device data buffer for {} in pipeconf {}", SPECTRUM_BIN, pipeconf.id());
        ByteBuffer deviceData;
        try {
            deviceData = extensionBuffer(pipeconf, SPECTRUM_BIN);
        } catch (ExtensionException e) {
            log.error("Failed to create device data buffer for {} in pipeconf {}", SPECTRUM_BIN, pipeconf.id());
            return null;
        }
        // flip buffer data so they can be read
        deviceData.flip();
        return deviceData.asReadOnlyBuffer();
    }

    @Override
    public Optional<PiPipeconf> getDefaultPipeconf() {
        return handler().get(PiPipeconfService.class)
                .getPipeconf(MLNX_FABRIC_PIPECONF_ID);
    }

    private ByteBuffer extensionBuffer(PiPipeconf pipeconf, ExtensionType extType) {
        if (!pipeconf.extension(extType).isPresent()) {
            log.warn("Missing extension {} in pipeconf {}", extType, pipeconf.id());
            throw new ExtensionException();
        }
        try {
            byte[] bytes = IOUtils.toByteArray(pipeconf.extension(extType).get());
            // Length of the extension + bytes.
            return ByteBuffer.allocate(bytes.length)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .put(bytes);
        } catch (IOException ex) {
            log.warn("Unable to read extension {} from pipeconf {}: {}",
                     extType, pipeconf.id(), ex.getMessage());
            throw new ExtensionException();
        }
    }

    private static class ExtensionException extends IllegalArgumentException {
    }
}
