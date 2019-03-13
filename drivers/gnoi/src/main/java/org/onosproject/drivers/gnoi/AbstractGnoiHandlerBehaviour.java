/*
 * Copyright 2019-present Open Networking Foundation
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

package org.onosproject.drivers.gnoi;

import com.google.common.base.Strings;
import org.onosproject.gnoi.api.GnoiClient;
import org.onosproject.gnoi.api.GnoiClientKey;
import org.onosproject.gnoi.api.GnoiController;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.BasicDeviceConfig;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Abstract implementation of a behaviour handler for a gNOI device.
 */
public class AbstractGnoiHandlerBehaviour extends AbstractHandlerBehaviour {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected DeviceId deviceId;
    protected DeviceService deviceService;
    protected Device device;
    protected GnoiController controller;
    protected GnoiClient client;

    protected boolean setupBehaviour() {
        deviceId = handler().data().deviceId();
        deviceService = handler().get(DeviceService.class);
        controller = handler().get(GnoiController.class);
        client = controller.getClient(deviceId);

        if (client == null) {
            log.warn("Unable to find client for {}, aborting operation", deviceId);
            return false;
        }

        return true;
    }

    GnoiClient getClientByKey() {
        final GnoiClientKey clientKey = clientKey();
        if (clientKey == null) {
            return null;
        }
        return handler().get(GnoiController.class).getClient(clientKey);
    }

    protected GnoiClientKey clientKey() {
        deviceId = handler().data().deviceId();

        final BasicDeviceConfig cfg = handler().get(NetworkConfigService.class)
                .getConfig(deviceId, BasicDeviceConfig.class);
        if (cfg == null || Strings.isNullOrEmpty(cfg.managementAddress())) {
            log.error("Missing or invalid config for {}, cannot derive " +
                    "gNOI server endpoints", deviceId);
            return null;
        }

        try {
            return new GnoiClientKey(
                    deviceId, new URI(cfg.managementAddress()));
        } catch (URISyntaxException e) {
            log.error("Management address of {} is not a valid URI: {}",
                    deviceId, cfg.managementAddress());
            return null;
        }
    }
}
