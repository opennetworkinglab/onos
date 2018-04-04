/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.segmentrouting.web;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.tuple.Pair;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.VlanId;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.ConnectPoint;
import org.onosproject.segmentrouting.pwaas.DefaultL2Tunnel;
import org.onosproject.segmentrouting.pwaas.DefaultL2TunnelDescription;
import org.onosproject.segmentrouting.pwaas.DefaultL2TunnelPolicy;
import org.onosproject.segmentrouting.pwaas.L2Mode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.onosproject.segmentrouting.pwaas.PwaasUtil.*;

/**
 * Codec of PseudowireCodec class.
 */
public final class PseudowireCodec extends JsonCodec<DefaultL2TunnelDescription> {

    // JSON field names
    private static final String PW_ID = "pwId";
    private static final String CP1 = "cP1";
    private static final String CP2 = "cP2";
    private static final String CP1_INNER_TAG = "cP1InnerTag";
    private static final String CP1_OUTER_TAG = "cP1OuterTag";
    private static final String CP2_INNER_TAG = "cP2InnerTag";
    private static final String CP2_OUTER_TAG = "cP2OouterTag";
    private static final String MODE = "mode";
    private static final String SERVICE_DELIM_TAG = "serviceTag";
    private static final String PW_LABEL = "pwLabel";

    // JSON field names for error in return
    private static final String FAILED_PWS = "failedPws";
    private static final String FAILED_PW = "pw";
    private static final String REASON = "reason";

    private static Logger log = LoggerFactory
            .getLogger(PseudowireCodec.class);

    @Override
    public ObjectNode encode(DefaultL2TunnelDescription pseudowire, CodecContext context) {
        final ObjectNode result = context.mapper().createObjectNode()
                .put(PW_ID, pseudowire.l2Tunnel().tunnelId());

        result.put(CP1, pseudowire.l2TunnelPolicy().cP1().toString());
        result.put(CP2, pseudowire.l2TunnelPolicy().cP2().toString());

        result.put(CP1_INNER_TAG, pseudowire.l2TunnelPolicy().cP1InnerTag().toString());
        result.put(CP1_OUTER_TAG, pseudowire.l2TunnelPolicy().cP1OuterTag().toString());


        result.put(CP2_INNER_TAG, pseudowire.l2TunnelPolicy().cP2InnerTag().toString());
        result.put(CP2_OUTER_TAG, pseudowire.l2TunnelPolicy().cP2OuterTag().toString());

        result.put(MODE, pseudowire.l2Tunnel().pwMode() == L2Mode.RAW ? "RAW" : "TAGGED");
        result.put(SERVICE_DELIM_TAG, pseudowire.l2Tunnel().sdTag().toString());
        result.put(PW_LABEL, pseudowire.l2Tunnel().pwLabel().toString());

        return result;
    }

    /**
     * Encoded in an Object Node the pseudowire and the specificError it failed.
     *
     * @param failedPW The failed pseudowire
     * @param specificError The specificError it failed
     * @param context Our context
     * @return A node containing the information we provided
     */
    public ObjectNode encodeError(DefaultL2TunnelDescription failedPW, String specificError,
                                          CodecContext context) {
        ObjectNode result = context.mapper().createObjectNode();

        ObjectNode pw = encode(failedPW, context);
        result.set(FAILED_PW, pw);
        result.put(REASON, specificError);

        return result;
    }

    /**
     * Returns a JSON containing the failed pseudowires and the reason that its one failed.
     *
     * @param failedPws Pairs of pws and reasons.
     * @param context The context
     * @return ObjectNode representing the json to return
     */
    public ObjectNode encodeFailedPseudowires(
            List<Pair<DefaultL2TunnelDescription, String>> failedPws,
            CodecContext context) {

        ArrayNode failedNodes = context.mapper().createArrayNode();
        failedPws.stream()
                .forEach(failed -> failedNodes.add(encodeError(failed.getKey(), failed.getValue(), context)));
        final ObjectNode toReturn = context.mapper().createObjectNode();
        toReturn.set(FAILED_PWS, failedNodes);
        return toReturn;
    }

    /**
     * Decodes a json containg a single field with the pseudowire id.
     *
     * @param json Json to decode.
     * @return The pseudowire id.
     */
    public static Integer decodeId(ObjectNode json) {

        Integer id = parsePwId(json.path(PW_ID).asText());
        if (id == null) {
            log.error("Pseudowire id is not an integer!");
            return null;
        }

        return id;
    }

    @Override
    public DefaultL2TunnelDescription decode(ObjectNode json, CodecContext context) {

        String tempString;

        Integer id = parsePwId(json.path(PW_ID).asText());
        if (id == null) {
            log.error("Pseudowire id is not an integer");
            return null;
        }

        ConnectPoint cP1, cP2;
        try {
            tempString = json.path(CP1).asText();
            cP1 = ConnectPoint.deviceConnectPoint(tempString);
        } catch (Exception e) {
            log.error("cP1 is not a valid connect point!");
            return null;
        }

        try {
            tempString = json.path(CP2).asText();
            cP2 = ConnectPoint.deviceConnectPoint(tempString);
        } catch (Exception e) {
            log.error("cP2 is not a valid connect point!");
            return null;
        }

        VlanId cP1InnerVlan = parseVlan(json.path(CP1_INNER_TAG).asText());
        VlanId cP1OuterVlan = parseVlan(json.path(CP1_OUTER_TAG).asText());
        VlanId cP2InnerVlan = parseVlan(json.path(CP2_INNER_TAG).asText());
        VlanId cP2OuterVlan = parseVlan(json.path(CP2_OUTER_TAG).asText());
        if ((cP1InnerVlan == null) || (cP1OuterVlan == null) ||
                (cP2InnerVlan == null) || (cP2OuterVlan == null)) {
            log.error("One or more vlan for cp1 or cp2 is malformed, it shouldbe an integer / Any / None / *");
            return null;
        }

        L2Mode mode = parseMode(json.path(MODE).asText());
        if (mode == null) {
            log.error("Mode should be RAW or TAGGED!");
            return null;
        }

        VlanId sdTag = parseVlan(json.path(SERVICE_DELIM_TAG).asText());
        if (sdTag == null) {
            log.error("SD tag is malformed, it should be an integer / Any / None / *");
            return null;
        }

        MplsLabel pwLabel = parsePWLabel(json.path(PW_LABEL).asText());
        if (pwLabel == null) {
            log.error("PW label is malformed, should be an integer!");
            return null;
        }

        DefaultL2Tunnel l2Tunnel;
        DefaultL2TunnelPolicy l2Policy;

        l2Tunnel = new DefaultL2Tunnel(mode, sdTag, id, pwLabel);
        l2Policy = new DefaultL2TunnelPolicy(id, cP1, cP1InnerVlan, cP1OuterVlan,
                                             cP2, cP2InnerVlan, cP2OuterVlan);

        return new DefaultL2TunnelDescription(l2Tunnel, l2Policy);

    }
}
