/*
 * Copyright 2015-present Open Networking Laboratory
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
 * Top level exception for Store failures.
 */
@SuppressWarnings("serial")
public class StorageException extends RuntimeException {
    public StorageException() {
    }

    public StorageException(String message) {
        super(message);
    }

    public StorageException(Throwable t) {
        super(t);
    }

    /**
     * Store is temporarily unavailable.
     */
    public static class Unavailable extends StorageException {
    }

    /**
     * Store operation timeout.
     */
    public static class Timeout extends StorageException {
    }

    /**
     * Store update conflicts with an in flight transaction.
     */
    public static class ConcurrentModification extends StorageException {
    }

    /**
     * Store operation interrupted.
     */
    public static class Interrupted extends StorageException {
    }
}
