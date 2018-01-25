/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.restconf.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;

/**
 * Restconf error structured as in "ietf-restconf@2017-01-26.yang".
 * An entry containing information about one specific error that occurred while
 * processing a RESTCONF request.
 */
public final class RestconfError {
    private final ErrorType errorType;
    private final ErrorTag errorTag;
    private String errorAppTag;
    private String errorPath;
    private String errorMessage;
    private String errorInfo;

    private RestconfError(ErrorType errorType, ErrorTag errorTag) {
        this.errorType = errorType;
        this.errorTag = errorTag;
    }

    /**
     * The protocol layer where the error occurred.
     * @return The error type
     */
    public ErrorType getErrorType() {
        return errorType;
    }

    /**
     * The enumerated error-tag.
     * @return The enumerated error-tag
     */
    public ErrorTag getErrorTag() {
        return errorTag;
    }

    /**
     * The application-specific error-tag.
     * @return The application-specific error-tag
     */
    public String getErrorAppTag() {
        return errorAppTag;
    }

    /**
     * The YANG instance identifier associated with the error node.
     * @return A yang instance represented in XPath
     */
    public String getErrorPath() {
        return errorPath;
    }

    /**
     * A message describing the error.
     * @return A message describing the error
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * A container with zero or more data nodes representing additional error information.
     * @return A string serialization of datanodes e.g. a stack trace
     */
    public String getErrorInfo() {
        return errorInfo;
    }

    /**
     * Convert the restconf error to Json - this is for one error - many may be included in a response.
     * @return A JSON object containing the details of one error
     */
    public ObjectNode toJson() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode errorTags = (ObjectNode) mapper.createObjectNode();
        errorTags.put("error-type", errorType.name().toLowerCase());
        errorTags.put("error-tag", errorTag.text());

        if (errorAppTag != null) {
            errorTags.put("error-app-tag", errorAppTag);
        }

        if (errorPath != null) {
            errorTags.put("error-path", errorPath);
        }

        if (errorMessage != null) {
            errorTags.put("error-message", errorMessage);
        }

        if (errorInfo != null) {
            errorTags.put("error-info", errorInfo);
        }

        ObjectNode errorNode = (ObjectNode) mapper.createObjectNode();
        errorNode.put("error", errorTags);
        return errorNode;
    }

    /**
     * An enumerated set of the protocol layer involved in a RESTCONF request.
     */
    public enum ErrorType {
        //The transport layer
        TRANSPORT,
        //The rpc or notification layer
        RPC,
        //The protocol operation layer
        PROTOCOL,
        //The server application layer
        APPLICATION
    }

    /**
     * An enumerated set of error 'tags' - consistent labels for error causes.
     * See <a href="https://tools.ietf.org/html/rfc8040#section-7">Section 7 of RFC 8040</a>
     * for a list of HTTP status codes associated with these
     * error tags. Developers should ensure that suitable HTTP status codes are used
     * when raising RESTCONF errors
     */
    public enum ErrorTag {
        /**
         * Use with Response 409 {@link javax.ws.rs.core.Response.Status#CONFLICT}.
         */
        IN_USE("in-use"),
        /**
         * Use with Response 400 {@link javax.ws.rs.core.Response.Status#BAD_REQUEST},
         * 404 {@link javax.ws.rs.core.Response.Status#NOT_FOUND} or
         * 406 {@link javax.ws.rs.core.Response.Status#NOT_ACCEPTABLE}.
         */
        INVALID_VALUE("invalid-value"),
        /**
         * Use with Response 413 {@link javax.ws.rs.core.Response.Status#REQUEST_ENTITY_TOO_LARGE}.
         */
        REQUEST_TOO_BIG("too-big"),
        /**
         * Use with Response 400 {@link javax.ws.rs.core.Response.Status#BAD_REQUEST}.
         */
        RESPONSE_TOO_BIG("too-big"),
        /**
         * Use with Response 400 {@link javax.ws.rs.core.Response.Status#BAD_REQUEST}.
         */
        MISSING_ATTRIBUTE("missing-attribute"),
        /**
         * Use with Response 400 {@link javax.ws.rs.core.Response.Status#BAD_REQUEST}.
         */
        BAD_ATTRIBUTE("bad-attribute"),
        /**
         * Use with Response 400 {@link javax.ws.rs.core.Response.Status#BAD_REQUEST}.
         */
        UNKNOWN_ATTRIBUTE("unknown-attribute"),
        /**
         * Use with Response 400 {@link javax.ws.rs.core.Response.Status#BAD_REQUEST}.
         */
        BAD_ELEMENT("bad-element"),
        /**
         * Use with Response 400 {@link javax.ws.rs.core.Response.Status#BAD_REQUEST}.
         */
        UNKNOWN_ELEMENT("unknown-element"),
        /**
         * Use with Response 400 {@link javax.ws.rs.core.Response.Status#BAD_REQUEST}.
         */
        UNKNOWN_NAMESPACE("unknown-namespace"),
        /**
         * Use with Response 401 {@link javax.ws.rs.core.Response.Status#UNAUTHORIZED},
         * or 403 {@link javax.ws.rs.core.Response.Status#FORBIDDEN}.
         */
        ACCESS_DENIED("access-denied"),
        /**
         * Use with Response 409 {@link javax.ws.rs.core.Response.Status#CONFLICT}.
         */
        LOCK_DENIED("lock-denied"),
        /**
         * Use with Response 409 {@link javax.ws.rs.core.Response.Status#CONFLICT}.
         */
        RESOURCE_DENIED("resource-denied"),
        /**
         * Use with Response 500 {@link javax.ws.rs.core.Response.Status#INTERNAL_SERVER_ERROR}.
         */
        ROLLBACK_FAILED("rollback-failed"),
        /**
         * Use with Response 409 {@link javax.ws.rs.core.Response.Status#CONFLICT}.
         */
        DATA_EXISTS("data-exists"),
        /**
         * Use with Response 409 {@link javax.ws.rs.core.Response.Status#CONFLICT}.
         */
        DATA_MISSING("data-missing"),
        /**
         * Use with Response 405 {@link javax.ws.rs.core.Response.Status#METHOD_NOT_ALLOWED},
         * or 501 {@link javax.ws.rs.core.Response.Status#NOT_IMPLEMENTED}.
         */
        OPERATION_NOT_SUPPORTED("operation-not-supported"),
        /**
         * Use with Response 412 {@link javax.ws.rs.core.Response.Status#PRECONDITION_FAILED},
         * or 500 {@link javax.ws.rs.core.Response.Status#INTERNAL_SERVER_ERROR}.
         */
        OPERATION_FAILED("operation-failed"),
        /**
         * Use with Response 500 {@link javax.ws.rs.core.Response.Status#INTERNAL_SERVER_ERROR}.
         */
        PARTIAL_OPERATION("partial-operation"),
        /**
         * Use with Response 400 {@link javax.ws.rs.core.Response.Status#BAD_REQUEST}.
         */
        MALFORMED_MESSAGE("malformed-message");

        private String text;

        ErrorTag(String text) {
            this.text = text;
        }

        /**
         * Lowercase version of the error tag compliant with the standard.
         * @return the associated lowercase version
         */
        public String text() {
            return text;
        }
    }

    /**
     * Build a new RestconfError. ErrorTag and ErrorType are mandatory parameters.
     * @param errorType The error type
     * @param errorTag The error Tag
     * @return A build which an be used to create the RestconfError
     */
    public static RestconfError.Builder builder(ErrorType errorType, ErrorTag errorTag) {
        return new Builder(errorType, errorTag);
    }

    /**
     * Create the complete JSON representation of the errors for the Response.
     * Note this can contain many individual RestconfErrors
     * @param restconfErrors A list of {@link RestconfError}s
     * @return A JSON node which can be used in a response
     */
    public static ObjectNode wrapErrorAsJson(List<RestconfError> restconfErrors) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode errorArray = mapper.createArrayNode();
        restconfErrors.forEach(error -> errorArray.add(error.toJson()));
        ObjectNode errorsNode = (ObjectNode) mapper.createObjectNode();
        errorsNode.put("ietf-restconf:errors", errorArray);
        return errorsNode;
    }

    /**
     * A builder for the Restconf Error. ErrorTag and ErrorType are mandatory parameters.
     */
    public static final class Builder {
        private RestconfError restconfError;

        /**
         * ErrorTag and ErrorType are mandatory parameters of the error.
         * @param errorType The error-type
         * @param errorTag The error-tag
         */
        private Builder(ErrorType errorType, ErrorTag errorTag) {
            restconfError = new RestconfError(errorType, errorTag);
        }

        /**
         * Set an error-app-tag on the error.
         * @param errorAppTag a tag relevant to the error in the application
         * @return The builder
         */
        public Builder errorAppTag(String errorAppTag) {
            this.restconfError.errorAppTag = errorAppTag;
            return this;
        }

        /**
         * Set an error-path on the error.
         * @param errorPath A path to the resource that caused the error
         * @return The builder
         */
        public Builder errorPath(String errorPath) {
            this.restconfError.errorPath = errorPath;
            return this;
        }

        /**
         * Set an error-message on the error.
         * @param errorMessage an explaination of the error
         * @return The builder
         */
        public Builder errorMessage(String errorMessage) {
            this.restconfError.errorMessage = errorMessage;
            return this;
        }

        /**
         * Set an error-info on the error.
         * @param errorInfo Any additional infor about the error
         * @return The builder
         */
        public Builder errorInfo(String errorInfo) {
            this.restconfError.errorInfo = errorInfo;
            return this;
        }

        /**
         * Build the contents of the builder in to a {@link RestconfError}.
         * @return A RestconfError
         */
        public RestconfError build() {
            return restconfError;
        }
    }
}
