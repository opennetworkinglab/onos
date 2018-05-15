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

import java.util.List;

import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.PortDescription;

import com.google.common.annotations.Beta;

/**
 * DeviceDescriptionDiscovery used in ODTN.
 *
 * Just declaring certain Annotations will be required.
 */
@Beta
public interface OdtnDeviceDescriptionDiscovery
        extends DeviceDescriptionDiscovery {

    /**
     * Annotations key intended for a Port, which stores OpenConfig component name.
     */
    String OC_NAME = "oc-name";

    /**
     * Annotations key intended for a Port, which stores OpenConfig component type.
     */
    String OC_TYPE = "oc-type";

    /**
     * Annotations key intended for a Port,
     * which stores string identifier used to
     * logically group Ports corresponding to a transponder, etc.
     */
    String CONNECTION_ID = "odtn-connection-id";


    /**
     * OpenConfig component property name to store,
     * decimal integer index to be used when creating PortNumber.
     */
    String ONOS_PORT_INDEX = "onos-index";

    // overriding just to make checkstyle happy
    @Override
    List<PortDescription> discoverPortDetails();

}
