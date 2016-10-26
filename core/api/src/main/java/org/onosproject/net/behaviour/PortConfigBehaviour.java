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
package org.onosproject.net.behaviour;

import com.google.common.annotations.Beta;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.driver.HandlerBehaviour;

/**
 * Means to configure a logical port at the device.
 */
@Beta
public interface PortConfigBehaviour extends HandlerBehaviour {

    /**
     * Apply QoS configuration on a device.
     * @param port a port description
     * @param qosDesc qos description
     */
    void applyQoS(PortDescription port, QosDescription qosDesc);

    /**
     * Remove a QoS configuration.
     * @param portNumber port identifier
     */
    void removeQoS(PortNumber portNumber);

}
