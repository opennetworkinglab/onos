/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.vtnweb.web;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Before;
import org.junit.Test;
import org.onosproject.codec.JsonCodec;
import org.onosproject.vtnrsc.PortPair;
import org.onosproject.vtnrsc.PortPairId;
import org.onosproject.vtnrsc.TenantId;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Port pair codec unit tests.
 */
public class PortPairCodecTest {

    SfcCodecContext context;
    JsonCodec<PortPair> portPairCodec;
    /**
     * Sets up for each test.  Creates a context and fetches the port pair
     * codec.
     */
    @Before
    public void setUp() {
        context = new SfcCodecContext();
        portPairCodec = context.codec(PortPair.class);
        assertThat(portPairCodec, notNullValue());
    }

    /**
     * Reads in a port pair from the given resource and decodes it.
     *
     * @param resourceName resource to use to read the JSON for the port pair
     * @return decoded port pair
     * @throws IOException if processing the resource fails
     */
    private PortPair getPortPair(String resourceName) throws IOException {
        InputStream jsonStream = PortPairCodecTest.class
                .getResourceAsStream(resourceName);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(jsonStream);
        assertThat(json, notNullValue());
        PortPair portPair = portPairCodec.decode((ObjectNode) json, context);
        assertThat(portPair, notNullValue());
        return portPair;
    }

    /**
     * Checks that a simple port pair decodes properly.
     *
     * @throws IOException if the resource cannot be processed
     */
    @Test
    public void codecPortPairTest() throws IOException {

        PortPair portPair = getPortPair("portPair.json");

        assertThat(portPair, notNullValue());

        PortPairId portPairId = PortPairId.of("78dcd363-fc23-aeb6-f44b-56dc5e2fb3ae");
        TenantId tenantId = TenantId.tenantId("d382007aa9904763a801f68ecf065cf5");

        assertThat(portPair.portPairId().toString(), is(portPairId.toString()));
        assertThat(portPair.name(), is("PP1"));
        assertThat(portPair.tenantId().toString(), is(tenantId.toString()));
        assertThat(portPair.description(), is("SF-A"));
        assertThat(portPair.ingress().toString(), is("dace4513-24fc-4fae-af4b-321c5e2eb3d1"));
        assertThat(portPair.egress().toString(), is("aef3478a-4a56-2a6e-cd3a-9dee4e2ec345"));
    }
}
