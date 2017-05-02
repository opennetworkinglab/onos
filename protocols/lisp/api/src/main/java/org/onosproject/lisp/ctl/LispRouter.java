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
package org.onosproject.lisp.ctl;

import io.netty.channel.Channel;
import org.onlab.packet.IpAddress;
import org.onosproject.lisp.msg.protocols.LispEidRecord;
import org.onosproject.lisp.msg.protocols.LispMessage;
import org.onosproject.net.Device;

import java.util.List;

/**
 * Represents to provider facing side of a router.
 */
public interface LispRouter {

    /**
     * Writes the LISP control message to the channel.
     *
     * @param msg the message to be written
     */
    void sendMessage(LispMessage msg);

    /**
     * Handles the LISP control message from the channel.
     *
     * @param msg the message to be handled
     */
    void handleMessage(LispMessage msg);

    /**
     * Returns the router device type.
     *
     * @return device type
     */
    Device.Type deviceType();

    /**
     * Identifies the channel used to communicate with the router.
     *
     * @return string representation of the connection to the device
     */
    String channelId();

    /**
     * Sets the associated Netty channel for this router.
     *
     * @param channel the Netty channel
     */
    void setChannel(Channel channel);

    /**
     * Obtains a string version of the ID for this router.
     *
     * @return string representation of the device identifier
     */
    String stringId();

    /**
     * Obtains an IpAddress version of the ID for this router.
     *
     * @return raw IP address of the device identifier
     */
    IpAddress routerId();

    /**
     * Checks if the router is connected.
     *
     * @return whether the router is connected
     */
    boolean isConnected();

    /**
     * Sets whether the router is connected.
     *
     * @param connected whether the router is connected
     */
    void setConnected(boolean connected);

    /**
     * Checks if the router is subscribed.
     * As long as a router sends Map-Request message,
     * we treat the router is subscribed.
     *
     * @return whether the router is subscribed
     */
    boolean isSubscribed();

    /**
     * Sets whether the router is subscribed.
     *
     * @param subscribed whether the router is subscribed
     */
    void setSubscribed(boolean subscribed);

    /**
     * Obtains a collection of EID records that associated with this router.
     *
     * @return a collection EID records that associated with this router
     */
    List<LispEidRecord> getEidRecords();

    /**
     * Associates the EID records to this router.
     *
     * @param records a collection of EID records
     */
    void setEidRecords(List<LispEidRecord> records);

    /**
     * Sets the LISP agent to be used. This method can only be invoked once.
     *
     * @param agent the agent to set
     */
    void setAgent(LispRouterAgent agent);

    /**
     * Announces to the LISP agent that this router has connected.
     *
     * @return true if successful, false if duplicate router
     */
    boolean connectRouter();

    /**
     * Disconnects the router by closing UDP connection.
     * Results in a call to the channel handler's close method for cleanup.
     */
    void disconnectRouter();
}
