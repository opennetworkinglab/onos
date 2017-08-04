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

package org.onosproject.netconf;

import org.onosproject.event.AbstractEvent;

import com.google.common.base.MoreObjects;

import java.util.Optional;

/**
 * Describes a NETCONF device related event.
 */
public final class NetconfDeviceOutputEvent extends
        AbstractEvent<NetconfDeviceOutputEvent.Type, Object> {

    private final String messagePayload;
    private final Optional<Integer> messageID;
    private final NetconfDeviceInfo deviceInfo;

    /**
     * Type of device related events.
     */
    public enum Type {
        /**
         * Signifies that sent a reply to a request.
         */
        DEVICE_REPLY,

        /**
         * Signifies that the device sent a notification.
         */
        DEVICE_NOTIFICATION,

        /**
         * Signifies that the device is not reachable.
         */
        DEVICE_UNREGISTERED,

        /**
         * Signifies that the device has encountered an error.
         */
        DEVICE_ERROR,

        /**
         * Signifies that the device has closed the session.
         * ONOS will try to reopen it, if it fails again
         * it will mark the device as unreachable.
         */
        SESSION_CLOSED,

    }

    /**
     * Creates an event of a given type and for the specified subject and the
     * current time.
     *
     * @param type              event type
     * @param subject           event subject
     * @param payload           message from the device
     * @param msgID             id of the message related to the event
     * @param netconfDeviceInfo device of event
     */
    public NetconfDeviceOutputEvent(Type type, Object subject, String payload,
                                    Optional<Integer> msgID,
                                    NetconfDeviceInfo netconfDeviceInfo) {
        super(type, subject);
        messagePayload = payload;
        this.messageID = msgID;
        deviceInfo = netconfDeviceInfo;
    }

    /**
     * Creates an event of a given type and for the specified subject and time.
     *
     * @param type              event type
     * @param subject           event subject
     * @param payload           message from the device
     * @param msgID             id of the message related to the event
     * @param netconfDeviceInfo device of event
     * @param time              occurrence time
     */
    public NetconfDeviceOutputEvent(Type type, Object subject, String payload,
                                    Optional<Integer> msgID,
                                    NetconfDeviceInfo netconfDeviceInfo,
                                    long time) {
        super(type, subject, time);
        messagePayload = payload;
        deviceInfo = netconfDeviceInfo;
        this.messageID = msgID;
    }

    /**
     * return the message payload of the reply form the device.
     * @return reply
     */
    public String getMessagePayload() {
        return messagePayload;
    }

    /**
     * Event-related device information.
     * @return information about the device
     */
    public NetconfDeviceInfo getDeviceInfo() {
        return deviceInfo;
    }

    /**
     * Reply messageId.
     * @return messageId
     */
    public Optional<Integer> getMessageID() {
        return messageID;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("messageID", messageID)
                .add("deviceInfo", deviceInfo)
                .add("messagePayload", messagePayload)
                .toString();
    }
}
