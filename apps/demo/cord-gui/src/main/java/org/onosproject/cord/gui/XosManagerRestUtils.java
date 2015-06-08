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
 *
 */

package org.onosproject.cord.gui;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import org.slf4j.Logger;

import static com.google.common.net.MediaType.JSON_UTF_8;
import static java.net.HttpURLConnection.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Utility RESTful methods for dealing with the XOS server.
 */
public class XosManagerRestUtils {
    private static final String XOSLIB = "/xoslib";
    private static final String AUTH_USER = "padmin@vicci.org";
    private static final String AUTH_PASS = "letmein";

    private static final String UTF_8 = JSON_UTF_8.toString();

    private final Logger log = getLogger(getClass());

    private final String xosServerAddress;
    private final int xosServerPort;
    private final String baseUri;


    /**
     * Constructs a utility class, using the supplied server address and port,
     * using the given base URI.
     * <p>
     * Note that the uri should start and end with a slash; for example:
     * {@code "/volttenant/"}. This example would result in URIs of the form:
     * <pre>
     *     "http://[server]:[port]/xoslib/volttenant/"
     * </pre>
     *
     * @param xosServerAddress server IP address
     * @param xosServerPort server port
     * @param baseUri base URI
     */
    public XosManagerRestUtils(String xosServerAddress, int xosServerPort,
                               String baseUri) {
        this.xosServerAddress = xosServerAddress;
        this.xosServerPort = xosServerPort;
        this.baseUri = baseUri;
        log.info("XMRU:: {}:{}{}", xosServerAddress, xosServerPort, baseUri);
    }

    // build the base URL from the pieces we know...
    private String baseUrl() {
        return "http://" + xosServerAddress + ":" +
                Integer.toString(xosServerPort) + XOSLIB + baseUri;
    }

    /**
     * Gets a client web resource builder for the base XOS REST API
     * with no additional URI.
     *
     * @return web resource builder
     */
    public WebResource.Builder getClientBuilder() {
        return getClientBuilder("");
    }

    /**
     * Gets a client web resource builder for the base XOS REST API
     * with an optional additional URI.
     *
     * @param uri URI suffix to append to base URI
     * @return web resource builder
     */
    public WebResource.Builder getClientBuilder(String uri) {
        Client client = Client.create();
        client.addFilter(new HTTPBasicAuthFilter(AUTH_USER, AUTH_PASS));
        WebResource resource = client.resource(baseUrl() + uri);
        log.info("XOS REST CALL>> {}", resource);
        return resource.accept(UTF_8).type(UTF_8);
    }

    /**
     * Performs a REST GET operation on the base XOS REST URI.
     *
     * @return JSON string fetched by the GET operation
     */
    public String getRest() {
        return getRest("");
    }

    /**
     * Performs a REST GET operation on the base XOS REST URI with
     * an optional additional URI.
     *
     * @param uri URI suffix to append to base URI
     * @return JSON string fetched by the GET operation
     */
    public String getRest(String uri) {
        WebResource.Builder builder = getClientBuilder(uri);
        ClientResponse response = builder.get(ClientResponse.class);

        if (response.getStatus() != HTTP_OK) {
            log.info("REST GET request returned error code {}",
                     response.getStatus());
        }
        return response.getEntity(String.class);
    }

    /**
     * Performs a REST PUT operation on the base XOS REST URI.
     *
     * @return JSON string returned by the PUT operation
     */
    public String putRest() {
        return putRest("");
    }

    /**
     * Performs a REST PUT operation on the base XOS REST URI with
     * an optional additional URI.
     *
     * @param uri URI suffix to append to base URI
     * @return JSON string returned by the PUT operation
     */
    public String putRest(String uri) {
        WebResource.Builder builder = getClientBuilder(uri);
        ClientResponse response;

        try {
            response = builder.put(ClientResponse.class);
        } catch (ClientHandlerException e) {
            log.warn("Unable to contact REST server: {}", e.getMessage());
            return "";
        }

        if (response.getStatus() != HTTP_OK) {
            log.info("REST PUT request returned error code {}",
                     response.getStatus());
        }
        return response.getEntity(String.class);
    }

    /**
     * Performs a REST POST operation of a json string on the base
     * XOS REST URI with an optional additional URI.
     *
     * @param json JSON string to post
     */
    public void postRest(String json) {
        postRest("", json);
    }

    /**
     * Performs a REST POST operation of a json string on the base
     * XOS REST URI with an optional additional URI suffix.
     *
     * @param uri URI suffix to append to base URI
     * @param json JSON string to post
     */
    public void postRest(String uri, String json) {
        WebResource.Builder builder = getClientBuilder(uri);
        ClientResponse response;

        try {
            response = builder.post(ClientResponse.class, json);
        } catch (ClientHandlerException e) {
            log.warn("Unable to contact REST server: {}", e.getMessage());
            return;
        }

        if (response.getStatus() != HTTP_CREATED) {
            log.info("REST POST request returned error code {}",
                     response.getStatus());
        }
    }

    /**
     * Performs a REST DELETE operation on the base
     * XOS REST URI with an optional additional URI.
     *
     * @param uri URI suffix to append to base URI
     */
    public void deleteRest(String uri) {
        WebResource.Builder builder = getClientBuilder(uri);
        ClientResponse response = builder.delete(ClientResponse.class);

        if (response.getStatus() != HTTP_NO_CONTENT) {
            log.info("REST DELETE request returned error code {}",
                     response.getStatus());
        }
    }

}
