/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.ovsdb.rfc.exception;


/**
 * This exception is used when the a table or row is accessed though a typed
 * interface and the version requirements are not met.
 */
public class VersionMismatchException extends RuntimeException {
    private static final long serialVersionUID = -8439624321110133595L;

    /**
     * Constructs a VersionMismatchException object.
     * @param message error message
     */
    public VersionMismatchException(String message) {
        super(message);
    }

    /**
     * Constructs a VersionMismatchException object.
     * @param message error message
     * @param cause Throwable
     */
    public VersionMismatchException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Create error message.
     * @param actualVersion the actual version
     * @param fromVersion the initial version
     * @return message
     */
    public static String createFromMessage(String actualVersion, String fromVersion) {
        String message = "The fromVersion should less than the actualVersion.\n fromVersion: "
                + fromVersion + ".\n" + "actualVersion: " + actualVersion;
        return message;
    }

    /**
     * Create error message.
     * @param actualVersion the actual version
     * @param toVersion the end version
     * @return message
     */
    public static String createToMessage(String actualVersion, String toVersion) {
        String message = "The toVersion should greater than the actualVersion.\n"
                + "toVersion: " + toVersion + ".\n" + " actualVersion: " + actualVersion;
        return message;
    }
}
