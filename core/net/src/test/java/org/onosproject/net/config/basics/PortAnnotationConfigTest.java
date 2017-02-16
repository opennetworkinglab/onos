/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.net.config.basics;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.onosproject.net.ConnectPoint.deviceConnectPoint;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onlab.junit.TestUtils.TestUtilsException;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onosproject.codec.CodecService;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.config.BaseConfig;
import org.onosproject.net.config.ConfigApplyDelegate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.google.common.collect.ImmutableMap;

public class PortAnnotationConfigTest {

    private static final String SAMPLE_JSONFILE = "port_annotation_config.json";

    private static TestServiceDirectory directory;
    private static ServiceDirectory original;

    private ObjectMapper mapper;

    private final ConfigApplyDelegate noopDelegate = cfg -> { };

    /**
     * {@value #SAMPLE_JSONFILE} after parsing.
     */
    private JsonNode node;

    // sample data
    private final ConnectPoint cp = deviceConnectPoint("of:0000000000000001/2");

    private final String key = "foo";
    private final String value = "bar";


    // TODO consolidate code-clone in ProtectionConfigTest, and define constants for field name
    @BeforeClass
    public static void setUpClass() throws TestUtilsException {
        directory = new TestServiceDirectory();

        CodecManager codecService = new CodecManager();
        codecService.activate();
        directory.add(CodecService.class, codecService);

        // replace service directory used by BaseConfig
        original = TestUtils.getField(BaseConfig.class, "services");
        TestUtils.setField(BaseConfig.class, "services", directory);
    }

    @AfterClass
    public static void tearDownClass() throws TestUtilsException {
        TestUtils.setField(BaseConfig.class, "services", original);
    }

    @Before
    public void setUp() throws JsonProcessingException, IOException, TestUtilsException {

        mapper = new ObjectMapper();
        // Jackson configuration for ease of Numeric node comparison
        // - treat integral number node as long node
        mapper.enable(DeserializationFeature.USE_LONG_FOR_INTS);
        mapper.setNodeFactory(new JsonNodeFactory(false) {
            @Override
            public NumericNode numberNode(int v) {
                return super.numberNode((long) v);
            }
            @Override
            public NumericNode numberNode(short v) {
                return super.numberNode((long) v);
            }
        });

        InputStream stream = PortAnnotationConfig.class
                                .getResourceAsStream(SAMPLE_JSONFILE);
        JsonNode tree = mapper.readTree(stream);

        node = tree.path("ports")
                            .path(cp.toString())
                            .path(PortAnnotationConfig.CONFIG_KEY);
        assertTrue(node.isObject());
    }

    @Test
    public void readTest() {
        PortAnnotationConfig sut = new PortAnnotationConfig();
        sut.init(cp, PortAnnotationConfig.CONFIG_KEY, node, mapper, noopDelegate);

        assertThat(sut.subject(), is(cp));
        Map<String, String> annotations = sut.annotations();
        assertThat(annotations.size(), is(1));
        assertThat(annotations.get(key), is(value));
    }

    public void writeEntryTest() throws JsonProcessingException, IOException {

        PortAnnotationConfig w = new PortAnnotationConfig();
        w.init(cp, PortAnnotationConfig.CONFIG_KEY, mapper.createObjectNode(), mapper, noopDelegate);

        // write equivalent to sample
        w.annotation(key, value);

        // reparse JSON
        JsonNode r = mapper.readTree(mapper.writeValueAsString(w.node()));

        PortAnnotationConfig sut = new PortAnnotationConfig();
        sut.init(cp, PortAnnotationConfig.CONFIG_KEY, r, mapper, noopDelegate);

        assertThat(sut.subject(), is(cp));
        Map<String, String> annotations = sut.annotations();
        assertThat(annotations.size(), is(1));
        assertThat(annotations.get(key), is(value));
    }

    public void writeMapTest() throws JsonProcessingException, IOException {

        PortAnnotationConfig w = new PortAnnotationConfig();
        w.init(cp, PortAnnotationConfig.CONFIG_KEY, mapper.createObjectNode(), mapper, noopDelegate);

        // write equivalent to sample
        w.annotations(ImmutableMap.of(key, value));

        // reparse JSON
        JsonNode r = mapper.readTree(mapper.writeValueAsString(w.node()));

        PortAnnotationConfig sut = new PortAnnotationConfig();
        sut.init(cp, PortAnnotationConfig.CONFIG_KEY, r, mapper, noopDelegate);

        assertThat(sut.subject(), is(cp));
        Map<String, String> annotations = sut.annotations();
        assertThat(annotations.size(), is(1));
        assertThat(annotations.get(key), is(value));
    }

}
