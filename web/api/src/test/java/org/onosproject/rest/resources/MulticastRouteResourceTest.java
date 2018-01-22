/*
 * Copyright 2016-present Open Networking Foundation
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
import org.glassfish.jersey.client.ClientProperties;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onlab.packet.IpAddress;
import org.onosproject.codec.CodecService;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.net.mcast.McastRoute;
import org.onosproject.net.mcast.MulticastRouteService;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Set;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Unit tests for multicast route REST APIs.
 */
public class MulticastRouteResourceTest extends ResourceTest {

    final MulticastRouteService mockMulticastRouteService =
            createMock(MulticastRouteService.class);

    private McastRoute route1;
    private McastRoute route2;
    private McastRoute route3;

    private void initMcastRouteMocks() {
        IpAddress source1 = IpAddress.valueOf("1.1.1.1");
        IpAddress source2 = IpAddress.valueOf("2.2.2.2");
        IpAddress source3 = IpAddress.valueOf("3.3.3.3");

        IpAddress group = IpAddress.valueOf("224.0.0.1");

        route1 = new McastRoute(source1, group, McastRoute.Type.PIM);
        route2 = new McastRoute(source2, group, McastRoute.Type.IGMP);
        route3 = new McastRoute(source3, group, McastRoute.Type.STATIC);
    }

    @Before
    public void setupTest() {
        final CodecManager codecService = new CodecManager();
        codecService.activate();

        ServiceDirectory testDirectory =
                new TestServiceDirectory()
                        .add(MulticastRouteService.class, mockMulticastRouteService)
                        .add(CodecService.class, codecService);
        setServiceDirectory(testDirectory);
    }

    /**
     * Hamcrest matcher to check that a mcast route representation in JSON matches
     * the actual mcast route.
     */
    public static class McastRouteJsonMatcher extends TypeSafeMatcher<JsonObject> {
        private final McastRoute route;
        private String reason = "";

        public McastRouteJsonMatcher(McastRoute mcastRoute) {
            this.route = mcastRoute;
        }

        @Override
        protected boolean matchesSafely(JsonObject jsonMcastRoute) {

            // check source
            String jsonSource = jsonMcastRoute.get("source").asString();
            String source = route.source().toString();
            if (!jsonSource.equals(source)) {
                reason = "Mcast route source was " + jsonSource;
                return false;
            }

            // check group
            String jsonGroup = jsonMcastRoute.get("group").asString();
            String group = route.group().toString();
            if (!jsonGroup.equals(group)) {
                reason = "Mcast route group was " + jsonSource;
                return false;
            }

            // check type
            String jsonType = jsonMcastRoute.get("type").asString();
            String type = route.type().toString();
            if (!jsonType.equals(type)) {
                reason = "Mcast route type was " + jsonSource;
                return false;
            }

            return true;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(reason);
        }
    }

    private static McastRouteJsonMatcher matchesMcastRoute(McastRoute route) {
        return new McastRouteJsonMatcher(route);
    }

    /**
     * Hamcrest matcher to check that a Mcast route is represented properly in
     * a JSON array of Mcastroutes.
     */
    public static class McastRouteJsonArrayMatcher extends TypeSafeMatcher<JsonArray> {
        private final McastRoute route;
        private String reason = "";

        public McastRouteJsonArrayMatcher(McastRoute mcastRoute) {
            this.route = mcastRoute;
        }

        @Override
        protected boolean matchesSafely(JsonArray json) {
            boolean found = false;
            for (int index = 0; index < json.size(); index++) {
                final JsonObject jsonMcastRoute = json.get(index).asObject();

                final String source = route.source().toString();
                final String group = route.group().toString();
                final String type = route.type().toString();
                final String jsonSource = jsonMcastRoute.get("source").asString();
                final String jsonGroup = jsonMcastRoute.get("group").asString();
                final String jsonType = jsonMcastRoute.get("type").asString();

                if (jsonSource.equals(source) && jsonGroup.equals(group) &&
                        jsonType.equals(type)) {
                    found = true;
                    assertThat(jsonMcastRoute, matchesMcastRoute(route));
                }
            }

            return found;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(reason);
        }
    }

    private static McastRouteJsonArrayMatcher hasMcastRoute(McastRoute route) {
        return new McastRouteJsonArrayMatcher(route);
    }

    /**
     * Tests the results of the REST API GET when there are active mcastroutes.
     */
    @Test
    public void testMcastRoutePopulatedArray() {
        initMcastRouteMocks();
        final Set<McastRoute> mcastRoutes = ImmutableSet.of(route1, route2, route3);
        expect(mockMulticastRouteService.getRoutes()).andReturn(mcastRoutes).anyTimes();
        replay(mockMulticastRouteService);

        final WebTarget wt = target();
        final String response = wt.path("mcast").request().get(String.class);
        final JsonObject result = Json.parse(response).asObject();
        assertThat(result, notNullValue());

        assertThat(result.names(), hasSize(1));
        assertThat(result.names().get(0), is("routes"));
        final JsonArray jsonMcastRoutes = result.get("routes").asArray();
        assertThat(jsonMcastRoutes, notNullValue());
        assertThat(jsonMcastRoutes, hasMcastRoute(route1));
        assertThat(jsonMcastRoutes, hasMcastRoute(route2));
        assertThat(jsonMcastRoutes, hasMcastRoute(route3));
    }

    /**
     * Tests creating a Mcast route with POST.
     */
    @Test
    public void testMcastRoutePost() {
        mockMulticastRouteService.add(anyObject());
        expectLastCall();
        replay(mockMulticastRouteService);

        WebTarget wt = target();
        InputStream jsonStream = MulticastRouteResourceTest.class
                .getResourceAsStream("mcastroute.json");

        Response response = wt.path("mcast/")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(jsonStream));
        assertThat(response.getStatus(), is(HttpURLConnection.HTTP_CREATED));

        verify(mockMulticastRouteService);
    }

    /**
     * Tests deletion a Mcast route with DELETE.
     */
    @Test
    public void testMcastRouteDelete() {
        mockMulticastRouteService.remove(anyObject());
        expectLastCall();
        replay(mockMulticastRouteService);

        WebTarget wt = target().property(
                ClientProperties.SUPPRESS_HTTP_COMPLIANCE_VALIDATION, true);
        InputStream jsonStream = MulticastRouteResourceTest.class
                .getResourceAsStream("mcastroute.json");
        wt.request().method("DELETE", Entity.json(jsonStream));
    }
}
