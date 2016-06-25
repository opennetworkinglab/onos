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
import org.onosproject.vtnrsc.PortPairGroup;
import org.onosproject.vtnrsc.PortPairGroupId;
import org.onosproject.vtnrsc.TenantId;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Flow rule codec unit tests.
 */
public class PortPairGroupCodecTest {

    SfcCodecContext context;
    JsonCodec<PortPairGroup> portPairGroupCodec;
    /**
     * Sets up for each test.  Creates a context and fetches the flow rule
     * codec.
     */
    @Before
    public void setUp() {
        context = new SfcCodecContext();
        portPairGroupCodec = context.codec(PortPairGroup.class);
        assertThat(portPairGroupCodec, notNullValue());
    }

    /**
     * Reads in a rule from the given resource and decodes it.
     *
     * @param resourceName resource to use to read the JSON for the rule
     * @return decoded flow rule
     * @throws IOException if processing the resource fails
     */
    private PortPairGroup getPortPairGroup(String resourceName) throws IOException {
        InputStream jsonStream = PortPairGroupCodecTest.class
                .getResourceAsStream(resourceName);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(jsonStream);
        assertThat(json, notNullValue());
        PortPairGroup portPairGroup = portPairGroupCodec.decode((ObjectNode) json, context);
        assertThat(portPairGroup, notNullValue());
        return portPairGroup;
    }

    /**
     * Checks that a simple rule decodes properly.
     *
     * @throws IOException if the resource cannot be processed
     */
    @Test
    public void codecPortPairGroupTest() throws IOException {

        PortPairGroup portPairGroup = getPortPairGroup("portPairGroup.json");

        assertThat(portPairGroup, notNullValue());

        PortPairGroupId portPairGroupId = PortPairGroupId.of("4512d643-24fc-4fae-af4b-321c5e2eb3d1");
        TenantId tenantId = TenantId.tenantId("d382007aa9904763a801f68ecf065cf5");

        assertThat(portPairGroup.portPairGroupId().toString(), is(portPairGroupId.toString()));
        assertThat(portPairGroup.name(), is("PG1"));
        assertThat(portPairGroup.tenantId().toString(), is(tenantId.toString()));
        assertThat(portPairGroup.description(), is("Two port-pairs"));
        assertThat(portPairGroup.portPairs(), notNullValue());
    }
}
