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
package org.onosproject.config;

import com.google.common.annotations.Beta;

/**
 * Exceptions for use by the {@code ConfigFilter}.
 */
@Beta
public class InvalidFilterException extends RuntimeException {

    /**
     * Constructs a new runtime exception with no error message.
     */
    public InvalidFilterException() {
        super();
    }

    /**
     * Constructs a new runtime exception with the given error message.
     *
     * @param message error message
     */
    public InvalidFilterException(String message) {
        super(message);
    }
}