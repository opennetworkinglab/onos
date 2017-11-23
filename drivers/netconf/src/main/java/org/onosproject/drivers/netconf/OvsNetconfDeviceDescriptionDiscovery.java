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

package org.onosproject.drivers.netconf;

import com.google.common.collect.ImmutableList;
import org.onlab.packet.ChassisId;
import org.onosproject.net.Device;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.slf4j.Logger;

import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Discovers the device detail of the ovs based simulator used in NETCONF SB testing and development.
 */
public class OvsNetconfDeviceDescriptionDiscovery
        extends AbstractHandlerBehaviour implements DeviceDescriptionDiscovery {

    private final Logger log = getLogger(getClass());

    @Override
    public DeviceDescription discoverDeviceDetails() {
        log.debug("Discovering device details {}", handler().data().deviceId());
        return new DefaultDeviceDescription(handler().data().deviceId().uri(),
                Device.Type.VIRTUAL,
                "Of-Config",
                "VirtualBox",
                "1.0",
                "1",
                new ChassisId());
    }

    @Override
    public List<PortDescription> discoverPortDetails() {
        log.debug("Discovering device ports {}", handler().data().deviceId());
        return ImmutableList.of();
    }
}
