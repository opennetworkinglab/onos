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

import org.projectfloodlight.openflow.protocol.OFMessage;

import java.util.concurrent.CompletableFuture;

/**
 * Abstraction of an OpenFlow controller. Serves as a one stop
 * shop for obtaining OpenFlow devices and (un)register listeners
 * on OpenFlow events
 */
public interface OpenFlowController {

    /**
     * Returns all switches known to this OF controller.
     * @return Iterable of dpid elements
     */
    Iterable<OpenFlowSwitch> getSwitches();

    /**
     * Returns all master switches known to this OF controller.
     * @return Iterable of dpid elements
     */
    Iterable<OpenFlowSwitch> getMasterSwitches();

    /**
     * Returns all equal switches known to this OF controller.
     * @return Iterable of dpid elements
     */
    Iterable<OpenFlowSwitch> getEqualSwitches();


    /**
     * Returns the actual switch for the given Dpid.
     * @param dpid the switch to fetch
     * @return the interface to this switch
     */
    OpenFlowSwitch getSwitch(Dpid dpid);

    /**
     * Returns the actual master switch for the given Dpid, if one exists.
     * @param dpid the switch to fetch
     * @return the interface to this switch
     */
    OpenFlowSwitch getMasterSwitch(Dpid dpid);

    /**
     * Returns the actual equal switch for the given Dpid, if one exists.
     * @param dpid the switch to fetch
     * @return the interface to this switch
     */
    OpenFlowSwitch getEqualSwitch(Dpid dpid);

    /**
     * Register a listener for meta events that occur to OF
     * devices.
     * @param listener the listener to notify
     */
    void addListener(OpenFlowSwitchListener listener);

    /**
     * Unregister a listener.
     *
     * @param listener the listener to unregister
     */
    void removeListener(OpenFlowSwitchListener listener);

    /**
     * Register a listener for all OF msg types.
     *
     * @param listener the listener to notify
     */
    void addMessageListener(OpenFlowMessageListener listener);

    /**
     * Unregister a listener for all OF msg types.
     *
     * @param listener the listener to notify
     */
    void removeMessageListener(OpenFlowMessageListener listener);

    /**
     * Register a listener for packet events.
     * @param priority the importance of this listener, lower values are more important
     * @param listener the listener to notify
     */
    void addPacketListener(int priority, PacketListener listener);

    /**
     * Unregister a listener.
     *
     * @param listener the listener to unregister
     */
    void removePacketListener(PacketListener listener);

    /**
     * Register a listener for OF msg events.
     *
     * @param listener the listener to notify
     */
    void addEventListener(OpenFlowEventListener listener);

    /**
     * Unregister a listener.
     *
     * @param listener the listener to unregister
     */
    void removeEventListener(OpenFlowEventListener listener);

    /**
     * Send a message to a particular switch.
     * @param dpid the switch to send to.
     * @param msg the message to send
     */
    void write(Dpid dpid, OFMessage msg);

    /**
     * Send a message to a particular switch and return the future response.
     *
     * @param dpid the switch to send to
     * @param msg the message to send
     * @return future for response message
     */
    CompletableFuture<OFMessage> writeResponse(Dpid dpid, OFMessage msg);

    /**
     * Process a message and notify the appropriate listeners.
     *
     * @param dpid the dpid the message arrived on
     * @param msg the message to process.
     */
    void processPacket(Dpid dpid, OFMessage msg);

    /**
     * Sets the role for a given switch.
     * @param role the desired role
     * @param dpid the switch to set the role for.
     */
    void setRole(Dpid dpid, RoleState role);

    /**
     * Remove OpenFlow classifier listener from runtime store of classifiers listener.
     *
     * @param listener the OpenFlow classifier to remove
     */
    void removeClassifierListener(OpenFlowClassifierListener listener);

    /**
     * Add OpenFlow classifier listener to runtime store of classifiers listener.
     *
     * @param listener the OpenFlow classifier listener
     */
    void addClassifierListener(OpenFlowClassifierListener listener);
}
