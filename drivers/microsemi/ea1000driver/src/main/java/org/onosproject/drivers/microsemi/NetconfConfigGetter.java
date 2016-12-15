/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.drivers.microsemi;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.ConfigGetter;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.netconf.NetconfController;
import org.slf4j.Logger;

import com.google.common.base.Preconditions;

/**
 * Used with the onos:device-configuration CLI command.
 *
 * This allows the full configuration to be retrieved from the device
 */
public class NetconfConfigGetter extends AbstractHandlerBehaviour implements ConfigGetter {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    private PacketProcessor testProcessor;

    // FIXME the error string should be universal for all implementations of
    // ConfigGetter
    public static final String UNABLE_TO_READ_CONFIG = "config retrieval error";

    @Override
    public String getConfiguration(String type) {
        DriverHandler handler = handler();
        NetconfController controller = handler.get(NetconfController.class);

        DeviceId ofDeviceId = handler.data().deviceId();
        Preconditions.checkNotNull(controller, "Netconf controller is null");
        if (type == null ||
                (!type.equalsIgnoreCase("running")
                        && !type.equalsIgnoreCase("candidate")
                        && !type.equalsIgnoreCase("startup"))) {
            log.error("Configuration type must be either 'running', 'startup' or 'candidate'. '{}' is invalid", type);
            return UNABLE_TO_READ_CONFIG;
        }
        try {
            return controller.getDevicesMap().get(ofDeviceId).getSession().getConfig(type.replace("cfgType=", ""));
        } catch (IOException e) {
            log.error("Configuration could not be retrieved {}", e.getMessage());
        }
        return UNABLE_TO_READ_CONFIG;
    }
}
