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

package org.onosproject.pim.impl;

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.config.Config;

import java.util.Optional;

/**
 * Configuration for a PIM interface.
 */
public class PimInterfaceConfig extends Config<ConnectPoint> {

    private static final String INTERFACE_NAME = "interfaceName";
    private static final String ENABLED = "enabled";
    private static final String HELLO_INTERVAL = "helloInterval";
    private static final String HOLD_TIME = "holdTime";
    private static final String PRIORITY = "priority";
    private static final String PROPAGATION_DELAY = "propagationDelay";
    private static final String OVERRIDE_INTERVAL = "overrideInterval";

    /**
     * Gets the name of the interface. This links the PIM configuration with
     * an existing ONOS interface.
     *
     * @return interface name
     */
    public String getInterfaceName() {
        return node.path(INTERFACE_NAME).asText();
    }

    /**
     * Returns whether PIM is enabled on the interface or not.
     *
     * @return true if PIM is enabled, otherwise false
     */
    public boolean isEnabled() {
        return node.path(ENABLED).asBoolean(false);
    }

    /**
     * Gets the hello interval of the interface.
     *
     * @return hello interval
     */
    public Optional<Integer> getHelloInterval() {
        if (node.path(HELLO_INTERVAL).isMissingNode()) {
            return Optional.empty();
        }
        return Optional.of(Integer.parseInt(node.path(HELLO_INTERVAL).asText()));
    }

    /**
     * Gets the HELLO hold time of the interface.
     *
     * @return hold time
     */
    public Optional<Short> getHoldTime() {
        if (node.path(HOLD_TIME).isMissingNode()) {
            return Optional.empty();
        }
        return Optional.of(Short.parseShort(node.path(HOLD_TIME).asText()));
    }

    /**
     * Gets the priority of the interface.
     *
     * @return priority
     */
    public Optional<Integer> getPriority() {
        if (node.path(PRIORITY).isMissingNode()) {
            return Optional.empty();
        }
        return Optional.of(node.path(PRIORITY).asInt());
    }

    /**
     * Gets the propagation delay of the interface.
     *
     * @return propagation delay
     */
    public Optional<Short> getPropagationDelay() {
        if (node.path(PROPAGATION_DELAY).isMissingNode()) {
            return Optional.empty();
        }
        return Optional.of(Short.parseShort(node.path(PROPAGATION_DELAY).asText()));
    }

    /**
     * Gets the override interval of the interface.
     *
     * @return override interval
     */
    public Optional<Short> getOverrideInterval() {
        if (node.path(OVERRIDE_INTERVAL).isMissingNode()) {
            return Optional.empty();
        }
        return Optional.of(Short.parseShort(node.path(OVERRIDE_INTERVAL).asText()));
    }

    @Override
    public boolean isValid() {
        if (!hasOnlyFields(INTERFACE_NAME, ENABLED, HELLO_INTERVAL, HOLD_TIME,
                PRIORITY, PROPAGATION_DELAY, OVERRIDE_INTERVAL)) {
            return false;
        }

        return isString(INTERFACE_NAME, FieldPresence.MANDATORY) &&
                isBoolean(ENABLED, FieldPresence.MANDATORY) &&
                isIntegralNumber(HELLO_INTERVAL, FieldPresence.OPTIONAL) &&
                isIntegralNumber(HOLD_TIME, FieldPresence.OPTIONAL) &&
                isIntegralNumber(PRIORITY, FieldPresence.OPTIONAL) &&
                isIntegralNumber(PROPAGATION_DELAY, FieldPresence.OPTIONAL) &&
                isIntegralNumber(OVERRIDE_INTERVAL, FieldPresence.OPTIONAL);
    }
}
