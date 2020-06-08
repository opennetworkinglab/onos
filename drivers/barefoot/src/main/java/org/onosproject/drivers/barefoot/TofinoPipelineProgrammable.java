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

package org.onosproject.drivers.barefoot;

import com.google.common.collect.Lists;
import org.apache.commons.io.IOUtils;
import org.onosproject.drivers.p4runtime.AbstractP4RuntimePipelineProgrammable;
import org.onosproject.net.behaviour.PiPipelineProgrammable;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.model.PiPipeconf.ExtensionType;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.onosproject.net.pi.model.PiPipeconf.ExtensionType.RAW_DEVICE_CONFIG;
import static org.onosproject.net.pi.model.PiPipeconf.ExtensionType.TOFINO_BIN;
import static org.onosproject.net.pi.model.PiPipeconf.ExtensionType.TOFINO_CONTEXT_JSON;

/**
 * Implementation of the PiPipelineProgrammable behaviour for a Tofino-based switch.
 */
public class TofinoPipelineProgrammable
        extends AbstractP4RuntimePipelineProgrammable
        implements PiPipelineProgrammable {

    @Override
    public Optional<PiPipeconf> getDefaultPipeconf() {
        return Optional.empty();
    }

    @Override
    public ByteBuffer createDeviceDataBuffer(PiPipeconf pipeconf) {
        List<ByteBuffer> buffers = Lists.newLinkedList();

        if (pipeconf.extension(RAW_DEVICE_CONFIG).isPresent()) {
            buffers.add(rawDeviceConfig(pipeconf));
        } else {
            try {
                buffers.add(nameBuffer(pipeconf));
                buffers.add(extensionBuffer(pipeconf, TOFINO_BIN));
                buffers.add(extensionBuffer(pipeconf, TOFINO_CONTEXT_JSON));
            } catch (ExtensionException e) {
                return null;
            }
        }

        // Concatenate buffers (flip so they can be read).
        int len = buffers.stream().mapToInt(Buffer::limit).sum();
        ByteBuffer deviceData = ByteBuffer.allocate(len);
        buffers.forEach(b -> deviceData.put((ByteBuffer) b.flip()));
        deviceData.flip();

        return deviceData.asReadOnlyBuffer();
    }

    private ByteBuffer nameBuffer(PiPipeconf pipeconf) {
        // Length of the name + name.
        String name = pipeconf.id().toString();
        return ByteBuffer.allocate(Integer.BYTES + name.length())
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt(name.length())
                .put(name.getBytes(StandardCharsets.UTF_8));
    }

    private ByteBuffer extensionBuffer(PiPipeconf pipeconf, ExtensionType extType) {
        if (!pipeconf.extension(extType).isPresent()) {
            log.warn("Missing extension {} in pipeconf {}", extType, pipeconf.id());
            throw new ExtensionException();
        }
        try {
            byte[] bytes = IOUtils.toByteArray(pipeconf.extension(extType).get());
            // Length of the extension + bytes.
            return ByteBuffer.allocate(Integer.BYTES + bytes.length)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .putInt(bytes.length)
                    .put(bytes);
        } catch (IOException ex) {
            log.warn("Unable to read extension {} from pipeconf {}: {}",
                     extType, pipeconf.id(), ex.getMessage());
            throw new ExtensionException();
        }
    }

    private ByteBuffer rawDeviceConfig(PiPipeconf pipeconf) {
        try {
            byte[] bytes = IOUtils.toByteArray(pipeconf.extension(RAW_DEVICE_CONFIG).get());
            return ByteBuffer.allocate(bytes.length)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .put(bytes);
        } catch (IOException ex) {
            log.warn("Unable to read raw device config from pipeconf {}: {}",
                     pipeconf.id(), ex.getMessage());
            throw new ExtensionException();
        }
    }

    private static class ExtensionException extends IllegalArgumentException {
    }
}
