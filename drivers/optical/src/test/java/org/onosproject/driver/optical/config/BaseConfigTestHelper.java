/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.driver.optical.config;

import java.io.IOException;
import java.io.InputStream;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.onlab.junit.TestUtils;
import org.onlab.junit.TestUtils.TestUtilsException;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onosproject.codec.CodecService;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.net.config.BaseConfig;
import org.onosproject.net.config.ConfigApplyDelegate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NumericNode;

/**
 * Common utility required for testing BaseConfig instance.
 */
public class BaseConfigTestHelper {

    /**
     * {@link ServiceDirectory} to be used by BaseConfig.
     */
    protected static TestServiceDirectory directory;

    /**
     * No-op ConfigApplyDelegate.
     */
    protected final ConfigApplyDelegate noopDelegate = cfg -> { };

    private static ServiceDirectory original;


    @BeforeClass
    public static void setUpBaseConfigClass() throws TestUtilsException {
        directory = new TestServiceDirectory();

        CodecManager codecService = new CodecManager();
        codecService.activate();
        directory.add(CodecService.class, codecService);

        // replace service directory used by BaseConfig
        original = TestUtils.getField(BaseConfig.class, "services");
        TestUtils.setField(BaseConfig.class, "services", directory);
    }

    @AfterClass
    public static void tearDownBaseConfigClass() throws TestUtilsException {
        TestUtils.setField(BaseConfig.class, "services", original);
    }

    /**
     * Returns ObjectMapper configured for ease of testing.
     * <p>
     * It will treat all integral number node as long node.
     *
     * @return mapper
     */
    public static ObjectMapper testFriendlyMapper() {
        ObjectMapper mapper = new ObjectMapper();
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

        return mapper;
    }

    /**
     * Load JSON file from resource.
     *
     * @param filename JSON file name
     * @param mapper to use to read file.
     * @return JSON node
     * @throws JsonProcessingException
     * @throws IOException
     */
    public JsonNode loadJsonFromResource(String filename, ObjectMapper mapper)
                throws JsonProcessingException, IOException {

        InputStream stream = getClass().getResourceAsStream(filename);
        JsonNode tree = mapper.readTree(stream);
        return tree;
    }
}
