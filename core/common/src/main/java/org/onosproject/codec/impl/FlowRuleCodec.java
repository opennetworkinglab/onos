/*
 * Copyright 2015-present Open Networking Laboratory
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;

/**
 * Flow rule JSON codec.
 */
public final class FlowRuleCodec extends JsonCodec<FlowRule> {

    private static final String PRIORITY = "priority";
    private static final String TIMEOUT = "timeout";
    private static final String IS_PERMANENT = "isPermanent";
    private static final String APP_ID = "appId";
    private static final String TABLE_ID = "tableId";
    private static final String DEVICE_ID = "deviceId";
    private static final String TREATMENT = "treatment";
    private static final String SELECTOR = "selector";
    private static final String MISSING_MEMBER_MESSAGE =
                                " member is required in FlowRule";
    public static final String REST_APP_ID = "org.onosproject.rest";

    @Override
    public ObjectNode encode(FlowRule flowRule, CodecContext context) {
        checkNotNull(flowRule, "Flow rule cannot be null");

        CoreService service = context.getService(CoreService.class);
        ApplicationId appId = service.getAppId(flowRule.appId());
        String strAppId = (appId == null) ? "<none>" : appId.name();

        final ObjectNode result = context.mapper().createObjectNode()
                .put("id", Long.toString(flowRule.id().value()))
                .put("tableId", flowRule.tableId())
                .put("appId", strAppId)
                .put("priority", flowRule.priority())
                .put("timeout", flowRule.timeout())
                .put("isPermanent", flowRule.isPermanent())
                .put("deviceId", flowRule.deviceId().toString());

        if (flowRule.treatment() != null) {
            final JsonCodec<TrafficTreatment> treatmentCodec =
                    context.codec(TrafficTreatment.class);
            result.set("treatment", treatmentCodec.encode(flowRule.treatment(), context));
        }

        if (flowRule.selector() != null) {
            final JsonCodec<TrafficSelector> selectorCodec =
                    context.codec(TrafficSelector.class);
            result.set("selector", selectorCodec.encode(flowRule.selector(), context));
        }

        return result;
    }

    @Override
    public FlowRule decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        FlowRule.Builder resultBuilder = new DefaultFlowRule.Builder();

        CoreService coreService = context.getService(CoreService.class);
        JsonNode appIdJson = json.get(APP_ID);
        String appId = appIdJson != null ? appIdJson.asText() : REST_APP_ID;
        resultBuilder.fromApp(coreService.registerApplication(appId));

        int priority = nullIsIllegal(json.get(PRIORITY),
                PRIORITY + MISSING_MEMBER_MESSAGE).asInt();
        resultBuilder.withPriority(priority);

        boolean isPermanent = nullIsIllegal(json.get(IS_PERMANENT),
                IS_PERMANENT + MISSING_MEMBER_MESSAGE).asBoolean();
        if (isPermanent) {
            resultBuilder.makePermanent();
        } else {
            resultBuilder.makeTemporary(nullIsIllegal(json.get(TIMEOUT),
                            TIMEOUT
                            + MISSING_MEMBER_MESSAGE
                            + " if the flow is temporary").asInt());
        }

        JsonNode tableIdJson = json.get(TABLE_ID);
        if (tableIdJson != null) {
            resultBuilder.forTable(tableIdJson.asInt());
        }

        DeviceId deviceId = DeviceId.deviceId(nullIsIllegal(json.get(DEVICE_ID),
                DEVICE_ID + MISSING_MEMBER_MESSAGE).asText());
        resultBuilder.forDevice(deviceId);

        ObjectNode treatmentJson = get(json, TREATMENT);
        if (treatmentJson != null) {
            JsonCodec<TrafficTreatment> treatmentCodec =
                    context.codec(TrafficTreatment.class);
            resultBuilder.withTreatment(treatmentCodec.decode(treatmentJson, context));
        }

        ObjectNode selectorJson = get(json, SELECTOR);
        if (selectorJson != null) {
            JsonCodec<TrafficSelector> selectorCodec =
                    context.codec(TrafficSelector.class);
            resultBuilder.withSelector(selectorCodec.decode(selectorJson, context));
        }

        return resultBuilder.build();
    }
}
