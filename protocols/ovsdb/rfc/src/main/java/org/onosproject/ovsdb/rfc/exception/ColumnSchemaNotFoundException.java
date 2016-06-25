/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.ovsdb.rfc.exception;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * This exception is thrown when a ColumnSchema cannot be found.
 */
public class ColumnSchemaNotFoundException extends RuntimeException {
    private static final long serialVersionUID = -4325190659387339524L;

    /**
     * Constructs a ColumnSchemaNotFoundException object.
     * @param message error message
     */
    public ColumnSchemaNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a ColumnSchemaNotFoundException object.
     * @param message error message
     * @param cause Throwable
     */
    public ColumnSchemaNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Create error message.
     * @param columnName column name
     * @param tableName table name
     * @return message
     */
    public static String createMessage(String columnName, String tableName) {
        String message = toStringHelper("ColumnSchemaNotFoundException")
                .addValue("Could not find ColumnSchema for " + columnName
                                  + " in " + tableName).toString();
        return message;
    }
}
