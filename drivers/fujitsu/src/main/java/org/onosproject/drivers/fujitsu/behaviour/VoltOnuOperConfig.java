/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.drivers.fujitsu.behaviour;

import com.google.common.annotations.Beta;
import org.onosproject.net.driver.HandlerBehaviour;

/**
 * Device behaviour to perform actions in an ONU in vOLT.
 */
@Beta
public interface VoltOnuOperConfig extends HandlerBehaviour {

    /**
     * Reboot an ONU in the device.
     *
     * @param target input data in string
     * @return response string
     */
    String rebootOnu(String target);

    /**
     * Operate/release loopback on Ethernet port an ONU in the device.
     *
     * @param target input data in string
     * @return response string
     */
    String loopbackEthOnu(String target);

}
