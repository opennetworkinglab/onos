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

/**
 * DeviceDescriptionDiscovery used in ODTN.
 *
 * Just declaring certain Annotations will be required.
 */
public interface OdtnDeviceDescriptionDiscovery
        extends DeviceDescriptionDiscovery {

    /**
     * Annotations key, which stores OpenConfig component name.
     */
    String OC_NAME = "oc-name";

    /**
     * Annotations key, which stores OpenConfig component type.
     */
    String OC_TYPE = "oc-type";

    // overriding just to make checkstyle happy
    @Override
    List<PortDescription> discoverPortDetails();

}
