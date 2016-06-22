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
package org.onosproject.xosclient.impl;

import org.glassfish.jersey.client.ClientProperties;
import org.onosproject.xosclient.api.XosAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import static com.google.common.net.MediaType.JSON_UTF_8;
import static java.net.HttpURLConnection.HTTP_OK;

/**
 * XOS common REST API implementation.
 */
public class XosApi {

    private final Logger log = LoggerFactory.getLogger(getClass());

    protected static final String EMPTY_STRING = "";
    protected static final String EMPTY_JSON_STRING = "{}";

    protected final String baseUrl;
    protected final XosAccess access;
    protected final Client client;

    private static final int DEFAULT_TIMEOUT_MS = 2000;

    /**
     * Default constructor.
     *
     * @param baseUrl base url of this api
     * @param xosAccess xos access
     */
    public XosApi(String baseUrl, XosAccess xosAccess) {
        this.baseUrl = baseUrl;
        this.access = xosAccess;
        this.client = ClientBuilder.newClient();

        client.property(ClientProperties.CONNECT_TIMEOUT, DEFAULT_TIMEOUT_MS);
        client.property(ClientProperties.READ_TIMEOUT, DEFAULT_TIMEOUT_MS);
    }

    /**
     * Returns the access of this api.
     *
     * @return xos access
     */
    public XosAccess access() {
        return this.access;
    }

    /**
     * Returns the base url of this api.
     *
     * @return base url
     */
    public String baseUrl() {
        return this.baseUrl;
    }

    /**
     * Returns response of the REST get operation with a given additional path.
     *
     * @param path path or null
     * @return response json string
     */
    public String restGet(String path) {
        WebTarget wt = client.target(access.endpoint() + baseUrl).path(path);
        Invocation.Builder builder = wt.request(JSON_UTF_8.toString());
        try {
            Response response = builder.get();
            if (response.getStatus() != HTTP_OK) {
                log.warn("Failed to get resource {}", access.endpoint() + baseUrl + path);
                return EMPTY_JSON_STRING;
            }
        } catch (javax.ws.rs.ProcessingException e) {
            return EMPTY_JSON_STRING;
        }
        return builder.get(String.class);
    }
}
