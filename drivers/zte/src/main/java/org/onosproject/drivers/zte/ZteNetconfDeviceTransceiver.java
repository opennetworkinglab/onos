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
package org.onosproject.drivers.zte;

import com.google.common.base.Strings;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.odtn.behaviour.ConfigurableTransceiver;
import org.onosproject.odtn.behaviour.PlainTransceiver;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.List;

import static org.onosproject.odtn.behaviour.OdtnDeviceDescriptionDiscovery.OC_NAME;
import static org.slf4j.LoggerFactory.getLogger;

public class ZteNetconfDeviceTransceiver
        extends PlainTransceiver
        implements ConfigurableTransceiver {

    private final Logger log = getLogger(getClass());
    private static final String ANOTATION_NAME = "xc:operation";

    @Override
    public List<CharSequence> enable(PortNumber client, PortNumber line, boolean enable) {
        DeviceId deviceId = handler().data().deviceId();
        log.info("Discovering ZTE device {}", deviceId);

        Port port = handler().get(DeviceService.class).getPort(deviceId, client);
        if (port == null) {
            log.warn("{} does not exist on {}", client, deviceId);
            return Collections.emptyList();
        }

        String component = port.annotations().value(OC_NAME);
        if (Strings.isNullOrEmpty(component)) {
            log.warn("{} annotation not found on {}@{}", OC_NAME, client, deviceId);
            return Collections.emptyList();
        }

        String componentName = component.replace("PORT", "TRANSCEIVER");
        return enable(componentName, enable);
    }


}
