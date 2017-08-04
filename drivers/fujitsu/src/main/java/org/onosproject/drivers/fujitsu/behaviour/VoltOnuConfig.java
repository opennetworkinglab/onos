/*
 * Copyright 2016-present Open Networking Foundation
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
 * Device behaviour to obtain and set parameters of ONUs in vOLT.
 */
@Beta
public interface VoltOnuConfig extends HandlerBehaviour {

    /**
     * Obtain all ONUs or a specific ONU in the device.
     *
     * @param target input data in string
     * @return response string
     */
    String getOnus(String target);

    /**
     * Set a parameter value of an ONU in the device.
     *
     * @param target input data in string
     * @return response string
     */
    String setOnu(String target);

    /**
     * Obtain all or a specific ONU statistics in the device.
     *
     * @param target input data in string
     * @return response string
     */
    String getOnuStatistics(String target);

}
