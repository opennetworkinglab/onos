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

import org.onosproject.grpc.net.flow.models.FlowEntryProtoOuterClass.FlowEntryProto;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.FlowEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * gRPC FlowEntryProto message to equivalent ONOS FlowEntry conversion related utilities.
 */
public final class FlowEntryProtoTranslator {

    private static final Logger log = LoggerFactory.getLogger(FlowEntryProtoTranslator.class);

    /**
     * Translates {@link FlowEntry} to gRPC FlowEntryProto.
     *
     * @param flowEntry {@link FlowEntry}
     * @return gRPC message
     */
    public static FlowEntryProto translate(FlowEntry flowEntry) {

        if (flowEntry != null) {
            FlowEntryProto.Builder builder = FlowEntryProto.newBuilder();
            builder.setLife(flowEntry.life())
                    .setPackets(flowEntry.packets())
                    .setBytes(flowEntry.bytes())
                    .setLastSeen(flowEntry.lastSeen())
                    .setErrType(flowEntry.errType())
                    .setErrCode(flowEntry.errCode())
                    .setState(FlowEntryEnumsProtoTranslator.translate(flowEntry.state()))
                    .setLiveType(FlowEntryEnumsProtoTranslator.translate(flowEntry.liveType()));
            return builder.build();
        }

        return FlowEntryProto.getDefaultInstance();
    }

    /**
     * Translates gRPC FlowEntry to {@link FlowEntry}.
     *
     * @param flowEntry gRPC message
     * @return {@link FlowEntry}
     */
    public static FlowEntry translate(FlowEntryProto flowEntry) {
        if (flowEntry.equals(FlowEntryProto.getDefaultInstance())) {
            return null;
        }

        FlowEntry.FlowEntryState state =
                FlowEntryEnumsProtoTranslator.translate(flowEntry.getState()).get();
        FlowEntry.FlowLiveType liveType =
                FlowEntryEnumsProtoTranslator.translate(flowEntry.getLiveType()).get();

        // TODO: need to instantiate FlowRule later
        return new DefaultFlowEntry(null, state, flowEntry.getLife(), liveType,
                flowEntry.getPackets(), flowEntry.getBytes());
    }

    // Utility class not intended for instantiation.
    private FlowEntryProtoTranslator() {}
}
