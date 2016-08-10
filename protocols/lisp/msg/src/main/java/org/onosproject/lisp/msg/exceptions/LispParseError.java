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
package org.onosproject.lisp.msg.exceptions;

/**
 * LISP control message parse error.
 */
public class LispParseError extends Exception {

    /**
     * Constructor for LispParseError.
     */
    public LispParseError() {
        super();
    }

    /**
     * Constructor for LispParseError with message and cause parameters.
     *
     * @param message error message
     * @param cause   throwable cause
     */
    public LispParseError(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor for LispParseError with message parameter.
     *
     * @param message error message
     */
    public LispParseError(final String message) {
        super(message);
    }

    /**
     * Constructor for LispParseError with cause parameter.
     *
     * @param cause throwable cause
     */
    public LispParseError(final Throwable cause) {
        super(cause);
    }
}
