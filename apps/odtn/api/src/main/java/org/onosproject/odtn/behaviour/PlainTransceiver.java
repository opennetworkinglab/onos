/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.odtn.behaviour;

import static org.onosproject.odtn.behaviour.OdtnDeviceDescriptionDiscovery.OC_NAME;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collections;
import java.util.List;

import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.odtn.behaviour.OdtnTerminalDeviceDriver.Operation;
import org.onosproject.odtn.utils.openconfig.OpenConfigComponentsHandler;
import org.onosproject.odtn.utils.openconfig.OpenConfigComponentHandler;
import org.onosproject.odtn.utils.openconfig.OpenConfigConfigOfComponentHandler;
import org.onosproject.odtn.utils.openconfig.OpenConfigConfigOfTransceiverHandler;
import org.onosproject.odtn.utils.openconfig.OpenConfigTransceiverHandler;
import org.slf4j.Logger;

import com.google.common.base.Strings;

/**
 * Plain OpenConfig based implementation.
 */
public class PlainTransceiver extends AbstractHandlerBehaviour
        implements ConfigurableTransceiver {

    private final Logger log = getLogger(getClass());

    private static final String ANOTATION_NAME   = "xc:operation";

    @Override
    public List<CharSequence> enable(PortNumber client, PortNumber line, boolean enable) {
        DeviceId did = this.data().deviceId();
        Port port = handler().get(DeviceService.class).getPort(did, client);
        if (port == null) {
            log.warn("{} does not exist on {}", client, did);
            return Collections.emptyList();
        }
        String component = port.annotations().value(OC_NAME);
        if (Strings.isNullOrEmpty(component)) {
            log.warn("{} annotation not found on {}@{}", OC_NAME, client, did);
            return Collections.emptyList();
        }
        return enable(component, enable);
    }

    @Override
    public List<CharSequence> enable(String componentName, boolean enable) {
        // create <components xmlns="http://openconfig.net/yang/platform"
        //                    xc:operation="merge/delete">
        //        </components>
        OpenConfigComponentsHandler components = new OpenConfigComponentsHandler();
        if (enable) {
            components.addAnnotation(ANOTATION_NAME, Operation.MERGE.value());
        } else {
            components.addAnnotation(ANOTATION_NAME, Operation.DELETE.value());
        }

        // add <component><name>"componentName"</name></component>
        OpenConfigComponentHandler component
            = new OpenConfigComponentHandler(componentName, components);

        // add <config><name>"componentName"</name></config>
        OpenConfigConfigOfComponentHandler configOfComponent
            = new OpenConfigConfigOfComponentHandler(component);
        configOfComponent.addName(componentName);

        // add <transceiver xmlns="http://openconfig.net/yang/platform/transceiver"></transceiver>
        OpenConfigTransceiverHandler transceiver = new OpenConfigTransceiverHandler(component);

        // add <config><enabled>true/false</enabled></config>
        OpenConfigConfigOfTransceiverHandler configOfTransceiver
            = new OpenConfigConfigOfTransceiverHandler(transceiver);
        configOfTransceiver.addEnabled(enable);

        return components.getListCharSequence();
    }
}
