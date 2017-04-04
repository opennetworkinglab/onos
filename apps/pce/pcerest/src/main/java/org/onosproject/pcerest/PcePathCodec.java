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
package org.onosproject.pcerest;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.Link.State.ACTIVE;
import static org.onosproject.net.Link.Type.DIRECT;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.DeviceId;
import org.onosproject.net.NetworkResource;
import org.onosproject.net.PortNumber;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.pce.pceservice.ExplicitPathInfo;
import org.onosproject.pce.pceservice.PcePath;
import org.onosproject.pce.pceservice.DefaultPcePath;
import org.onosproject.pce.pceservice.constraint.CostConstraint;
import org.onosproject.pce.pceservice.constraint.PceBandwidthConstraint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * PCE path json codec.
 */
public final class PcePathCodec extends JsonCodec<PcePath> {
    private final Logger log = LoggerFactory.getLogger(PcePathCodec.class);
    private static final String SOURCE = "source";
    private static final String DESTINATION = "destination";
    private static final String LSP_TYPE = "pathType";
    private static final String SYMBOLIC_PATH_NAME = "name";
    private static final String CONSTRAINT = "constraint";
    private static final String COST = "cost";
    private static final String BANDWIDTH = "bandwidth";
    private static final String PATH_ID = "pathId";
    private static final String EXPLICIT_PATH_INFO = "explicitPathInfo";
    private static final String MISSING_MEMBER_MESSAGE = " member is required in pce-path";
    public static final String JSON_NOT_NULL = "JsonNode can not be null";
    public static final byte SOURCE_DEVICEID_INDEX = 0;
    public static final byte SOURCE_PORTNO_INDEX = 1;
    public static final byte DESTINATION_DEVICEID_INDEX = 2;
    public static final byte DESTINATION_PORTNO_INDEX = 3;

    @Override
    public PcePath decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            log.error("Empty json input");
            return null;
        }

        // build pce-path
        PcePath.Builder resultBuilder = new DefaultPcePath.Builder();

        // retrieve source
        JsonNode jNode = json.get(SOURCE);
        if (jNode != null) {
            String src = jNode.asText();
            resultBuilder.source(src);
        }

        // retrieve destination
        jNode = json.get(DESTINATION);
        if (jNode != null) {
            String dst = jNode.asText();
            resultBuilder.destination(dst);
        }

        // retrieve lsp-type
        jNode = json.get(LSP_TYPE);
        if (jNode != null) {
            String lspType = jNode.asText();
            //Validating LSP type
            int type = Integer.parseInt(lspType);
            if ((type < 0) || (type > 2)) {
                return null;
            }
            resultBuilder.lspType(lspType);
        }

        // retrieve symbolic-path-name
        jNode = json.get(SYMBOLIC_PATH_NAME);
        if (jNode != null) {
            String name = jNode.asText();
            resultBuilder.name(name);
        }

        // retrieve constraint
        JsonNode constraintJNode = (JsonNode) json.path(CONSTRAINT);
        if ((constraintJNode != null) && (!constraintJNode.isMissingNode())) {
            // retrieve cost
            jNode = constraintJNode.get(COST);
            if (jNode != null) {
                String cost = jNode.asText();
                //Validating Cost type
                int costType = Integer.parseInt(cost);
                if ((costType < 1) || (costType > 2)) {
                    return null;
                }
                resultBuilder.costConstraint(cost);
            }

            // retrieve bandwidth
            jNode = constraintJNode.get(BANDWIDTH);
            if (jNode != null) {
                String bandwidth = jNode.asText();
                double bw = Double.parseDouble(bandwidth);
                if (bw < 0) {
                    return null;
                }
                resultBuilder.bandwidthConstraint(bandwidth);
            }
        }

        // Retrieve explicit path info
        JsonNode explicitPathInfo = json.get(EXPLICIT_PATH_INFO);
        if (explicitPathInfo != null) {
            List<ExplicitPathInfo> explicitPathInfoList =
                    ImmutableList.copyOf(jsonNodeToExplicitPathInfo(explicitPathInfo));
            if (explicitPathInfoList != null) {
                resultBuilder.explicitPathInfo(explicitPathInfoList);
            }
        }

        return resultBuilder.build();
    }

    private ExplicitPathInfo createListOfExplicitPathObj(JsonNode node) {
        int explicitPathType = Integer.parseInt(node.get("type").asText());
        DeviceId deviceId;
        PortNumber portNo;
        NetworkResource res;
        LinkedList<ExplicitPathInfo> list = Lists.newLinkedList();
        if ((explicitPathType < 0) || (explicitPathType > 1)) {
            return null;
        }
        ExplicitPathInfo.Type type = ExplicitPathInfo.Type.values()[explicitPathType];
        String subType = node.get("subtype").asText();
        if (Integer.parseInt(subType) == 0) {
            res = DeviceId.deviceId(node.get("value").asText());
        } else if (Integer.parseInt(subType) == 1) {

            String[] splitted = node.get("value").asText().split("/");

            if (splitted[SOURCE_DEVICEID_INDEX] != null
                    && splitted[SOURCE_PORTNO_INDEX] != null
                    && splitted[DESTINATION_DEVICEID_INDEX] != null
                    && splitted[DESTINATION_PORTNO_INDEX] != null) {
                return null;
            }
            deviceId = DeviceId.deviceId(splitted[SOURCE_DEVICEID_INDEX]);
            portNo = PortNumber.portNumber(splitted[SOURCE_PORTNO_INDEX]);
            ConnectPoint cpSrc = new ConnectPoint(deviceId, portNo);
            deviceId = DeviceId.deviceId(splitted[DESTINATION_DEVICEID_INDEX]);
            portNo = PortNumber.portNumber(splitted[DESTINATION_PORTNO_INDEX]);
            ConnectPoint cpDst = new ConnectPoint(deviceId, portNo);
            res = DefaultLink.builder()
                    .providerId(ProviderId.NONE)
                    .src(cpSrc)
                    .dst(cpDst)
                    .type(DIRECT)
                    .state(ACTIVE)
                    .build();
        } else {
            return null;
        }

        return new ExplicitPathInfo(type, res);
    }

    private Collection<ExplicitPathInfo> jsonNodeToExplicitPathInfo(JsonNode explicitPathInfo) {
        checkNotNull(explicitPathInfo, JSON_NOT_NULL);

        Integer i = 0;
        NetworkResource res;
        LinkedList<ExplicitPathInfo> list = Lists.newLinkedList();
        if (explicitPathInfo.isArray()) {
            for (JsonNode node : explicitPathInfo) {
                ExplicitPathInfo obj = createListOfExplicitPathObj(node);
                if (obj == null) {
                    return null;
                }
                list.add(obj);
            }
        } else {
            ExplicitPathInfo obj = createListOfExplicitPathObj(explicitPathInfo);
            if (obj == null) {
                return null;
            }
            list.add(obj);
        }

        return Collections.unmodifiableCollection(list);
    }

    @Override
    public ObjectNode encode(PcePath path, CodecContext context) {
        checkNotNull(path, "path output cannot be null");
        ObjectNode result = context.mapper()
                .createObjectNode()
                .put(PATH_ID, path.id().id())
                .put(SOURCE, path.source())
                .put(DESTINATION, path.destination())
                .put(LSP_TYPE, path.lspType().type())
                .put(SYMBOLIC_PATH_NAME, path.name());

        ObjectNode constraintNode = context.mapper()
                .createObjectNode()
                .put(COST, ((CostConstraint) path.costConstraint()).type().type())
                .put(BANDWIDTH, ((PceBandwidthConstraint) path.bandwidthConstraint()).bandwidth().bps());

        if (path.explicitPathInfo() != null && !path.explicitPathInfo().isEmpty()) {
            ArrayNode arrayNode = context.mapper().createArrayNode();
            for (ExplicitPathInfo e : path.explicitPathInfo()) {
                ObjectNode node = context.mapper()
                        .createObjectNode()
                        .put("type", e.type().toString())
                        .put("value", e.value().toString());
                arrayNode.add(node);
            }
            result.set(EXPLICIT_PATH_INFO, arrayNode);
        }

        result.set(CONSTRAINT, constraintNode);
        return result;
    }
}
