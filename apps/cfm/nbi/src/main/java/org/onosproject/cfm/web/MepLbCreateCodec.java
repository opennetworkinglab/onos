/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.cfm.web;

import org.onlab.packet.MacAddress;
import org.onlab.util.HexString;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.incubator.net.l2monitoring.cfm.DefaultMepLbCreate;
import org.onosproject.incubator.net.l2monitoring.cfm.Mep.Priority;
import org.onosproject.incubator.net.l2monitoring.cfm.MepLbCreate;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Encode and decode to/from JSON to MepLbCreate object.
 */
public class MepLbCreateCodec extends JsonCodec<MepLbCreate> {

    public static final String NUMBER_MESSAGES = "numberMessages";
    public static final String REMOTE_MEP_ID = "remoteMepId";
    public static final String REMOTE_MEP_MAC = "remoteMepMac";
    public static final String DATA_TLV_HEX = "dataTlvHex";
    public static final String VLAN_DROP_ELIGIBLE = "vlanDropEligible";
    public static final String VLAN_PRIORITY = "vlanPriority";
    public static final String LOOPBACK = "loopback";

    /**
     * Encodes the MepLbCreate entity into JSON.
     *
     * @param mepLbCreate MepLbCreate to encode
     * @param context encoding context
     * @return JSON node
     * @throws java.lang.UnsupportedOperationException if the codec does not
     *                                                 support encode operations
     */
    @Override
    public ObjectNode encode(MepLbCreate mepLbCreate, CodecContext context) {
        checkNotNull(mepLbCreate, "Mep Lb Create cannot be null");
        ObjectNode result = context.mapper().createObjectNode()
                .put(NUMBER_MESSAGES, mepLbCreate.numberMessages());

        if (mepLbCreate.remoteMepId() != null) {
            result.put(REMOTE_MEP_ID, mepLbCreate.remoteMepId().value());
        } else {
            result.put(REMOTE_MEP_MAC, mepLbCreate.remoteMepAddress().toString());
        }

        if (mepLbCreate.dataTlvHex() != null) {
            result.put(DATA_TLV_HEX, mepLbCreate.dataTlvHex());
        }
        if (mepLbCreate.vlanDropEligible() != null) {
            result.put(VLAN_DROP_ELIGIBLE, mepLbCreate.vlanDropEligible());
        }
        if (mepLbCreate.vlanPriority() != null) {
            result.put(VLAN_PRIORITY, mepLbCreate.vlanPriority().ordinal());
        }
        return result;
    }

    /**
     * Decodes the MepLbCreate entity from JSON.
     *
     * @param json    JSON to decode
     * @param context decoding context
     * @return decoded MepLbCreate
     * @throws java.lang.UnsupportedOperationException if the codec does not
     *                                                 support decode operations
     */
    @Override
    public MepLbCreate decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        JsonNode loopbackNode = json.get(LOOPBACK);

        JsonNode remoteMepIdNode = loopbackNode.get(REMOTE_MEP_ID);
        JsonNode remoteMepMacNode = loopbackNode.get(REMOTE_MEP_MAC);

        MepLbCreate.MepLbCreateBuilder lbCreateBuilder;
        if (remoteMepIdNode != null) {
            MepId remoteMepId = MepId.valueOf((short) remoteMepIdNode.asInt());
            lbCreateBuilder = DefaultMepLbCreate.builder(remoteMepId);
        } else if (remoteMepMacNode != null) {
            MacAddress remoteMepMac = MacAddress.valueOf(
                                            remoteMepMacNode.asText());
            lbCreateBuilder = DefaultMepLbCreate.builder(remoteMepMac);
        } else {
            throw new IllegalArgumentException(
                    "Either a remoteMepId or a remoteMepMac");
        }

        JsonNode numMessagesNode = loopbackNode.get(NUMBER_MESSAGES);
        if (numMessagesNode != null) {
            int numMessages = numMessagesNode.asInt();
            lbCreateBuilder.numberMessages(numMessages);
        }

        JsonNode vlanDropEligibleNode = loopbackNode.get(VLAN_DROP_ELIGIBLE);
        if (vlanDropEligibleNode != null) {
            boolean vlanDropEligible = vlanDropEligibleNode.asBoolean();
            lbCreateBuilder.vlanDropEligible(vlanDropEligible);
        }

        JsonNode vlanPriorityNode = loopbackNode.get(VLAN_PRIORITY);
        if (vlanPriorityNode != null) {
            short vlanPriority = (short) vlanPriorityNode.asInt();
            lbCreateBuilder.vlanPriority(Priority.values()[vlanPriority]);
        }

        JsonNode dataTlvHexNode = loopbackNode.get(DATA_TLV_HEX);
        if (dataTlvHexNode != null) {
            String dataTlvHex = loopbackNode.get(DATA_TLV_HEX).asText();
            if (!dataTlvHex.isEmpty()) {
                lbCreateBuilder.dataTlv(HexString.fromHexString(dataTlvHex));
            }
        }

        return lbCreateBuilder.build();
    }
}
