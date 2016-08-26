/*
 * Copyright 2016-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.protocol.restconf.server.api;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status;

/**
 * Exceptions raised during RESTCONF operations. This class extends
 * WebApplicationException. The design intention is to create a place holder
 * for RESTCONF specific errors and to be able to add more functions as the
 * subsystem grows.
 */
public class RestconfException extends WebApplicationException {

    // This is a randomly generated value. A WebApplicationException class is required to define it.
    private static final long SERIAL_VERSION_UID = 3275970397584007046L;

    /**
     * Constructs a new RESTCONF server error exception. The caller raising this
     * exception may pass in a HTTP error status code and an error message. The
     * error code will be displayed to the RESTCONF client as part of the
     * response from the RESTCONF server. The error message is a string which
     * may be saved in a log file and may be later retrieved by the
     * getMessage() method.
     *
     * @param message the detailed error message
     * @param status  HTTP error status
     * @throws IllegalArgumentException in case the status code is null or is not from
     *                                  javax.ws.rs.core.Response.Status.Family
     *                                  status code family
     */
    public RestconfException(String message, Status status) {
        super(message, null, Response.status(status).build());
    }

    /**
     * Constructs a new RESTCONF server error exception. The caller raising
     * this exception may pass in the numerical value of a HTTP error
     * status code, The error code will be displayed to the RESTCONF client
     * as a response from the RESTCONF server.
     *
     * @param status HTTP error status
     * @throws IllegalArgumentException in case the status code is not a valid
     *                                  HTTP status code or if it is not from the
     *                                  javax.ws.rs.core.Response.Status.Family
     *                                  status code family
     */
    public RestconfException(int status) {
        super((Throwable) null, Response.status(status).build());
    }
}
