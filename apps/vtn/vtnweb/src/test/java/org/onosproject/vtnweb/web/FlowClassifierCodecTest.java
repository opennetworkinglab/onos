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
import org.onosproject.vtnrsc.FlowClassifier;
import org.onosproject.vtnrsc.FlowClassifierId;
import org.onosproject.vtnrsc.TenantId;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Flow classifier codec unit tests.
 */
public class FlowClassifierCodecTest {

    SfcCodecContext context;
    JsonCodec<FlowClassifier> flowClassifierCodec;
    /**
     * Sets up for each test.  Creates a context and fetches the flow classifier
     * codec.
     */
    @Before
    public void setUp() {
        context = new SfcCodecContext();
        flowClassifierCodec = context.codec(FlowClassifier.class);
        assertThat(flowClassifierCodec, notNullValue());
    }

    /**
     * Reads in a flow classifier from the given resource and decodes it.
     *
     * @param resourceName resource to use to read the JSON for the flow classifier
     * @return decoded flow classifier
     * @throws IOException if processing the resource fails
     */
    private FlowClassifier getFlowClassifier(String resourceName) throws IOException {
        InputStream jsonStream = FlowClassifierCodecTest.class
                .getResourceAsStream(resourceName);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(jsonStream);
        assertThat(json, notNullValue());
        FlowClassifier flowClassifier = flowClassifierCodec.decode((ObjectNode) json, context);
        assertThat(flowClassifier, notNullValue());
        return flowClassifier;
    }

    /**
     * Checks that a simple flow classifier decodes properly.
     *
     * @throws IOException if the resource cannot be processed
     */
    @Test
    public void codecFlowClassifierTest() throws IOException {

        FlowClassifier flowClassifier = getFlowClassifier("flowClassifier.json");

        assertThat(flowClassifier, notNullValue());

        FlowClassifierId flowClassifierId = FlowClassifierId.of("4a334cd4-fe9c-4fae-af4b-321c5e2eb051");
        TenantId tenantId = TenantId.tenantId("1814726e2d22407b8ca76db5e567dcf1");

        assertThat(flowClassifier.flowClassifierId().toString(), is(flowClassifierId.toString()));
        assertThat(flowClassifier.name(), is("flow1"));
        assertThat(flowClassifier.tenantId().toString(), is(tenantId.toString()));
        assertThat(flowClassifier.description(), is("flow classifier"));
        assertThat(flowClassifier.protocol(), is("tcp"));
        assertThat(flowClassifier.priority(), is(65535));
        assertThat(flowClassifier.minSrcPortRange(), is(22));
        assertThat(flowClassifier.maxSrcPortRange(), is(4000));
        assertThat(flowClassifier.minDstPortRange(), is(80));
        assertThat(flowClassifier.maxDstPortRange(), is(80));

    }
}
