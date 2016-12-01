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

package org.onosproject.provider.te.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit test for codec tools.
 */
public class CodecToolsTest {
    private static final ObjectMapper MAP = new ObjectMapper();
    private final ObjectNode simpleObject = MAP.createObjectNode();
    private String simpleString;


    @Before
    public void setup() throws Exception {
        simpleObject.put("field1", 1);
        simpleString = "{\n" +
                "  \"field1\" : 1\n" +
                "}";
    }

    @Test
    public void toJson() throws Exception {
        Assert.assertEquals(simpleObject, CodecTools.toJson(simpleString));
    }

    @Test
    public void jsonToString() throws Exception {
        assertEquals(simpleString, CodecTools.jsonToString(simpleObject));
    }
}