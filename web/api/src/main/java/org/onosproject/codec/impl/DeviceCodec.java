/*
 * Copyright 2014 Open Networking Laboratory
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
import org.onosproject.codec.CodecContext;
import org.onosproject.net.Device;
import org.onosproject.net.device.DeviceService;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Device JSON codec.
 */
public class DeviceCodec extends AnnotatedCodec<Device> {

    @Override
    public ObjectNode encode(Device device, CodecContext context) {
        checkNotNull(device, "Device cannot be null");
        DeviceService service = context.get(DeviceService.class);
        ObjectNode result = context.mapper().createObjectNode()
                .put("id", device.id().toString())
                .put("available", service.isAvailable(device.id()))
                .put("role", service.getRole(device.id()).toString())
                .put("mfr", device.manufacturer())
                .put("hw", device.hwVersion())
                .put("sw", device.swVersion())
                .put("serial", device.serialNumber());
        return annotate(result, device, context);
    }

}
