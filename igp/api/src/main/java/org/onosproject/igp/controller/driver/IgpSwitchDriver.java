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

import java.util.List;

import org.jboss.netty.channel.Channel;
import org.onosproject.igp.controller.IgpSwitch;
import org.onosproject.net.flowextend.FlowRuleExtendEntry;

/**
 * Represents the driver side of an OpenFlow switch.
 * This interface should never be exposed to consumers.
 *
 */
public interface IgpSwitchDriver extends IgpSwitch {

    /**
     * Sets the OpenFlow agent to be used. This method
     * can only be called once.
     * @param agent the agent to set.
     */
    public void setAgent(IgpAgent agent);

    /**
     * Remove this switch from the openflow agent.
     */
    public void removeConnectedSwitch();

    /**
     * Sets the associated Netty channel for this switch.
     * @param channel the Netty channel
     */
    public void setChannel(Channel channel);

    /**
     * Sets whether the switch is connected.
     *
     * @param connected whether the switch is connected
     */
    public void setConnected(boolean connected);

    /**
     * Writes the message to the output stream
     * in a driver specific manner.
     *
     * @param msg the message to write
     */
    public void write(FlowRuleExtendEntry  msg);

    /**
     * Writes to the OFMessage list to the output stream
     * in a driver specific manner.
     *
     * @param msgs the messages to be written
     */
    public void write(List<FlowRuleExtendEntry> msgs);
}

