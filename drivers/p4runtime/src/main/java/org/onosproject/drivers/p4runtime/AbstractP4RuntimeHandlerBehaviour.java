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

import org.onosproject.grpc.utils.AbstractGrpcHandlerBehaviour;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.service.PiPipeconfService;
import org.onosproject.net.pi.service.PiTranslationService;
import org.onosproject.p4runtime.api.P4RuntimeClient;
import org.onosproject.p4runtime.api.P4RuntimeController;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.drivers.p4runtime.P4RuntimeDriverUtils.extractP4DeviceId;

/**
 * Abstract implementation of a behaviour handler for a P4Runtime device.
 */
public abstract class AbstractP4RuntimeHandlerBehaviour
        extends AbstractGrpcHandlerBehaviour<P4RuntimeClient, P4RuntimeController> {

    // Initialized by setupBehaviour()
    protected Long p4DeviceId;
    protected PiPipeconf pipeconf;
    PiTranslationService translationService;


    public AbstractP4RuntimeHandlerBehaviour() {
        super(P4RuntimeController.class);
    }

    /**
     * Initializes this behaviour attributes. Returns true if the operation was
     * successful, false otherwise.
     *
     * @param opName name of the operation
     * @return true if successful, false otherwise
     */
    protected boolean setupBehaviour(String opName) {
        if (!super.setupBehaviour(opName)) {
            return false;
        }

        p4DeviceId = extractP4DeviceId(mgmtUriFromNetcfg());
        if (p4DeviceId == null) {
            log.warn("Unable to obtain P4Runtime-internal device_id from " +
                             "config of {}, cannot perform {}",
                     deviceId, opName);
            return false;
        }

        final PiPipeconfService pipeconfService = handler().get(
                PiPipeconfService.class);
        pipeconf = pipeconfService.getPipeconf(deviceId).orElse(null);
        if (pipeconf == null) {
            log.warn("Missing pipeconf for {}, cannot perform {}", deviceId, opName);
            return false;
        }

        translationService = handler().get(PiTranslationService.class);

        return true;
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
