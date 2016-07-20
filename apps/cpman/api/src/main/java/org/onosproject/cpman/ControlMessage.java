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
package org.onosproject.cpman;

import org.onosproject.net.DeviceId;

/**
 * Abstraction of control message.
 */
public interface ControlMessage {

    enum Type {
        INBOUND_PACKET, OUTBOUND_PACKET, FLOW_MOD_PACKET,
        FLOW_REMOVED_PACKET, REQUEST_PACKET, REPLY_PACKET
    }

    /**
     * Returns the control message type.
     *
     * @return control message type
     */
    Type type();

    /**
     * Returns the device identification.
     *
     * @return device identification
     */
    DeviceId deviceId();

    /**
     * Returns the latest control message load.
     *
     * @return control message load
     */
    long load();

    /**
     * Returns the latest control message rate.
     *
     * @return control message rate
     */
    long rate();

    /**
     * Returns the latest control message packet count.
     *
     * @return packet count
     */
    long count();

    /**
     * Returns the time that this control message stats collected.
     *
     * @return time stamp.
     */
    long timestamp();
}
