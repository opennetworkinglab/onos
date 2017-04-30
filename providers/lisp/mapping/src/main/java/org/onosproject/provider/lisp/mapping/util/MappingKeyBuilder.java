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
package org.onosproject.provider.lisp.mapping.util;

import org.onosproject.lisp.msg.types.LispAfiAddress;
import org.onosproject.mapping.DefaultMappingKey;
import org.onosproject.mapping.MappingKey;
import org.onosproject.mapping.addresses.MappingAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onosproject.provider.lisp.mapping.util.MappingAddressBuilder.getAddress;

/**
 * Mapping key builder class.
 */
public class MappingKeyBuilder {
    private static final Logger log =
            LoggerFactory.getLogger(MappingKeyBuilder.class);

    private final LispAfiAddress address;

    private final DeviceId deviceId;

    private final DeviceService deviceService;


    /**
     * Default constructor for MappingKeyBuilder.
     *
     * @param deviceService device service
     * @param deviceId      device identifier
     * @param afiAddress    AFI address
     */
    public MappingKeyBuilder(DeviceService deviceService, DeviceId deviceId,
                             LispAfiAddress afiAddress) {
        this.deviceId = deviceId;
        this.address = afiAddress;
        this.deviceService = deviceService;
    }

    /**
     * Builds mapping key from a AFI address.
     *
     * @return mapping key
     */
    public MappingKey build() {
        MappingAddress mappingAddress = getAddress(deviceService, deviceId, address);
        return DefaultMappingKey.builder().withAddress(mappingAddress).build();
    }
}
