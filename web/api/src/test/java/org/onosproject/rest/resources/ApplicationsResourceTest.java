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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
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
import org.onosproject.core.CoreService;
import org.onosproject.core.DefaultApplication;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.core.Version;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import java.io.InputStream;
import java.net.URI;
import java.util.Optional;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit tests for applications REST APIs.
 */

public class ApplicationsResourceTest extends ResourceTest {

    private static class MockCodecContextWithAppService extends MockCodecContext {
        private ApplicationAdminService appService;

        MockCodecContextWithAppService(ApplicationAdminService appService) {
            this.appService = appService;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getService(Class<T> serviceClass) {
            return (T) appService;
        }
    }

    private static class MockCodecContextWithCoreService extends MockCodecContext {
        private CoreService coreService;

        MockCodecContextWithCoreService(CoreService coreService) {
            this.coreService = coreService;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getService(Class<T> serviceClass) {
            return (T) coreService;
        }
    }

    private ApplicationAdminService appService;
    private CoreService coreService;
    private ApplicationId id1 = new DefaultApplicationId(1, "app1");
    private ApplicationId id2 = new DefaultApplicationId(2, "app2");
    private ApplicationId id3 = new DefaultApplicationId(3, "app3");
    private ApplicationId id4 = new DefaultApplicationId(4, "app4");

    private static final URI FURL = URI.create("mvn:org.foo-features/1.2a/xml/features");
    private static final Version VER = Version.version(1, 2, "a", null);

    private DefaultApplication.Builder baseBuilder = DefaultApplication.builder()
                .withVersion(VER)
                .withIcon(new byte[0])
                .withRole(ApplicationRole.ADMIN)
                .withPermissions(ImmutableSet.of())
                .withFeaturesRepo(Optional.of(FURL))
                .withFeatures(ImmutableList.of("My Feature"))
                .withRequiredApps(ImmutableList.of());

    private Application app1 =
            DefaultApplication.builder(baseBuilder)
                .withAppId(id1)
                .withTitle("title1")
                .withDescription("desc1")
                .withOrigin("origin1")
                .withCategory("category1")
                .withUrl("url1")
                .withReadme("readme1")
                .build();
    private Application app2 =
            DefaultApplication.builder(baseBuilder)
                    .withAppId(id2)
                    .withTitle("title2")
                    .withDescription("desc2")
                    .withOrigin("origin2")
                    .withCategory("category2")
                    .withUrl("url2")
                    .withReadme("readme2")
                    .build();
    private Application app3 =
            DefaultApplication.builder(baseBuilder)
                    .withAppId(id3)
                    .withTitle("title3")
                    .withDescription("desc3")
                    .withOrigin("origin3")
                    .withCategory("category3")
                    .withUrl("url3")
                    .withReadme("readme3")
                    .build();
    private Application app4 =
            DefaultApplication.builder(baseBuilder)
                    .withAppId(id4)
                    .withTitle("title4")
                    .withDescription("desc4")
                    .withOrigin("origin4")
                    .withCategory("category4")
                    .withUrl("url4")
                    .withReadme("readme4")
                    .build();

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
     * Hamcrest matcher to check that an application id representation in JSON.
     */
    private static final class AppIdJsonMatcher extends TypeSafeMatcher<JsonObject> {
        private final ApplicationId appId;
        private String reason = "";

        private AppIdJsonMatcher(ApplicationId appId) {
            this.appId = appId;
        }

        @Override
        protected boolean matchesSafely(JsonObject jsonAppId) {
            // check id
            short jsonId = (short) jsonAppId.get("id").asInt();
            if (jsonId != appId.id()) {
                reason = "id " + appId.id();
                return false;
            }

            // check name
            String jsonName = jsonAppId.get("name").asString();
            if (!jsonName.equals(appId.name())) {
                reason = "name " + appId.name();
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
     * Factory to allocate an application Id matcher.
     *
     * @param appId application Id object we are looking for
     * @return matcher
     */
    private static AppIdJsonMatcher matchesAppId(ApplicationId appId) {
        return new AppIdJsonMatcher(appId);
    }

    /**
     * Initializes test mocks and environment.
     */
    @Before
    public void setUpMocks() {
        appService = createMock(ApplicationAdminService.class);
        coreService = createMock(CoreService.class);

        expect(appService.getId("one"))
                .andReturn(id1)
                .anyTimes();
        expect(appService.getId("two"))
                .andReturn(id2)
                .anyTimes();
        expect(appService.getId("three"))
                .andReturn(id3)
                .anyTimes();
        expect(appService.getId("four"))
                .andReturn(id4)
                .anyTimes();

        expect(appService.getApplication(id3))
                .andReturn(app3)
                .anyTimes();
        expect(appService.getState(isA(ApplicationId.class)))
                .andReturn(ApplicationState.ACTIVE)
                .anyTimes();

        // Register the services needed for the test
        CodecManager codecService = new CodecManager();
        codecService.activate();
        ServiceDirectory testDirectory =
                new TestServiceDirectory()
                        .add(ApplicationAdminService.class, appService)
                        .add(ApplicationService.class, appService)
                        .add(CoreService.class, coreService)
                        .add(CodecService.class, codecService);

        setServiceDirectory(testDirectory);
    }

    /**
     * Tests a GET of all applications when no applications are present.
     */
    @Test
    public void getAllApplicationsEmpty() {
        expect(appService.getApplications())
                .andReturn(ImmutableSet.of());
        replay(appService);

        WebTarget wt = target();
        String response = wt.path("applications").request().get(String.class);
        assertThat(response, is("{\"applications\":[]}"));

        verify(appService);
    }

    /**
     * Tests a GET of all applications with data.
     */
    @Test
    public void getAllApplicationsPopulated() {
        expect(appService.getApplications())
                .andReturn(ImmutableSet.of(app1, app2, app3, app4));
        replay(appService);

        WebTarget wt = target();
        String response = wt.path("applications").request().get(String.class);
        assertThat(response, containsString("{\"applications\":["));

        JsonObject result = Json.parse(response).asObject();
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

        verify(appService);
    }

    /**
     * Tests a GET of a single application.
     */
    @Test
    public void getSingleApplication() {
        replay(appService);

        WebTarget wt = target();
        String response = wt.path("applications/three").request().get(String.class);

        JsonObject result = Json.parse(response).asObject();
        assertThat(result, notNullValue());

        assertThat(result, matchesApp(app3));

        verify(appService);
    }

    /**
     * Tests a DELETE of a single application - this should
     * attempt to uninstall it.
     */
    @Test
    public void deleteApplication() {
        appService.uninstall(id3);
        expectLastCall();

        replay(appService);

        WebTarget wt = target();
        wt.path("applications/three").request().delete();

        verify(appService);
    }

    /**
     * Tests a DELETE of a single active application - this should
     * attempt to uninstall it.
     */
    @Test
    public void deleteActiveApplication() {
        appService.deactivate(id3);
        expectLastCall();

        replay(appService);

        WebTarget wt = target();
        wt.path("applications/three/active").request().delete();

        verify(appService);
    }

    /**
     * Tests a POST operation to the "active" URL.  This should attempt to
     * activate the application.
     */
    @Test
    public void postActiveApplication() {
        appService.activate(id3);
        expectLastCall();

        replay(appService);

        WebTarget wt = target();
        wt.path("applications/three/active").request().post(null);

        verify(appService);
    }

    /**
     * Tests a POST operation.  This should attempt to
     * install the application.
     */
    @Test
    public void postApplication() {
        expect(appService.install(isA(InputStream.class)))
                .andReturn(app4)
                .once();

        replay(appService);

        ApplicationCodec codec = new ApplicationCodec();
        String app4Json = codec.encode(app4,
                new MockCodecContextWithAppService(appService))
                .asText();

        WebTarget wt = target();
        String response = wt.path("applications").request().post(
                Entity.entity(app4Json, MediaType.APPLICATION_OCTET_STREAM), String.class);

        JsonObject result = Json.parse(response).asObject();
        assertThat(result, notNullValue());

        assertThat(result, matchesApp(app4));

        verify(appService);
    }

    /**
     * Tests a POST operation. This should attempt to register an on/off platform
     * application ID.
     */
    @Test
    public void postRegisterAppId() {
        expect(coreService.registerApplication("app1")).andReturn(id1).anyTimes();
        replay(coreService);

        WebTarget wt = target();
        wt.path("applications/app1/register").request().post(null);

        verify(coreService);
    }

    /**
     * Tests a GET of all application Ids when no applications are present.
     */
    @Test
    public void getAllApplicationIdsEmpty() {
        expect(coreService.getAppIds()).andReturn(ImmutableSet.of());
        replay(coreService);

        WebTarget wt = target();
        String response = wt.path("applications/ids").request().get(String.class);
        assertThat(response, is("{\"applicationIds\":[]}"));

        verify(coreService);
    }

    /**
     * Tests a GET of all application Ids.
     */
    @Test
    public void getAllApplicationIdsPopulated() {
        expect(coreService.getAppIds())
                .andReturn(ImmutableSet.of(id1, id2, id3, id4));
        replay(coreService);

        WebTarget wt = target();
        String response = wt.path("applications/ids").request().get(String.class);

        assertThat(response, containsString("{\"applicationIds\":["));

        JsonObject result = Json.parse(response).asObject();
        assertThat(result, notNullValue());

        assertThat(result.names(), hasSize(1));
        assertThat(result.names().get(0), is("applicationIds"));

        JsonArray jsonApps = result.get("applicationIds").asArray();
        assertThat(jsonApps, notNullValue());
        assertThat(jsonApps.size(), is(4));

        assertThat(jsonApps.get(0).asObject(), matchesAppId(id1));
        assertThat(jsonApps.get(1).asObject(), matchesAppId(id2));
        assertThat(jsonApps.get(2).asObject(), matchesAppId(id3));
        assertThat(jsonApps.get(3).asObject(), matchesAppId(id4));

        verify(coreService);
    }

    /**
     * Tests a GET of an applicationId entry with the given numeric id.
     */
    @Test
    public void getAppIdByShortId() {
        expect(coreService.getAppId((short) 1)).andReturn(id1);
        replay(coreService);

        WebTarget wt = target();
        String response = wt.path("applications/ids/entry")
                .queryParam("id", 1).request().get(String.class);

        JsonObject result = Json.parse(response).asObject();
        assertThat(result, notNullValue());

        assertThat(result, matchesAppId(id1));

        verify(coreService);
    }

    /**
     * Tests a GET of an applicationId entry with the given application name.
     */
    @Test
    public void getAppIdByName() {
        expect(coreService.getAppId("app2")).andReturn(id2);
        replay(coreService);

        WebTarget wt = target();
        String response = wt.path("applications/ids/entry")
                .queryParam("name", "app2").request().get(String.class);

        JsonObject result = Json.parse(response).asObject();
        assertThat(result, notNullValue());

        assertThat(result, matchesAppId(id2));

        verify(coreService);
    }

    /**
     * Tests a GET of an applicationId without specifying any parameters.
     */
    @Test
    public void getAppWithNoParam() {
        WebTarget wt = target();

        try {
            wt.path("applications/ids/entry").request().get();
        } catch (NotFoundException ex) {
            Assert.assertThat(ex.getMessage(),
                    containsString("HTTP 404 Not Found"));
        }
    }
}
