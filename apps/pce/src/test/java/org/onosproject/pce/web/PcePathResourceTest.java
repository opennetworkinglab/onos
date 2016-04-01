/*
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.pce.web;

//import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
//import static org.easymock.EasyMock.expect;
//import static org.easymock.EasyMock.replay;
//import static org.hamcrest.Matchers.containsString;
//import static org.hamcrest.Matchers.is;
//import static org.hamcrest.Matchers.notNullValue;
//import static org.junit.Assert.assertThat;
//import static org.junit.Assert.fail;

//import javax.ws.rs.NotFoundException;
//import javax.ws.rs.client.Entity;
//import javax.ws.rs.client.WebTarget;
//import javax.ws.rs.core.MediaType;
//import javax.ws.rs.core.Response;
//import java.io.InputStream;
//import java.net.HttpURLConnection;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Objects;
//import java.util.Optional;
//import java.util.Set;

//import com.eclipsesource.json.Json;
//import com.eclipsesource.json.JsonObject;
//import com.google.common.collect.ImmutableList;
//import com.google.common.collect.Lists;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onlab.rest.BaseResource;
import org.onosproject.codec.CodecService;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.net.intent.Constraint;
import org.onosproject.pce.pceservice.PcePath;
import org.onosproject.pce.pceservice.LspType;
import org.onosproject.pce.pceservice.api.PceService;

/**
 * Unit tests for pce path REST APIs.
 */
public class PcePathResourceTest extends PceResourceTest {
    final PceService pceService = createMock(PceService.class);
    final TunnelId pcePathId1 = TunnelId.valueOf("1");
    //TODO: will be uncommented below lines once CostConstraint and LocalBandwidthConstraint classes are ready
    final Constraint costConstraint = null; //CostConstraint.of("2");
    final Constraint bandwidthConstraint = null; //LocalBandwidthConstraint.of("200.0");
    final LspType lspType = LspType.WITH_SIGNALLING;
    final MockPcePath pcePath1 = new MockPcePath(pcePathId1, "11.0.0.1", "11.0.0.2", lspType, "pcc2",
                                                 costConstraint, bandwidthConstraint);

    /**
     * Mock class for a pce path.
     */
    private static class MockPcePath implements PcePath {
        private TunnelId id;
        private String source;
        private String destination;
        private LspType lspType;
        private String name;
        private Constraint costConstraint;
        private Constraint bandwidthConstraint;

        /**
         * Constructor to initialize member variables.
         *
         * @param id pce path id
         * @param src source device
         * @param dst destination device
         * @param type lsp type
         * @param name symbolic path name
         * @param constrnt pce constraint
         */
        public MockPcePath(TunnelId id, String src, String dst, LspType type, String name,
                           Constraint costConstrnt, Constraint bandwidthConstrnt) {
            this.id = id;
            this.source = src;
            this.destination = dst;
            this.name = name;
            this.lspType = type;
            this.costConstraint = costConstrnt;
            this.bandwidthConstraint = bandwidthConstrnt;
        }

        @Override
        public TunnelId id() {
            return id;
        }

        @Override
        public void id(TunnelId id) {
            this.id = id;
        }

        @Override
        public String source() {
            return source;
        }

        @Override
        public void source(String src) {
            this.source = src;
        }

        @Override
        public String destination() {
            return destination;
        }

        @Override
        public void destination(String dst) {
            this.destination = dst;
        }

        @Override
        public LspType lspType() {
            return lspType;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Constraint costConstraint() {
            return costConstraint;
        }

        @Override
        public Constraint bandwidthConstraint() {
            return bandwidthConstraint;
        }

        @Override
        public PcePath copy(PcePath path) {
            if (null != path.source()) {
                this.source = path.source();
            }
            if (null != path.destination()) {
                this.destination = path.destination();
            }
            if (this.lspType != path.lspType()) {
                this.lspType = path.lspType();
            }
            if (null != path.name()) {
                this.name = path.name();
            }
            if (null != path.costConstraint()) {
                this.costConstraint = path.costConstraint();
            }
            if (null != path.bandwidthConstraint()) {
                this.bandwidthConstraint = path.bandwidthConstraint();
            }
            return this;
        }
    }

    /**
     * Sets up the global values for all the tests.
     */
    @Before
    public void setUpTest() {
        MockPceCodecContext context = new MockPceCodecContext();
        ServiceDirectory testDirectory = new TestServiceDirectory().add(PceService.class, pceService)
                .add(CodecService.class, context.codecManager());
        BaseResource.setServiceDirectory(testDirectory);
    }

    /**
     * Cleans up.
     */
    @After
    public void tearDownTest() {
    }

    /**
     * Tests the result of the rest api GET when there are no pce paths.
     */
    @Test
    public void testPcePathsEmpty() {
        //TODO: will be uncommented below code once PceService is ready
        //expect(pceService.queryAllPath()).andReturn(null).anyTimes();
        //replay(pceService);
        //final WebTarget wt = target();
        //final String response = wt.path("path").request().get(String.class);
        //assertThat(response, is("{\"paths\":[]}"));
    }

    /**
     * Tests the result of a rest api GET for pce path id.
     */
    @Test
    public void testGetTunnelId() {
        //TODO: will be uncommented below code once PceService is ready
        //final Set<PcePath> pcePaths = new HashSet<>();
        //pcePaths.add(pcePath1);

        //expect(pceService.queryPath(anyObject())).andReturn(pcePath1).anyTimes();
        //replay(pceService);

        //final WebTarget wt = target();
        //final String response = wt.path("path/1").request().get(String.class);
        //final JsonObject result = Json.parse(response).asObject();
        //assertThat(result, notNullValue());
    }

    /**
     * Tests that a fetch of a non-existent pce path object throws an exception.
     */
    @Test
    public void testBadGet() {
        //TODO: will be uncommented below code once PceService is ready
        //expect(pceService.queryPath(anyObject()))
        //        .andReturn(null).anyTimes();
        //replay(pceService);

        //WebTarget wt = target();
        //try {
        //    wt.path("path/1").request().get(String.class);
        //    fail("Fetch of non-existent pce path did not throw an exception");
        //} catch (NotFoundException ex) {
        //    assertThat(ex.getMessage(),
        //               containsString("HTTP 404 Not Found"));
        //}
    }

    /**
     * Tests creating a pce path with POST.
     */
    @Test
    public void testPost() {
        //TODO: will be uncommented below code once PceService is ready
        //expect(pceService.setupPath(anyObject()))
        //        .andReturn(true).anyTimes();
        //replay(pceService);

        //WebTarget wt = target();
        //InputStream jsonStream = PcePathResourceTest.class.getResourceAsStream("post-PcePath.json");

        //Response response = wt.path("path")
        //        .request(MediaType.APPLICATION_JSON_TYPE)
        //        .post(Entity.json(jsonStream));
        //assertThat(response.getStatus(), is(HttpURLConnection.HTTP_OK));
    }

    /**
     * Tests creating a pce path with PUT.
     */
    @Test
    public void testPut() {
        //TODO: will be uncommented below code once PceService is ready
        //expect(pceService.updatePath(anyObject()))
        //        .andReturn(true).anyTimes();
        //replay(pceService);

        //WebTarget wt = target();
        //InputStream jsonStream = PcePathResourceTest.class.getResourceAsStream("post-PcePath.json");

        //Response response = wt.path("path/1")
        //        .request(MediaType.APPLICATION_JSON_TYPE)
        //        .put(Entity.json(jsonStream));
        //assertThat(response.getStatus(), is(HttpURLConnection.HTTP_OK));
    }

    /**
     * Tests deleting a pce path.
     */
    @Test
    public void testDelete() {
        //TODO: will be uncommented below code once PceService is ready
        //expect(pceService.releasePath(anyObject()))
        //        .andReturn(true).anyTimes();
        //replay(pceService);

        //WebTarget wt = target();

        //String location = "path/1";

        //Response deleteResponse = wt.path(location)
        //        .request(MediaType.APPLICATION_JSON_TYPE)
        //        .delete();
        //assertThat(deleteResponse.getStatus(),
        //           is(HttpURLConnection.HTTP_OK));
    }
}
