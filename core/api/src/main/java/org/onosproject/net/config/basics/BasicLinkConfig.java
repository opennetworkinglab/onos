/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.net.config.basics;

import org.onosproject.net.Link;
import org.onosproject.net.LinkKey;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Duration;

/**
 * Basic configuration for network infrastructure link.
 */
public class BasicLinkConfig extends AllowedEntityConfig<LinkKey> {

    public static final String TYPE = "type";
    public static final String LATENCY = "latency";
    public static final String BANDWIDTH = "bandwidth";
    public static final String IS_DURABLE = "durable";

    /**
     * Returns the link type.
     *
     * @return link type override
     */
    public Link.Type type() {
        return get(TYPE, Link.Type.DIRECT, Link.Type.class);
    }

    /**
     * Sets the link type.
     *
     * @param type link type override
     * @return self
     */
    public BasicLinkConfig type(Link.Type type) {
        return (BasicLinkConfig) setOrClear(TYPE, type);
    }

    /**
     * Returns link latency in terms of nanos.
     *
     * @return link latency; -1 if not set
     */
    public Duration latency() {
        return Duration.ofNanos(get(LATENCY, -1));
    }

    /**
     * Sets the link latency.
     *
     * @param latency new latency; null to clear
     * @return self
     */
    public BasicLinkConfig latency(Duration latency) {
        Long nanos = latency == null ? null : latency.toNanos();
        return (BasicLinkConfig) setOrClear(LATENCY, nanos);
    }

    /**
     * Returns link bandwidth in terms of Mbps.
     *
     * @return link bandwidth; -1 if not set
     */
    public long bandwidth() {
        return get(BANDWIDTH, -1);
    }

    /**
     * Sets the link bandwidth.
     *
     * @param bandwidth new bandwidth; null to clear
     * @return self
     */
    public BasicLinkConfig bandwidth(Long bandwidth) {
        return (BasicLinkConfig) setOrClear(BANDWIDTH, bandwidth);
    }

    /**
     * Returns if link is durable in the network model or not.
     *
     * @return true for durable, false otherwise
     */
    public Boolean isDurable() {
        JsonNode res = object.path(IS_DURABLE);
        if (res.isMissingNode()) {
            return null;
        }
        return res.asBoolean();
    }

    /**
     * Sets durability for this link.
     *
     * @param isDurable true for durable, false otherwise
     * @return this BasicLinkConfig
     */
    public BasicLinkConfig isDurable(Boolean isDurable) {
        return (BasicLinkConfig) setOrClear(IS_DURABLE, isDurable);
    }
}
