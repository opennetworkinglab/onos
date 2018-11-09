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

import org.onosproject.grpc.net.flow.models.FlowEntryEnumsProto;
import org.onosproject.net.flow.FlowEntry.FlowEntryState;
import org.onosproject.net.flow.FlowEntry.FlowLiveType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;


/**
 * gRPC FlowEntryEnumsProto message to equivalent ONOS FlowEntry enums conversion related utilities.
 */
public final class FlowEntryEnumsProtoTranslator {

    private static final Logger log = LoggerFactory.getLogger(FlowEntryEnumsProtoTranslator.class);

    /**
     * Translates {@link FlowEntryState} to gRPC FlowEntryState.
     *
     * @param flowEntryState {@link FlowEntryState}
     * @return gRPC message
     */
    public static FlowEntryEnumsProto.FlowEntryStateProto translate(FlowEntryState flowEntryState) {

        switch (flowEntryState) {
            case PENDING_ADD:
                return FlowEntryEnumsProto.FlowEntryStateProto.PENDING_ADD;
            case ADDED:
                return FlowEntryEnumsProto.FlowEntryStateProto.ADDED;
            case PENDING_REMOVE:
                return FlowEntryEnumsProto.FlowEntryStateProto.PENDING_REMOVE;
            case REMOVED:
                return FlowEntryEnumsProto.FlowEntryStateProto.REMOVED;
            case FAILED:
                return FlowEntryEnumsProto.FlowEntryStateProto.FAILED;

            default:
                log.warn("Unexpected flow entry state: {}", flowEntryState);
                return FlowEntryEnumsProto.FlowEntryStateProto.UNRECOGNIZED;
        }
    }

    /**
     * Translates gRPC FlowEntryState to {@link FlowEntryState}.
     *
     * @param flowEntryState gRPC message
     * @return {@link FlowEntryState}
     */
    public static Optional<FlowEntryState> translate(FlowEntryEnumsProto.FlowEntryStateProto flowEntryState) {

        switch (flowEntryState) {
            case PENDING_ADD:
                return Optional.of(FlowEntryState.PENDING_ADD);
            case ADDED:
                return Optional.of(FlowEntryState.ADDED);
            case PENDING_REMOVE:
                return Optional.of(FlowEntryState.PENDING_REMOVE);
            case REMOVED:
                return Optional.of(FlowEntryState.REMOVED);
            case FAILED:
                return Optional.of(FlowEntryState.FAILED);

            default:
                log.warn("Unexpected flow entry state: {}", flowEntryState);
                return Optional.empty();
        }
    }

    /**
     * Translates {@link FlowLiveType} to gRPC FlowLiveType.
     *
     * @param flowLiveType {@link FlowLiveType}
     * @return gRPC message
     */
    public static FlowEntryEnumsProto.FlowLiveTypeProto translate(FlowLiveType flowLiveType) {

        switch (flowLiveType) {
            case IMMEDIATE:
                return FlowEntryEnumsProto.FlowLiveTypeProto.IMMEDIATE;
            case SHORT:
                return FlowEntryEnumsProto.FlowLiveTypeProto.SHORT;
            case MID:
                return FlowEntryEnumsProto.FlowLiveTypeProto.MID;
            case LONG:
                return FlowEntryEnumsProto.FlowLiveTypeProto.LONG;
            case UNKNOWN:
                return FlowEntryEnumsProto.FlowLiveTypeProto.UNKNOWN;

            default:
                log.warn("Unexpected flow live type : {}", flowLiveType);
                return FlowEntryEnumsProto.FlowLiveTypeProto.UNRECOGNIZED;
        }
    }

    /**
     * Translates gRPC FlowLiveType to {@link FlowLiveType}.
     *
     * @param flowLiveType gRPC message
     * @return {@link FlowLiveType}
     */
    public static Optional<FlowLiveType> translate(FlowEntryEnumsProto.FlowLiveTypeProto flowLiveType) {

        switch (flowLiveType) {
            case IMMEDIATE:
                return Optional.of(FlowLiveType.IMMEDIATE);
            case SHORT:
                return Optional.of(FlowLiveType.SHORT);
            case MID:
                return Optional.of(FlowLiveType.MID);
            case LONG:
                return Optional.of(FlowLiveType.LONG);
            case UNKNOWN:
                return Optional.of(FlowLiveType.UNKNOWN);

            default:
                log.warn("Unexpected flow live type : {}", flowLiveType);
                return Optional.empty();
        }
    }

    // Utility class not intended for instantiation.
    private FlowEntryEnumsProtoTranslator() {}
}
