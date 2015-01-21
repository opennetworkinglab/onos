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
package org.onosproject.igp.controller;

import org.onosproject.net.flowext.FlowRuleBatchExtRequest;


/**
 * Abstraction of an OpenFlow controller. Serves as a one stop
 * shop for obtaining OpenFlow devices and (un)register listeners
 * on OpenFlow events
 */
public interface IGPController {

    /**
     * Returns all switches known to this OF controller.
     * @return Iterable of dpid elements
     */
    public Iterable<IgpSwitch> getSwitches();

    /**
     * Returns the actual switch for the given Dpid.
     * @param dpid the switch to fetch
     * @return the interface to this switch
     */
    public IgpSwitch getSwitch(IgpDpid dpid);

    /**
     * Register a listener for meta events that occur to OF
     * devices.
     * @param listener the listener to notify
     */
    public void addListener(IgpSwitchListener listener);

    /**
     * Unregister a listener.
     *
     * @param listener the listener to unregister
     */
    public void removeListener(IgpSwitchListener listener);

	void write(IgpDpid dpid, FlowRuleBatchExtRequest msg);

}
