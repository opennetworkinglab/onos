/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.openstacknetworkingui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Parser class for flow trace result string from OVS28.
 */
public final class Ovs28FlowTraceResultParser {
    private static final String TRACE_NODE_NAME = "traceNodeName";
    private static final String IS_SUCCESS = "isSuccess";
    private static final String FLOW_RULES = "flowRules";
    private static final String TABLE = "table";
    private static final String PRIORITY = "priority";
    private static final String SELECTOR = "selector";
    private static final String ACTIONS = "actions";
    private static final String BRIDGE = "bridge";
    private static final String DATAPATH = "Datapath";
    private static final String DROP = "drop";
    private static final String COMMA = ",";
    private static final String DOT = "\\.";
    private static final String NEW_LINE = "\n";

    private Ovs28FlowTraceResultParser() {
    }

    public static ObjectNode flowTraceResultInJson(String outputStream, String hostName) {
        if (outputStream == null || hostName == null) {
            return null;
        }

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode jsonNode = mapper.createObjectNode();

        int flowRuleStartLineNum = 0;
        int flowRuleEndLineNum = 0;

        jsonNode.put(TRACE_NODE_NAME, hostName);

        String[] lines = outputStream.split(NEW_LINE);

        for (int i = 0; i < lines.length; i++) {
            if (lines[i].startsWith(BRIDGE)) {
                flowRuleStartLineNum = i + 2;
            }

            if (lines[i].startsWith(DATAPATH)) {
                flowRuleEndLineNum = i;
                break;
            }
        }

        ArrayNode arrayNode = jsonNode.putArray(FLOW_RULES);

        for (int i = flowRuleStartLineNum; i < flowRuleEndLineNum; i = i + 2) {
            if (!isNewFlowTable(lines[i])) {
                break;
            }

            ObjectNode flowRule = arrayNode.addObject();

            flowRule.put(TABLE, tableNum(lines[i]));
            flowRule.put(PRIORITY, priority(lines[i]));
            flowRule.put(SELECTOR, selector(lines[i]));

            String actions = action(lines[i + 1]);

            if (!isNewFlowTable(lines[i + 2])) {
                actions = actions + "\n" + action(lines[i + 2]);
                i = i + 1;
            }

            flowRule.put(ACTIONS, actions);
        }

        if (lines[flowRuleEndLineNum].contains(DROP)) {
            jsonNode.put(IS_SUCCESS, false);
        } else {
            jsonNode.put(IS_SUCCESS, true);
        }

        return jsonNode;
    }

    private static boolean isNewFlowTable(String line) {
        return line.contains(PRIORITY);
    }

    private static String tableNum(String line) {
        return line.split(DOT)[0];
    }

    private static String priority(String line) {
        return line.split(PRIORITY)[1].trim().split(COMMA)[0];
    }

    private static String selector(String line) {
        if (!hasSelector(line)) {
            return "";
        }

        String selectorString = line.trim().split(PRIORITY)[0].split(" ")[1];

        return selectorString.replaceAll(COMMA, NEW_LINE);
    }

    private static boolean hasSelector(String line) {
        String tableNum = tableNum(line);
        return !line.replaceFirst(tableNum + DOT + " ", "").startsWith(PRIORITY);
    }

    private static String action(String line) {
        return line.trim();
    }


}
