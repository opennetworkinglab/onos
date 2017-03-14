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

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Flow entry JSON codec.
 */
public final class FlowEntryCodec extends JsonCodec<FlowEntry> {

    @Override
    public ObjectNode encode(FlowEntry flowEntry, CodecContext context) {
        checkNotNull(flowEntry, "Flow entry cannot be null");

        CoreService service = context.getService(CoreService.class);

        ApplicationId appId = service.getAppId(flowEntry.appId());

        String strAppId = (appId == null) ? "<none>" : appId.name();

        final ObjectNode result = context.mapper().createObjectNode()
                .put("id", Long.toString(flowEntry.id().value()))
                .put("tableId", flowEntry.tableId())
                .put("appId", strAppId)
                .put("groupId", flowEntry.groupId().id())
                .put("priority", flowEntry.priority())
                .put("timeout", flowEntry.timeout())
                .put("isPermanent", flowEntry.isPermanent())
                .put("deviceId", flowEntry.deviceId().toString())
                .put("state", flowEntry.state().toString())
                .put("life", flowEntry.life()) //FIXME life is destroying precision (seconds granularity is default)
                .put("packets", flowEntry.packets())
                .put("bytes", flowEntry.bytes())
                .put("liveType", flowEntry.liveType().toString())
                .put("lastSeen", flowEntry.lastSeen());

        if (flowEntry.treatment() != null) {
            final JsonCodec<TrafficTreatment> treatmentCodec =
                    context.codec(TrafficTreatment.class);
            result.set("treatment", treatmentCodec.encode(flowEntry.treatment(), context));
        }

        if (flowEntry.selector() != null) {
            final JsonCodec<TrafficSelector> selectorCodec =
                    context.codec(TrafficSelector.class);
            result.set("selector", selectorCodec.encode(flowEntry.selector(), context));
        }

        return result;
    }

}

