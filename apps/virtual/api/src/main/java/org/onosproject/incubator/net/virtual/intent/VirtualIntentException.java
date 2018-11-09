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

package org.onosproject.incubator.net.virtual.intent;

public class VirtualIntentException extends RuntimeException {

    //FIXME: how to obatin UID?
    private static final long serialVersionUID = 1907263634145241319L;

    /**
     * Constructs an exception with no message and no underlying cause.
     */
    public VirtualIntentException() {
    }

    /**
     * Constructs an exception with the specified message.
     *
     * @param message the message describing the specific nature of the error
     */
    public VirtualIntentException(String message) {
        super(message);
    }

    /**
     * Constructs an exception with the specified message and the underlying cause.
     *
     * @param message the message describing the specific nature of the error
     * @param cause   the underlying cause of this error
     */
    public VirtualIntentException(String message, Throwable cause) {
        super(message, cause);
    }
}
