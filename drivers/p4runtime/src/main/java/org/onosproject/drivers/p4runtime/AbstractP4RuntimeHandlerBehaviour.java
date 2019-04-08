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

package org.onosproject.drivers.p4runtime;

import com.google.common.base.Strings;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.BasicDeviceConfig;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.model.PiPipelineInterpreter;
import org.onosproject.net.pi.service.PiPipeconfService;
import org.onosproject.net.pi.service.PiTranslationService;
import org.onosproject.p4runtime.api.P4RuntimeClient;
import org.onosproject.p4runtime.api.P4RuntimeClientKey;
import org.onosproject.p4runtime.api.P4RuntimeController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Abstract implementation of a behaviour handler for a P4Runtime device.
 */
public class AbstractP4RuntimeHandlerBehaviour extends AbstractHandlerBehaviour {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    // Initialized by setupBehaviour()
    protected DeviceId deviceId;
    protected PiPipeconf pipeconf;
    protected P4RuntimeClient client;
    protected PiTranslationService translationService;

    /**
     * Initializes this behaviour attributes. Returns true if the operation was
     * successful, false otherwise.
     *
     * @param opName name of the operation
     * @return true if successful, false otherwise
     */
    protected boolean setupBehaviour(String opName) {
        deviceId = handler().data().deviceId();

        client = getClientByKey();
        if (client == null) {
            log.warn("Missing client for {}, aborting {}", deviceId, opName);
            return false;
        }

        PiPipeconfService piPipeconfService = handler().get(PiPipeconfService.class);
        if (!piPipeconfService.getPipeconf(deviceId).isPresent()) {
            log.warn("Missing pipeconf for {}, cannot perform {}", deviceId, opName);
            return false;
        }
        pipeconf = piPipeconfService.getPipeconf(deviceId).get();

        translationService = handler().get(PiTranslationService.class);

        return true;
    }

    /**
     * Returns an instance of the interpreter implementation for this device,
     * null if an interpreter cannot be retrieved.
     *
     * @return interpreter or null
     */
    PiPipelineInterpreter getInterpreter() {
        final Device device = handler().get(DeviceService.class).getDevice(deviceId);
        if (device == null) {
            log.warn("Unable to find device {}, cannot get interpreter", deviceId);
            return null;
        }
        if (!device.is(PiPipelineInterpreter.class)) {
            log.warn("Unable to get interpreter for {}, missing behaviour",
                     deviceId);
            return null;
        }
        return device.as(PiPipelineInterpreter.class);
    }

    /**
     * Returns a P4Runtime client previsouly created for this device, null if
     * such client does not exist.
     *
     * @return client or null
     */
    P4RuntimeClient getClientByKey() {
        final P4RuntimeClientKey clientKey = clientKey();
        if (clientKey == null) {
            return null;
        }
        return handler().get(P4RuntimeController.class).getClient(clientKey);
    }

    protected P4RuntimeClientKey clientKey() {
        deviceId = handler().data().deviceId();

        final BasicDeviceConfig cfg = handler().get(NetworkConfigService.class)
                .getConfig(deviceId, BasicDeviceConfig.class);
        if (cfg == null || Strings.isNullOrEmpty(cfg.managementAddress())) {
            log.error("Missing or invalid config for {}, cannot derive " +
                              "P4Runtime server endpoints", deviceId);
            return null;
        }

        try {
            return new P4RuntimeClientKey(
                    deviceId, new URI(cfg.managementAddress()));
        } catch (URISyntaxException e) {
            log.error("Management address of {} is not a valid URI: {}",
                      deviceId, cfg.managementAddress());
            return null;
        }
    }

    /**
     * Returns the value of the given driver property, if present, otherwise
     * returns the given default value.
     *
     * @param propName   property name
     * @param defaultVal default value
     * @return boolean
     */
    boolean driverBoolProperty(String propName, boolean defaultVal) {
        checkNotNull(propName);
        if (handler().driver().getProperty(propName) == null) {
            return defaultVal;
        } else {
            return Boolean.parseBoolean(handler().driver().getProperty(propName));
        }
    }
}
