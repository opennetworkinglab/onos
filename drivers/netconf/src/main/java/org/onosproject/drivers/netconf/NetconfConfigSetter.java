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

package org.onosproject.drivers.netconf;

import org.onosproject.net.behaviour.ConfigSetter;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import com.google.common.base.Preconditions;
import org.onosproject.net.DeviceId;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.netconf.NetconfController;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Sets the configuration included in the specified file to the specified
 * device. It returns the response of the device.
 * Temporary Developer tool, NOT TO BE USED in production or as example for
 * future drivers/behaviors.
 */
//FIXME this should eventually be removed.

public class NetconfConfigSetter extends AbstractHandlerBehaviour
        implements ConfigSetter {

    private final Logger log = getLogger(getClass());

    private static final String UNABLE_TO_READ_FILE =
            "Configuration cannot be retrieved from file";
    private static final String UNABLE_TO_SET_CONFIG =
            "Configuration cannot be set";

    @Override
    public String setConfiguration(String filePath) {
        DriverHandler handler = handler();
        NetconfController controller = handler.get(NetconfController.class);
        DeviceId deviceId = handler.data().deviceId();
        Preconditions.checkNotNull(controller, "Netconf controller is null");

        String request;
        try {
            request = new String(Files.readAllBytes(Paths.get(filePath)));
        } catch (IOException e) {
            log.error("Cannot read configuration file", e);
            return UNABLE_TO_READ_FILE;
        }

        try {
            return controller.getDevicesMap()
                    .get(deviceId)
                    .getSession()
                    .requestSync(request);
        } catch (IOException e) {
            log.error("Configuration could not be set", e);
        }
        return UNABLE_TO_SET_CONFIG;
    }

}
