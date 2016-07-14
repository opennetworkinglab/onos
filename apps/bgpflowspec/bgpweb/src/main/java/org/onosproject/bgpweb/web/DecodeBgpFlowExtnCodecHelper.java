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
package org.onosproject.bgpweb.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onosproject.flowapi.DefaultExtDscpValue;
import org.onosproject.flowapi.DefaultExtFragment;
import org.onosproject.flowapi.DefaultExtIcmpCode;
import org.onosproject.flowapi.DefaultExtIcmpType;
import org.onosproject.flowapi.DefaultExtIpProtocol;
import org.onosproject.flowapi.DefaultExtKeyName;
import org.onosproject.flowapi.DefaultExtPacketLength;
import org.onosproject.flowapi.DefaultExtPort;
import org.onosproject.flowapi.DefaultExtPrefix;
import org.onosproject.flowapi.DefaultExtTarget;
import org.onosproject.flowapi.DefaultExtTcpFlag;
import org.onosproject.flowapi.DefaultExtTrafficAction;
import org.onosproject.flowapi.DefaultExtTrafficMarking;
import org.onosproject.flowapi.DefaultExtTrafficRate;
import org.onosproject.flowapi.DefaultExtTrafficRedirect;
import org.onosproject.flowapi.DefaultExtWideCommunityInt;
import org.onosproject.flowapi.ExtDscpValue;
import org.onosproject.flowapi.ExtFlowTypes;
import org.onosproject.flowapi.ExtFragment;
import org.onosproject.flowapi.ExtIcmpCode;
import org.onosproject.flowapi.ExtIcmpType;
import org.onosproject.flowapi.ExtIpProtocol;
import org.onosproject.flowapi.ExtKeyName;
import org.onosproject.flowapi.ExtPacketLength;
import org.onosproject.flowapi.ExtPort;
import org.onosproject.flowapi.ExtPrefix;
import org.onosproject.flowapi.ExtTarget;
import org.onosproject.flowapi.ExtTcpFlag;
import org.onosproject.flowapi.ExtTrafficAction;
import org.onosproject.flowapi.ExtTrafficMarking;
import org.onosproject.flowapi.ExtTrafficRate;
import org.onosproject.flowapi.ExtTrafficRedirect;
import org.onosproject.flowapi.ExtWideCommunityInt;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.onlab.util.Tools.nullIsIllegal;

/**
 * Decode the Ext extension codec.
 */
public final class DecodeBgpFlowExtnCodecHelper {

    private final ObjectNode json;

    protected static final String MISSING_MEMBER_MESSAGE =
            " member is required in ExtTypes";

    protected static final String MALFORMED_MEMBER_MESSAGE =
            " member is malformed";

    private interface ExtensionDecoder {
        ExtFlowTypes decodeExtension(ObjectNode json);
    }

    private final Map<String, ExtensionDecoder> decoderMap;

    BgpParseAttributes parse = new BgpParseAttributes();

    /**
     * Creates a decode extension codec object.
     * Initializes the lookup map for Bgp extension types.
     *
     * @param json JSON object to decode
     */
    public DecodeBgpFlowExtnCodecHelper(ObjectNode json) {
        this.json = json;
        decoderMap = new HashMap<>();

        decoderMap.put(ExtFlowTypes.ExtType.IPV4_SRC_PFX.name(), new BgpSourcePrefixDecode());
        decoderMap.put(ExtFlowTypes.ExtType.IPV4_DST_PFX.name(), new BgpDestinationPrefixDecode());
        decoderMap.put(ExtFlowTypes.ExtType.EXT_FLOW_RULE_KEY.name(), new BgpFlowRuleKeyDecode());
        decoderMap.put(ExtFlowTypes.ExtType.IP_PROTO_LIST.name(), new BgpIpProtocolDecode());
        decoderMap.put(ExtFlowTypes.ExtType.IN_PORT_LIST.name(), new BgpInPortDecode());
        decoderMap.put(ExtFlowTypes.ExtType.DST_PORT_LIST.name(), new BgpDestinationPortDecode());
        decoderMap.put(ExtFlowTypes.ExtType.SRC_PORT_LIST.name(), new BgpSourcePortDecode());
        decoderMap.put(ExtFlowTypes.ExtType.ICMP_TYPE_LIST.name(), new BgpIcmpTypeDecode());
        decoderMap.put(ExtFlowTypes.ExtType.ICMP_CODE_LIST.name(), new BgpIcmpCodeDecode());
        decoderMap.put(ExtFlowTypes.ExtType.TCP_FLAG_LIST.name(), new BgpTcpFlagDecode());
        decoderMap.put(ExtFlowTypes.ExtType.PACKET_LENGTH_LIST.name(), new BgpPacketLengthDecode());
        decoderMap.put(ExtFlowTypes.ExtType.DSCP_VALUE_LIST.name(), new BgpDscpValueDecode());
        decoderMap.put(ExtFlowTypes.ExtType.FRAGMENT_LIST.name(), new BgpFragmentDecode());

        decoderMap.put(ExtFlowTypes.ExtType.TRAFFIC_RATE.name(), new BgpTrafficRateDecode());
        decoderMap.put(ExtFlowTypes.ExtType.TRAFFIC_ACTION.name(), new BgpTrafficActionDecode());
        decoderMap.put(ExtFlowTypes.ExtType.TRAFFIC_REDIRECT.name(), new BgpTrafficRedirectDecode());
        decoderMap.put(ExtFlowTypes.ExtType.TRAFFIC_MARKING.name(), new BgpTrafficMarkingDecode());

        decoderMap.put(ExtFlowTypes.ExtType.WIDE_COMM_FLAGS.name(), new BgpWcommFlagsDecode());
        decoderMap.put(ExtFlowTypes.ExtType.WIDE_COMM_HOP_COUNT.name(), new BgpWcommHopCountDecode());
        decoderMap.put(ExtFlowTypes.ExtType.WIDE_COMM_COMMUNITY.name(), new BgpWcommCommunityDecode());
        decoderMap.put(ExtFlowTypes.ExtType.WIDE_COMM_CONTEXT_AS.name(), new BgpWcommContextAsDecode());
        decoderMap.put(ExtFlowTypes.ExtType.WIDE_COMM_LOCAL_AS.name(), new BgpWcommLocalAsDecode());
        decoderMap.put(ExtFlowTypes.ExtType.WIDE_COMM_TARGET.name(), new BgpWcommTargetDecode());
        decoderMap.put(ExtFlowTypes.ExtType.WIDE_COMM_EXT_TARGET.name(), new BgpWcommExtTargetDecode());
        decoderMap.put(ExtFlowTypes.ExtType.WIDE_COMM_PARAMETER.name(), new BgpWcommParameterDecode());
    }

    /** Source prefix decoder.*/
    private class BgpSourcePrefixDecode implements ExtensionDecoder {
        @Override
        public ExtFlowTypes decodeExtension(ObjectNode json) {
            if (json == null || !json.isObject()) {
                return null;
            }

            ExtPrefix.Builder resultBuilder = new DefaultExtPrefix.Builder();

            String ip = nullIsIllegal(json.get(BgpFlowExtensionCodec.PREFIX),
                    BgpFlowExtensionCodec.PREFIX + MISSING_MEMBER_MESSAGE).asText();
            resultBuilder.setPrefix(IpPrefix.valueOf(ip));
            resultBuilder.setType(ExtFlowTypes.ExtType.IPV4_SRC_PFX);

            return resultBuilder.build();
        }
    }

    /** Destination prefix decoder.*/
    private class BgpDestinationPrefixDecode implements ExtensionDecoder {
        @Override
        public ExtFlowTypes decodeExtension(ObjectNode json) {
            if (json == null || !json.isObject()) {
                return null;
            }

            ExtPrefix.Builder resultBuilder = new DefaultExtPrefix.Builder();

            String ip = nullIsIllegal(json.get(BgpFlowExtensionCodec.PREFIX),
                    BgpFlowExtensionCodec.PREFIX + MISSING_MEMBER_MESSAGE).asText();
            resultBuilder.setPrefix(IpPrefix.valueOf(ip));
            resultBuilder.setType(ExtFlowTypes.ExtType.IPV4_DST_PFX);

            return resultBuilder.build();
        }
    }

    /** Flow rule key decoder.*/
    private class BgpFlowRuleKeyDecode implements ExtensionDecoder {
        @Override
        public ExtFlowTypes decodeExtension(ObjectNode json) {
            if (json == null || !json.isObject()) {
                return null;
            }

            ExtKeyName.Builder resultBuilder = new DefaultExtKeyName.Builder();

            String name = nullIsIllegal(json.get(BgpFlowExtensionCodec.NAME),
                    BgpFlowExtensionCodec.NAME + MISSING_MEMBER_MESSAGE).asText();
            resultBuilder.setKeyName(name);
            resultBuilder.setType(ExtFlowTypes.ExtType.EXT_FLOW_RULE_KEY);

            return resultBuilder.build();
        }
    }

    /** Ip protocol decoder.*/
    private class BgpIpProtocolDecode implements ExtensionDecoder {
        @Override
        public ExtFlowTypes decodeExtension(ObjectNode json) {
            if (json == null || !json.isObject()) {
                return null;
            }

            ExtIpProtocol.Builder resultBuilder = new DefaultExtIpProtocol.Builder();

            String protocols = nullIsIllegal(json.get(BgpFlowExtensionCodec.PROTOCOLS),
                    BgpFlowExtensionCodec.PROTOCOLS + MISSING_MEMBER_MESSAGE).asText();
            resultBuilder.setIpProtocol(parse.parseIpProtocol(protocols));
            resultBuilder.setType(ExtFlowTypes.ExtType.IP_PROTO_LIST);

            return resultBuilder.build();
        }
    }

    /** In port decoder.*/
    private class BgpInPortDecode implements ExtensionDecoder {
        @Override
        public ExtFlowTypes decodeExtension(ObjectNode json) {
            if (json == null || !json.isObject()) {
                return null;
            }

            ExtPort.Builder resultBuilder = new DefaultExtPort.Builder();

            String portList = nullIsIllegal(json.get(BgpFlowExtensionCodec.PORT),
                    BgpFlowExtensionCodec.PORT + MISSING_MEMBER_MESSAGE).asText();
            resultBuilder.setPort(parse.parsePort(portList));
            resultBuilder.setType(ExtFlowTypes.ExtType.IN_PORT_LIST);

            return resultBuilder.build();
        }
    }

    /** Destination decoder.*/
    private class BgpDestinationPortDecode implements ExtensionDecoder {
        @Override
        public ExtFlowTypes decodeExtension(ObjectNode json) {
            if (json == null || !json.isObject()) {
                return null;
            }

            ExtPort.Builder resultBuilder = new DefaultExtPort.Builder();

            String portList = nullIsIllegal(json.get(BgpFlowExtensionCodec.DST_PORT),
                    BgpFlowExtensionCodec.DST_PORT + MISSING_MEMBER_MESSAGE).asText();
            resultBuilder.setPort(parse.parsePort(portList));
            resultBuilder.setType(ExtFlowTypes.ExtType.DST_PORT_LIST);

            return resultBuilder.build();
        }
    }

    /** Source decoder.*/
    private class BgpSourcePortDecode implements ExtensionDecoder {
        @Override
        public ExtFlowTypes decodeExtension(ObjectNode json) {
            if (json == null || !json.isObject()) {
                return null;
            }

            ExtPort.Builder resultBuilder = new DefaultExtPort.Builder();

            String portList = nullIsIllegal(json.get(BgpFlowExtensionCodec.SRC_PORT),
                    BgpFlowExtensionCodec.SRC_PORT + MISSING_MEMBER_MESSAGE).asText();
            resultBuilder.setPort(parse.parsePort(portList));
            resultBuilder.setType(ExtFlowTypes.ExtType.SRC_PORT_LIST);

            return resultBuilder.build();
        }
    }

    /** Icmp type decoder.*/
    private class BgpIcmpTypeDecode implements ExtensionDecoder {
        @Override
        public ExtFlowTypes decodeExtension(ObjectNode json) {
            if (json == null || !json.isObject()) {
                return null;
            }

            ExtIcmpType.Builder resultBuilder = new DefaultExtIcmpType.Builder();

            String icmpType = nullIsIllegal(json.get(BgpFlowExtensionCodec.ICMP_TYPE),
                    BgpFlowExtensionCodec.ICMP_TYPE + MISSING_MEMBER_MESSAGE).asText();
            resultBuilder.setIcmpType(parse.parseIcmpType(icmpType));
            resultBuilder.setType(ExtFlowTypes.ExtType.ICMP_TYPE_LIST);

            return resultBuilder.build();
        }
    }

    /** Icmp code decoder.*/
    private class BgpIcmpCodeDecode implements ExtensionDecoder {
        @Override
        public ExtFlowTypes decodeExtension(ObjectNode json) {
            if (json == null || !json.isObject()) {
                return null;
            }

            ExtIcmpCode.Builder resultBuilder = new DefaultExtIcmpCode.Builder();

            String icmpCode = nullIsIllegal(json.get(BgpFlowExtensionCodec.ICMP_CODE),
                    BgpFlowExtensionCodec.ICMP_CODE + MISSING_MEMBER_MESSAGE).asText();
            resultBuilder.setIcmpCode(parse.parseIcmpCode(icmpCode));
            resultBuilder.setType(ExtFlowTypes.ExtType.ICMP_CODE_LIST);

            return resultBuilder.build();
        }
    }

    /** Tcp flag decoder.*/
    private class BgpTcpFlagDecode implements ExtensionDecoder {
        @Override
        public ExtFlowTypes decodeExtension(ObjectNode json) {
            if (json == null || !json.isObject()) {
                return null;
            }

            ExtTcpFlag.Builder resultBuilder = new DefaultExtTcpFlag.Builder();

            String tcpFlag = nullIsIllegal(json.get(BgpFlowExtensionCodec.TCP_FLAG),
                    BgpFlowExtensionCodec.TCP_FLAG + MISSING_MEMBER_MESSAGE).asText();
            resultBuilder.setTcpFlag(parse.parseTcpFlags(tcpFlag));
            resultBuilder.setType(ExtFlowTypes.ExtType.TCP_FLAG_LIST);

            return resultBuilder.build();
        }
    }

    /** Packet length decoder.*/
    private class BgpPacketLengthDecode implements ExtensionDecoder {
        @Override
        public ExtFlowTypes decodeExtension(ObjectNode json) {
            if (json == null || !json.isObject()) {
                return null;
            }

            ExtPacketLength.Builder resultBuilder = new DefaultExtPacketLength.Builder();

            String packetLength = nullIsIllegal(json.get(BgpFlowExtensionCodec.PACKET_LENGTH),
                    BgpFlowExtensionCodec.PACKET_LENGTH + MISSING_MEMBER_MESSAGE).asText();
            resultBuilder.setPacketLength(parse.parsePacketLength(packetLength));
            resultBuilder.setType(ExtFlowTypes.ExtType.PACKET_LENGTH_LIST);

            return resultBuilder.build();
        }
    }

    /** Dscp value decoder.*/
    private class BgpDscpValueDecode implements ExtensionDecoder {
        @Override
        public ExtFlowTypes decodeExtension(ObjectNode json) {
            if (json == null || !json.isObject()) {
                return null;
            }

            ExtDscpValue.Builder resultBuilder = new DefaultExtDscpValue.Builder();

            String dscpValue = nullIsIllegal(json.get(BgpFlowExtensionCodec.DSCP_VALUE),
                    BgpFlowExtensionCodec.DSCP_VALUE + MISSING_MEMBER_MESSAGE).asText();
            resultBuilder.setDscpValue(parse.parseDscp(dscpValue));
            resultBuilder.setType(ExtFlowTypes.ExtType.DSCP_VALUE_LIST);

            return resultBuilder.build();
        }
    }

    /** Fragment decoder.*/
    private class BgpFragmentDecode implements ExtensionDecoder {
        @Override
        public ExtFlowTypes decodeExtension(ObjectNode json) {
            if (json == null || !json.isObject()) {
                return null;
            }

            ExtFragment.Builder resultBuilder = new DefaultExtFragment.Builder();

            String fragment = nullIsIllegal(json.get(BgpFlowExtensionCodec.FRAGMENT),
                    BgpFlowExtensionCodec.FRAGMENT + MISSING_MEMBER_MESSAGE).asText();
            resultBuilder.setFragment(parse.parseFragment(fragment));
            resultBuilder.setType(ExtFlowTypes.ExtType.FRAGMENT_LIST);

            return resultBuilder.build();
        }
    }

    /** Traffic rate decoder.*/
    private class BgpTrafficRateDecode implements ExtensionDecoder {
        @Override
        public ExtFlowTypes decodeExtension(ObjectNode json) {
            if (json == null || !json.isObject()) {
                return null;
            }

            ExtTrafficRate.Builder resultBuilder = new DefaultExtTrafficRate.Builder();

            String rate = nullIsIllegal(json.get(BgpFlowExtensionCodec.TRAFFIC_RATE),
                    BgpFlowExtensionCodec.TRAFFIC_RATE + MISSING_MEMBER_MESSAGE).asText();

            String[] commaPart = rate.split(",");
            String[] valuePart = commaPart[0].split("=");

            if (valuePart[0].matches(BgpFlowExtensionCodec.TRAFFIC_RATE_ASN)) {
                short s = Short.decode(valuePart[1].trim()).shortValue();
                resultBuilder.setAsn(s);
            } else {
                nullIsIllegal(valuePart[0], BgpFlowExtensionCodec.TRAFFIC_RATE + MALFORMED_MEMBER_MESSAGE);
            }

            valuePart = commaPart[1].split("=");
            if (valuePart[0].matches(BgpFlowExtensionCodec.TRAFFIC_RATE_RATE)) {
                float f = Float.parseFloat(valuePart[1].trim());
                resultBuilder.setRate(f);
            } else {
                nullIsIllegal(valuePart[0], BgpFlowExtensionCodec.TRAFFIC_RATE + MALFORMED_MEMBER_MESSAGE);
            }

            resultBuilder.setType(ExtFlowTypes.ExtType.TRAFFIC_RATE);

            return resultBuilder.build();
        }
    }

    /** Traffic action decoder.*/
    private class BgpTrafficActionDecode implements ExtensionDecoder {
        @Override
        public ExtFlowTypes decodeExtension(ObjectNode json) {
            if (json == null || !json.isObject()) {
                return null;
            }

            ExtTrafficAction.Builder resultBuilder = new DefaultExtTrafficAction.Builder();

            String rate = nullIsIllegal(json.get(BgpFlowExtensionCodec.TRAFFIC_ACTION),
                    BgpFlowExtensionCodec.TRAFFIC_ACTION + MISSING_MEMBER_MESSAGE).asText();

            String[] commaPart = rate.split(",");
            String[] valuePart = commaPart[0].split("=");

            if (valuePart[0].matches(BgpFlowExtensionCodec.TRAFFIC_ACTION_TERMINAL)) {
                boolean terminal = Boolean.parseBoolean(valuePart[1].trim());
                resultBuilder.setTerminal(terminal);
            } else {
                nullIsIllegal(valuePart[0], BgpFlowExtensionCodec.TRAFFIC_ACTION_TERMINAL + MISSING_MEMBER_MESSAGE);
            }

            valuePart = commaPart[1].split("=");
            if (valuePart[0].matches(BgpFlowExtensionCodec.TRAFFIC_ACTION_SAMPLE)) {
                boolean sample = Boolean.parseBoolean(valuePart[1].trim());
                resultBuilder.setSample(sample);
            } else {
                nullIsIllegal(valuePart[0], BgpFlowExtensionCodec.TRAFFIC_ACTION_SAMPLE + MISSING_MEMBER_MESSAGE);
            }

            valuePart = commaPart[2].split("=");
            if (valuePart[0].matches(BgpFlowExtensionCodec.TRAFFIC_ACTION_RPD)) {
                boolean rpd = Boolean.parseBoolean(valuePart[1].trim());
                resultBuilder.setRpd(rpd);
            } else {
                nullIsIllegal(valuePart[0], BgpFlowExtensionCodec.TRAFFIC_ACTION_RPD + MISSING_MEMBER_MESSAGE);
            }

            resultBuilder.setType(ExtFlowTypes.ExtType.TRAFFIC_ACTION);

            return resultBuilder.build();
        }
    }

    /** Traffic redirect decoder.*/
    private class BgpTrafficRedirectDecode implements ExtensionDecoder {
        @Override
        public ExtFlowTypes decodeExtension(ObjectNode json) {
            if (json == null || !json.isObject()) {
                return null;
            }

            ExtTrafficRedirect.Builder resultBuilder = new DefaultExtTrafficRedirect.Builder();

            String action = nullIsIllegal(json.get(BgpFlowExtensionCodec.TRAFFIC_REDIRECTION),
                    BgpFlowExtensionCodec.TRAFFIC_REDIRECTION + MISSING_MEMBER_MESSAGE).asText();
            resultBuilder.setRedirect(action);
            resultBuilder.setType(ExtFlowTypes.ExtType.TRAFFIC_REDIRECT);

            return resultBuilder.build();
        }
    }

    /** Traffic marking decoder.*/
    private class BgpTrafficMarkingDecode implements ExtensionDecoder {
        @Override
        public ExtFlowTypes decodeExtension(ObjectNode json) {
            if (json == null || !json.isObject()) {
                return null;
            }

            ExtTrafficMarking.Builder resultBuilder = new DefaultExtTrafficMarking.Builder();

            String action = nullIsIllegal(json.get(BgpFlowExtensionCodec.TRAFFIC_MARKING),
                    BgpFlowExtensionCodec.TRAFFIC_MARKING + MISSING_MEMBER_MESSAGE).asText();

            if ((action.length() != 1) || action.isEmpty()) {
                nullIsIllegal(action, BgpFlowExtensionCodec.TRAFFIC_MARKING + MALFORMED_MEMBER_MESSAGE);
            }

            resultBuilder.setMarking((byte) action.charAt(0));
            resultBuilder.setType(ExtFlowTypes.ExtType.TRAFFIC_MARKING);

            return resultBuilder.build();
        }
    }

    /** Wide community flag decoder.*/
    private class BgpWcommFlagsDecode implements ExtensionDecoder {
        @Override
        public ExtFlowTypes decodeExtension(ObjectNode json) {
            if (json == null || !json.isObject()) {
                return null;
            }

            ExtWideCommunityInt.Builder resultBuilder = new DefaultExtWideCommunityInt.Builder();

            String wideComm = nullIsIllegal(json.get(BgpFlowExtensionCodec.WIDE_COMM_FLAGS),
                    BgpFlowExtensionCodec.WIDE_COMM_FLAGS + MISSING_MEMBER_MESSAGE).asText();
            resultBuilder.setwCommInt(Integer.valueOf(wideComm));
            resultBuilder.setType(ExtFlowTypes.ExtType.WIDE_COMM_FLAGS);

            return resultBuilder.build();
        }
    }

    /** Wide community hop count decoder.*/
    private class BgpWcommHopCountDecode implements ExtensionDecoder {
        @Override
        public ExtFlowTypes decodeExtension(ObjectNode json) {
            if (json == null || !json.isObject()) {
                return null;
            }

            ExtWideCommunityInt.Builder resultBuilder = new DefaultExtWideCommunityInt.Builder();

            String wideComm = nullIsIllegal(json.get(BgpFlowExtensionCodec.WIDE_COMM_HOP_COUNT),
                    BgpFlowExtensionCodec.WIDE_COMM_HOP_COUNT + MISSING_MEMBER_MESSAGE).asText();
            resultBuilder.setwCommInt(Integer.valueOf(wideComm));
            resultBuilder.setType(ExtFlowTypes.ExtType.WIDE_COMM_HOP_COUNT);

            return resultBuilder.build();
        }
    }

    /** Wide community decoder.*/
    private class BgpWcommCommunityDecode implements ExtensionDecoder {
        @Override
        public ExtFlowTypes decodeExtension(ObjectNode json) {
            if (json == null || !json.isObject()) {
                return null;
            }

            ExtWideCommunityInt.Builder resultBuilder = new DefaultExtWideCommunityInt.Builder();

            String wideComm = nullIsIllegal(json.get(BgpFlowExtensionCodec.WIDE_COMM_COMMUNITY),
                    BgpFlowExtensionCodec.WIDE_COMM_COMMUNITY + MISSING_MEMBER_MESSAGE).asText();
            resultBuilder.setwCommInt(Integer.valueOf(wideComm));
            resultBuilder.setType(ExtFlowTypes.ExtType.WIDE_COMM_COMMUNITY);

            return resultBuilder.build();
        }
    }

    /** Wide community context AS decoder.*/
    private class BgpWcommContextAsDecode implements ExtensionDecoder {
        @Override
        public ExtFlowTypes decodeExtension(ObjectNode json) {
            if (json == null || !json.isObject()) {
                return null;
            }

            ExtWideCommunityInt.Builder resultBuilder = new DefaultExtWideCommunityInt.Builder();

            String wideComm = nullIsIllegal(json.get(BgpFlowExtensionCodec.WIDE_COMM_CONTEXT_AS),
                    BgpFlowExtensionCodec.WIDE_COMM_CONTEXT_AS + MISSING_MEMBER_MESSAGE).asText();
            resultBuilder.setwCommInt(Integer.valueOf(wideComm));
            resultBuilder.setType(ExtFlowTypes.ExtType.WIDE_COMM_CONTEXT_AS);

            return resultBuilder.build();
        }
    }

    /** Wide community local AS decoder.*/
    private class BgpWcommLocalAsDecode implements ExtensionDecoder {
        @Override
        public ExtFlowTypes decodeExtension(ObjectNode json) {
            if (json == null || !json.isObject()) {
                return null;
            }

            ExtWideCommunityInt.Builder resultBuilder = new DefaultExtWideCommunityInt.Builder();

            String wideComm = nullIsIllegal(json.get(BgpFlowExtensionCodec.WIDE_COMM_LOCAL_AS),
                    BgpFlowExtensionCodec.WIDE_COMM_LOCAL_AS + MISSING_MEMBER_MESSAGE).asText();
            resultBuilder.setwCommInt(Integer.valueOf(wideComm));
            resultBuilder.setType(ExtFlowTypes.ExtType.WIDE_COMM_LOCAL_AS);

            return resultBuilder.build();
        }
    }

    /** Wide community parameter decoder.*/
    private class BgpWcommParameterDecode implements ExtensionDecoder {
        @Override
        public ExtFlowTypes decodeExtension(ObjectNode json) {
            if (json == null || !json.isObject()) {
                return null;
            }

            ExtWideCommunityInt.Builder resultBuilder = new DefaultExtWideCommunityInt.Builder();

            String wideComm = nullIsIllegal(json.get(BgpFlowExtensionCodec.WIDE_COMM_PARAMETER),
                    BgpFlowExtensionCodec.WIDE_COMM_PARAMETER + MISSING_MEMBER_MESSAGE).asText();
            resultBuilder.setwCommInt(Integer.valueOf(wideComm));
            resultBuilder.setType(ExtFlowTypes.ExtType.WIDE_COMM_PARAMETER);

            return resultBuilder.build();
        }
    }

    /** Wide community target decoder.*/
    private class BgpWcommTargetDecode implements ExtensionDecoder {
        @Override
        public ExtFlowTypes decodeExtension(ObjectNode json) {
            if (json == null || !json.isObject()) {
                return null;
            }

            ExtTarget.Builder resultBuilder = new DefaultExtTarget.Builder();

            JsonNode jsonNodes = json.get(BgpFlowExtensionCodec.WIDE_COMM_TARGET);
            if (jsonNodes == null) {
                nullIsIllegal(json.get(BgpFlowExtensionCodec.WIDE_COMM_TARGET),
                        BgpFlowExtensionCodec.WIDE_COMM_TARGET + MISSING_MEMBER_MESSAGE).asText();
            }

            JsonNode array = jsonNodes.path(BgpFlowExtensionCodec.WIDE_COMM_TGT_LOCAL_SP);
            if (array == null) {
                nullIsIllegal(array, BgpFlowExtensionCodec.WIDE_COMM_TGT_LOCAL_SP + MISSING_MEMBER_MESSAGE).asText();
            }

            ExtPrefix.Builder resultBuilderPfx = parseIpArrayToPrefix(array);
            resultBuilderPfx.setType(ExtFlowTypes.ExtType.IPV4_SRC_PFX);
            resultBuilder.setLocalSpeaker(resultBuilderPfx.build());

            array = jsonNodes.path(BgpFlowExtensionCodec.WIDE_COMM_TGT_REMOTE_SP);
            if (array == null) {
                nullIsIllegal(array, BgpFlowExtensionCodec.WIDE_COMM_TGT_REMOTE_SP + MISSING_MEMBER_MESSAGE).asText();
            }

            resultBuilderPfx = parseIpArrayToPrefix(array);
            resultBuilderPfx.setType(ExtFlowTypes.ExtType.IPV4_DST_PFX);
            resultBuilder.setRemoteSpeaker(resultBuilderPfx.build());
            resultBuilder.setType(ExtFlowTypes.ExtType.WIDE_COMM_TARGET);

            return resultBuilder.build();
        }
    }

    /** Wide community extended target decoder.*/
    private class BgpWcommExtTargetDecode implements ExtensionDecoder {
        @Override
        public ExtFlowTypes decodeExtension(ObjectNode json) {
            if (json == null || !json.isObject()) {
                return null;
            }

            ExtTarget.Builder resultBuilder = new DefaultExtTarget.Builder();

            JsonNode jsonNodes = json.get(BgpFlowExtensionCodec.WIDE_COMM_EXT_TARGET);
            if (jsonNodes == null) {
                nullIsIllegal(json.get(BgpFlowExtensionCodec.WIDE_COMM_EXT_TARGET),
                        BgpFlowExtensionCodec.WIDE_COMM_EXT_TARGET + MISSING_MEMBER_MESSAGE).asText();
            }

            JsonNode array = jsonNodes.path(BgpFlowExtensionCodec.WIDE_COMM_TGT_LOCAL_SP);
            if (array == null) {
                nullIsIllegal(array, BgpFlowExtensionCodec.WIDE_COMM_TGT_LOCAL_SP + MISSING_MEMBER_MESSAGE).asText();
            }

            ExtPrefix.Builder resultBuilderPfx = parseIpArrayToPrefix(array);
            resultBuilderPfx.setType(ExtFlowTypes.ExtType.IPV4_SRC_PFX);
            resultBuilder.setLocalSpeaker(resultBuilderPfx.build());

            array = jsonNodes.path(BgpFlowExtensionCodec.WIDE_COMM_TGT_REMOTE_SP);
            if (array == null) {
                nullIsIllegal(array, BgpFlowExtensionCodec.WIDE_COMM_TGT_REMOTE_SP + MISSING_MEMBER_MESSAGE).asText();
            }

            resultBuilderPfx = parseIpArrayToPrefix(array);
            resultBuilderPfx.setType(ExtFlowTypes.ExtType.IPV4_DST_PFX);
            resultBuilder.setRemoteSpeaker(resultBuilderPfx.build());
            resultBuilder.setType(ExtFlowTypes.ExtType.WIDE_COMM_EXT_TARGET);

            return resultBuilder.build();
        }
    }

    /** Ip address parser decoder.*/
    public ExtPrefix.Builder parseIpArrayToPrefix(JsonNode array) {

        ExtPrefix.Builder resultBuilder = new DefaultExtPrefix.Builder();
        String ip;
        IpPrefix prefix;
        IpAddress ipAddr;

        Iterator<JsonNode> itr =  array.iterator();
        while (itr.hasNext()) {
            ip = itr.next().asText();
            ipAddr = IpAddress.valueOf(ip);
            prefix = IpPrefix.valueOf(ipAddr, 32);
            resultBuilder.setPrefix(prefix);
        }

        return resultBuilder;
    }

    /**
     * Decodes the JSON into a BgpFlowType extension object.
     *
     * @return ExtFlowTypes object
     * @throws IllegalArgumentException if the JSON is invalid
     */
    public ExtFlowTypes decode() {
        String type = json.get(BgpFlowExtensionCodec.TYPE).asText();

        ExtensionDecoder decoder = decoderMap.get(type);
        if (decoder != null) {
            return decoder.decodeExtension(json);
        }

        throw new IllegalArgumentException("Type " + type + " is unknown");
    }
}
