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
package org.onosproject.pcerest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Before;
import org.junit.Test;

import org.onlab.util.DataRateUnit;
import org.onosproject.codec.JsonCodec;
import org.onosproject.pce.pceservice.PcePath;
import org.onosproject.net.intent.Constraint;
import org.onosproject.pce.pceservice.constraint.CostConstraint;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.pce.pceservice.constraint.PceBandwidthConstraint;

/**
 * PCE path codec unit tests.
 */
public class PcePathCodecTest {

    MockPceCodecContext context;
    JsonCodec<PcePath> pcePathCodec;
    /**
     * Sets up for each test. Creates a context and fetches the PCE path codec.
     */
    @Before
    public void setUp() {
        context = new MockPceCodecContext();
        pcePathCodec = context.codec(PcePath.class);
        assertThat(pcePathCodec, notNullValue());
    }

    /**
     * Reads in a pce-path from the given resource and decodes it.
     *
     * @param resourceName resource to use to read the json for the pce-path
     * @return decoded pce-path
     * @throws IOException if processing the resource fails
     */
    private PcePath getPcePath(String resourceName) throws IOException {
        InputStream jsonStream = PcePathCodecTest.class
                .getResourceAsStream(resourceName);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(jsonStream);
        assertThat(json, notNullValue());
        PcePath pcePath = pcePathCodec.decode((ObjectNode) json, context);
        assertThat(pcePath, notNullValue());
        return pcePath;
    }

    /**
     * Checks that a simple pce-path is decoded properly.
     *
     * @throws IOException if the resource cannot be processed
     */
    @Test
    public void codecPcePathTest() throws IOException {

        PcePath pcePath = getPcePath("pcePath.json");

        assertThat(pcePath, notNullValue());

        assertThat(pcePath.source().toString(), is("11.0.0.1"));
        assertThat(pcePath.destination(), is("11.0.0.2"));
        assertThat(pcePath.lspType().toString(), is("WITHOUT_SIGNALLING_AND_WITHOUT_SR"));
        // testing cost type
        String cost = "2";
        Constraint costConstraint = CostConstraint.of(CostConstraint.Type.values()[Integer.valueOf(cost) - 1]);
        assertThat(pcePath.costConstraint(), is(costConstraint));
        // testing bandwidth
        String bandwidth = "200";
        Constraint bandwidthConstraint = PceBandwidthConstraint.of(Double.valueOf(bandwidth), DataRateUnit
                    .valueOf("BPS"));
        assertThat(pcePath.bandwidthConstraint(), is(bandwidthConstraint));
    }
}
