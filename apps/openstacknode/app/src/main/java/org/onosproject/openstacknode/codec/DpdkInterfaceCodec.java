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
package org.onosproject.openstacknode.codec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.openstacknode.api.DefaultDpdkInterface;
import org.onosproject.openstacknode.api.DpdkInterface;
import org.onosproject.openstacknode.api.DpdkInterface.Type;
import org.slf4j.Logger;

import java.util.Locale;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * DPDK interface codec used for serializing and de-serializing JSON string.
 */
public class DpdkInterfaceCodec extends JsonCodec<DpdkInterface> {
    protected final Logger log = getLogger(getClass());

    private static final String DEVICE_NAME = "deviceName";
    private static final String INTF = "intf";
    private static final String PCI_ADDRESS = "pciAddress";
    private static final String TYPE = "type";
    private static final String MTU = "mtu";

    private static final String NOT_NULL_MESSAGE = "dpdk interface cannot be null";
    private static final String MISSING_MESSAGE = " is required in DpdkInterface";

    @Override
    public ObjectNode encode(DpdkInterface entity, CodecContext context) {
        checkNotNull(entity, NOT_NULL_MESSAGE);

        return context.mapper().createObjectNode()
                .put(DEVICE_NAME, entity.deviceName())
                .put(INTF, entity.intf())
                .put(PCI_ADDRESS, entity.pciAddress())
                .put(TYPE, entity.type().name())
                .put(MTU, entity.mtu().toString());
    }

    @Override
    public DpdkInterface decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        String deviceName = nullIsIllegal(json.get(DEVICE_NAME).asText(),
                DEVICE_NAME + MISSING_MESSAGE);
        String intf = nullIsIllegal(json.get(INTF).asText(),
                INTF + MISSING_MESSAGE);
        String pciAddress = nullIsIllegal(json.get(PCI_ADDRESS).asText(),
                PCI_ADDRESS + MISSING_MESSAGE);
        String typeString = nullIsIllegal(json.get(TYPE).asText(),
                TYPE + MISSING_MESSAGE);

        Type type;
        try {
            type = Type.valueOf(typeString.toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException e) {
            log.error(TYPE + MISSING_MESSAGE);
            throw new IllegalArgumentException(e);
        }

        DpdkInterface.Builder builder = DefaultDpdkInterface.builder()
                .deviceName(deviceName)
                .intf(intf)
                .pciAddress(pciAddress)
                .type(type);

        JsonNode mtuJson = json.get(MTU);

        if (mtuJson != null) {
            builder.mtu(Long.parseLong(mtuJson.asText()));
        }

        return builder.build();
    }
}
