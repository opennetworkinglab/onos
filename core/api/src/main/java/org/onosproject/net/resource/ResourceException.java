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
package org.onosproject.net.resource;

/**
 * Represents a resource related error.
 */
public class ResourceException extends RuntimeException {

    /**
     * Constructs an exception with no message and no underlying cause.
     */
    public ResourceException() {
    }

    /**
     * Constructs an exception with the specified message.
     *
     * @param message the message describing the specific nature of the error
     */
    public ResourceException(String message) {
        super(message);
    }

    /**
     * Constructs an exception with the specified message and the underlying cause.
     *
     * @param message the message describing the specific nature of the error
     * @param cause   the underlying cause of this error
     */
    public ResourceException(String message, Throwable cause) {
        super(message, cause);
    }

}
