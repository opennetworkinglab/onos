/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.store.service;

/**
 * Base exception type for database failures.
 */
@SuppressWarnings("serial")
public class DatabaseException extends RuntimeException {
    public DatabaseException(String message, Throwable t) {
        super(message, t);
    }

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(Throwable t) {
        super(t);
    }

    public DatabaseException() {
    };

    public static class Timeout extends DatabaseException {
        public Timeout(String message, Throwable t) {
            super(message, t);
        }

        public Timeout(String message) {
            super(message);
        }

        public Timeout(Throwable t) {
            super(t);
        }
    }
}
