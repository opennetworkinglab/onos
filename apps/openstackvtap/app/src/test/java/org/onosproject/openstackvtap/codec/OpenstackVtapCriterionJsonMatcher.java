/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.openstackvtap.codec;

import com.fasterxml.jackson.databind.JsonNode;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.onosproject.openstackvtap.api.OpenstackVtapCriterion;

import static org.onosproject.openstackvtap.util.OpenstackVtapUtil.getProtocolStringFromType;

/**
 * Hamcrest matcher for openstack vtap criterion config.
 */
public final class OpenstackVtapCriterionJsonMatcher extends TypeSafeDiagnosingMatcher<JsonNode> {

    private final OpenstackVtapCriterion criterion;

    private static final String SRC_IP = "srcIp";
    private static final String DST_IP = "dstIp";
    private static final String IP_PROTOCOL = "ipProto";
    private static final String SRC_PORT = "srcPort";
    private static final String DST_PORT = "dstPort";

    private OpenstackVtapCriterionJsonMatcher(OpenstackVtapCriterion criterion) {
        this.criterion = criterion;
    }

    @Override
    protected boolean matchesSafely(JsonNode jsonNode, Description description) {

        // check source IP address
        JsonNode jsonSrcIp = jsonNode.get(SRC_IP);
        String srcIp = criterion.srcIpPrefix().address().toString();
        if (!jsonSrcIp.asText().equals(srcIp)) {
            description.appendText("Source IP address was " + jsonSrcIp);
            return false;
        }

        // check destination IP address
        JsonNode jsonDstIp = jsonNode.get(DST_IP);
        String dstIp = criterion.dstIpPrefix().address().toString();
        if (!jsonDstIp.asText().equals(dstIp)) {
            description.appendText("Destination IP address was " + jsonDstIp);
            return false;
        }

        // check IP protocol
        JsonNode jsonIpProto = jsonNode.get(IP_PROTOCOL);
        if (jsonIpProto != null) {
            String ipProto = getProtocolStringFromType(criterion.ipProtocol());
            if (!jsonIpProto.asText().equals(ipProto)) {
                description.appendText("IP protocol was " + jsonIpProto);
                return false;
            }
        }

        // check source port number
        JsonNode jsonSrcPort = jsonNode.get(SRC_PORT);
        if (jsonSrcPort != null) {
            int srcPort = criterion.srcTpPort().toInt();
            if (jsonSrcPort.asInt() != srcPort) {
                description.appendText("Source port number was " + jsonSrcPort);
                return false;
            }
        }

        // check destination port number
        JsonNode jsonDstPort = jsonNode.get(DST_PORT);
        if (jsonDstPort != null) {
            int dstPort = criterion.dstTpPort().toInt();
            if (jsonDstPort.asInt() != dstPort) {
                description.appendText("Destination port number was " + jsonDstPort);
                return false;
            }
        }

        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(criterion.toString());
    }

    /**
     * Factory to allocate openstack vtap criterion matcher.
     *
     * @param criterion vtap criterion object we are looking for
     * @return matcher
     */
    public static OpenstackVtapCriterionJsonMatcher matchVtapCriterion(OpenstackVtapCriterion criterion) {
        return new OpenstackVtapCriterionJsonMatcher(criterion);
    }
}
