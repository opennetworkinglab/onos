/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.net.device;

import org.onlab.util.Tools;
import org.onosproject.event.AbstractEvent;
import org.onosproject.net.DeviceId;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Describes event related to a channel established by ONOS with a device.
 */
public class ChannelEvent extends AbstractEvent<ChannelEvent.Type, DeviceId> {

    private final Throwable throwable;

    /**
     * Type of device events.
     */
    public enum Type {
        /**
         * Signifies that the channel has properly connected.
         */
        CHANNEL_CONNECTED,

        /**
         * Signifies that the channel has disconnected.
         */
        CHANNEL_DISCONNECTED,

        /**
         * Signifies that an error happened on the channel with the given device.
         */
        CHANNEL_ERROR

    }

    /**
     * Creates an event of a given type and for the specified device.
     *
     * @param type     device event type
     * @param deviceId event device subject
     */
    public ChannelEvent(Type type, DeviceId deviceId) {
        this(type, deviceId, null);
    }

    /**
     * Creates an event of a given type and for the specified device, given a certain throwable.
     *
     * @param type      device event type
     * @param deviceId  event device subject
     * @param throwable exception happened on the channel
     */
    public ChannelEvent(Type type, DeviceId deviceId, Throwable throwable) {
        super(type, deviceId);
        this.throwable = throwable;
    }

    /**
     * Creates an event of a given type and for the specified device and the current time.
     *
     * @param type      device event type
     * @param deviceId  event device subject
     * @param throwable exception happened on the channel
     * @param time      occurrence time
     */
    public ChannelEvent(Type type, DeviceId deviceId, Throwable throwable, long time) {
        super(type, deviceId, time);
        this.throwable = throwable;
    }

    /**
     * Returns the exception that happened on the channel.
     *
     * @return a throwable if associated to the event, otherwise null.
     */
    public Throwable throwable() {
        return throwable;
    }

    @Override
    public String toString() {
        if (throwable == null) {
            return super.toString();
        }
        return toStringHelper(this)
                .add("time", Tools.defaultOffsetDataTime(time()))
                .add("type", type())
                .add("subject", subject())
                .add("throwable", throwable)
                .toString();
    }
}
