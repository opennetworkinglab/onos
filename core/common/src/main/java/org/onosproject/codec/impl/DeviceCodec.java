/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.codec.impl;

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.onlab.packet.ChassisId;
import org.onosproject.codec.CodecContext;
import org.onosproject.net.Annotations;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.Device;
import org.onosproject.net.Device.Type;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.provider.ProviderId;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.DeviceId.deviceId;

/**
 * Device JSON codec.
 */
public final class DeviceCodec extends AnnotatedCodec<Device> {

    // JSON fieldNames
    private static final String ID = "id";
    private static final String TYPE = "type";
    private static final String MFR = "mfr";
    private static final String HW = "hw";
    private static final String SW = "sw";
    private static final String SERIAL = "serial";
    private static final String CHASSIS_ID = "chassisId";


    @Override
    public ObjectNode encode(Device device, CodecContext context) {
        checkNotNull(device, "Device cannot be null");
        DeviceService service = context.getService(DeviceService.class);
        ObjectNode result = context.mapper().createObjectNode()
                .put(ID, device.id().toString())
                .put(TYPE, device.type().name())
                .put("available", service.isAvailable(device.id()))
                .put("role", service.getRole(device.id()).toString())
                .put(MFR, device.manufacturer())
                .put(HW, device.hwVersion())
                .put(SW, device.swVersion())
                .put(SERIAL, device.serialNumber())
                .put(CHASSIS_ID, device.chassisId().toString());
        return annotate(result, device, context);
    }


    /**
     * {@inheritDoc}
     *
     * Note: ProviderId is not part of JSON representation.
     *       Returned object will have random ProviderId set.
     */
    @Override
    public Device decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        DeviceId id = deviceId(json.get(ID).asText());
        // TODO: add providerId to JSON if we need to recover them.
        ProviderId pid = new ProviderId(id.uri().getScheme(), "DeviceCodec");

        Type type = Type.valueOf(json.get(TYPE).asText());
        String mfr = json.get(MFR).asText();
        String hw = json.get(HW).asText();
        String sw = json.get(SW).asText();
        String serial = json.get(SERIAL).asText();
        ChassisId chassisId = new ChassisId(json.get(CHASSIS_ID).asText());
        Annotations annotations = extractAnnotations(json, context);

        return new DefaultDevice(pid, id, type, mfr, hw, sw, serial,
                                 chassisId, annotations);
    }
}
