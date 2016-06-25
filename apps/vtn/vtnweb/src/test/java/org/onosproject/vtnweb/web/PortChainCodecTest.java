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
import org.onosproject.vtnrsc.PortChain;
import org.onosproject.vtnrsc.PortChainId;
import org.onosproject.vtnrsc.TenantId;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Flow rule codec unit tests.
 */
public class PortChainCodecTest {

    SfcCodecContext context;
    JsonCodec<PortChain> portChainCodec;
    /**
     * Sets up for each test.  Creates a context and fetches the flow rule
     * codec.
     */
    @Before
    public void setUp() {
        context = new SfcCodecContext();
        portChainCodec = context.codec(PortChain.class);
        assertThat(portChainCodec, notNullValue());
    }

    /**
     * Reads in a rule from the given resource and decodes it.
     *
     * @param resourceName resource to use to read the JSON for the rule
     * @return decoded flow rule
     * @throws IOException if processing the resource fails
     */
    private PortChain getPortChain(String resourceName) throws IOException {
        InputStream jsonStream = PortChainCodecTest.class
                .getResourceAsStream(resourceName);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(jsonStream);
        assertThat(json, notNullValue());
        PortChain portChain = portChainCodec.decode((ObjectNode) json, context);
        assertThat(portChain, notNullValue());
        return portChain;
    }

    /**
     * Checks that a simple rule decodes properly.
     *
     * @throws IOException if the resource cannot be processed
     */
    @Test
    public void codecPortChainTest() throws IOException {

        PortChain portChain = getPortChain("portChain.json");

        assertThat(portChain, notNullValue());

        PortChainId portChainId = PortChainId.of("1278dcd4-459f-62ed-754b-87fc5e4a6751");
        TenantId tenantId = TenantId.tenantId("d382007aa9904763a801f68ecf065cf5");

        assertThat(portChain.portChainId().toString(), is(portChainId.toString()));
        assertThat(portChain.name(), is("PC2"));
        assertThat(portChain.tenantId().toString(), is(tenantId.toString()));
        assertThat(portChain.description(), is("Two flows and two port-pair-groups"));

        assertThat(portChain.flowClassifiers(), notNullValue());
        assertThat(portChain.portPairGroups(), notNullValue());
    }
}
