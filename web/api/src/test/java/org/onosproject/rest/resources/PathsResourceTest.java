/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.rest.resources;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.google.common.collect.ImmutableSet;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onlab.rest.BaseResource;
import org.onosproject.codec.CodecService;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.net.ElementId;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.topology.PathService;

import javax.ws.rs.client.WebTarget;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.onosproject.net.NetTestTools.createPath;
import static org.onosproject.net.NetTestTools.did;
import static org.onosproject.net.NetTestTools.hid;

/**
 * Unit tests for paths REST APIs.
 */
public class PathsResourceTest extends ResourceTest {
    Path path1 = createPath("dev1", "dev2");
    Path path2 = createPath("dev2", "dev3");
    Set<Path> paths = ImmutableSet.of(path1, path2);

    final PathService mockPathService = createMock(PathService.class);

    /**
     * Hamcrest matcher for a path and its JSON representation.
     */
    private final class PathJsonMatcher extends TypeSafeDiagnosingMatcher<JsonObject> {

        private final Path path;

        /**
         * Creates the matcher.
         *
         * @param pathValue the path object to match
         */
        private PathJsonMatcher(Path pathValue) {
            path = pathValue;
        }

        @Override
        public boolean matchesSafely(JsonObject pathJson, Description description) {

            double jsonCost = pathJson.get("cost").asDouble();
            if (jsonCost != path.cost()) {
                description.appendText("src device was " + jsonCost);
                return false;
            }

            JsonArray jsonLinks = pathJson.get("links").asArray();
            assertThat(jsonLinks.size(), is(path.links().size()));

            for (int linkIndex = 0; linkIndex < jsonLinks.size(); linkIndex++) {
                Link link = path.links().get(linkIndex);
                JsonObject jsonLink = jsonLinks.get(0).asObject();

                JsonObject jsonLinkSrc = jsonLink.get("src").asObject();
                String srcDevice = jsonLinkSrc.get("device").asString();
                if (!srcDevice.equals(link.src().deviceId().toString())) {
                    description.appendText("src device was " + jsonLinkSrc);
                    return false;
                }

                JsonObject jsonLinkDst = jsonLink.get("dst").asObject();
                String dstDevice = jsonLinkDst.get("device").asString();
                if (!dstDevice.equals(link.dst().deviceId().toString())) {
                    description.appendText("dst device was " + jsonLinkDst);
                    return false;
                }
            }

            return true;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(path.toString());
        }
    }

    /**
     * Factory to allocate an connect point matcher.
     *
     * @param path path object we are looking for
     * @return matcher
     */
    private PathJsonMatcher matchesPath(Path path) {
        return new PathJsonMatcher(path);
    }

    /**
     * Initializes test mocks and environment.
     */
    @Before
    public void setUpTest() {

        // Register the services needed for the test
        CodecManager codecService =  new CodecManager();
        codecService.activate();
        ServiceDirectory testDirectory =
                new TestServiceDirectory()
                        .add(PathService.class, mockPathService)
                        .add(CodecService.class, codecService);

        BaseResource.setServiceDirectory(testDirectory);
    }

    /**
     * Tears down test mocks and environment.
     */
    @After
    public void tearDownTest() {
        verify(mockPathService);
    }

    /**
     * Tests a REST path GET for the given endpoints.
     *
     * @param srcElement source element of the path
     * @param dstElement destination element of the path
     *
     * @throws UnsupportedEncodingException
     */
    private void runTest(ElementId srcElement, ElementId dstElement)
                 throws UnsupportedEncodingException {
        expect(mockPathService.getPaths(srcElement, dstElement))
                .andReturn(paths)
                .once();
        replay(mockPathService);

        String srcId = URLEncoder.encode(srcElement.toString(),
                                         StandardCharsets.UTF_8.name());
        String dstId = URLEncoder.encode(dstElement.toString(),
                                         StandardCharsets.UTF_8.name());

        String url = "paths/" + srcId + "/" + dstId;
        WebTarget wt = target();
        String response = wt.path(url).request().get(String.class);
        assertThat(response, containsString("{\"paths\":["));

        JsonObject result = Json.parse(response).asObject();
        assertThat(result, notNullValue());

        assertThat(result.names(), hasSize(1));
        assertThat(result.names().get(0), is("paths"));

        JsonArray jsonPaths = result.get("paths").asArray();
        assertThat(jsonPaths, notNullValue());
        assertThat(jsonPaths.size(), is(2));

        JsonObject path1Json = jsonPaths.get(0).asObject();
        assertThat(path1Json, matchesPath(path1));

        JsonObject path2Json = jsonPaths.get(1).asObject();
        assertThat(path2Json, matchesPath(path2));
    }

    /**
     * Tests a path between two hosts.
     *
     * @throws UnsupportedEncodingException if UTF-8 not found
     */
    @Test
    public void hostToHost() throws UnsupportedEncodingException {
        runTest(hid("01:23:45:67:89:AB/2"), hid("AB:89:67:45:23:01/4"));
    }

    /**
     * Tests a path with a host as the source and a switch as the destination.
     *
     * @throws UnsupportedEncodingException if UTF-8 not found
     */
    @Test
    public void hostToDevice() throws UnsupportedEncodingException {
        runTest(hid("01:23:45:67:89:AB/2"), did("switch1"));
    }

    /**
     * Tests a path with a switch as the source and a host as the destination.
     *
     * @throws UnsupportedEncodingException if UTF-8 not found
     */
    @Test
    public void deviceToHost() throws UnsupportedEncodingException {
        runTest(did("switch1"), hid("01:23:45:67:89:AB/2"));
    }

    /**
     * Tests a path between two switches.
     *
     * @throws UnsupportedEncodingException if UTF-8 not found
     */
    @Test
    public void deviceToDevice() throws UnsupportedEncodingException {
        runTest(did("switch1"), did("switch2"));
    }
}
