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

package org.onosproject.drivers.p4runtime;

import org.onlab.packet.ChassisId;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.PortDescription;

import java.util.Collections;
import java.util.List;

import static org.onosproject.drivers.p4runtime.P4RuntimeDriverUtils.extractP4DeviceId;

/**
 * Implementation of DeviceDescriptionDiscovery for P4Runtime devices.
 */
public class P4RuntimeDeviceDescriptionDiscovery
        extends AbstractP4RuntimeHandlerBehaviour
        implements DeviceDescriptionDiscovery {

    private static final String UNKNOWN = "unknown";
    private static final String P4_DEVICE_ID = "p4DeviceId";

    @Override
    public DeviceDescription discoverDeviceDetails() {
        final Long p4DeviceId = extractP4DeviceId(mgmtUriFromNetcfg());
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
                        .set(P4_DEVICE_ID, p4DeviceId == null
                                ? UNKNOWN : String.valueOf(p4DeviceId))
                        .set(AnnotationKeys.PROTOCOL, "P4Runtime")
                        .build());
    }

    @Override
    public List<PortDescription> discoverPortDetails() {
        return Collections.emptyList();
    }
}
