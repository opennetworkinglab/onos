/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.xosintegration;

import java.util.Dictionary;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.net.MediaType.JSON_UTF_8;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.slf4j.LoggerFactory.getLogger;


/**
 * XOS interface application.
 */
@Component(immediate = true)
@Service
public class OnosXOSIntegrationManager implements VoltTenantService {

    private static final String TEST_XOS_SERVER_ADDRESS = "10.254.1.22";
    private static final int TEST_XOS_SERVER_PORT = 8000;
    private static final String XOS_TENANT_BASE_URI = "/xoslib/volttenant/";

    private final Logger log = getLogger(getClass());
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService cfgService;
    @Property(name = "XOSServerAddress",
              value = TEST_XOS_SERVER_ADDRESS,
              label = "XOS Server address")
    protected String xosServerAddress = TEST_XOS_SERVER_ADDRESS;
    @Property(name = "XOSServerPort",
              intValue = TEST_XOS_SERVER_PORT,
              label = "XOS Server port")
    protected int xosServerPort = TEST_XOS_SERVER_PORT;
    private ApplicationId appId;

    @Activate
    public void activate(ComponentContext context) {
        log.info("XOS app is starting");
        cfgService.registerProperties(getClass());
        appId = coreService.registerApplication("org.onosproject.xosintegration");
        readComponentConfiguration(context);

        log.info("XOS({}) started", appId.id());
    }

    @Deactivate
    public void deactivate() {
        cfgService.unregisterProperties(getClass(), false);
        log.info("XOS({}) stopped", appId.id());
    }

    @Modified
    public void modified(ComponentContext context) {
        readComponentConfiguration(context);
    }

    /**
     * Converts a JSON representation of a tenant into a tenant object.
     *
     * @param jsonTenant JSON object representing the tenant
     * @return volt tenant object
     */
    private VoltTenant jsonToTenant(JsonObject jsonTenant) {
        return VoltTenant.builder()
                .withHumanReadableName(jsonTenant.get("humanReadableName").asString())
                .withId(jsonTenant.get("id").asInt())
                .withProviderService(jsonTenant.get("provider_service").asInt())
                .withServiceSpecificId(jsonTenant.get("service_specific_id").asString())
                .withVlanId(jsonTenant.get("vlan_id").asString())
                .build();
    }

    /**
     * Converts a tenant object into a JSON string.
     *
     * @param tenant volt tenant object to convert
     * @return JSON string for the tenant
     */
    private String tenantToJson(VoltTenant tenant) {
        return "{"
                    + "\"humanReadableName\": \"" + tenant.humanReadableName() + "\","
                    + "\"id\": \"" + tenant.id() + "\","
                    + "\"provider_service\": \"" + tenant.providerService() + "\","
                    + "\"service_specific_id\": \"" + tenant.serviceSpecificId() + "\","
                    + "\"vlan_id\": \"" + tenant.vlanId() + "\""
                    + "}";
    }

    /**
     * Gets a client web resource builder for the base XOS REST API
     * with no additional URI.
     *
     * @return web resource builder
     */
    private WebResource.Builder getClientBuilder() {
        return getClientBuilder("");
    }

    /**
     * Gets a client web resource builder for the base XOS REST API
     * with an optional additional URI.
     *
     * @return web resource builder
     */
    private WebResource.Builder getClientBuilder(String uri) {
        String baseUrl = "http://" + xosServerAddress + ":"
                + Integer.toString(xosServerPort);
        Client client = Client.create();
        client.addFilter(new HTTPBasicAuthFilter("padmin@vicci.org", "letmein"));
        WebResource resource = client.resource(baseUrl
                + XOS_TENANT_BASE_URI + uri);
        return resource.accept(JSON_UTF_8.toString())
                .type(JSON_UTF_8.toString());
    }

    /**
     * Performs a REST GET operation on the base XOS REST URI.
     *
     * @return JSON string fetched by the GET operation
     */
    private String getRest() {
        return getRest("");
    }

    /**
     * Performs a REST GET operation on the base XOS REST URI with
     * an optional additional URI.
     *
     * @return JSON string fetched by the GET operation
     */
    private String getRest(String uri) {
        WebResource.Builder builder = getClientBuilder(uri);
        ClientResponse response = builder.get(ClientResponse.class);

        if (response.getStatus() != HTTP_OK) {
            log.info("REST GET request returned error code {}",
                    response.getStatus());
        }
        String jsonString = response.getEntity(String.class);
        log.info("JSON read:\n{}", jsonString);

        return jsonString;
    }

    /**
     * Performs a REST POST operation of a json string on the base
     * XOS REST URI with an optional additional URI.
     *
     * @param json JSON string to post
     */
    private void postRest(String json) {
        WebResource.Builder builder = getClientBuilder();
        ClientResponse response = builder.post(ClientResponse.class, json);

        if (response.getStatus() != HTTP_CREATED) {
            log.info("REST POST request returned error code {}",
                    response.getStatus());
        }
    }

    /**
     * Performs a REST DELETE operation on the base
     * XOS REST URI with an optional additional URI.
     *
     * @param uri optional additional URI
     */
    private void deleteRest(String uri) {
        WebResource.Builder builder = getClientBuilder(uri);
        ClientResponse response = builder.delete(ClientResponse.class);

        if (response.getStatus() != HTTP_NO_CONTENT) {
            log.info("REST DELETE request returned error code {}",
                    response.getStatus());
        }
    }

    /**
     * Deletes the tenant with the given ID.
     *
     * @param tenantId ID of tenant to delete
     */
    private void deleteTenant(long tenantId) {
        deleteRest(Long.toString(tenantId));
    }

    @Override
    public Set<VoltTenant> getAllTenants() {
        String jsonString = getRest();

        JsonArray voltTenantItems = JsonArray.readFrom(jsonString);

        return IntStream.range(0, voltTenantItems.size())
                .mapToObj(index -> jsonToTenant(voltTenantItems.get(index).asObject()))
                .collect(Collectors.toSet());
    }

    @Override
    public void removeTenant(long id) {
        deleteTenant(id);
    }

    @Override
    public VoltTenant addTenant(VoltTenant newTenant) {
        String json = tenantToJson(newTenant);
        postRest(json);
        return newTenant;
    }

    @Override
    public VoltTenant getTenant(long id) {
        String jsonString = getRest(Long.toString(id));
        JsonObject jsonTenant = JsonObject.readFrom(jsonString);
        if (jsonTenant.get("id") != null) {
            return jsonToTenant(jsonTenant);
        } else {
            return null;
        }
    }

    /**
     * Extracts properties from the component configuration context.
     *
     * @param context the component context
     */
    private void readComponentConfiguration(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();

        String newXosServerAddress = Tools.get(properties, "XOSServerAddress");
        if (!isNullOrEmpty(newXosServerAddress)) {
            xosServerAddress = newXosServerAddress;
        }

        String newXosServerPortString = Tools.get(properties, "XOSServerPort");
        if (!isNullOrEmpty(newXosServerPortString)) {
            xosServerPort = Integer.parseInt(newXosServerPortString);
        }
        log.info("XOS URL is now http://{}:{}", xosServerAddress, xosServerPort);
    }
}


