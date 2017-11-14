/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.p4runtime.model;

/**
 * Signals an error occurred while parsing a P4Info object.
 */
public final class P4InfoParserException extends Exception {

    /**
     * Creates a new exception for the given message.
     *
     * @param message explanation
     */
    P4InfoParserException(String message) {
        super(message);
    }

    /**
     * Creates a new exception for the given message and cause.
     *
     * @param message message
     * @param cause cause
     */
    P4InfoParserException(String message, Throwable cause) {
        super(message, cause);
    }
}
