/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.vtnweb.web;


import static com.google.common.base.Preconditions.checkNotNull;

import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.vtnrsc.ServiceFunctionGroup;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Service function JSON codec.
 */
public final class ServiceFunctionCodec extends JsonCodec<ServiceFunctionGroup> {

    private static final String NAME = "name";
    private static final String DESCRIPTION = "description";
    private static final String PORT_PAIR_LOAD = "port_pair_load";
    @Override
    public ObjectNode encode(ServiceFunctionGroup serviceFunction, CodecContext context) {
        checkNotNull(serviceFunction, "service cannot be null");
        ObjectNode result = context.mapper().createObjectNode()
                .put(NAME, serviceFunction.name())
                .put(DESCRIPTION, serviceFunction.description())
                .put(PORT_PAIR_LOAD, serviceFunction.portPairLoadMap().toString());
        return result;
    }
}
