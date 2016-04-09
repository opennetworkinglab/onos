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

package org.onosproject.net.behaviour;

import org.onosproject.net.driver.HandlerBehaviour;

/**
 * Behaviour that sets the configuration to a device.
 *
 */
//Temporary Developer tool, NOT TO BE USED in production or as example for
// future drivers/behaviors.
//FIXME this should eventually be removed.
public interface ConfigSetter extends HandlerBehaviour {

    /**
     * Sets the configuration contained in the file at the file path, returns
     * the response of the device.
     * @param filePath the path to the configuration file.
     * @return string response received from the device.
     */
    String setConfiguration(String filePath);
}
