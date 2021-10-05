/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.snmp;

/**
 * Custom Exception for SNMP.
 */
public class SnmpException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Default constructor to create a new snmp exception.
     */
    public SnmpException() {
        super();
    }

    /**
     * Constructor to create exception from message and cause.
     *
     * @param text      the detail of exception in string
     * @param variables underlying cause of exception variables
     */
    public SnmpException(String text, String... variables) {
        super(format(text, variables));
    }

    private static String format(String text, String... variables) {
        return String.format(text, (Object[]) variables);
    }

    /**
     * Constructor to create exception from message and cause.
     *
     * @param cause     underlying cause of the error
     * @param text      the detail of exception in string
     * @param variables underlying cause of exception variables
     */
    public SnmpException(Throwable cause, String text, String... variables) {
        super(format(text, variables), cause);

    }
}