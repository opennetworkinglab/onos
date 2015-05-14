package org.onosproject.openflow.controller;

import java.util.List;
import java.util.Set;

import org.projectfloodlight.openflow.protocol.OFObject;

/**
 * An interface implemented by OpenFlow devices that enables providers to
 * retrieve ports based on port property.
 */
public interface WithTypedPorts {

    /**
     * Return a list of interfaces (ports) of the type associated with this
     * OpenFlow switch.
     *
     * @param type The port description property type of requested ports
     * @return A potentially empty list of ports.
     */
    List<? extends OFObject> getPortsOf(PortDescPropertyType type);

    /**
     * Returns the port property types supported by the driver implementing this
     * interface.
     *
     * @return A set of port property types
     */
    Set<PortDescPropertyType> getPortTypes();
}
