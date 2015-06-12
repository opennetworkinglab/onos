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
package org.onosproject.virtualbng;

import static com.google.common.net.MediaType.JSON_UTF_8;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.slf4j.LoggerFactory.getLogger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import java.io.IOException;

import org.onlab.packet.IpAddress;
import org.slf4j.Logger;

public class RestClient {
    private final Logger log = getLogger(getClass());
    private static final String UTF_8 = JSON_UTF_8.toString();
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final String url;

    /**
     * Constructor.
     *
     * @param xosServerIpAddress the IP address of the XOS server
     * @param xosServerPort the port for the REST service on XOS server
     */
    RestClient(IpAddress xosServerIpAddress, int xosServerPort) {
        this.url = "http://" + xosServerIpAddress.toString() + ":"
                + xosServerPort + "/xoslib/rs/vbng_mapping/";
    }
    /**
     * Gets a client web resource builder.
     *
     * @param url the URL to access remote resource
     * @return web resource builder
     */
    public WebResource.Builder getClientBuilder(String url) {
        log.info("URL: {}", url);
        Client client = Client.create();
        WebResource resource = client.resource(url);
        return resource.accept(UTF_8).type(UTF_8);
    }

    /**
     * Builds a REST client and fetches XOS mapping data in JSON format.
     *
     * @return the vBNG map if REST GET succeeds, otherwise return null
     */
    public ObjectNode getRest() {
        WebResource.Builder builder = getClientBuilder(url);
        ClientResponse response = builder.get(ClientResponse.class);

        if (response.getStatus() != HTTP_OK) {
            log.info("REST GET request returned error code {}",
                     response.getStatus());
            return null;
        }

        String jsonString = response.getEntity(String.class);
        log.info("Fetched JSON string: {}", jsonString);

        JsonNode node;
        try {
            node = MAPPER.readTree(jsonString);
        } catch (IOException e) {
            log.error("Failed to read JSON string", e);
            return null;
        }

        return (ObjectNode) node;
    }
}
