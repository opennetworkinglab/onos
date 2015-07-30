package org.onosproject.incubator.net.config.basics;

import java.util.Optional;

import org.onosproject.incubator.net.config.Config;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Port;

import com.fasterxml.jackson.databind.JsonNode;


/**
 * Configurations for an optical port on a device.
 */
public class OpticalPortConfig extends Config<ConnectPoint> {
    // optical type {OMS, OCH, ODUClt, fiber}
    public static final String TYPE = "type";

    // port name. "name" is the alphanumeric name of the port, but "port" refers
    // to the port number used as a name string (i.e., for ports without
    // alphanumeric names). this should be linked to ConnectPoint.
    public static final String NAME = "name";
    public static final String PORT = "port";

    /**
     * Returns the Enum value representing the type of port.
     *
     * @return the port type, or null if invalid or unset
     */
    public Port.Type type() {
        JsonNode type = node.path(TYPE);
        if (type.isMissingNode()) {
            return null;
        }
        return Port.Type.valueOf(type.asText());
    }

    /**
     * Returns the port name associated with this port configuration. The Name
     * may either be an alphanumeric string, or a string representation of the
     * port number, falling back on the latter if the former doesn't exist.
     *
     * @return the name of this port, else, an empty string
     */
    public String name() {
        String name = getStringValue(NAME);
        return name.isEmpty() ? getStringValue(PORT) : name;
    }

    /**
     * Returns the string-representation of name of the output port. This is
     * usually an OMS port for an OCH input ports, or an OCH port for ODU input
     * ports.
     *
     * @return the name of this port, else, an empty string
     */
    public String staticPort() {
        return getStringValue(AnnotationKeys.STATIC_PORT);
    }

    private String getStringValue(String field) {
        JsonNode name = node.path(field);
        return name.isMissingNode() ? "" : name.asText();
    }

    /**
     * Returns the output lambda configured for this port. The lambda value is
     * expressed as a frequency value.
     *
     * @return an Optional that may contain a frequency value.
     */
    public Optional<Long> staticLambda() {
        JsonNode sl = node.path(AnnotationKeys.STATIC_LAMBDA);
        if (sl.isMissingNode()) {
            return Optional.empty();
        }
        return Optional.of(sl.asLong());
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
        return (OpticalPortConfig) setOrClear(AnnotationKeys.STATIC_PORT, name);
    }

    /**
     * Sets the output lambda index, or updates it if already set. A null argument
     * removes this field.
     *
     * @param index the output lambda
     * @return this OpticalPortConfig instance
     */
    public OpticalPortConfig staticLambda(Long index) {
        return (OpticalPortConfig) setOrClear(AnnotationKeys.STATIC_LAMBDA, index);
    }

}
