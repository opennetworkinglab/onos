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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onlab.rest.BaseResource;
import org.onosproject.codec.CodecService;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.link.LinkService;

import javax.ws.rs.client.WebTarget;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.onosproject.net.NetTestTools.link;

/**
 * Unit tests for links REST APIs.
 */
public class LinksResourceTest extends ResourceTest {
    LinkService mockLinkService;

    Link link1 = link("src1", 1, "dst1", 1);
    Link link2 = link("src2", 2, "dst2", 2);
    Link link3 = link("src3", 3, "dst3", 3);

    /**
     * Hamcrest matcher to check that an link representation in JSON matches
     * the actual link.
     */
    public static class LinkJsonMatcher extends TypeSafeMatcher<JsonObject> {
        private final Link link;
        private String reason = "";

        public LinkJsonMatcher(Link linkValue) {
            link = linkValue;
        }

        @Override
        public boolean matchesSafely(JsonObject jsonLink) {
            JsonObject jsonSrc = jsonLink.get("src").asObject();
            String jsonSrcDevice = jsonSrc.get("device").asString();
            String jsonSrcPort = jsonSrc.get("port").asString();

            JsonObject jsonDst = jsonLink.get("dst").asObject();
            String jsonDstDevice = jsonDst.get("device").asString();
            String jsonDstPort = jsonDst.get("port").asString();

            return jsonSrcDevice.equals(link.src().deviceId().toString()) &&
                   jsonSrcPort.equals(link.src().port().toString()) &&
                   jsonDstDevice.equals(link.dst().deviceId().toString()) &&
                   jsonDstPort.equals(link.dst().port().toString());
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(reason);
        }
    }

    /**
     * Factory to allocate an link matcher.
     *
     * @param link link object we are looking for
     * @return matcher
     */
    private static LinkJsonMatcher matchesLink(Link link) {
        return new LinkJsonMatcher(link);
    }

    /**
     * Hamcrest matcher to check that an link is represented properly in a JSON
     * array of links.
     */
    private static class LinkJsonArrayMatcher extends TypeSafeMatcher<JsonArray> {
        private final Link link;
        private String reason = "";

        public LinkJsonArrayMatcher(Link linkValue) {
            link = linkValue;
        }

        @Override
        public boolean matchesSafely(JsonArray json) {
            final int expectedAttributes = 2;

            for (int jsonLinkIndex = 0; jsonLinkIndex < json.size();
                 jsonLinkIndex++) {

                JsonObject jsonLink = json.get(jsonLinkIndex).asObject();

                if (matchesLink(link).matchesSafely(jsonLink)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(reason);
        }
    }

    /**
     * Factory to allocate an link array matcher.
     *
     * @param link link object we are looking for
     * @return matcher
     */
    private static LinkJsonArrayMatcher hasLink(Link link) {
        return new LinkJsonArrayMatcher(link);
    }

    /**
     * Initializes test mocks and environment.
     */
    @Before
    public void setUpTest() {
        mockLinkService = createMock(LinkService.class);

        // Register the services needed for the test
        CodecManager codecService =  new CodecManager();
        codecService.activate();
        ServiceDirectory testDirectory =
                new TestServiceDirectory()
                        .add(LinkService.class, mockLinkService)
                        .add(CodecService.class, codecService);

        BaseResource.setServiceDirectory(testDirectory);
    }

    /**
     * Tears down and verifies test mocks and environment.
     */
    @After
    public void tearDownTest() {
        verify(mockLinkService);
    }

    /**
     * Tests the result of the rest api GET when there are no links.
     */
    @Test
    public void testLinksEmptyArray() {
        expect(mockLinkService.getLinks()).andReturn(ImmutableList.of());
        replay(mockLinkService);

        WebTarget wt = target();
        String response = wt.path("links").request().get(String.class);
        assertThat(response, is("{\"links\":[]}"));
    }

    /**
     * Tests the result of the rest api GET when there are links present.
     */
    @Test
    public void testLinks() {
        expect(mockLinkService.getLinks())
                .andReturn(ImmutableList.of(link1, link2, link3))
                .anyTimes();

        replay(mockLinkService);

        WebTarget wt = target();
        String response = wt.path("links").request().get(String.class);
        assertThat(response, containsString("{\"links\":["));

        JsonObject result = Json.parse(response).asObject();
        assertThat(result, notNullValue());

        assertThat(result.names(), hasSize(1));
        assertThat(result.names().get(0), is("links"));

        JsonArray jsonLinks = result.get("links").asArray();
        assertThat(jsonLinks, notNullValue());
        assertThat(jsonLinks.size(), is(3));

        assertThat(jsonLinks, hasLink(link1));
        assertThat(jsonLinks, hasLink(link2));
        assertThat(jsonLinks, hasLink(link3));
    }

    /**
     * Tests the result of the rest api GET of links for a specific device.
     */
    @Test
    public void testLinksByDevice() {
        expect(mockLinkService.getDeviceLinks(isA(DeviceId.class)))
                .andReturn(ImmutableSet.of(link2))
                .anyTimes();

        replay(mockLinkService);

        WebTarget wt = target();
        String response = wt
                .path("links")
                .queryParam("device", "src2")
                .request()
                .get(String.class);
        assertThat(response, containsString("{\"links\":["));

        JsonObject result = Json.parse(response).asObject();
        assertThat(result, notNullValue());

        assertThat(result.names(), hasSize(1));
        assertThat(result.names().get(0), is("links"));

        JsonArray jsonLinks = result.get("links").asArray();
        assertThat(jsonLinks, notNullValue());
        assertThat(jsonLinks.size(), is(1));

        assertThat(jsonLinks, hasLink(link2));
    }

    /**
     * Tests the result of the rest api GET of links for a specific device
     * and port.
     */
    @Test
    public void testLinksByDevicePort() {

        expect(mockLinkService.getLinks(isA(ConnectPoint.class)))
                .andReturn(ImmutableSet.of(link2))
                .anyTimes();

        replay(mockLinkService);

        WebTarget wt = target();
        String response = wt
                .path("links")
                .queryParam("device", "src2")
                .queryParam("port", "2")
                .request()
                .get(String.class);
        assertThat(response, containsString("{\"links\":["));

        JsonObject result = Json.parse(response).asObject();
        assertThat(result, notNullValue());

        assertThat(result.names(), hasSize(1));
        assertThat(result.names().get(0), is("links"));

        JsonArray jsonLinks = result.get("links").asArray();
        assertThat(jsonLinks, notNullValue());
        assertThat(jsonLinks.size(), is(1));

        assertThat(jsonLinks, hasLink(link2));
    }

    /**
     * Tests the result of the rest api GET of links for a specific
     * device, port, and direction.
     */
    @Test
    public void testLinksByDevicePortDirection() {

        expect(mockLinkService.getIngressLinks(isA(ConnectPoint.class)))
                .andReturn(ImmutableSet.of(link2))
                .anyTimes();

        replay(mockLinkService);

        WebTarget wt = target();
        String response = wt
                .path("links")
                .queryParam("device", "src2")
                .queryParam("port", "2")
                .queryParam("direction", "INGRESS")
                .request()
                .get(String.class);
        assertThat(response, containsString("{\"links\":["));

        JsonObject result = Json.parse(response).asObject();
        assertThat(result, notNullValue());

        assertThat(result.names(), hasSize(1));
        assertThat(result.names().get(0), is("links"));

        JsonArray jsonLinks = result.get("links").asArray();
        assertThat(jsonLinks, notNullValue());
        assertThat(jsonLinks.size(), is(1));

        assertThat(jsonLinks, hasLink(link2));
    }

    /**
     * Tests the result of the rest api GET of links for a specific
     * device and direction.
     */
    @Test
    public void testLinksByDeviceDirection() {

        expect(mockLinkService.getDeviceIngressLinks(isA(DeviceId.class)))
                .andReturn(ImmutableSet.of(link2))
                .anyTimes();

        replay(mockLinkService);

        WebTarget wt = target();
        String response = wt
                .path("links")
                .queryParam("device", "src2")
                .queryParam("direction", "INGRESS")
                .request()
                .get(String.class);
        assertThat(response, containsString("{\"links\":["));

        JsonObject result = Json.parse(response).asObject();
        assertThat(result, notNullValue());

        assertThat(result.names(), hasSize(1));
        assertThat(result.names().get(0), is("links"));

        JsonArray jsonLinks = result.get("links").asArray();
        assertThat(jsonLinks, notNullValue());
        assertThat(jsonLinks.size(), is(1));

        assertThat(jsonLinks, hasLink(link2));
    }
}
