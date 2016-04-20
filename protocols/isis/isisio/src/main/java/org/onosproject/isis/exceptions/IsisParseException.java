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
package org.onosproject.isis.exceptions;

import com.google.common.base.MoreObjects;

/**
 * Representation of a custom exception for ISIS.
 */
public class IsisParseException extends Exception {

    private static final long serialVersionUID = 1L;
    private byte errorCode;
    private byte errorSubCode;

    /**
     * Creates a new ISIS exception.
     */
    public IsisParseException() {
        super();
    }

    /**
     * Creates a new ISIS exception based on the given arguments.
     *
     * @param message the detail of exception in string
     * @param cause   underlying cause of the error
     */
    public IsisParseException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new ISIS exception for the given message.
     *
     * @param message the detail of exception in string
     */
    public IsisParseException(final String message) {
        super(message);
    }

    /**
     * Creates a new ISIS exception from throwable instance.
     *
     * @param cause underlying cause of the error
     */
    public IsisParseException(final Throwable cause) {
        super(cause);
    }

    /**
     * Creates a new ISIS exception from error code and error sub code.
     *
     * @param errorCode    error code of ISIS message
     * @param errorSubCode error sub code of ISIS message
     */
    public IsisParseException(final byte errorCode, final byte errorSubCode) {
        super();
        this.errorCode = errorCode;
        this.errorSubCode = errorSubCode;
    }

    /**
     * Returns error code for this exception.
     *
     * @return error code for this exception
     */
    public byte errorCode() {
        return this.errorCode;
    }

    /**
     * Returns error sub code for this exception.
     *
     * @return error sub code for this exception
     */
    public byte errorSubCode() {
        return this.errorSubCode;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("errorCode", errorCode)
                .add("errorSubCode", errorSubCode)
                .toString();
    }
}