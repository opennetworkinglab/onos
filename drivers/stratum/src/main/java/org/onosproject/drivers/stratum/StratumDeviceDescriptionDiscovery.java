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

package org.onosproject.drivers.stratum;

import org.onosproject.drivers.gnmi.OpenConfigGnmiDeviceDescriptionDiscovery;
import org.onosproject.drivers.gnoi.GnoiDeviceDescriptionDiscovery;
import org.onosproject.drivers.p4runtime.P4RuntimeDeviceDescriptionDiscovery;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.PortDescription;

import java.util.List;

import static java.lang.String.format;

/**
 * Implementation of DeviceDescriptionDiscovery for Stratum devices.
 */
public class StratumDeviceDescriptionDiscovery
        extends AbstractStratumBehaviour<DeviceDescriptionDiscovery>
        implements DeviceDescriptionDiscovery {

    private static final String UNKNOWN = "unknown";


    public StratumDeviceDescriptionDiscovery() {
        super(new P4RuntimeDeviceDescriptionDiscovery(),
              new OpenConfigGnmiDeviceDescriptionDiscovery(),
              new GnoiDeviceDescriptionDiscovery());
    }

    @Override
    public DeviceDescription discoverDeviceDetails() {
        final DeviceDescription p4Descr = p4runtime.discoverDeviceDetails();
        final DeviceDescription gnoiDescr = gnoi.discoverDeviceDetails();
        final DeviceDescription gnmiDescr = gnmi.discoverDeviceDetails();
        return new DefaultDeviceDescription(
                data().deviceId().uri(),
                Device.Type.SWITCH,
                data().driver().manufacturer(),
                data().driver().hwVersion(),
                data().driver().swVersion(),
                UNKNOWN,
                p4Descr.chassisId(),
                // Availability is mandated by P4Runtime.
                p4Descr.isDefaultAvailable(),
                DefaultAnnotations.builder()
                        .putAll(p4Descr.annotations())
                        .putAll(gnmiDescr.annotations())
                        .putAll(gnoiDescr.annotations())
                        .set(AnnotationKeys.PROTOCOL, format(
                                "%s, %s, %s",
                                p4Descr.annotations().value(AnnotationKeys.PROTOCOL),
                                gnmiDescr.annotations().value(AnnotationKeys.PROTOCOL),
                                gnoiDescr.annotations().value(AnnotationKeys.PROTOCOL)))
                        .build());
    }

    @Override
    public List<PortDescription> discoverPortDetails() {
        return gnmi.discoverPortDetails();
    }
}
