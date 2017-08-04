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
package org.onosproject.net.behaviour.protection;



/**
 * Base class for Protection related Exceptions.
 */
public abstract class ProtectionException extends Exception {

    private static final long serialVersionUID = 5230741971525527020L;

    /**
     * Exception thrown when specified configuration was invalid.
     */
    public static class InvalidConfigException extends ProtectionException {

        private static final long serialVersionUID = 6532157856911418461L;

        /**
         * {@link InvalidConfigException}.
         */
        public InvalidConfigException() {
        }

        /**
         * {@link InvalidConfigException}.
         *
         * @param message describing error
         */
        public InvalidConfigException(String message) {
            super(message);
        }

        /**
         * {@link InvalidConfigException}.
         *
         * @param cause of this Exception
         */
        public InvalidConfigException(Throwable cause) {
            super(cause);
        }

        /**
         * {@link InvalidConfigException}.
         *
         * @param message describing error
         * @param cause of this Exception
         */
        public InvalidConfigException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Default constructor.
     */
    protected ProtectionException() {}

    /**
     * Creates an exception.
     * @param message describing error
     */
    protected ProtectionException(String message) {
        super(message);
    }

    /**
     * Creates an exception.
     * @param cause of this Exception
     */
    protected ProtectionException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates an exception.
     * @param message describing error
     * @param cause of this Exception
     */
    protected ProtectionException(String message, Throwable cause) {
        super(message, cause);
    }

}
