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

/**
 * This exception is thrown when a TableSchema cannot be found.
 */
public class TableSchemaNotFoundException extends RuntimeException {

    public TableSchemaNotFoundException(String message) {
        super(message);
    }

    public TableSchemaNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public static String createMessage(String tableName, String schemaName) {
        String message = "Unable to locate TableSchema for " + tableName
                + " in " + schemaName;
        return message;
    }

}
