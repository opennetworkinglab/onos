/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.igp.controller.driver;

import org.onosproject.igp.controller.IgpSwitch;

/**
 * Responsible for keeping track of the current set of switches
 * connected to the system. As well as whether they are in Master
 * role or not.
 *
 */
public interface IgpAgent {

    /**
     * Add a switch that has just connected to the system.
     * @param dpid the dpid to add
     * @param sw the actual switch object.
     * @return true if added, false otherwise.
     */
    public boolean addConnectedSwitch(int dpid, IgpSwitch sw);

    /**
     * Clear all state in controller switch maps for a switch that has
     * disconnected from the local controller. Also release control for
     * that switch from the global repository. Notify switch listeners.
     * @param dpid the dpid to remove.
     */
    public void removeConnectedSwitch(int dpid);

}
