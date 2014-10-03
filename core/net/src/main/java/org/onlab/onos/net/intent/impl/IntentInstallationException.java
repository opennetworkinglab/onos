package org.onlab.onos.net.intent.impl;

import org.onlab.onos.net.intent.IntentException;

/**
 * An exception thrown when intent installation fails.
 */
public class IntentInstallationException extends IntentException {
    private static final long serialVersionUID = 3720268258616014168L;

    public IntentInstallationException() {
        super();
    }

    public IntentInstallationException(String message) {
        super(message);
    }

    public IntentInstallationException(String message, Throwable cause) {
        super(message, cause);
    }
}
