/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.core;

/**
 * Represents that there is no available IDs.
 */
public class UnavailableIdException extends RuntimeException {

    private static final long serialVersionUID = -2287403908433720122L;

    /**
     * Constructs an exception with no message and no underlying cause.
     */
    public UnavailableIdException() {
    }

    /**
     * Constructs an exception with the specified message.
     *
     * @param message the message describing the specific nature of the error
     */
    public UnavailableIdException(String message) {
        super(message);
    }

    /**
     * Constructs an exception with the specified message and the underlying cause.
     *
     * @param message the message describing the specific nature of the error
     * @param cause the underlying cause of this error
     */
    public UnavailableIdException(String message, Throwable cause) {
        super(message, cause);
    }
}
