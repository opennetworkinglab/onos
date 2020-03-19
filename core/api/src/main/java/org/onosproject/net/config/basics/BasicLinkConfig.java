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
package org.onosproject.net.config.basics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.onosproject.net.Link;
import org.onosproject.net.LinkKey;
import org.onosproject.net.config.inject.DeviceInjectionConfig;

import java.time.Duration;

import static org.onosproject.net.config.Config.FieldPresence.OPTIONAL;

/**
 * Basic configuration for network infrastructure link.
 */
public final class BasicLinkConfig extends AllowedEntityConfig<LinkKey> {

    /**
     * Configuration key for {@link DeviceInjectionConfig}.
     */
    public static final String CONFIG_KEY = "basic";


    public static final String TYPE = "type";
    public static final String METRIC = "metric";
    public static final String LATENCY = "latency";
    public static final String BANDWIDTH = "bandwidth";
    public static final String JITTER = "jitter";
    public static final String DELAY = "delay";
    public static final String LOSS = "loss";
    public static final String AVAILABILITY = "availability";
    public static final String TIER = "tier";
    public static final String METERED_USAGE = "meteredUsage";
    public static final String FLAPPING = "flapping";
    public static final String IS_DURABLE = "durable";
    public static final String IS_BIDIRECTIONAL = "bidirectional";
    public static final String IS_METERED = "metered";

    @Override
    public boolean isValid() {
        // Validate type/devices
        type();

        return hasOnlyFields(ALLOWED, TYPE, METRIC, LATENCY, BANDWIDTH, JITTER, DELAY, LOSS, AVAILABILITY, FLAPPING,
                IS_DURABLE, IS_BIDIRECTIONAL, IS_METERED, TIER, METERED_USAGE) &&
                isBoolean(ALLOWED, OPTIONAL) && isNumber(METRIC, OPTIONAL) &&
                isNumber(LATENCY, OPTIONAL) && isNumber(BANDWIDTH, OPTIONAL) && isDecimal(JITTER, OPTIONAL) &&
                isDecimal(DELAY, OPTIONAL) && isDecimal(LOSS, OPTIONAL) && isDecimal(AVAILABILITY, OPTIONAL) &&
                isDecimal(FLAPPING, OPTIONAL) &&
                isNumber(TIER, OPTIONAL) &&
                isDecimal(METERED_USAGE, OPTIONAL) &&
                isBoolean(IS_BIDIRECTIONAL, OPTIONAL) &&
                isBoolean(IS_METERED, OPTIONAL);
    }

    /**
     * Returns if the link type is configured.
     *
     * @return true if config contains link type
     */
    public boolean isTypeConfigured() {
        return hasField(TYPE);
    }

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
     * Returns link metric value for use by
     * {@link org.onosproject.net.topology.MetricLinkWeight} function.
     *
     * @return link metric; -1 if not set
     */
    public double metric() {
        return get(METRIC, -1);
    }

    /**
     * Sets the link metric for use by
     * {@link org.onosproject.net.topology.MetricLinkWeight} function.
     *
     * @param metric new metric; null to clear
     * @return self
     */
    public BasicLinkConfig metric(Double metric) {
        return (BasicLinkConfig) setOrClear(METRIC, metric);
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

    /**
     * Returns if link is bidirectional in the network model or not.
     *
     * @return true for bidirectional, false otherwise
     */
    public boolean isBidirectional() {
        JsonNode res = object.path(IS_BIDIRECTIONAL);
        if (res.isMissingNode()) {
            return true;
        }
        return res.asBoolean();
    }

    /**
     * Sets durability for this link.
     *
     * @param isBidirectional true for directional, false otherwise
     * @return this BasicLinkConfig
     */
    public BasicLinkConfig isBidirectional(Boolean isBidirectional) {
        return (BasicLinkConfig) setOrClear(IS_BIDIRECTIONAL, isBidirectional);
    }

    /**
     * Create a {@link BasicLinkConfig} for specified Device.
     * <p>
     * Note: created instance is not bound to NetworkConfigService,
     * cannot use {@link #apply()}. Must be passed to the service
     * using NetworkConfigService#applyConfig
     *
     * @param linkKey subject of this Config
     */
    public BasicLinkConfig(LinkKey linkKey) {
        ObjectMapper mapper = new ObjectMapper();
        init(linkKey, CONFIG_KEY, mapper.createObjectNode(), mapper, null);
    }

    /**
     * Returns link jitter in terms of seconds.
     *
     * @return link jitter valuer; -1 if not set
     */
    public double jitter() {
        return get(JITTER, -1.0);
    }

    /**
     * Sets the link jitter.
     *
     * @param jitter new jitter value; null to clear
     * @return self
     */
    public BasicLinkConfig jitter(Double jitter) {
        return (BasicLinkConfig) setOrClear(JITTER, jitter);
    }

    /**
     * Returns link delay in terms of seconds.
     *
     * @return link delay value; -1 if not set
     */
    public double delay() {
        return get(DELAY, -1.0);
    }

    /**
     * Sets the link delay.
     *
     * @param delay new delay value; null to clear
     * @return self
     */
    public BasicLinkConfig delay(Double delay) {
        return (BasicLinkConfig) setOrClear(DELAY, delay);
    }

    /**
     * Returns link loss in terms of Percentage.
     *
     * @return link loss value; -1 if not set
     */
    public double loss() {
        return get(LOSS, -1.0);
    }

    /**
     * Sets the link loss.
     *
     * @param loss new loss value; null to clear
     * @return self
     */
    public BasicLinkConfig loss(Double loss) {
        return (BasicLinkConfig) setOrClear(LOSS, loss);
    }

    /**
     * Returns link availability in terms of percentage.
     *
     * @return link availability value; -1 if not set
     */
    public double availability() {
        return get(AVAILABILITY, -1.0);
    }

    /**
     * Sets the link availability.
     *
     * @param availability new availability value; null to clear
     * @return self
     */
    public BasicLinkConfig availability(Double availability) {
        return (BasicLinkConfig) setOrClear(AVAILABILITY, availability);
    }

    /**
     * Returns link flapping in terms of percentage.
     *
     * @return link flapping value; -1 if not set
     */
    public double flapping() {
        return get(FLAPPING, -1.0);
    }

    /**
     * Sets the link flapping.
     *
     * @param flapping new flapping value; null to clear
     * @return self
     */
    public BasicLinkConfig flapping(Double flapping) {
        return (BasicLinkConfig) setOrClear(FLAPPING, flapping);
    }

    /**
     * Returns if link is metered in the network model or not.
     *
     * @return true for metered, false otherwise
     */
    public Boolean isMetered() {
        JsonNode res = object.path(IS_METERED);
        if (res.isMissingNode()) {
            return true;
        }
        return res.asBoolean();
    }

    /**
     * Sets metered flag for this link.
     *
     * @param isMetered true for metered, false otherwise
     * @return this BasicLinkConfig
     */
    public BasicLinkConfig isMetered(Boolean isMetered) {
        return (BasicLinkConfig) setOrClear(IS_METERED, isMetered);
    }

    /**
     * Returns link tier.
     *
     * @return link tier value; -1 if not set
     */
    public long tier() {
        return get(TIER, -1);
    }

    /**
     * Sets the link tier.
     *
     * @param tier new link tier value; null to clear
     * @return self
     */
    public BasicLinkConfig tier(Long tier) {
        return (BasicLinkConfig) setOrClear(TIER, tier);
    }

    /**
     * Returns metered link usage in terms of percentage.
     *
     * @return metered link usage value; -1 if not set
     */
    public double meteredUsage() {
        return get(METERED_USAGE, -1.0);
    }

    /**
     * Sets the metered link usage.
     *
     * @param meteredUsage new metered usage value; null to clear
     * @return self
     */
    public BasicLinkConfig meteredUsage(Double meteredUsage) {
        return (BasicLinkConfig) setOrClear(METERED_USAGE, meteredUsage);
    }

    /**
     * Create a {@link BasicLinkConfig} instance.
     * <p>
     * Note: created instance needs to be initialized by #init(..) before using.
     */
    public BasicLinkConfig() {
        super();
    }
}
