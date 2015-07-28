/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.ovsdb.rfc.error;

/**
 * This exception is thrown when a result does not meet any of the known formats
 * in RFC7047.
 */
public class UnknownResultException extends RuntimeException {
    private static final long serialVersionUID = 1377011546616825375L;

    /**
     * Constructs a UnknownResultException object.
     * @param message error message
     */
    public UnknownResultException(String message) {
        super(message);
    }

    /**
     * Constructs a UnknownResultException object.
     * @param message error message
     * @param cause Throwable
     */
    public UnknownResultException(String message, Throwable cause) {
        super(message, cause);
    }
}
