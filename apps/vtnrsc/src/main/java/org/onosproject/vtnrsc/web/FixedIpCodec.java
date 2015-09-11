/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.vtnrsc.web;

import static com.google.common.base.Preconditions.checkNotNull;

import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.vtnrsc.FixedIp;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * VirtualPort FixedIp codec.
 */
public final class FixedIpCodec extends JsonCodec<FixedIp> {

    @Override
    public ObjectNode encode(FixedIp fixIp, CodecContext context) {
        checkNotNull(fixIp, "FixedIp cannot be null");
        ObjectNode result = context.mapper().createObjectNode()
                .put("subnet_id", fixIp.subnetId().toString())
                .put("ip_address", fixIp.ip().toString());
        return result;
    }

}
