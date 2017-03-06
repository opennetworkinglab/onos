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
package org.onosproject.net.optical.config;

import java.util.Optional;

import org.onosproject.net.config.Config;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Port;

import com.fasterxml.jackson.databind.JsonNode;

import static org.onosproject.net.config.Config.FieldPresence.OPTIONAL;


/**
 * Configurations for an optical port on a device.
 *
 * <p>
 * Example:
 * <pre>
 * "ports": {
 *   "(device Id)/(port number)": {
 *     {@value #CONFIG_KEY}: {
 *              {@value #TYPE}: "OCH",
 *              {@value #SPEED}: 0,
 *              {@value #PORT}: 10
 *              ...
 *     }
 *   }
 * }
 * </pre>
 */
public final class OpticalPortConfig extends Config<ConnectPoint> {

    /**
     * Configuration key for {@link OpticalPortConfig}.
     */
    public static final String CONFIG_KEY = "optical";

    // optical type {OMS, OCH, ODUClt, fiber}
    public static final String TYPE = "type";

    // port name. "name" is the alphanumeric name of the port, but "port" refers
    // to the port number used as a name string (i.e., for ports without
    // alphanumeric names).
    public static final String NAME = "name";
    public static final String PORT = "port";
    public static final String STATIC_PORT = "staticPort";
    public static final String STATIC_LAMBDA = "staticLambda";

    // **Linc-OE : remove if it's not needed after all.**
    public static final String SPEED = "speed";

    @Override
    public boolean isValid() {
        return hasOnlyFields(TYPE, NAME, PORT, STATIC_PORT, STATIC_LAMBDA, SPEED) &&
                isNumber(STATIC_LAMBDA, OPTIONAL) && isNumber(SPEED, OPTIONAL);
    }

    /**
     * Returns the Enum value representing the type of port.
     *
     * @return the port type, or null if invalid or unset
     */
    public Port.Type type() {
        JsonNode type = object.path(TYPE);
        if (type.isMissingNode()) {
            return null;
        }
        return Port.Type.valueOf(type.asText());
    }

    /**
     * Returns the port name associated with this port configuration. The Name
     * is an alphanumeric string.
     *
     * @return the name of this port, else, an empty string
     */
    public String name() {
        return getStringValue(NAME);
    }

    /**
     * Returns a stringified representation of the port number, configured in
     * some port types without an alphanumeric name as the port name.
     *
     * @return A string representation of the port number
     */
    public String numberName() {
        return getStringValue(PORT);
    }

    /**
     * Returns the string-representation of name of the output port. This is
     * usually an OMS port for an OCH input ports, or an OCH port for ODU input
     * ports.
     *
     * @return the name of this port, else, an empty string
     */
    public String staticPort() {
        return getStringValue(STATIC_PORT);
    }

    private String getStringValue(String field) {
        JsonNode name = object.path(field);
        return name.isMissingNode() ? "" : name.asText();
    }

    /**
     * Returns the output lambda configured for this port. The lambda value is
     * expressed as a frequency value. If the port type doesn't have a notion of
     * lambdas, this returns an empty Optional.
     *
     * @return an Optional that may contain a frequency value.
     */
    public Optional<Long> staticLambda() {
        JsonNode sl = object.path(STATIC_LAMBDA);
        if (sl.isMissingNode()) {
            return Optional.empty();
        }
        return Optional.of(sl.asLong());
    }

    /**
     * Returns the port speed configured for this port. If the port doesn't have
     * a notion of speed, this returns an empty Optional.
     *
     * @return a port speed value whose default is 0.
     */
    public Optional<Integer> speed() {
        JsonNode s = object.path(SPEED);
        if (s.isMissingNode()) {
            return Optional.empty();
        }
        return Optional.of(s.asInt());
    }

    /**
     * Sets the port type, or updates it if it's already set. A null argument removes
     * this field.
     *
     * @param type the port type
     * @return this OpticalPortConfig instance
     */
    public OpticalPortConfig portType(Port.Type type) {
        // if unspecified, ideally fall back on FIBER or PACKET.
        String pt = (type == null) ? null : type.toString();
        return (OpticalPortConfig) setOrClear(TYPE, pt);
    }

    /**
     * Sets the port name, or updates it if already set. A null argument removes
     * this field.
     *
     * @param name the port's name
     * @return this OpticalPortConfig instance
     */
    public OpticalPortConfig portName(String name) {
        return (OpticalPortConfig) setOrClear(NAME, name);
    }

    /**
     * Sets the port name from port number, or updates it if already set. A null
     * argument removes this field.
     *
     * @param name the port number, to be used as name
     * @return this OpticalPortConfig instance
     */
    public OpticalPortConfig portNumberName(Long name) {
        return (OpticalPortConfig) setOrClear(PORT, name);
    }

    /**
     * Sets the output port name, or updates it if already set. A null argument
     * removes this field.
     *
     * @param name the output port's name
     * @return this OpticalPortConfig instance
     */
    public OpticalPortConfig staticPort(String name) {
        return (OpticalPortConfig) setOrClear(STATIC_PORT, name);
    }

    /**
     * Sets the output lambda center frequency, or updates it if already set.
     * A null argument removes this field.
     *
     * @param index the output lambda
     * @return this OpticalPortConfig instance
     */
    public OpticalPortConfig staticLambda(Long index) {
        return (OpticalPortConfig) setOrClear(STATIC_LAMBDA, index);
    }

    /**
     * Sets the port speed, or updates it if already set. A null argument
     * removes this field.
     *
     * @param bw the port bandwidth
     * @return this OpticalPortConfig instance
     */
    public OpticalPortConfig speed(Integer bw) {
        return (OpticalPortConfig) setOrClear(SPEED, bw);
    }
}
