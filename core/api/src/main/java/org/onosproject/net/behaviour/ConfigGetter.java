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
 * Behaviour that gets the configuration of the specified type from the device.
 *
 * This is a temporary development tool for use until yang integration is complete.
 * This is not a properly specified behavior. DO NOT USE AS AN EXAMPLE.
 */
//FIXME this should eventually be removed.
public interface ConfigGetter extends HandlerBehaviour {

    /**
     * Returns the string representation of a device configuration, returns a
     * failure string if the configuration cannot be retrieved.
     * @param type the type of configuration to get (i.e. running).
     * @return string representation of the configuration or an error string.
     */
    String getConfiguration(String type);
}
