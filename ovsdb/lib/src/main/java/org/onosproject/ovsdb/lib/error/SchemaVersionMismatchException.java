/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors : Dave Tucker
 */

package org.onosproject.ovsdb.lib.error;

import org.onosproject.ovsdb.lib.notation.Version;

/**
 * This exception is used when the a table or row is accessed though a typed
 * interface and the version requirements are not met.
 */
public class SchemaVersionMismatchException extends RuntimeException {

    public SchemaVersionMismatchException(String message) {
        super(message);
    }

    public SchemaVersionMismatchException(String message, Throwable cause) {
        super(message, cause);
    }

    public static String createMessage(Version currentVersion,
                                       Version requiredVersion) {
        String message = "The schema version used to access this Table/Column does not match the required version.\n"
                + "Current Version: " + currentVersion.toString() + "\n";

        if (currentVersion.compareTo(requiredVersion) > 1) {
            message += "Removed in Version: " + requiredVersion.toString();

        } else {
            message += "Added in Version: " + requiredVersion.toString();

        }

        return message;
    }
}
