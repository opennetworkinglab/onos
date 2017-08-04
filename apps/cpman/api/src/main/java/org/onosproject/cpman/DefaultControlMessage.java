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
package org.onosproject.cpman;

import org.onosproject.net.DeviceId;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Default control message implementation.
 */
public class DefaultControlMessage implements ControlMessage {

    private final Type type;
    private final DeviceId deviceId;
    private final long load;
    private final long rate;
    private final long count;
    private final long timestamp;

    /**
     * Generates a control message instance using given type and statistic
     * information.
     *
     * @param type control message type
     * @param deviceId device identification
     * @param load control message load
     * @param rate control message rate
     * @param count control message count
     * @param timestamp time stamp of the control message stats
     */
    public DefaultControlMessage(Type type, DeviceId deviceId, long load,
                                 long rate, long count, long timestamp) {
        this.type = type;
        this.deviceId = deviceId;
        this.load = load;
        this.rate = rate;
        this.count = count;
        this.timestamp = timestamp;
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public DeviceId deviceId() {
        return deviceId;
    }

    @Override
    public long load() {
        return load;
    }

    @Override
    public long rate() {
        return rate;
    }

    @Override
    public long count() {
        return count;
    }

    @Override
    public long timestamp() {
        return timestamp;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, deviceId.toString(), load, rate, count, timestamp);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultControlMessage) {
            final DefaultControlMessage other = (DefaultControlMessage) obj;
            return Objects.equals(this.type, other.type) &&
                    Objects.equals(this.deviceId, other.deviceId) &&
                    Objects.equals(this.load, other.load) &&
                    Objects.equals(this.rate, other.rate) &&
                    Objects.equals(this.count, other.count) &&
                    Objects.equals(this.timestamp, other.timestamp);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("type", type)
                .add("deviceId", deviceId.toString())
                .add("load", load)
                .add("rate", rate)
                .add("count", count)
                .add("timestamp", timestamp)
                .toString();
    }
}
