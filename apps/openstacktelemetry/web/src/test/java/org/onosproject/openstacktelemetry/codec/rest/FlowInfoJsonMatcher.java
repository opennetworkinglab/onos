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
package org.onosproject.openstacktelemetry.codec.rest;

import com.fasterxml.jackson.databind.JsonNode;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.onosproject.openstacktelemetry.api.FlowInfo;
import org.onosproject.openstacktelemetry.api.StatsInfo;

/**
 * Hamcrest matcher for flow info.
 */
public final class FlowInfoJsonMatcher extends TypeSafeDiagnosingMatcher<JsonNode> {

    private final FlowInfo flowInfo;

    private static final String FLOW_TYPE = "flowType";
    private static final String DEVICE_ID = "deviceId";
    private static final String INPUT_INTERFACE_ID = "inputInterfaceId";
    private static final String OUTPUT_INTERFACE_ID = "outputInterfaceId";

    private static final String VLAN_ID = "vlanId";
    private static final String VXLAN_ID = "vxlanId";
    private static final String SRC_IP = "srcIp";
    private static final String SRC_IP_PREFIX_LEN = "srcIpPrefixLength";
    private static final String DST_IP = "dstIp";
    private static final String DST_IP_PREFIX_LEN = "dstIpPrefixLength";
    private static final String SRC_PORT = "srcPort";
    private static final String DST_PORT = "dstPort";
    private static final String PROTOCOL = "protocol";
    private static final String SRC_MAC = "srcMac";
    private static final String DST_MAC = "dstMac";
    private static final String STATS_INFO = "statsInfo";

    private FlowInfoJsonMatcher(FlowInfo flowInfo) {
        this.flowInfo = flowInfo;
    }

    @Override
    protected boolean matchesSafely(JsonNode jsonNode, Description description) {

        // check flow type
        String jsonFlowType = jsonNode.get(FLOW_TYPE).asText();
        String flowType = String.valueOf(flowInfo.flowType());
        if (!jsonFlowType.equals(flowType)) {
            description.appendText("flow type was " + jsonFlowType);
            return false;
        }

        // check device id
        String jsonDeviceId = jsonNode.get(DEVICE_ID).asText();
        String deviceId = flowInfo.deviceId().toString();
        if (!jsonDeviceId.equals(deviceId)) {
            description.appendText("device id was " + jsonDeviceId);
            return false;
        }

        // check input interface id
        int jsonInputInterfaceId = jsonNode.get(INPUT_INTERFACE_ID).asInt();
        int inputInterfaceId = flowInfo.inputInterfaceId();
        if (jsonInputInterfaceId != inputInterfaceId) {
            description.appendText("input interface id was " + jsonInputInterfaceId);
            return false;
        }

        // check output interface id
        int jsonOutputInterfaceId = jsonNode.get(OUTPUT_INTERFACE_ID).asInt();
        int outputInterfaceId = flowInfo.outputInterfaceId();
        if (jsonOutputInterfaceId != outputInterfaceId) {
            description.appendText("output interface id was " + jsonInputInterfaceId);
            return false;
        }

        // check vlan id
        try {
            if (!(jsonNode.get(VLAN_ID).isNull())) {
                String jsonVlanId = jsonNode.get(VLAN_ID).asText();
                String vlanId = flowInfo.vlanId().toString();
                if (!jsonVlanId.equals(vlanId)) {
                    description.appendText("VLAN id was " + jsonVlanId);
                    return false;
                }
            }
        } catch (NullPointerException ex) {
            description.appendText("VLAN id was null");
        }

        // check source IP
        String jsonSrcIp = jsonNode.get(SRC_IP).asText();
        String srcIp = flowInfo.srcIp().address().toString();
        if (!jsonSrcIp.equals(srcIp)) {
            description.appendText("Source IP was " + jsonSrcIp);
            return false;
        }

        // check destination IP
        String jsonDstIp = jsonNode.get(DST_IP).asText();
        String dstIp = flowInfo.dstIp().address().toString();
        if (!jsonDstIp.equals(dstIp)) {
            description.appendText("Destination IP was " + jsonDstIp);
            return false;
        }

        // check source IP prefix length
        int jsonSrcPrefixLength = jsonNode.get(SRC_IP_PREFIX_LEN).asInt();
        int srcPrefixLength = flowInfo.srcIp().prefixLength();
        if (jsonSrcPrefixLength != srcPrefixLength) {
            description.appendText("Source IP prefix length was " + jsonSrcPrefixLength);
            return false;
        }

        // check destination IP prefix length
        int jsonDstPrefixLength = jsonNode.get(DST_IP_PREFIX_LEN).asInt();
        int dstPrefixLength = flowInfo.dstIp().prefixLength();
        if (jsonDstPrefixLength != dstPrefixLength) {
            description.appendText("Destination IP prefix length was " + jsonDstPrefixLength);
            return false;
        }

        // check source port
        int jsonSrcPort = jsonNode.get(SRC_PORT).asInt();
        int srcPort = flowInfo.srcPort().toInt();
        if (jsonSrcPort != srcPort) {
            description.appendText("Source port was " + jsonSrcPort);
            return false;
        }

        // check destination port
        int jsonDstPort = jsonNode.get(DST_PORT).asInt();
        int dstPort = flowInfo.dstPort().toInt();
        if (jsonDstPort != dstPort) {
            description.appendText("Destination port was " + jsonDstPort);
            return false;
        }

        // check protocol
        String jsonProtocol = jsonNode.get(PROTOCOL).asText();
        String protocol = String.valueOf(flowInfo.protocol());
        if (!jsonProtocol.equals(protocol)) {
            description.appendText("Protocol was " + jsonProtocol);
            return false;
        }

        // check source mac
        String jsonSrcMac = jsonNode.get(SRC_MAC).asText();
        String srcMac = flowInfo.srcMac().toString();
        if (!jsonSrcMac.equals(srcMac)) {
            description.appendText("Source MAC was " + jsonSrcMac);
            return false;
        }

        // check destination mac
        String jsonDstMac = jsonNode.get(DST_MAC).asText();
        String dstMac = flowInfo.dstMac().toString();
        if (!jsonDstMac.equals(dstMac)) {
            description.appendText("Destination MAC was " + jsonDstMac);
            return false;
        }

        // check stats info
        JsonNode jsonStatsInfo = jsonNode.get(STATS_INFO);
        if (jsonStatsInfo != null) {
            StatsInfo statsInfo = flowInfo.statsInfo();
            StatsInfoJsonMatcher infoMatcher =
                    StatsInfoJsonMatcher.matchStatsInfo(statsInfo);
            return infoMatcher.matches(jsonStatsInfo);
        }

        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(flowInfo.toString());
    }

    /**
     * Factory to allocate an flow info matcher.
     *
     * @param flowInfo flow info object we are looking for
     * @return matcher
     */
    public static FlowInfoJsonMatcher matchesFlowInfo(FlowInfo flowInfo) {
        return new FlowInfoJsonMatcher(flowInfo);
    }
}
