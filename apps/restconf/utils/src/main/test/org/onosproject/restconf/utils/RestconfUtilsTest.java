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
package org.onosproject.restconf.utils;

import static org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Before;
import static org.junit.Test;

/**
 * Unit tests for RESTCONF utils.
 */
public class RestconfUtilsTest {

    private static final String testJson = "{\"alpha\":\"abc\",\"beta\":123,\"gamma\":true}";

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void convertInputStreamToObjectNode() throws Exception {
        InputStream inputStream = IOUtils.toInputStream(testJson);
        ObjectNode testNode = ParseUtils.convertInputStreamToObjectNode(inputStream);
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode compareNode = mapper.createObjectNode()
                .put("alpha", "abc")
                .put("beta", 123)
                .put("gamma", true);
        assertEquals(testNode, compareNode);
    }

    @Test
    public void convertObjectNodeToInputStream() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode compareNode = mapper.createObjectNode()
                .put("alpha", "abc")
                .put("beta", 123)
                .put("gamma", true);
        InputStream inputStream = ParseUtils.convertObjectNodeToInputStream(compareNode);
        String compareJson = IOUtils.toString(inputStream);
        assertEquals(testJson, compareJson);
    }

    @Test
    public void convertJsonToDataNode() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode compareNode = mapper.createObjectNode()
                .put("alpha", "abc")
                .put("beta", 123)
                .put("gamma", true);
        ResourceData resourceData = ParseUtils.convertJsonToDataNode("/xyz", compareNode);
        ObjectNode testNode = ParseUtils.convertDataNodeToJson(resourceData.resourceId(), resourceData.dataNode());
        assertEquals(testNode, compareNode);
    }

}