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
package org.onlab.osgi;

/**
 * Represents condition where some service is not found or not available.
 */
public class ServiceNotFoundException extends RuntimeException {

    /**
     * Creates a new exception with no message.
     */
    public ServiceNotFoundException() {
    }

    /**
     * Creates a new exception with the supplied message.
     * @param message error message
     */
    public ServiceNotFoundException(String message) {
        super(message);
    }

    /**
     * Creates a new exception with the supplied message and cause.
     * @param message error message
     * @param cause cause of the error
     */
    public ServiceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

}
