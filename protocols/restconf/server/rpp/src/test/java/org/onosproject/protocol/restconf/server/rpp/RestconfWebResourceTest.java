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
package org.onosproject.protocol.restconf.server.rpp;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.easymock.EasyMock;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Before;
import org.junit.Test;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onosproject.rest.resources.ResourceTest;
import org.onosproject.restconf.api.RestconfError;
import org.onosproject.restconf.api.RestconfException;
import org.onosproject.restconf.api.RestconfService;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test the RestconfWebResource.
 */
public class RestconfWebResourceTest extends ResourceTest {

    public static final String DATA_IETF_SYSTEM_SYSTEM = "data/ietf-system:system";

    private static final Pattern RESTCONF_ERROR_REGEXP =
            Pattern.compile("(\\{\"ietf-restconf:errors\":\\[)\\R?"
                    + "((\\{\"error\":)\\R?"
                    + "(\\{\"error-type\":\")((protocol)|(transport)|(rpc)|(application))(\",)\\R?"
                    + "(\"error-tag\":\")[a-z\\-]*(\",)\\R?"
                    + "((\"error-app-tag\":\").*(\",))?\\R?"
                    + "((\"error-path\":\").*(\",))?\\R?"
                    + "((\"error-message\":\").*(\"))?(\\}\\},?))*(\\]\\})", Pattern.DOTALL);

    private RestconfService restconfService = createMock(RestconfService.class);

    public RestconfWebResourceTest() {
        super(ResourceConfig.forApplicationClass(RestconfProtocolProxy.class));
    }

    @Before
    public void setup() {
        ServiceDirectory testDirectory = new TestServiceDirectory()
                .add(RestconfService.class, restconfService);
        setServiceDirectory(testDirectory);
    }

    /**
     * Test handleGetRequest when an Json object is returned.
     */
    @Test
    public void testHandleGetRequest() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        expect(restconfService
                .runGetOperationOnDataResource(URI.create(getBaseUri() + DATA_IETF_SYSTEM_SYSTEM)))
                .andReturn(node).anyTimes();
        replay(restconfService);

        WebTarget wt = target();
        String response = wt.path("/" + DATA_IETF_SYSTEM_SYSTEM).request().get(String.class);
        assertNotNull(response);
    }

    /**
     * Test handleGetRequest when nothing is returned.
     */
    @Test
    public void testHandleGetRequestNotFound() {
        expect(restconfService
                .runGetOperationOnDataResource(URI.create(getBaseUri() + DATA_IETF_SYSTEM_SYSTEM)))
                .andReturn(null).anyTimes();
        replay(restconfService);

        WebTarget wt = target();
        try {
            String response = wt.path("/" + DATA_IETF_SYSTEM_SYSTEM).request().get(String.class);
            fail("Expecting fail as response is none");
        } catch (NotFoundException e) {
            assertNotNull(e.getResponse());
            assertRestconfErrorJson(e.getResponse());
        }
    }

    /**
     * Test handleGetRequest when an RestconfException is thrown.
     */
    @Test
    public void testHandleGetRequestRestconfException() {
        expect(restconfService
                .runGetOperationOnDataResource(URI.create(getBaseUri() + DATA_IETF_SYSTEM_SYSTEM)))
                .andThrow(new RestconfException("Suitable error message",
                        RestconfError.ErrorTag.OPERATION_FAILED, INTERNAL_SERVER_ERROR,
                        Optional.of("/" + DATA_IETF_SYSTEM_SYSTEM),
                        Optional.of("More info about the error")))
                .anyTimes();
        replay(restconfService);

        WebTarget wt = target();
        try {
            String response = wt.path("/" + DATA_IETF_SYSTEM_SYSTEM).request().get(String.class);
            fail("Expecting fail as response is RestconfException");
        } catch (InternalServerErrorException e) {
            assertNotNull(e.getResponse());
            assertRestconfErrorJson(e.getResponse());
        }
    }

    /**
     * Test handleGetRequest when an Exception is thrown.
     */
    @Test
    public void testHandleGetRequestIoException() {
        expect(restconfService
                .runGetOperationOnDataResource(URI.create(getBaseUri() + DATA_IETF_SYSTEM_SYSTEM)))
                .andThrow(new IllegalArgumentException("A test exception"))
                .anyTimes();
        replay(restconfService);

        WebTarget wt = target();
        try {
            String response = wt.path("/" + DATA_IETF_SYSTEM_SYSTEM).request().get(String.class);
            fail("Expecting fail as response is IllegalArgumentException");
        } catch (InternalServerErrorException e) {
            assertNotNull(e.getResponse());
            assertRestconfErrorJson(e.getResponse());
        }
    }

    /**
     * Test handlePostRequest with no exception.
     */
    @Test
    public void testHandlePostRequest() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode ietfSystemSubNode = mapper.createObjectNode();
        ietfSystemSubNode.put("contact", "Open Networking Foundation");
        ietfSystemSubNode.put("hostname", "host1");
        ietfSystemSubNode.put("location", "The moon");

        ObjectNode ietfSystemNode = mapper.createObjectNode();
        ietfSystemNode.put("ietf-system:system", ietfSystemSubNode);

        WebTarget wt = target();
        Response response = wt.path("/" + DATA_IETF_SYSTEM_SYSTEM)
                .request()
                .post(Entity.json(ietfSystemNode.toString()));
        assertEquals(201, response.getStatus());
    }

    /**
     * Test handlePostRequest with 'already exists' exception.
     */
    @Test
    public void testHandlePostRequestAlreadyExists() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode ietfSystemSubNode = mapper.createObjectNode();
        ietfSystemSubNode.put("contact", "Open Networking Foundation");
        ietfSystemSubNode.put("hostname", "host1");
        ietfSystemSubNode.put("location", "The moon");

        ObjectNode ietfSystemNode = mapper.createObjectNode();
        ietfSystemNode.put("ietf-system:system", ietfSystemSubNode);

        restconfService.runPostOperationOnDataResource(
                EasyMock.<URI>anyObject(), EasyMock.<ObjectNode>anyObject());
        expectLastCall().andThrow(new RestconfException("Requested node already present", null,
                RestconfError.ErrorTag.DATA_EXISTS, CONFLICT,
                Optional.of("/" + DATA_IETF_SYSTEM_SYSTEM)));
        replay(restconfService);

        WebTarget wt = target();
        Response response = wt.path("/" + DATA_IETF_SYSTEM_SYSTEM)
                .request()
                .post(Entity.json(ietfSystemNode.toString()));
        assertEquals(409, response.getStatus());
    }

    private static void assertRestconfErrorJson(Response errorResponse) {
        ByteArrayInputStream in = (ByteArrayInputStream) errorResponse.getEntity();
        int n = in.available();
        byte[] bytes = new byte[n];
        in.read(bytes, 0, n);

        Matcher m = RESTCONF_ERROR_REGEXP.matcher(new String(bytes, StandardCharsets.UTF_8));
        assertTrue(m.matches());
    }
}
