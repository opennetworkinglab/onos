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

import org.onlab.packet.VlanId;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.incubator.net.l2monitoring.cfm.DefaultMep;
import org.onosproject.incubator.net.l2monitoring.cfm.Mep;
import org.onosproject.incubator.net.l2monitoring.cfm.Mep.MepBuilder;
import org.onosproject.incubator.net.l2monitoring.cfm.Mep.MepDirection;
import org.onosproject.incubator.net.l2monitoring.cfm.Mep.Priority;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdCharStr;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdShort;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdId;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdIdCharStr;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Duration;

/**
 * Encode and decode to/from JSON to Mep object.
 */
public class MepCodec extends JsonCodec<Mep> {
    private static final String ADMINISTRATIVE_STATE = "administrative-state";
    private static final String PRIMARY_VID = "primary-vid";
    private static final String CCM_LTM_PRIORITY = "ccm-ltm-priority";
    private static final String CCI_ENABLED = "cci-enabled";
    private static final String FNG_ADDRESS = "fng-address";
    private static final String LOWEST_FAULT_PRIORITY_DEFECT = "lowest-fault-priority-defect";
    private static final String DEFECT_PRESENT_TIME = "defect-present-time";
    private static final String DEFECT_ABSENT_TIME = "defect-absent-time";

    /**
     * Decodes the Mep entity from JSON.
     *
     * @param json    JSON to decode
     * @param context decoding context
     * @param mdName The MD name
     * @param maName The MA name
     * @return decoded Mep
     * @throws java.lang.UnsupportedOperationException if the codec does not
     *                                                 support decode operations
     */
     public Mep decode(ObjectNode json, CodecContext context, String
                        mdName, String maName) {
        if (json == null || !json.isObject()) {
            return null;
        }

        JsonNode mepNode = json.get("mep");

        int mepId = Integer.parseInt(
                nullIsIllegal(mepNode.get("mepId"), "mepId is required").asText());
        DeviceId deviceId = DeviceId.deviceId(
                nullIsIllegal(mepNode.get("deviceId"), "deviceId is required")
                .asText());
        PortNumber port = PortNumber
                .portNumber(Long.parseLong(
                        nullIsIllegal(mepNode.get("port"), "port is required")
                        .asText()));
        MepDirection direction = MepDirection.valueOf(
                nullIsIllegal(mepNode.get("direction"), "direction is required").
                asText());

        try {
            MdId mdId = MdIdCharStr.asMdId(mdName);
            MaIdShort maId = MaIdCharStr.asMaId(maName);
            MepBuilder mepBuilder = DefaultMep
                    .builder(MepId.valueOf((short) mepId),
                            deviceId, port, direction, mdId, maId);

            if (mepNode.get(PRIMARY_VID) != null) {
                mepBuilder.primaryVid(VlanId.vlanId(
                        (short) mepNode.get(PRIMARY_VID).asInt(0)));
            }

            if (mepNode.get(ADMINISTRATIVE_STATE) != null) {
                mepBuilder.administrativeState(mepNode.get(ADMINISTRATIVE_STATE)
                        .asBoolean());
            }

            if (mepNode.get(CCM_LTM_PRIORITY) != null) {
                mepBuilder.ccmLtmPriority(
                        Priority.values()[mepNode.get(CCM_LTM_PRIORITY).asInt(0)]);
            }

            if (mepNode.get(CCI_ENABLED) != null) {
                mepBuilder.cciEnabled(mepNode.get(CCI_ENABLED).asBoolean());
            }

            if (mepNode.get(LOWEST_FAULT_PRIORITY_DEFECT) != null) {
                mepBuilder.lowestFaultPriorityDefect(
                        Mep.LowestFaultDefect.values()[mepNode.get(LOWEST_FAULT_PRIORITY_DEFECT).asInt()]);
            }

            if (mepNode.get(DEFECT_ABSENT_TIME) != null) {
                mepBuilder.defectAbsentTime(
                        Duration.parse(mepNode.get(DEFECT_ABSENT_TIME).asText()));
            }

            if (mepNode.get(DEFECT_PRESENT_TIME) != null) {
                mepBuilder.defectPresentTime(
                        Duration.parse(mepNode.get(DEFECT_PRESENT_TIME).asText()));
            }

            if (mepNode.get(FNG_ADDRESS) != null) {
                mepBuilder.fngAddress((new FngAddressCodec())
                        .decode((ObjectNode) mepNode, context));
            }


            return mepBuilder.build();
        } catch (CfmConfigException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Encodes the Mep entity into JSON.
     *
     * @param mep Mep to encode
     * @param context encoding context
     * @return JSON node
     * @throws java.lang.UnsupportedOperationException if the codec does not
     *                                                 support encode operations
     */
    @Override
    public ObjectNode encode(Mep mep, CodecContext context) {
        checkNotNull(mep, "Mep cannot be null");
        ObjectNode result = context.mapper().createObjectNode()
                .put("mepId", mep.mepId().id())
                .put("deviceId", mep.deviceId().toString())
                .put("port", mep.port().toLong())
                .put("direction", mep.direction().name())
                .put("mdName", mep.mdId().toString())
                .put("maName", mep.maId().toString())
                .put(ADMINISTRATIVE_STATE, mep.administrativeState())
                .put(CCI_ENABLED, mep.cciEnabled());
        if (mep.ccmLtmPriority() != null) {
            result.put(CCM_LTM_PRIORITY, mep.ccmLtmPriority().ordinal());
        }
        if (mep.primaryVid() != null) {
            result.put(PRIMARY_VID, mep.primaryVid().toShort());
        }
        if (mep.fngAddress() != null) {
            result.put(FNG_ADDRESS, new FngAddressCodec().encode(mep.fngAddress(), context));
        }
        if (mep.lowestFaultPriorityDefect() != null) {
            result.put(LOWEST_FAULT_PRIORITY_DEFECT, mep.lowestFaultPriorityDefect().ordinal());
        }
        if (mep.defectPresentTime() != null) {
            result.put(DEFECT_PRESENT_TIME, mep.defectPresentTime().toString());
        }
        if (mep.defectAbsentTime() != null) {
            result.put(DEFECT_ABSENT_TIME, mep.defectAbsentTime().toString());
        }

        return result;
    }
}
