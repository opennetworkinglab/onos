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
package org.onosproject.openflow.controller;

import org.projectfloodlight.openflow.protocol.OFMessage;

import java.util.List;

/**
 * Notifies providers about all OpenFlow messages.
 */
public interface OpenFlowMessageListener {

    /**
     * Handles all incoming OpenFlow messages.
     *
     * @param dpid the switch where the message generated
     * @param msg raw OpenFlow message
     */
    void handleIncomingMessage(Dpid dpid, OFMessage msg);

    /**
     * Handles all outgoing OpenFlow messages.
     *
     * @param dpid the switch where the message to be sent
     * @param msgs a collection of raw OpenFlow message
     */
    void handleOutgoingMessage(Dpid dpid, List<OFMessage> msgs);
}
