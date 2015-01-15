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

import java.util.List;

import org.onosproject.net.flowextend.FlowRuleExtendEntry;


/**
 * Represents to provider facing side of a switch.
 */
public interface IgpSwitch {

    /**
     * Writes the message to the driver.
     *
     * @param msg the message to write
     */
    public void sendMsg(FlowRuleExtendEntry msg);

    /**
     * Writes to the OFMessage list to the driver.
     *
     * @param msgs the messages to be written
     */
    public void sendMsg(List<FlowRuleExtendEntry> msgs);

    /**
     * Checks if the switch is still connected.
     *
     * @return whether the switch is still connected
     */
    public boolean isConnected();

    /**
     * Disconnects the switch by closing the TCP connection. Results in a call
     * to the channel handler's channelDisconnected method for cleanup
     */
    public void disconnectSwitch();
    
    public boolean connectSwitch();

}
