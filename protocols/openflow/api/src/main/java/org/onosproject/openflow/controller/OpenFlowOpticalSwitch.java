/*
 * Copyright 2015-present Open Networking Laboratory
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

import java.util.Collections;
import java.util.List;

import org.onosproject.net.device.PortDescription;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPortDesc;

import com.google.common.annotations.Beta;

/**
 * A marker interface for optical switches, which require the ability to pass
 * port information to a Device provider.
 */
public interface OpenFlowOpticalSwitch extends OpenFlowSwitch, WithTypedPorts {

    // OpenFlowOpticalSwitch only returns Ethernet ports.
    // This is a limitation due to issue described in ONOS-3796.
    // This method should return all port type once the limitation is fixed.
    /**
     * Returns a list of standard (Ethernet) ports.
     *
     * @return List of standard (Ethernet) ports
     */
    @Beta
    @Override
    abstract List<OFPortDesc> getPorts();

    /**
     * Returns updated PortDescriptions built from experimenter message
     * received from device.
     *
     * @param msg OpenFlow message from device.
     * @return List of updated PortDescriptions.
     */
    default List<PortDescription> processExpPortStats(OFMessage msg) {
        return Collections.emptyList();
    }
}
