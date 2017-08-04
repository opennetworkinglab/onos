/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.driver.extensions.codec;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.driver.extensions.Ofdpa3SetMplsL2Port;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;

/**
 * JSON Codec for Ofdpa set ovid class.
 */
public class Ofdpa3SetMplsL2PortCodec extends JsonCodec<Ofdpa3SetMplsL2Port> {

    private static final String MPLS_L2_PORT = "mplsL2Port";
    private static final String MISSING_MEMBER_MESSAGE = " member is required in Ofdpa3SetMplsL2Port";
    private static final String MISSING_MPLS_L2_PORT_MESSAGE = "MPLS_L2_PORT cannot be null";

    @Override
    public ObjectNode encode(Ofdpa3SetMplsL2Port setMplsL2Port, CodecContext context) {
        checkNotNull(setMplsL2Port, MISSING_MPLS_L2_PORT_MESSAGE);
        return context.mapper().createObjectNode()
                .put(MPLS_L2_PORT, setMplsL2Port.mplsL2Port());
    }

    @Override
    public Ofdpa3SetMplsL2Port decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        // parse ofdpa action set mpls l2 port
        int mplsL2Port = (int) nullIsIllegal(json.get(MPLS_L2_PORT),
                MPLS_L2_PORT + MISSING_MEMBER_MESSAGE).asInt();
        return new Ofdpa3SetMplsL2Port(mplsL2Port);
    }
}
