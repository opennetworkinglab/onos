/*
 * Copyright 2019-present Open Networking Foundation
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

package org.onosproject.drivers.gnoi;

import org.onlab.packet.ChassisId;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.driver.AbstractHandlerBehaviour;

import java.util.Collections;
import java.util.List;

/**
 * Implementation of DeviceDescriptionDiscovery for gNOI devices.
 */
public class GnoiDeviceDescriptionDiscovery
        extends AbstractHandlerBehaviour
        implements DeviceDescriptionDiscovery {

    private static final String UNKNOWN = "unknown";

    @Override
    public DeviceDescription discoverDeviceDetails() {

        return new DefaultDeviceDescription(
                data().deviceId().uri(),
                Device.Type.SWITCH,
                data().driver().manufacturer(),
                data().driver().hwVersion(),
                data().driver().swVersion(),
                UNKNOWN,
                new ChassisId(),
                false,
                DefaultAnnotations.builder()
                        .set(AnnotationKeys.PROTOCOL, "gNOI")
                        .build());
    }


    @Override
    public List<PortDescription> discoverPortDetails() {
        return Collections.emptyList();
    }
}
