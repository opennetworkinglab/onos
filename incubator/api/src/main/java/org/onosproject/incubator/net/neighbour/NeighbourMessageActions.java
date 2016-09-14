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

package org.onosproject.incubator.net.neighbour;

import com.google.common.annotations.Beta;
import org.onlab.packet.MacAddress;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.net.ConnectPoint;

/**
 * Performs actions on a neighbour message contexts.
 */
@Beta
public interface NeighbourMessageActions {

    /**
     * Replies to an incoming request with the given MAC address.
     *
     * @param context incoming message context
     * @param targetMac target MAC address.
     */
    void reply(NeighbourMessageContext context, MacAddress targetMac);

    /**
     * Forwards the incoming message to the given connect point.
     *
     * @param context incoming message context
     * @param outPort port to send the message out
     */
    void forward(NeighbourMessageContext context, ConnectPoint outPort);

    /**
     * Forwards the incoming message to a given interface. The message will be
     * modified to fit the parameters of the outgoing interface.
     *
     * @param context incoming message context
     * @param outIntf interface to send the message out
     */
    void forward(NeighbourMessageContext context, Interface outIntf);

    /**
     * Floods the incoming message to all edge ports except the in port.
     *
     * @param context incoming message context
     */
    void flood(NeighbourMessageContext context);

    /**
     * Drops the incoming message.
     *
     * @param context incoming message context
     */
    void drop(NeighbourMessageContext context);
}
