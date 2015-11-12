/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.rest;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.sun.jersey.api.client.WebResource;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onlab.rest.BaseResource;
import org.onosproject.app.ApplicationAdminService;
import org.onosproject.app.ApplicationService;
import org.onosproject.app.ApplicationState;
import org.onosproject.codec.CodecService;
import org.onosproject.codec.impl.ApplicationCodec;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.codec.impl.MockCodecContext;
import org.onosproject.core.Application;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.ApplicationRole;
import org.onosproject.core.DefaultApplication;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.core.Version;

import java.io.InputStream;
import java.net.URI;
import java.util.Optional;

import static org.easymock.EasyMock.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit tests for applications REST APIs.
 */

public class ApplicationsResourceTest extends ResourceTest {

    private static class MockCodecContextWithService extends MockCodecContext {
        private ApplicationAdminService service;

        MockCodecContextWithService(ApplicationAdminService service) {
            this.service = service;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getService(Class<T> serviceClass) {
            return (T) service;
        }
    }

    private ApplicationAdminService service;
    private ApplicationId id1 = new DefaultApplicationId(1, "app1");
    private ApplicationId id2 = new DefaultApplicationId(2, "app2");
    private ApplicationId id3 = new DefaultApplicationId(3, "app3");
    private ApplicationId id4 = new DefaultApplicationId(4, "app4");

    private static final URI FURL = URI.create("mvn:org.foo-features/1.2a/xml/features");
    private static final Version VER = Version.version(1, 2, "a", null);

    private Application app1 =
            new DefaultApplication(id1, VER,
                                   "app1", "origin1", ApplicationRole.ADMIN, ImmutableSet.of(), Optional.of(FURL),
                                   ImmutableList.of("My Feature"), ImmutableList.of());
    private Application app2 =
            new DefaultApplication(id2, VER,
                                   "app2", "origin2", ApplicationRole.ADMIN, ImmutableSet.of(), Optional.of(FURL),
                                   ImmutableList.of("My Feature"), ImmutableList.of());
    private Application app3 =
            new DefaultApplication(id3, VER,
                                   "app3", "origin3", ApplicationRole.ADMIN, ImmutableSet.of(), Optional.of(FURL),
                                   ImmutableList.of("My Feature"), ImmutableList.of());
    private Application app4 =
            new DefaultApplication(id4, VER,
                                   "app4", "origin4", ApplicationRole.ADMIN, ImmutableSet.of(), Optional.of(FURL),
                                   ImmutableList.of("My Feature"), ImmutableList.of());

    /**
     * Hamcrest matcher to check that an application representation in JSON matches
     * the actual device.
     */
    private static class AppJsonMatcher extends TypeSafeMatcher<JsonObject> {
        private final Application app;
        private String reason = "";

        public AppJsonMatcher(Application appValue) {
            app = appValue;
        }

        @Override
        public boolean matchesSafely(JsonObject jsonApp) {
            // check id
            short jsonId = (short) jsonApp.get("id").asInt();
            if (jsonId != app.id().id()) {
                reason = "id " + app.id().id();
                return false;
            }

            // check name
            String jsonName = jsonApp.get("name").asString();
            if (!jsonName.equals(app.id().name())) {
                reason = "name " + app.id().name();
                return false;
            }

            // check origin
            String jsonOrigin = jsonApp.get("origin").asString();
            if (!jsonOrigin.equals(app.origin())) {
                reason = "manufacturer " + app.origin();
                return false;
            }

            return true;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(reason);
        }
    }

    /**
     * Factory to allocate an application matcher.
     *
     * @param app application object we are looking for
     * @return matcher
     */
    private static AppJsonMatcher matchesApp(Application app) {
        return new AppJsonMatcher(app);
    }

    /**
     * Initializes test mocks and environment.
     */
    @Before
    public void setUpMocks() {
        service = createMock(ApplicationAdminService.class);

        expect(service.getId("one"))
                .andReturn(id1)
                .anyTimes();
        expect(service.getId("two"))
                .andReturn(id2)
                .anyTimes();
        expect(service.getId("three"))
                .andReturn(id3)
                .anyTimes();
        expect(service.getId("four"))
                .andReturn(id4)
                .anyTimes();

        expect(service.getApplication(id3))
                .andReturn(app3)
                .anyTimes();
        expect(service.getState(isA(ApplicationId.class)))
                .andReturn(ApplicationState.ACTIVE)
                .anyTimes();

        // Register the services needed for the test
        CodecManager codecService = new CodecManager();
        codecService.activate();
        ServiceDirectory testDirectory =
                new TestServiceDirectory()
                        .add(ApplicationAdminService.class, service)
                        .add(ApplicationService.class, service)
                        .add(CodecService.class, codecService);

        BaseResource.setServiceDirectory(testDirectory);
    }

    /**
     * Verifies test mocks.
     */
    @After
    public void tearDownMocks() {
        verify(service);
    }

    /**
     * Tests a GET of all applications when no applications are present.
     */
    @Test
    public void getAllApplicationsEmpty() {
        expect(service.getApplications())
                .andReturn(ImmutableSet.of());
        replay(service);

        WebResource rs = resource();
        String response = rs.path("applications").get(String.class);
        assertThat(response, is("{\"applications\":[]}"));
    }

    /**
     * Tests a GET of all applications with data.
     */
    @Test
    public void getAllApplicationsPopulated() {
        expect(service.getApplications())
                .andReturn(ImmutableSet.of(app1, app2, app3, app4));
        replay(service);

        WebResource rs = resource();
        String response = rs.path("applications").get(String.class);
        assertThat(response, containsString("{\"applications\":["));

        JsonObject result = JsonObject.readFrom(response);
        assertThat(result, notNullValue());

        assertThat(result.names(), hasSize(1));
        assertThat(result.names().get(0), is("applications"));

        JsonArray jsonApps = result.get("applications").asArray();
        assertThat(jsonApps, notNullValue());
        assertThat(jsonApps.size(), is(4));

        assertThat(jsonApps.get(0).asObject(), matchesApp(app1));
        assertThat(jsonApps.get(1).asObject(), matchesApp(app2));
        assertThat(jsonApps.get(2).asObject(), matchesApp(app3));
        assertThat(jsonApps.get(3).asObject(), matchesApp(app4));
    }

    /**
     * Tests a GET of a single application.
     */
    @Test
    public void getSingleApplication() {
        replay(service);

        WebResource rs = resource();
        String response = rs.path("applications/three").get(String.class);

        JsonObject result = JsonObject.readFrom(response);
        assertThat(result, notNullValue());

        assertThat(result, matchesApp(app3));
    }

    /**
     * Tests a DELETE of a single application - this should
     * attempt to uninstall it.
     */
    @Test
    public void deleteApplication() {
        service.uninstall(id3);
        expectLastCall();

        replay(service);

        WebResource rs = resource();
        rs.path("applications/three").delete();
    }

    /**
     * Tests a DELETE of a single active application - this should
     * attempt to uninstall it.
     */
    @Test
    public void deleteActiveApplication() {
        service.deactivate(id3);
        expectLastCall();

        replay(service);

        WebResource rs = resource();
        rs.path("applications/three/active").delete();
    }

    /**
     * Tests a POST operation to the "active" URL.  This should attempt to
     * activate the application.
     */
    @Test
    public void postActiveApplication() {
        service.activate(id3);
        expectLastCall();

        replay(service);

        WebResource rs = resource();
        rs.path("applications/three/active").post();
    }

    /**
     * Tests a POST operation.  This should attempt to
     * install the application.
     */
    @Test
    public void postApplication() {
        expect(service.install(isA(InputStream.class)))
                .andReturn(app4)
                .once();

        replay(service);

        ApplicationCodec codec = new ApplicationCodec();
        String app4Json = codec.encode(app4,
                                       new MockCodecContextWithService(service))
                .asText();

        WebResource rs = resource();
        String response = rs.path("applications").post(String.class, app4Json);

        JsonObject result = JsonObject.readFrom(response);
        assertThat(result, notNullValue());

        assertThat(result, matchesApp(app4));
    }
}
