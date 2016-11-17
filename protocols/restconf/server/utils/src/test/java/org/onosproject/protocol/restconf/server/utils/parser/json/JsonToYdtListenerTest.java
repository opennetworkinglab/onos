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

package org.onosproject.protocol.restconf.server.utils.parser.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.protocol.restconf.server.utils.parser.api.JsonWalker;
import org.onosproject.protocol.restconf.server.utils.parser.api.TestYdtBuilder;
import org.onosproject.yms.ydt.YdtBuilder;
import org.onosproject.yms.ydt.YdtContext;

import java.io.InputStream;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.onosproject.yms.ydt.YdtType.*;
import static org.onosproject.yms.ydt.YmsOperationType.QUERY_REQUEST;

/**
 * Unit tests for JsonToYtdListener.
 */
public class JsonToYdtListenerTest {
    private static final String RESTCONF_ROOT = "/onos/restconf";
    private static final String WRONG_STRUCTURE = "The Ydt structure is wrong!";
    private static final String WRONG_TYPE = "The Ydt type is wrong!";

    private YdtBuilder builder;
    private JsonToYdtListener listener;
    private JsonWalker jsonWalker;

    @Before
    public void setup() throws Exception {
        builder = new TestYdtBuilder(RESTCONF_ROOT, null, QUERY_REQUEST);
        listener = new JsonToYdtListener(builder);
        jsonWalker = new DefaultJsonWalker();
    }

    private ObjectNode loadJsonFile(String path) throws Exception {
        InputStream jsonStream = getClass().getResourceAsStream(path);
        ObjectMapper mapper = new ObjectMapper();
        return (ObjectNode) mapper.readTree(jsonStream);
    }

    @Test
    public void testArrayNodeTransfer() throws Exception {
        ObjectNode arrayNode = loadJsonFile("/arrayNode.json");
        jsonWalker.walk(listener, null, arrayNode);
        YdtContext rootNode = builder.getRootNode();
        YdtContext firstChild = rootNode.getFirstChild();
        YdtContext nextSibling = firstChild.getNextSibling();
        assertEquals(WRONG_TYPE, SINGLE_INSTANCE_LEAF_VALUE_NODE,
                     firstChild.getYdtType());
        assertEquals(WRONG_STRUCTURE, "surname", firstChild.getName());
        assertEquals(WRONG_TYPE, MULTI_INSTANCE_NODE, nextSibling.getYdtType());
        assertEquals(WRONG_STRUCTURE, "networklist", nextSibling.getName());
        assertEquals(WRONG_STRUCTURE, "networklist",
                     nextSibling.getNextSibling().getName());
        assertEquals(WRONG_STRUCTURE, "networklist",
                     rootNode.getLastChild().getName());
    }

    @Test
    public void testListInListNodeTransfer() throws Exception {
        ObjectNode arrayNode = loadJsonFile("/listInList.json");
        jsonWalker.walk(listener, null, arrayNode);
        YdtContext rootNode = builder.getRootNode();
        YdtContext firstChild = rootNode.getFirstChild();
        YdtContext levelOneArray = firstChild.getFirstChild();
        assertEquals(WRONG_STRUCTURE, "container-identifier1",
                     firstChild.getName());
        assertEquals(WRONG_STRUCTURE, "list-identifier2",
                     levelOneArray.getName());
        assertEquals(WRONG_STRUCTURE, "list-identifier2",
                     levelOneArray.getNextSibling().getName());

        YdtContext identifier3Node = levelOneArray.getLastChild();
        assertEquals(WRONG_STRUCTURE, "container-identifier3",
                     identifier3Node.getName());
        YdtContext identifier4Node = identifier3Node.getLastChild();
        assertEquals(WRONG_STRUCTURE, "list-identifier4",
                     identifier4Node.getName());
        YdtContext identifier5ListNode = identifier4Node.getLastChild();
        assertEquals(WRONG_STRUCTURE, "leaf-list-identifier5",
                     identifier5ListNode.getName());
        assertEquals(WRONG_TYPE, MULTI_INSTANCE_LEAF_VALUE_NODE,
                     identifier5ListNode.getYdtType());
    }

    @Test
    public void testListAfterLeafList() throws Exception {
        ObjectNode arrayNode = loadJsonFile("/listAfterLeaflist.json");
        jsonWalker.walk(listener, null, arrayNode);
        YdtContext rootNode = builder.getRootNode();
        YdtContext firstChild = rootNode.getFirstChild();
        YdtContext second = firstChild.getNextSibling();
        YdtContext lastChild = rootNode.getLastChild();
        assertEquals(WRONG_STRUCTURE, "leaf-identifier1", firstChild.getName());
        assertEquals(WRONG_STRUCTURE, "leaf-list-identifier1", second.getName());
        assertEquals(WRONG_STRUCTURE, "list-identifier1", lastChild.getName());
    }

    @Test
    public void testLeafListAfterList() throws Exception {
        ObjectNode arrayNode = loadJsonFile("/leaflistAfterlist.json");
        jsonWalker.walk(listener, null, arrayNode);
        YdtContext rootNode = builder.getRootNode();
        YdtContext firstChild = rootNode.getFirstChild();
        YdtContext second = firstChild.getNextSibling();
        YdtContext lastChild = rootNode.getLastChild();
        assertEquals(WRONG_STRUCTURE, "leaf-identifier1", firstChild.getName());
        assertEquals(WRONG_STRUCTURE, "list-identifier1", second.getName());
        YdtContext level2Child = second.getFirstChild();
        assertEquals(WRONG_STRUCTURE, "leaf-identifier2", level2Child.getName());
        assertEquals(WRONG_STRUCTURE, "5", level2Child.getValue());
        assertEquals(WRONG_STRUCTURE, "leaf-list-identifier1", lastChild.getName());
        Set<String> sets =  ImmutableSet.of("5", "12");
        assertEquals(WRONG_STRUCTURE, sets, lastChild.getValueSet());
    }
}