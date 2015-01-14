/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.codec.impl;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.criteria.Criterion;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Hamcrest matcher for criterion objects.
 */
public final class CriterionJsonMatcher extends TypeSafeDiagnosingMatcher<JsonNode> {

    final Criterion criterion;

    private CriterionJsonMatcher(Criterion criterionValue) {
        criterion = criterionValue;
    }

    @Override
    public boolean matchesSafely(JsonNode jsonCriterion, Description description) {
        final String type = criterion.type().name();
        final String jsonType = jsonCriterion.get("type").asText();
        if (!type.equals(jsonType)) {
            description.appendText("type was " + type);
            return false;
        }

        switch (criterion.type()) {

            case IN_PORT:
                final Criteria.PortCriterion portCriterion = (Criteria.PortCriterion) criterion;
                final long port = portCriterion.port().toLong();
                final long jsonPort = jsonCriterion.get("port").asLong();
                if (port != jsonPort) {
                    description.appendText("port was " + Long.toString(jsonPort));
                    return false;
                }
                break;

            case ETH_SRC:
            case ETH_DST:
                final Criteria.EthCriterion ethCriterion = (Criteria.EthCriterion) criterion;
                final String mac = ethCriterion.mac().toString();
                final String jsonMac = jsonCriterion.get("mac").textValue();
                if (!mac.equals(jsonMac)) {
                    description.appendText("mac was " + jsonMac);
                    return false;
                }
                break;

            case ETH_TYPE:
                final Criteria.EthTypeCriterion ethTypeCriterion =
                        (Criteria.EthTypeCriterion) criterion;
                final String ethType = ethTypeCriterion.ethType().toString();
                final String jsonEthType = jsonCriterion.get("ethType").textValue();
                if (!ethType.equals(jsonEthType)) {
                    description.appendText("ethType was " + jsonEthType);
                    return false;
                }
                break;

            case IPV4_SRC:
            case IPV6_SRC:
            case IPV4_DST:
            case IPV6_DST:
                final Criteria.IPCriterion ipCriterion = (Criteria.IPCriterion) criterion;
                final String ip = ipCriterion.ip().toString();
                final String jsonIp = jsonCriterion.get("ip").textValue();
                if (!ip.equals(jsonIp)) {
                    description.appendText("ip was " + jsonIp);
                    return false;
                }
                break;

            case IP_PROTO:
                final Criteria.IPProtocolCriterion iPProtocolCriterion =
                        (Criteria.IPProtocolCriterion) criterion;
                final byte protocol = iPProtocolCriterion.protocol();
                final byte jsonProtocol = (byte) jsonCriterion.get("protocol").shortValue();
                if (protocol != jsonProtocol) {
                    description.appendText("protocol was " + Byte.toString(jsonProtocol));
                    return false;
                }
                break;

            case VLAN_PCP:
                final Criteria.VlanPcpCriterion vlanPcpCriterion =
                        (Criteria.VlanPcpCriterion) criterion;
                final byte priority = vlanPcpCriterion.priority();
                final byte jsonPriority = (byte) jsonCriterion.get("protocol").shortValue();
                if (priority != jsonPriority) {
                    description.appendText("priority was " + Byte.toString(jsonPriority));
                    return false;
                }
                break;

            case VLAN_VID:
                final Criteria.VlanIdCriterion vlanIdCriterion =
                        (Criteria.VlanIdCriterion) criterion;
                final short vlanId = vlanIdCriterion.vlanId().toShort();
                final short jsonvlanId = jsonCriterion.get("vlanId").shortValue();
                if (vlanId != jsonvlanId) {
                    description.appendText("vlanId was " + Short.toString(jsonvlanId));
                    return false;
                }
                break;

            case TCP_SRC:
            case TCP_DST:
                final Criteria.TcpPortCriterion tcpPortCriterion =
                        (Criteria.TcpPortCriterion) criterion;
                final byte tcpPort = tcpPortCriterion.tcpPort().byteValue();
                final byte jsonTcpPort = (byte) jsonCriterion.get("tcpPort").shortValue();
                if (tcpPort != jsonTcpPort) {
                    description.appendText("tcp port was " + Byte.toString(jsonTcpPort));
                    return false;
                }
                break;

            case MPLS_LABEL:
                final Criteria.MplsCriterion mplsCriterion =
                        (Criteria.MplsCriterion) criterion;
                final int label = mplsCriterion.label();
                final int jsonLabel = jsonCriterion.get("label").intValue();
                if (label != jsonLabel) {
                    description.appendText("label was " + Integer.toString(jsonLabel));
                    return false;
                }
                break;

            case OCH_SIGID:
                final Criteria.LambdaCriterion lambdaCriterion =
                        (Criteria.LambdaCriterion) criterion;
                final short lambda = lambdaCriterion.lambda();
                final short jsonLambda = jsonCriterion.get("lambda").shortValue();
                if (lambda != jsonLambda) {
                    description.appendText("lambda was " + Short.toString(lambda));
                    return false;
                }
                break;

            case OCH_SIGTYPE:
                final Criteria.OpticalSignalTypeCriterion opticalSignalTypeCriterion =
                        (Criteria.OpticalSignalTypeCriterion) criterion;
                final short signalType = opticalSignalTypeCriterion.signalType();
                final short jsonSignalType = jsonCriterion.get("signalType").shortValue();
                if (signalType != jsonSignalType) {
                    description.appendText("signal type was " + Short.toString(signalType));
                    return false;
                }
                break;

            default:
                // Don't know how to format this type
                description.appendText("unknown criterion type " +
                        criterion.type());
                return false;
        }
        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(criterion.toString());
    }

    /**
     * Factory to allocate an criterion matcher.
     *
     * @param criterion criterion object we are looking for
     * @return matcher
     */
    public static CriterionJsonMatcher matchesCriterion(Criterion criterion) {
        return new CriterionJsonMatcher(criterion);
    }
}
