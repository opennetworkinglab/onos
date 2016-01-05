package org.onosproject.net.behaviour;

import org.onosproject.net.driver.HandlerBehaviour;

/**
 * Behaviour that gets the configuration of the specified type from the device.
 *
 * This is a temporary development tool for use until yang integration is complete.
 * This is not a properly specified behavior. DO NOT USE AS AN EXAMPLE.
 */
//FIXME this should eventually be removed.
public interface ConfigGetter extends HandlerBehaviour {

    /**
     * Returns the string representation of a device configuration, returns a
     * failure string if the configuration cannot be retreived.
     * @param type the type of configuration to get (i.e. running).
     * @return string representation of the configuration or an error string.
     */
    public String getConfiguration(String type);
}
