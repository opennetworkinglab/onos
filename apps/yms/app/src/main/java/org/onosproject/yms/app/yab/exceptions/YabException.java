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

package org.onosproject.yms.app.yab.exceptions;

/**
 * Represents base class for exceptions in YDT operations.
 */
public class YabException extends RuntimeException {

    private static final long serialVersionUID = 20160211L;

    /**
     * Creates a new YDT exception.
     */
    public YabException() {
    }

    /**
     * Creates a new YDT exception with given message.
     *
     * @param message the detail of exception in string
     */
    public YabException(String message) {
        super(message);
    }

    /**
     * Creates a new YDT exception from given message and cause.
     *
     * @param message the detail of exception in string
     * @param cause   underlying cause of the error
     */
    public YabException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new YDT exception from cause.
     *
     * @param cause underlying cause of the error
     */
    public YabException(final Throwable cause) {
        super(cause);
    }
}
