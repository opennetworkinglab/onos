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
package org.onosproject.driver.optical.config;

import static org.onosproject.net.optical.device.OmsPortHelper.omsPortDescription;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.onlab.packet.ChassisId;
import org.onlab.util.Frequency;
import org.onlab.util.Spectrum;
import org.onosproject.net.Device.Type;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.driver.AbstractHandlerBehaviour;

/**
 * DeviceDescriptionDiscovery for Polatis.
 */
public class PolatisDeviceDiscovery
    extends AbstractHandlerBehaviour
    implements DeviceDescriptionDiscovery {


    @Override
    public DeviceDescription discoverDeviceDetails() {
        Type type = Type.FIBER_SWITCH;
        String manufacturer = "Polatis";
        String hwVersion = "N-VST-48x48-HU1-DMHNV-805";
        String swVersion = "6.6.1.7";
        String serialNumber = "1503";
        ChassisId chassis = new ChassisId();
        boolean defaultAvailable = true;
        SparseAnnotations annotations = DefaultAnnotations.builder().build();
        return new DefaultDeviceDescription(this.data().deviceId().uri(),
                                            type,
                                            manufacturer,
                                            hwVersion,
                                            swVersion,
                                            serialNumber,
                                            chassis,
                                            defaultAvailable,
                                            annotations);
    }

    @Override
    public List<PortDescription> discoverPortDetails() {
        SparseAnnotations ingress = DefaultAnnotations.builder()
                .set("port-type", "INGRESS_PORT")
                .build();
        SparseAnnotations egress = DefaultAnnotations.builder()
                .set("port-type", "EGRESS_PORT")
                .build();
        return LongStream.rangeClosed(1, 96)
            .mapToObj(n -> omsPortDescription(PortNumber.portNumber(n),
                           true,
                           Spectrum.U_BAND_MIN,
                           Spectrum.O_BAND_MAX,
                           Frequency.ofGHz(100),
                           (n <= 48) ? ingress : egress)
                )
            .collect(Collectors.toList());
    }

}
