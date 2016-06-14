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
package org.onosproject.ospf.exceptions;

import com.google.common.base.MoreObjects;

/**
 * Representation of a custom exception for OSPF.
 */
public class OspfParseException extends Exception {

    private static final long serialVersionUID = 1L;
    private byte errorCode;
    private byte errorSubCode;

    /**
     * Creates a new OSPF exception.
     */
    public OspfParseException() {
        super();
    }

    /**
     * Creates a new OSPF exception based on the given arguments.
     *
     * @param message the detail of exception in string
     * @param cause   underlying cause of the error
     */
    public OspfParseException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new OSPF exception for the given message.
     *
     * @param message the detail of exception in string
     */
    public OspfParseException(final String message) {
        super(message);
    }

    /**
     * Creates a new OSPF exception from throwable instance.
     *
     * @param cause underlying cause of the error
     */
    public OspfParseException(final Throwable cause) {
        super(cause);
    }

    /**
     * Creates a new OSPF exception from error code and error sub code.
     *
     * @param errorCode    error code of OSPF message
     * @param errorSubCode error sub code of OSPF message
     */
    public OspfParseException(final byte errorCode, final byte errorSubCode) {
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