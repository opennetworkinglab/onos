/*
 * Copyright 2018-present Open Networking Foundation
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
 *
 */

package org.onosproject.drivers.bmv2.api.runtime;

/**
 * General exception of the BMv2 runtime APIs.
 */
public final class Bmv2RuntimeException extends Exception {

    private final Code code;
    private String codeString;

    public Bmv2RuntimeException(String message) {
        super(message);
        this.code = Code.OTHER;
        this.codeString = message;
    }

    public Bmv2RuntimeException(Throwable cause) {
        super(cause);
        this.code = Code.OTHER;
        this.codeString = cause.toString();
    }

    public Bmv2RuntimeException(Code code) {
        super(code.name());
        this.code = code;
    }

    public Code getCode() {
        return this.code;
    }

    public String explain() {
        return (codeString == null) ? code.name() : code.name() + " " + codeString;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " " + explain();
    }

    /**
     * Code of BMv2 error.
     */
    public enum Code {
        /**
         * Indicates table is full.
         */
        TABLE_FULL,
        INVALID_MGID,
        /**
         * Indicates multicast group handle is invalid.
         */
        INVALID_MGRP_HANDLE,
        /**
         * Indicates l1 handle is not associated with a node.
         */
        INVALID_L1_HANDLE,
        /**
         * Indicates a general error.
         */
        MC_GENERAL_ERROR,
        /**
         * Indicates an unknown error.
         */
        MC_UNKNOWN_ERROR,
        OTHER
    }
}
