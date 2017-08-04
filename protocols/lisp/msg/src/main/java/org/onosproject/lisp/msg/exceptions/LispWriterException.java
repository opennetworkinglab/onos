/*
 * Copyright 2016-present Open Networking Foundation
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
 * LISP address or message writer exception.
 */
public class LispWriterException extends Exception {

    /**
     * Constructor for LispWriterException.
     */
    public LispWriterException() {
        super();
    }

    /**
     * Constructor for LispWriterException with message and cause parameters.
     *
     * @param message exception message
     * @param cause   throwable cause
     */
    public LispWriterException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor for LispWriterException with message parameter.
     *
     * @param message exception message
     */
    public LispWriterException(final String message) {
        super(message);
    }

    /**
     * Constructor for LispWriterException with cause parameter.
     *
     * @param cause throwable cause
     */
    public LispWriterException(final Throwable cause) {
        super(cause);
    }
}
