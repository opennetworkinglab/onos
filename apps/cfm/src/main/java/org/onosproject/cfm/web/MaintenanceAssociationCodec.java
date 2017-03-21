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

import java.util.List;

import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.incubator.net.l2monitoring.cfm.Component;
import org.onosproject.incubator.net.l2monitoring.cfm.DefaultMaintenanceAssociation;
import org.onosproject.incubator.net.l2monitoring.cfm.MaintenanceAssociation;
import org.onosproject.incubator.net.l2monitoring.cfm.MaintenanceAssociation.CcmInterval;
import org.onosproject.incubator.net.l2monitoring.cfm.MaintenanceAssociation.MaBuilder;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaId2Octet;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdCharStr;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdIccY1731;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdPrimaryVid;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdRfc2685VpnId;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdShort;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Encode and decode to/from JSON to MaintenanceAssociation object.
 */
public class MaintenanceAssociationCodec extends JsonCodec<MaintenanceAssociation> {

    private static final String MA_NAME_TYPE = "maNameType";
    private static final String MA_NUMERIC_ID = "maNumericId";
    private static final String MA_NAME = "maName";
    private static final String CCM_INTERVAL = "ccm-interval";
    private static final String COMPONENT_LIST = "component-list";
    private static final String RMEP_LIST = "rmep-list";
    private static final String MA = "ma";

    @Override
    public ObjectNode encode(MaintenanceAssociation ma, CodecContext context) {
        checkNotNull(ma, "Maintenance Association cannot be null");
        ObjectNode result = context.mapper().createObjectNode()
                .put(MA_NAME, ma.maId().toString())
                .put(MA_NAME_TYPE, ma.maId().nameType().name());
        if (ma.maNumericId() > 0) {
            result = result.put(MA_NUMERIC_ID, ma.maNumericId());
        }
        if (ma.ccmInterval() != null) {
            result = result.put(CCM_INTERVAL, ma.ccmInterval().name());
        }

        result.set(COMPONENT_LIST, new ComponentCodec().encode(ma.componentList(), context));
        result.set(RMEP_LIST, new RMepCodec().encode(ma.remoteMepIdList(), context));

        return result;
    }

    public MaintenanceAssociation decode(ObjectNode json, CodecContext context, int mdNameLen) {
        if (json == null || !json.isObject()) {
            return null;
        }

        JsonNode maNode = json.get(MA);

        String maName = nullIsIllegal(maNode.get(MA_NAME), "maName is required").asText();
        String maNameType = MaIdShort.MaIdType.CHARACTERSTRING.name();
        if (maNode.get(MA_NAME_TYPE) != null) {
            maNameType = maNode.get(MA_NAME_TYPE).asText();
        }

        try {
            MaIdShort maId = null;
            MaIdShort.MaIdType maIdType = MaIdShort.MaIdType.valueOf(maNameType);
            switch (maIdType) {
                case PRIMARYVID:
                    maId = MaIdPrimaryVid.asMaId(maName);
                    break;
                case TWOOCTET:
                    maId = MaId2Octet.asMaId(maName);
                    break;
                case RFC2685VPNID:
                    maId = MaIdRfc2685VpnId.asMaIdHex(maName);
                    break;
                case ICCY1731:
                    maId = MaIdIccY1731.asMaId(maName);
                    break;
                case CHARACTERSTRING:
                default:
                    maId = MaIdCharStr.asMaId(maName);
            }
            MaBuilder builder =
                    DefaultMaintenanceAssociation.builder(maId, mdNameLen);

            JsonNode maNumericIdNode = maNode.get(MA_NUMERIC_ID);
            if (maNumericIdNode != null) {
                short mdNumericId = (short) maNumericIdNode.asInt();
                builder = builder.maNumericId(mdNumericId);
            }
            if (maNode.get(CCM_INTERVAL) != null) {
                builder.ccmInterval(CcmInterval.valueOf(maNode.get(CCM_INTERVAL).asText()));
            }

            List<Component> componentList = (new ComponentCodec()).decode((ArrayNode)
                    nullIsIllegal(maNode.get(COMPONENT_LIST),
                            "component-list is required"), context);
            for (Component component:componentList) {
                builder = builder.addToComponentList(component);
            }

            List<MepId> remoteMeps = (new RMepCodec()).decode(
                    (ArrayNode) nullIsIllegal(maNode.get(RMEP_LIST), "rmep-list is required"), context);
            for (MepId remoteMep:remoteMeps) {
                builder = builder.addToRemoteMepIdList(remoteMep);
            }

            return builder.build();
        } catch (CfmConfigException e) {
            throw new IllegalArgumentException(e);
        }

    }

    @Override
    public ArrayNode encode(Iterable<MaintenanceAssociation> maEntities, CodecContext context) {
        ArrayNode an = context.mapper().createArrayNode();
        maEntities.forEach(ma -> an.add(encode(ma, context)));
        return an;
    }
}
