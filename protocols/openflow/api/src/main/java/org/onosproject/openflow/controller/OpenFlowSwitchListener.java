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

import org.projectfloodlight.openflow.protocol.OFPortStatus;

/**
 * Allows for providers interested in Switch events to be notified.
 */
public interface OpenFlowSwitchListener {

    /**
     * Notify that the switch was added.
     * @param dpid the switch where the event occurred
     */
    void switchAdded(Dpid dpid);

    /**
     * Notify that the switch was removed.
     * @param dpid the switch where the event occurred.
     */
    void switchRemoved(Dpid dpid);

    /**
     * Notify that the switch has changed in some way.
     * @param dpid the switch that changed
     */
    void switchChanged(Dpid dpid);

    /**
     * Notify that a port has changed.
     * @param dpid the switch on which the change happened.
     * @param status the new state of the port.
     */
    void portChanged(Dpid dpid, OFPortStatus status);

    /**
     * Notify that a role imposed on a switch failed to take hold.
     *
     * @param dpid the switch that failed role assertion
     * @param requested the role controller requested
     * @param response role reply from the switch
     */
    void receivedRoleReply(Dpid dpid, RoleState requested, RoleState response);

    /**
     * Notify that role of the switch changed to Master.
     *
     * @param dpid the switch for which the role is changed
     */
    default void roleChangedToMaster(Dpid dpid) {}
}
