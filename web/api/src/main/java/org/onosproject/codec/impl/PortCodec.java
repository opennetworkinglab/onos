/*
 * Copyright 2014 Open Networking Laboratory
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
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Device port JSON codec.
 */
public class PortCodec extends AnnotatedCodec<Port> {

    @Override
    public ObjectNode encode(Port port, CodecContext context) {
        checkNotNull(port, "Port cannot be null");
        ObjectNode result = context.mapper().createObjectNode()
                .put("port", portName(port.number()))
                .put("isEnabled", port.isEnabled())
                .put("type", port.type().toString().toLowerCase())
                .put("portSpeed", port.portSpeed());
        return annotate(result, port, context);
    }

    private String portName(PortNumber port) {
        return port.equals(PortNumber.LOCAL) ? "local" : port.toString();
    }

}
