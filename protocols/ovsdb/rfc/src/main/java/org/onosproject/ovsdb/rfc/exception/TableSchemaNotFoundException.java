/*
 * Copyright 2015-present Open Networking Foundation
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
 * This exception is thrown when a TableSchema cannot be found.
 */
public class TableSchemaNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 8431894450061740838L;

    /**
     * Constructs a TableSchemaNotFoundException object.
     * @param message error message
     */
    public TableSchemaNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a TableSchemaNotFoundException object.
     * @param message error message
     * @param cause Throwable
     */
    public TableSchemaNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Create error message.
     * @param tableName table name
     * @param schemaName database name
     * @return message
     */
    public static String createMessage(String tableName, String schemaName) {
        String message = toStringHelper("TableSchemaNotFoundException")
                .addValue("Can not find TableSchema for " + tableName + " in "
                        + schemaName).toString();
        return message;
    }

}
