/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.tl1;

import com.google.common.annotations.Beta;
import io.netty.channel.Channel;
import org.onlab.packet.IpAddress;

@Beta
public interface Tl1Device {
    IpAddress ip();

    int port();

    /**
     * The username to log in to the switch.
     *
     * @return the user name
     */
    String username();

    /**
     * The password to log in to the switch.
     *
     * @return the password
     */
    String password();

    /**
     * The target identifier (TID) of the device.
     *
     * @return the tid
     */
    String tid();

    /**
     * Check if the switch is connected.
     *
     * @return true if connected, false otherwise
     */
    boolean isConnected();

    /**
     * Returns the netty channel of the switch.
     *
     * @return the netty channel, null if disconnected
     */
    Channel channel();

    /**
     * Connects the switch to the channel.
     * @param channel the channel
     */
    void connect(Channel channel);

    /**
     * Disconnects the switch from its channel.
     */
    void disconnect();
}
