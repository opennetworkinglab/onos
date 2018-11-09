/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.incubator.net.virtual.rest;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.google.common.collect.ImmutableSet;
import org.glassfish.jersey.client.ClientProperties;
import org.hamcrest.Description;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onosproject.codec.CodecService;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.net.TenantId;
import org.onosproject.incubator.net.virtual.VirtualNetworkAdminService;
import org.onosproject.rest.resources.ResourceTest;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.HashSet;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Unit tests for tenant REST APIs.
 */
@Ignore
public class TenantWebResourceTest extends ResourceTest {

    private final VirtualNetworkAdminService mockVnetAdminService = createMock(VirtualNetworkAdminService.class);

    final HashSet<TenantId> tenantIdSet = new HashSet<>();

    private static final String ID = "id";

    private final TenantId tenantId1 = TenantId.tenantId("TenantId1");
    private final TenantId tenantId2 = TenantId.tenantId("TenantId2");
    private final TenantId tenantId3 = TenantId.tenantId("TenantId3");
    private final TenantId tenantId4 = TenantId.tenantId("TenantId4");

    /**
     * Sets up the global values for all the tests.
     */
    @Before
    public void setUpTest() {
        // Register the services needed for the test
        CodecManager codecService = new CodecManager();
        codecService.activate();
        ServiceDirectory testDirectory =
                new TestServiceDirectory()
                        .add(VirtualNetworkAdminService.class, mockVnetAdminService)
                        .add(CodecService.class, codecService);

        setServiceDirectory(testDirectory);
    }

    /**
     * Hamcrest matcher to check that a tenant id representation in JSON matches
     * the actual tenant id.
     */
    public static class TenantIdJsonMatcher extends TypeSafeMatcher<JsonObject> {
        private final TenantId tenantId;
        private String reason = "";

        public TenantIdJsonMatcher(TenantId tenantIdValue) {
            tenantId = tenantIdValue;
        }

        @Override
        public boolean matchesSafely(JsonObject jsonHost) {
            // Check the tenant id
            final String jsonId = jsonHost.get(ID).asString();
            if (!jsonId.equals(tenantId.id())) {
                reason = ID + " " + tenantId.id();
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
     * Factory to allocate a tenant id array matcher.
     *
     * @param tenantId tenant id object we are looking for
     * @return matcher
     */
    private static TenantIdJsonMatcher matchesTenantId(TenantId tenantId) {
        return new TenantIdJsonMatcher(tenantId);
    }

    /**
     * Hamcrest matcher to check that a tenant id is represented properly in a JSON
     * array of tenant ids.
     */
    public static class TenantIdJsonArrayMatcher extends TypeSafeMatcher<JsonArray> {
        private final TenantId tenantId;
        private String reason = "";

        public TenantIdJsonArrayMatcher(TenantId tenantIdValue) {
            tenantId = tenantIdValue;
        }

        @Override
        public boolean matchesSafely(JsonArray json) {
            boolean tenantIdFound = false;
            final int expectedAttributes = 1;
            for (int tenantIdIndex = 0; tenantIdIndex < json.size();
                 tenantIdIndex++) {

                final JsonObject jsonHost = json.get(tenantIdIndex).asObject();

                // Only 1 attribute - ID.
                if (jsonHost.names().size() < expectedAttributes) {
                    reason = "Found a tenant id with the wrong number of attributes";
                    return false;
                }

                final String jsonDeviceKeyId = jsonHost.get(ID).asString();
                if (jsonDeviceKeyId.equals(tenantId.id())) {
                    tenantIdFound = true;

                    //  We found the correct tenant id, check the tenant id attribute values
                    assertThat(jsonHost, matchesTenantId(tenantId));
                }
            }
            if (!tenantIdFound) {
                reason = "Tenant id " + tenantId.id() + " was not found";
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
     * Factory to allocate a tenant id array matcher.
     *
     * @param tenantId tenant id object we are looking for
     * @return matcher
     */
    private static TenantIdJsonArrayMatcher hasTenantId(TenantId tenantId) {
        return new TenantIdJsonArrayMatcher(tenantId);
    }

    /**
     * Tests the result of the REST API GET when there are no tenant ids.
     */
    @Test
    public void testGetTenantsEmptyArray() {
        expect(mockVnetAdminService.getTenantIds()).andReturn(ImmutableSet.of()).anyTimes();
        replay(mockVnetAdminService);

        WebTarget wt = target();
        String response = wt.path("tenants").request().get(String.class);
        assertThat(response, is("{\"tenants\":[]}"));

        verify(mockVnetAdminService);
    }

    /**
     * Tests the result of the REST API GET when tenant ids are defined.
     */
    @Test
    public void testGetTenantIdsArray() {
        tenantIdSet.add(tenantId1);
        tenantIdSet.add(tenantId2);
        tenantIdSet.add(tenantId3);
        tenantIdSet.add(tenantId4);
        expect(mockVnetAdminService.getTenantIds()).andReturn(tenantIdSet).anyTimes();
        replay(mockVnetAdminService);

        WebTarget wt = target();
        String response = wt.path("tenants").request().get(String.class);
        assertThat(response, containsString("{\"tenants\":["));

        final JsonObject result = Json.parse(response).asObject();
        assertThat(result, notNullValue());

        assertThat(result.names(), hasSize(1));
        assertThat(result.names().get(0), is("tenants"));

        final JsonArray tenantIds = result.get("tenants").asArray();
        assertThat(tenantIds, notNullValue());
        assertEquals("Device keys array is not the correct size.",
                     tenantIdSet.size(), tenantIds.size());

        tenantIdSet.forEach(tenantId -> assertThat(tenantIds, hasTenantId(tenantId)));

        verify(mockVnetAdminService);
    }

    /**
     * Tests adding of new tenant id using POST via JSON stream.
     */
    @Test
    public void testPost() {
        mockVnetAdminService.registerTenantId(anyObject());
        tenantIdSet.add(tenantId2);
        expect(mockVnetAdminService.getTenantIds()).andReturn(tenantIdSet).anyTimes();
        expectLastCall();

        replay(mockVnetAdminService);

        WebTarget wt = target();
        InputStream jsonStream = TenantWebResourceTest.class
                .getResourceAsStream("post-tenant.json");

        Response response = wt.path("tenants").request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(jsonStream));
        assertThat(response.getStatus(), is(HttpURLConnection.HTTP_CREATED));

        String location = response.getLocation().getPath();
        assertThat(location, Matchers.startsWith("/tenants/" + tenantId2));

        verify(mockVnetAdminService);
    }

    /**
     * Tests adding of a null tenant id using POST via JSON stream.
     */
    @Test
    public void testPostNullTenantId() {

        replay(mockVnetAdminService);

        WebTarget wt = target();
        try {
            String response = wt.path("tenants")
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .post(Entity.json(null), String.class);
            fail("POST of null tenant id did not throw an exception");
        } catch (BadRequestException ex) {
            assertThat(ex.getMessage(), containsString("HTTP 400 Bad Request"));
        }

        verify(mockVnetAdminService);
    }

    /**
     * Tests removing a tenant id with DELETE request.
     */
    @Test
    public void testDelete() {
        expect(mockVnetAdminService.getTenantIds())
                .andReturn(ImmutableSet.of(tenantId2)).anyTimes();
        mockVnetAdminService.unregisterTenantId(anyObject());
        expectLastCall();
        replay(mockVnetAdminService);

        WebTarget wt = target()
                .property(ClientProperties.SUPPRESS_HTTP_COMPLIANCE_VALIDATION, true);
        Response response = wt.path("tenants/" + tenantId2)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .delete();

        assertThat(response.getStatus(), is(HttpURLConnection.HTTP_NO_CONTENT));

        verify(mockVnetAdminService);
    }

    /**
     * Tests that a DELETE of a non-existent tenant id throws an exception.
     */
    @Test
    public void testDeleteNonExistentDeviceKey() {
        expect(mockVnetAdminService.getTenantIds())
                .andReturn(ImmutableSet.of())
                .anyTimes();
        expectLastCall();

        replay(mockVnetAdminService);

        WebTarget wt = target();

        try {
            wt.path("tenants/" + "NON_EXISTENT_TENANT_ID")
                    .request()
                    .delete(String.class);
            fail("Delete of a non-existent tenant did not throw an exception");
        } catch (NotFoundException ex) {
            assertThat(ex.getMessage(), containsString("HTTP 404 Not Found"));
        }

        verify(mockVnetAdminService);
    }
}
