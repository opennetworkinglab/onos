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
 * The JsonParsingException is thrown when JSON could not be successfully
 * parsed.
 */
public class JsonParsingException extends RuntimeException {
    private static final long serialVersionUID = 1424752181911923235L;

    /**
     * Constructs a JsonParsingException object.
     * @param message error message
     */
    public JsonParsingException(String message) {
        super(message);
    }

    /**
     * Constructs a JsonParsingException object.
     * @param message error message
     * @param cause Throwable
     */
    public JsonParsingException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a JsonParsingException object.
     * @param cause Throwable
     */
    public JsonParsingException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a JsonParsingException object.
     * @param message error message
     * @param cause Throwable
     * @param enableSuppression enable Suppression
     * @param writableStackTrace writable StackTrace
     */
    public JsonParsingException(String message, Throwable cause,
                                boolean enableSuppression,
                                boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
