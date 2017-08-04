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

/**
 * This exception is thrown when the caller invoke the unsupported method or
 * use the encoding is not supported.
 */
public class UnsupportedException extends RuntimeException {
    private static final long serialVersionUID = 1377011546616825375L;

    /**
     * Constructs a UnsupportedException object.
     * @param message error message
     */
    public UnsupportedException(String message) {
        super(message);
    }

    /**
     * Constructs a UnsupportedException object.
     * @param message error message
     * @param cause Throwable
     */
    public UnsupportedException(String message, Throwable cause) {
        super(message, cause);
    }
}
