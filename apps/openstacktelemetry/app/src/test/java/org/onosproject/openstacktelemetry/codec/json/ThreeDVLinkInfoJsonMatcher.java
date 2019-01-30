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
package org.onosproject.openstacktelemetry.codec.json;

import com.fasterxml.jackson.databind.JsonNode;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.onosproject.openstacktelemetry.api.LinkInfo;
import org.onosproject.openstacktelemetry.api.LinkStatsInfo;

import static org.onosproject.openstacktelemetry.codec.json.ThreeDVLinkStatsInfoJsonMatcher.matchesLinkStatsInfo;

/**
 * Hamcrest matcher for ThreeDVLinkInfoJsonCodec.
 */
public final class ThreeDVLinkInfoJsonMatcher extends TypeSafeDiagnosingMatcher<JsonNode> {

    private final LinkInfo info;

    private static final String LINK_ID = "linkId";
    private static final String SRC_IP = "srcIp";
    private static final String DST_IP = "dstIp";
    private static final String SRC_PORT = "srcPort";
    private static final String DST_PORT = "dstPort";
    private static final String PROTOCOL = "protocol";
    private static final String STATS_INFO = "statsInfo";

    private ThreeDVLinkInfoJsonMatcher(LinkInfo info) {
        this.info = info;
    }

    @Override
    protected boolean matchesSafely(JsonNode jsonNode, Description description) {

        // check link ID
        String jsonLinkId = jsonNode.get(LINK_ID).asText();
        String linkId = info.linkId();
        if (!jsonLinkId.equals(linkId)) {
            description.appendText("Link ID was " + jsonLinkId);
            return false;
        }

        // check source IP
        String jsonSrcIp = jsonNode.get(SRC_IP).asText();
        String srcIp = info.srcIp();
        if (!jsonSrcIp.equals(srcIp)) {
            description.appendText("Source IP was " + jsonSrcIp);
            return false;
        }

        // check destination IP
        String jsonDstIp = jsonNode.get(DST_IP).asText();
        String dstIp = info.dstIp();
        if (!jsonDstIp.equals(dstIp)) {
            description.appendText("Destination IP was " + jsonDstIp);
            return false;
        }

        // check source port
        int jsonSrcPort = jsonNode.get(SRC_PORT).asInt();
        int srcPort = info.srcPort();
        if (jsonSrcPort != srcPort) {
            description.appendText("Source port was " + jsonSrcPort);
            return false;
        }

        // check destination port
        int jsonDstPort = jsonNode.get(DST_PORT).asInt();
        int dstPort = info.dstPort();
        if (jsonDstPort != dstPort) {
            description.appendText("Destination port was " + jsonDstPort);
            return false;
        }

        // check protocol
        String jsonProtocol = jsonNode.get(PROTOCOL).asText();
        String protocol = info.protocol();
        if (!jsonProtocol.equals(protocol)) {
            description.appendText("Protocol was " + jsonProtocol);
            return false;
        }

        // check statsInfo
        LinkStatsInfo statsInfo = info.linkStats();
        ThreeDVLinkStatsInfoJsonMatcher statsInfoJsonMatcher =
                matchesLinkStatsInfo(statsInfo);

        return statsInfoJsonMatcher.matches(jsonNode.get(STATS_INFO));
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(info.toString());
    }

    /**
     * Factory to allocate a link info matcher.
     *
     * @param info link info object we are looking for
     * @return matcher
     */
    public static ThreeDVLinkInfoJsonMatcher matchesLinkInfo(LinkInfo info) {
        return new ThreeDVLinkInfoJsonMatcher(info);
    }
}
