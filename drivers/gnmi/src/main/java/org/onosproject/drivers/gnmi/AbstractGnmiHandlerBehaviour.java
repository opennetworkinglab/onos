/*
 * Copyright 2018-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.drivers.gnmi;

import org.onosproject.gnmi.api.GnmiClient;
import org.onosproject.gnmi.api.GnmiClientKey;
import org.onosproject.gnmi.api.GnmiController;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract implementation of a behaviour handler for a gNMI device.
 */
public class AbstractGnmiHandlerBehaviour extends AbstractHandlerBehaviour {

    public static final String GNMI_SERVER_ADDR_KEY = "gnmi_ip";
    public static final String GNMI_SERVER_PORT_KEY = "gnmi_port";
    private static final String GNMI_SERVICE_NAME = "gnmi";

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected DeviceId deviceId;
    protected DeviceService deviceService;
    protected Device device;
    protected GnmiController controller;
    protected GnmiClient client;

    protected boolean setupBehaviour() {
        // FIXME: Should create GnmiHandshaker which initialize the client
        // instead of create client here.
        deviceId = handler().data().deviceId();

        controller = handler().get(GnmiController.class);
        client = controller.getClient(deviceId);

        if (client == null) {
            client = createClient();
        }

        if (client == null) {
            log.warn("Can not create client for {} (see log above)", deviceId);
            return false;
        }

        return true;
    }

    protected GnmiClient createClient() {
        deviceId = handler().data().deviceId();
        controller = handler().get(GnmiController.class);

        final String serverAddr = this.data().value(GNMI_SERVER_ADDR_KEY);
        final String serverPortString = this.data().value(GNMI_SERVER_PORT_KEY);

        if (serverAddr == null || serverPortString == null) {
            log.warn("Unable to create client for {}, missing driver data key (required is {}, {}, and {})",
                    deviceId, GNMI_SERVER_ADDR_KEY, GNMI_SERVER_PORT_KEY);
            return null;
        }

        final int serverPort;
        try {
            serverPort = Integer.parseUnsignedInt(serverPortString);
        } catch (NumberFormatException e) {
            log.error("{} is not a valid gNMI port number", serverPortString);
            return null;
        }
        GnmiClientKey clientKey =
                new GnmiClientKey(GNMI_SERVICE_NAME, deviceId, serverAddr, serverPort);
        if (!controller.createClient(clientKey)) {
            log.warn("Unable to create client for {}, aborting operation", deviceId);
            return null;
        }
        return controller.getClient(deviceId);
    }
}
