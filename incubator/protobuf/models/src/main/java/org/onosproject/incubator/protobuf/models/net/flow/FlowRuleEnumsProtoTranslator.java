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

import org.onosproject.grpc.net.flow.models.FlowRuleEnumsProto;
import org.onosproject.net.flow.FlowRule.FlowRemoveReason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * gRPC FlowRuleEnumsProto message to equivalent ONOS FlowRule enums conversion related utilities.
 */
public final class FlowRuleEnumsProtoTranslator {

    private static final Logger log = LoggerFactory.getLogger(FlowRuleEnumsProtoTranslator.class);

    /**
     * Translates {@link FlowRemoveReason} to gRPC FlowRemoveReason.
     *
     * @param flowRemoveReason {@link FlowRemoveReason}
     * @return gRPC message
     */
    public static FlowRuleEnumsProto.FlowRemoveReasonProto translate(FlowRemoveReason flowRemoveReason) {

        switch (flowRemoveReason) {
            case DELETE:
                return FlowRuleEnumsProto.FlowRemoveReasonProto.DELETE;
            case EVICTION:
                return FlowRuleEnumsProto.FlowRemoveReasonProto.EVICTION;
            case GROUP_DELETE:
                return FlowRuleEnumsProto.FlowRemoveReasonProto.GROUP_DELETE;
            case METER_DELETE:
                return FlowRuleEnumsProto.FlowRemoveReasonProto.METER_DELETE;
            case HARD_TIMEOUT:
                return FlowRuleEnumsProto.FlowRemoveReasonProto.HARD_TIMEOUT;
            case IDLE_TIMEOUT:
                return FlowRuleEnumsProto.FlowRemoveReasonProto.IDLE_TIMEOUT;
            case NO_REASON:
                return FlowRuleEnumsProto.FlowRemoveReasonProto.NO_REASON;
            default:
                log.warn("Unexpected flow remove reason: {}", flowRemoveReason);
                return FlowRuleEnumsProto.FlowRemoveReasonProto.UNRECOGNIZED;
        }
    }

    /**
     * Translates gRPC FlowRemoveReason to {@link FlowRemoveReason}.
     *
     * @param flowRemoveReason gRPC message
     * @return {@link FlowRemoveReason}
     */
    public static Optional<FlowRemoveReason> translate(FlowRuleEnumsProto.FlowRemoveReasonProto flowRemoveReason) {

        switch (flowRemoveReason) {
            case DELETE:
                return Optional.of(FlowRemoveReason.DELETE);
            case EVICTION:
                return Optional.of(FlowRemoveReason.EVICTION);
            case GROUP_DELETE:
                return Optional.of(FlowRemoveReason.GROUP_DELETE);
            case METER_DELETE:
                return Optional.of(FlowRemoveReason.METER_DELETE);
            case HARD_TIMEOUT:
                return Optional.of(FlowRemoveReason.HARD_TIMEOUT);
            case IDLE_TIMEOUT:
                return Optional.of(FlowRemoveReason.IDLE_TIMEOUT);
            case NO_REASON:
                return Optional.of(FlowRemoveReason.NO_REASON);
            default:
                log.warn("Unexpected flow remove reason: {}", flowRemoveReason);
                return Optional.empty();
        }
    }

    // Utility class not intended for instantiation.
    private FlowRuleEnumsProtoTranslator() {}
}
