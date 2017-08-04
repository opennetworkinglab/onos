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
package org.onosproject.net.behaviour;

import org.onosproject.net.driver.HandlerBehaviour;

import java.util.List;

/**
 * Device behaviour to obtain, set and remove controllers at the device.
 */
public interface ControllerConfig extends HandlerBehaviour {

    //TODO: add other controller parameters as needed.

    /**
     * Obtain the list of controller which are currently configured.
     *
     * @return a list for controller descriptions
     */
    List<ControllerInfo> getControllers();

    /**
     * Set a list of controllers on a device.
     *
     * @param controllers a list of controller descriptions
     */
    void setControllers(List<ControllerInfo> controllers);

    /**
     * Remove a list of controllers on a device.
     *
     * @param controllers a list of controller descriptions
     */
    default void removeControllers(List<ControllerInfo> controllers) {
        List<ControllerInfo> controllersList = this.getControllers();
        controllersList.removeAll(controllers);
        this.setControllers(controllersList);
    }

}
