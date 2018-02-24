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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;

import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.incubator.net.l2monitoring.cfm.DefaultMaintenanceDomain;
import org.onosproject.incubator.net.l2monitoring.cfm.MaintenanceDomain;
import org.onosproject.incubator.net.l2monitoring.cfm.MaintenanceDomain.MdLevel;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdId;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdMaNameUtil;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Encode and decode to/from JSON to MaintenanceDomain object.
 */
public class MaintenanceDomainCodec extends JsonCodec<MaintenanceDomain> {

    private static final String MD_LEVEL = "mdLevel";
    private static final String MD_NUMERIC_ID = "mdNumericId";
    private static final String MD = "md";
    private static final String MD_NAME = "mdName";
    private static final String MD_NAME_TYPE = "mdNameType";

    public MaintenanceDomainCodec() {
        super();

    }

    /**
     * Encodes the MaintenanceDomain entity into JSON.
     *
     * @param md MaintenanceDomain to encode
     * @param context encoding context
     * @return JSON node
     * @throws java.lang.UnsupportedOperationException if the codec does not
     *                                                 support encode operations
     */
    @Override
    public ObjectNode encode(MaintenanceDomain md, CodecContext context) {
        checkNotNull(md, "Maintenance Domain cannot be null");
        ObjectNode result = context.mapper().createObjectNode()
                .put(MD_NAME, md.mdId().toString())
                .put(MD_NAME_TYPE, md.mdId().nameType().name())
                .put(MD_LEVEL, md.mdLevel().name());
        if (md.mdNumericId() > 0) {
            result = result.put(MD_NUMERIC_ID, md.mdNumericId());
        }
        result.set("maList",
                new MaintenanceAssociationCodec()
                .encode(md.maintenanceAssociationList(), context));

        return result;
    }

    /**
     * Decodes the MaintenanceDomain entity from JSON.
     *
     * @param json    JSON to decode
     * @param context decoding context
     * @return decoded MaintenanceDomain
     * @throws java.lang.UnsupportedOperationException if the codec does not
     *                                                 support decode operations
     */
    @Override
    public MaintenanceDomain decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        JsonNode mdNode = json.get(MD);

        String mdName = nullIsIllegal(mdNode.get(MD_NAME), "mdName is required").asText();
        String mdNameType = MdId.MdNameType.CHARACTERSTRING.name();
        if (mdNode.get(MD_NAME_TYPE) != null) {
            mdNameType = mdNode.get(MD_NAME_TYPE).asText();
        }

        try {
            MdId mdId = MdMaNameUtil.parseMdName(mdNameType, mdName);
            MaintenanceDomain.MdBuilder builder = DefaultMaintenanceDomain.builder(mdId);
            JsonNode mdLevelNode = mdNode.get(MD_LEVEL);
            if (mdLevelNode != null) {
                MdLevel mdLevel = MdLevel.valueOf(mdLevelNode.asText());
                builder = builder.mdLevel(mdLevel);
            }
            JsonNode mdNumericIdNode = mdNode.get(MD_NUMERIC_ID);
            if (mdNumericIdNode != null) {
                short mdNumericId = (short) mdNumericIdNode.asInt();
                builder = builder.mdNumericId(mdNumericId);
            }

            return builder.build();
        } catch (CfmConfigException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Encodes the collection of the MaintenanceDomain entities.
     *
     * @param mdEntities collection of MaintenanceDomain to encode
     * @param context  encoding context
     * @return JSON array
     * @throws java.lang.UnsupportedOperationException if the codec does not
     *                                                 support encode operations
     */
    @Override
    public ArrayNode encode(Iterable<MaintenanceDomain> mdEntities, CodecContext context) {
        ArrayNode an = context.mapper().createArrayNode();
        mdEntities.forEach(md -> an.add(encode(md, context)));
        return an;
    }
}
