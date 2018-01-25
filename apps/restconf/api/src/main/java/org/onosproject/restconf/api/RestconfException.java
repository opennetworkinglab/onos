/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.restconf.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.Optional;

import static javax.ws.rs.core.Response.Status;

/**
 * Exceptions raised during RESTCONF operations. This class extends WebApplicationException.
 * To comply with the RESTCONF specification on error handling, the parameters given
 * when creating this exception will be used to create one
 * {@link org.onosproject.restconf.api.RestconfError}. Additional
 * {@link org.onosproject.restconf.api.RestconfError}s may be added subsequently.
 * This exception should be converted to a Json object using the toRestconfError()
 * method before being passed back to the caller in a response
 */
public class RestconfException extends WebApplicationException {

    // This is a randomly generated value. A WebApplicationException class is required to define it.
    private static final long SERIAL_VERSION_UID = 1275970654684007046L;

    private ArrayList<RestconfError> restconfErrors = new ArrayList<>();

    /**
     * Constructs a new RESTCONF server error exception based of an existing exception.
     * The caller raising this exception may pass in a HTTP error status code
     * and an error message. The error code will be displayed to the RESTCONF
     * client as part of the response from the RESTCONF server. The error message
     * is a string which may be saved in a log file and may be later retrieved by
     * the getMessage() method. The parameters given will be formed in to a
     * {@link org.onosproject.restconf.api.RestconfError}. Additional errors may be
     * added after construction.
     *
     * @param message the detailed error message
     * @param throwable The existing exception that caused this response.
     *                  The message from this exception will be used as error-info
     * @param errorTag A Restconf Error tag
     * @param status  HTTP error status. Developers are asked to ensure that the correct one for the error-tag is used
     * @param errorPath An optional path that gives the item that caused the exception
     * @throws IllegalArgumentException in case the status code is null or is not from
     *                                  javax.ws.rs.core.Response.Status.Family
     *                                  status code family
     */
    public RestconfException(String message, Throwable throwable,
                             RestconfError.ErrorTag errorTag, Status status,
                             Optional<String> errorPath) {
        super(message, throwable, Response.status(status).build());
        RestconfError.Builder bldr = RestconfError
                        .builder(RestconfError.ErrorType.APPLICATION, errorTag)
                        .errorMessage(message);
        if (throwable != null) {
            bldr.errorInfo(throwable.getMessage());
        }
        if (errorPath.isPresent()) {
            bldr.errorPath(errorPath.get());
        }
        addToErrors(bldr.build());
    }

    /**
     * Constructs a new RESTCONF server error exception.
     * The caller raising this exception may pass in a HTTP error status code
     * and an error message. The error code will be displayed to the RESTCONF
     * client as part of the response from the RESTCONF server. The error message
     * is a string which may be saved in a log file and may be later retrieved by
     * the getMessage() method. The parameters given will be formed in to a
     * {@link org.onosproject.restconf.api.RestconfError}. Additional errors may be
     * added after construction.
     *
     * @param message the detailed error message
     * @param errorTag A Restconf Error tag
     * @param status  HTTP error status. Developers are asked to ensure that the correct one for the error-tag is used
     * @param errorPath An optional path that gives the item that caused the exception
     * @param errorInfo An optional string with more info about the error
     * @throws IllegalArgumentException in case the status code is null or is not from
     *                                  javax.ws.rs.core.Response.Status.Family
     *                                  status code family
     */
    public RestconfException(String message, RestconfError.ErrorTag errorTag,
                             Status status, Optional<String> errorPath,
                             Optional<String> errorInfo) {
        super(message, null, Response.status(status).build());
        RestconfError.Builder bldr = RestconfError
                .builder(RestconfError.ErrorType.APPLICATION, errorTag)
                .errorMessage(message);
        if (errorInfo.isPresent()) {
            bldr.errorInfo(errorInfo.get());
        }
        if (errorPath.isPresent()) {
            bldr.errorPath(errorPath.get());
        }
        addToErrors(bldr.build());
    }

    /**
     * Allows additional RestconfErrors to be added to the exception.
     * @param restconfError An additional RestconfError to be added to the response
     */
    public void addToErrors(RestconfError restconfError) {
        restconfErrors.add(restconfError);
    }

    /**
     * Convert the RestconfException and all of its RestconfErrors in to a Json object.
     * @return A json node generated from the RestconfException
     */
    public ObjectNode toRestconfErrorJson() {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode errorArray = mapper.createArrayNode();
        restconfErrors.forEach(error -> errorArray.add(error.toJson()));
        ObjectNode errorsNode = (ObjectNode) mapper.createObjectNode();
        errorsNode.put("ietf-restconf:errors", errorArray);
        return errorsNode;
    }
}
