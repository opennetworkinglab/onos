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

package org.onosproject.restconf.utils.parser.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.protocol.restconf.server.utils.parser.api.JsonListener;
import org.onosproject.protocol.restconf.server.utils.parser.json.DefaultJsonWalker;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class DefaultJsonWalkerTest {

    private static final String[] EXP_TEXT_ARRAY = {
            "Enter: type is OBJECT name is null",
            "Enter: type is STRING name is name",
            "Exit: type is STRING",
            "Enter: type is STRING name is surname",
            "Exit: type is STRING",
            "Exit: type is OBJECT"
    };

    private static final String[] EXP_NODE_LIST_ARRAY = {
            "Enter: type is OBJECT name is null",
            "Enter: type is STRING name is surname",
            "Exit: type is STRING",
            "Enter: type is ARRAY name is networklist",
            "Exit: type is ARRAY",
            "Exit: type is OBJECT"
    };

    private static final String[] EXP_NODE_WITH_ARRAY = {
            "Enter: type is OBJECT name is null",
            "Enter: type is STRING name is surname",
            "Exit: type is STRING",
            "Enter: type is ARRAY name is networklist",
            "Enter: type is OBJECT name is null",
            "Enter: type is STRING name is network-id",
            "Exit: type is STRING",
            "Enter: type is STRING name is server-provided",
            "Exit: type is STRING",
            "Exit: type is OBJECT",
            "Enter: type is OBJECT name is null",
            "Enter: type is STRING name is network-id",
            "Exit: type is STRING",
            "Enter: type is STRING name is server-provided",
            "Exit: type is STRING",
            "Exit: type is OBJECT",
            "Enter: type is OBJECT name is null",
            "Enter: type is STRING name is network-id",
            "Exit: type is STRING",
            "Enter: type is STRING name is server-provided",
            "Exit: type is STRING",
            "Exit: type is OBJECT",
            "Exit: type is ARRAY",
            "Exit: type is OBJECT"
    };
    private static final String WRONG_CONTENT_MSG = "Wrong content in array";

    private final List<String> logger = new ArrayList<>();

    DefaultJsonWalker defaultJsonWalker = new DefaultJsonWalker();
    InternalJsonListener jsonListener = new InternalJsonListener();

    ObjectNode arrayNode;
    ObjectNode textNode;
    ObjectNode listNode;

    @Before
    public void setup() throws Exception {
        textNode = loadJsonFile("/textNode.json");
        listNode = loadJsonFile("/listNode.json");
        arrayNode = loadJsonFile("/arrayNode.json");
    }

    private ObjectNode loadJsonFile(String path) throws Exception {
        InputStream jsonStream = getClass().getResourceAsStream(path);
        ObjectMapper mapper = new ObjectMapper();
        return (ObjectNode) mapper.readTree(jsonStream);
    }

    private void assertWalkResult(String[] expectArray, List<String> logger) {
        for (int i = 0; i < expectArray.length; i++) {
            assertThat(WRONG_CONTENT_MSG,
                       true,
                       is(logger.get(i).contentEquals(expectArray[i])));
        }
    }

    @Test
    public void testWalkTextNode() throws Exception {

        defaultJsonWalker.walk(jsonListener, null, textNode);
        assertWalkResult(EXP_TEXT_ARRAY, logger);
    }

    @Test
    public void testWalkNodeWithList() throws Exception {
        defaultJsonWalker.walk(jsonListener, null, listNode);
        assertWalkResult(EXP_NODE_LIST_ARRAY, logger);
    }

    @Test
    public void testWalkNodeWithArray() throws Exception {
        defaultJsonWalker.walk(jsonListener, null, arrayNode);
        assertWalkResult(EXP_NODE_WITH_ARRAY, logger);
    }

    private class InternalJsonListener implements JsonListener {

        @Override
        public void enterJsonNode(String fieldName, JsonNode node) {
            logger.add("Enter: type is " + node.getNodeType() + " name is " +
                               fieldName);
        }

        @Override
        public void exitJsonNode(JsonNode node) {
            logger.add("Exit: type is " + node.getNodeType());
        }
    }
}