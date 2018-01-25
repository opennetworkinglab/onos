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

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;

import javax.ws.rs.core.Response;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.Assert.assertEquals;

/**
 * Test the conversion of RestconfErrors in to JSON.
 */
public class RestconfExceptionTest {

    private RestconfError error1 = RestconfError
            .builder(RestconfError.ErrorType.TRANSPORT, RestconfError.ErrorTag.ACCESS_DENIED)
            .build();

    private RestconfError error2 = RestconfError
            .builder(RestconfError.ErrorType.TRANSPORT, RestconfError.ErrorTag.BAD_ATTRIBUTE)
            .build();

    private RestconfError error3 = RestconfError
            .builder(RestconfError.ErrorType.RPC, RestconfError.ErrorTag.BAD_ELEMENT)
            .errorAppTag("my-app-tag")
            .errorMessage("a message about the error")
            .errorPath("/a/b/c")
            .errorInfo("info about the error")
            .build();

    /**
     * Test a Restconf Exception with many RestconfErrors converted to Json.
     */
    @Test
    public void testToRestconfErrorJson() {
        IllegalArgumentException ie = new IllegalArgumentException("This is a test");
        RestconfException e = new RestconfException("Error in system", ie,
                RestconfError.ErrorTag.DATA_EXISTS, Response.Status.BAD_REQUEST,
                Optional.of("/some/path"));
        e.addToErrors(error1);
        e.addToErrors(error2);
        e.addToErrors(error3);

        assertEquals("{\"ietf-restconf:errors\":[" +
                "{\"error\":{" +
                    "\"error-type\":\"application\"," +
                    "\"error-tag\":\"data-exists\"," +
                    "\"error-path\":\"/some/path\"," +
                    "\"error-message\":\"Error in system\"," +
                    "\"error-info\":\"This is a test\"}}," +
                "{\"error\":{" +
                    "\"error-type\":\"transport\"," +
                    "\"error-tag\":\"access-denied\"}}," +
                "{\"error\":{" +
                    "\"error-type\":\"transport\"," +
                    "\"error-tag\":\"bad-attribute\"}}," +
                "{\"error\":{" +
                    "\"error-type\":\"rpc\"," +
                    "\"error-tag\":\"bad-element\"," +
                    "\"error-app-tag\":\"my-app-tag\"," +
                    "\"error-path\":\"/a/b/c\"," +
                    "\"error-message\":\"a message about the error\"," +
                    "\"error-info\":\"info about the error\"}}]}",
                e.toRestconfErrorJson().toString());
    }

    @Test
    public void testWrappingErrorsNotInException() {
        RestconfError error4 = RestconfError
                .builder(RestconfError.ErrorType.TRANSPORT, RestconfError.ErrorTag.UNKNOWN_ELEMENT)
                .build();

        ObjectNode json = RestconfError.wrapErrorAsJson(Arrays.asList(error4, error1, error2, error3));

        assertEquals("{\"ietf-restconf:errors\":[" +
                    "{\"error\":{" +
                        "\"error-type\":\"transport\"," +
                        "\"error-tag\":\"unknown-element\"}}," +
                    "{\"error\":{" +
                        "\"error-type\":\"transport\"," +
                        "\"error-tag\":\"access-denied\"}}," +
                    "{\"error\":{" +
                        "\"error-type\":\"transport\"," +
                        "\"error-tag\":\"bad-attribute\"}}," +
                    "{\"error\":{" +
                        "\"error-type\":\"rpc\"," +
                        "\"error-tag\":\"bad-element\"," +
                        "\"error-app-tag\":\"my-app-tag\"," +
                        "\"error-path\":\"/a/b/c\"," +
                        "\"error-message\":\"a message about the error\"," +
                        "\"error-info\":\"info about the error\"}}]}",
                json.toString());

    }
}
