/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.vtn.util;

import java.util.ArrayList;
import java.util.Collection;

import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

/**
 * VtnData utility class.
 */
public final class VtnData {

    private static final Logger log = LoggerFactory.getLogger(VtnData.class);
    private static final String SWITCH_CHANNEL_ID = "channelId";
    private static final String PORT_HEAD = "vxlan";

    /**
     * Constructs a VtnData object. Utility classes should not have a public or
     * default constructor, otherwise IDE will compile unsuccessfully. This
     * class should not be instantiated.
     */
    private VtnData() {
    }

    /**
     * Get the ControllerIp from the device .
     *
     * @param device Device
     * @return Controller Ip
     */
    public static String getControllerIpOfSwitch(Device device) {
        String url = device.annotations().value(SWITCH_CHANNEL_ID);
        return url.substring(0, url.lastIndexOf(":"));
    }

    /**
     * Get the ControllerId from the device .
     *
     * @param device Device
     * @param devices Devices
     * @return Controller Id
     */
    public static DeviceId getControllerId(Device device,
                                           Iterable<Device> devices) {
        for (Device d : devices) {
            if (d.type() == Device.Type.CONTROLLER && d.id().toString()
                    .contains(getControllerIpOfSwitch(device))) {
                return d.id();
            }
        }
        log.info("Can not find controller for device : {}", device.id());
        return null;
    }

    /**
     * Get local tunnel ports.
     *
     * @param ports Iterable of Port
     * @return Collection of PortNumber
     */
    public static Collection<PortNumber> getLocalTunnelPorts(Iterable<Port> ports) {
        Collection<PortNumber> localTunnelPorts = new ArrayList<>();
        Sets.newHashSet(ports).stream()
                .filter(p -> !p.number().equals(PortNumber.LOCAL))
                .forEach(p -> {
                    if (p.annotations().value(AnnotationKeys.PORT_NAME)
                            .startsWith(PORT_HEAD)) {
                        localTunnelPorts.add(p.number());
                    }
                });
        return localTunnelPorts;
    }

}
