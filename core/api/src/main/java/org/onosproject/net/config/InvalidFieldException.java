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

package org.onosproject.net.config;

/**
 * Indicates a field of a configuration was invalid.
 */
public class InvalidFieldException extends RuntimeException {

    private final String field;
    private final String reason;

    /**
     * Creates a new invalid field exception about a given field.
     *
     * @param field field name
     * @param reason reason the field is invalid
     */
    public InvalidFieldException(String field, String reason) {
        super(message(field, reason));
        this.field = field;
        this.reason = reason;
    }

    /**
     * Creates a new invalid field exception about a given field.
     *
     * @param field field name
     * @param cause throwable that occurred while trying to validate field
     */
    public InvalidFieldException(String field, Throwable cause) {
        super(message(field, cause.getMessage()));
        this.field = field;
        this.reason = cause.getMessage();
    }

    /**
     * Returns the field name.
     *
     * @return field name
     */
    public String field() {
        return field;
    }

    /**
     * Returns the reason the field failed to validate.
     *
     * @return reason
     */
    public String reason() {
        return reason;
    }

    private static String message(String field, String reason) {
        return "Field \"" + field + "\" is invalid: " + reason;
    }

}
