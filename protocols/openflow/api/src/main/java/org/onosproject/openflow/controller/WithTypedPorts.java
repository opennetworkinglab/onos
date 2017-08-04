/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.openflow.controller;

import java.util.List;
import java.util.Set;

import org.projectfloodlight.openflow.protocol.OFObject;

/**
 * An interface implemented by OpenFlow devices that enables providers to
 * retrieve ports based on port property.
 */
public interface WithTypedPorts {

    /**
     * Return a list of interfaces (ports) of the type associated with this
     * OpenFlow switch.
     *
     * @param type The port description property type of requested ports
     * @return A potentially empty list of ports.
     */
    List<? extends OFObject> getPortsOf(PortDescPropertyType type);

    /**
     * Returns the port property types supported by the driver implementing this
     * interface.
     *
     * @return A set of port property types
     */
    Set<PortDescPropertyType> getPortTypes();
}
