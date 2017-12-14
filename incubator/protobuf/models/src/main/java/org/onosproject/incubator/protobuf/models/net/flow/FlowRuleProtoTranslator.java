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
package org.onosproject.incubator.protobuf.models.net.flow;

import org.onosproject.grpc.net.flow.models.FlowRuleProtoOuterClass.FlowRuleProto;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.FlowRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * gRPC FlowRuleProto message to equivalent ONOS FlowRule conversion related utilities.
 */
public final class FlowRuleProtoTranslator {

    private static final Logger log = LoggerFactory.getLogger(FlowRuleProtoTranslator.class);

    /**
     * Translates {@link FlowRule} to gRPC FlowRuleProto.
     *
     * @param flowRule {@link FlowRule}
     * @return gRPC message
     */
    public static FlowRuleProto translate(FlowRule flowRule) {

        if (flowRule != null) {
            FlowRuleProto.Builder builder = FlowRuleProto.newBuilder();
            builder.setAppId(flowRule.appId())
                    .setDeviceId(flowRule.deviceId().toString())
                    .setFlowId(flowRule.id().value())
                    .setPermanent(flowRule.isPermanent())
                    .setPriority(flowRule.priority())
                    .setReason(FlowRuleEnumsProtoTranslator.translate(flowRule.reason()))
                    .setTableId(flowRule.tableId())
                    .setTableName(flowRule.table().toString());

            // TODO: need to set TrafficTreatment and TrafficSelector

            return builder.build();
        }
        return FlowRuleProto.getDefaultInstance();
    }

    /**
     * Translates gRPC FlowRule to {@link FlowRule}.
     *
     * @param flowRule gRPC message
     * @return {@link FlowRule}
     */
    public static FlowRule translate(FlowRuleProto flowRule) {

        if (flowRule.equals(FlowRuleProto.getDefaultInstance())) {
            return null;
        }

        DeviceId deviceId = DeviceId.deviceId(flowRule.getDeviceId());

        // TODO: to register AppId need to find a way to get CoreService

        FlowRule.FlowRemoveReason reason =
                FlowRuleEnumsProtoTranslator.translate(flowRule.getReason()).get();

        FlowRule.Builder resultBuilder = new DefaultFlowRule.Builder();
        resultBuilder.forDevice(deviceId);
        resultBuilder.forTable(flowRule.getTableId());
        resultBuilder.withPriority(flowRule.getPriority());
        resultBuilder.withCookie(flowRule.getFlowId());
        resultBuilder.withReason(reason);

        if (flowRule.getPermanent()) {
            resultBuilder.makePermanent();
        } else {
            resultBuilder.makeTemporary(flowRule.getTimeout());
        }

        // TODO: need to deal with TrafficTreatment and TrafficSelector

        return resultBuilder.build();
    }

    // Utility class not intended for instantiation.
    private FlowRuleProtoTranslator() {}
}
