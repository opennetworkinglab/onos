/*
 * Copyright 2015-present Open Networking Foundation
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

import org.onlab.packet.Ethernet;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ObjectNode;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Ethernet codec.
 */
public final class EthernetCodec extends JsonCodec<Ethernet> {

    private static final Logger log = LoggerFactory.getLogger(CriterionCodec.class);

    @Override
    public ObjectNode encode(Ethernet ethernet, CodecContext context) {
        checkNotNull(ethernet, "Ethernet cannot be null");

        final ObjectNode result = context.mapper().createObjectNode()
                .put("vlanId", ethernet.getVlanID())
                .put("etherType", ethernet.getEtherType())
                .put("priorityCode", ethernet.getPriorityCode())
                .put("pad", ethernet.isPad());

        if (ethernet.getDestinationMAC() != null) {
            result.put("destMac",
                       ethernet.getDestinationMAC().toString());
        }

        if (ethernet.getSourceMAC() != null) {
            result.put("srcMac",
                       ethernet.getSourceMAC().toString());
        }

        return result;
    }

}
