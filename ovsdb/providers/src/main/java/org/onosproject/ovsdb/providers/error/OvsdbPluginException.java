package org.onosproject.ovsdb.providers.error;

/**
 * Ovsdb Plugin Excepiton.
 *
 *
 */
public class OvsdbPluginException extends RuntimeException {
    public OvsdbPluginException(String message) {
        super(message);
    }

    public OvsdbPluginException(String message, Throwable cause) {
        super(message, cause);
    }
}
