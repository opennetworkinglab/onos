/*
 * Copyright 2020-present Open Networking Foundation
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
package org.onosproject.vpls.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.EncapsulationType;
import org.onosproject.net.intf.Interface;
import org.onosproject.vpls.api.VplsData;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;
/**
 * Vpls JSON codec.
 */
public final class VplsCodec extends JsonCodec<VplsData> {

    // JSON field names
    private static final String NAME = "name";
    private static final String ENCAPSULATION_TYPE = "encapsulation";
    private static final String INTERFACES = "interfaces";

    private final Logger log = getLogger(getClass());

    @Override
    public ObjectNode encode(VplsData vplsData, CodecContext context) {
        checkNotNull(vplsData, "Vpls cannot be null");
        ObjectNode result = context.mapper().createObjectNode()
                .put(NAME, vplsData.name())
                .put(ENCAPSULATION_TYPE, vplsData.encapsulationType().toString());
        ArrayNode interfaces = context.mapper().createArrayNode();
        vplsData.interfaces().forEach(interf -> {
            ObjectNode bandJson = context.codec(Interface.class).encode(interf, context);
            interfaces.add(bandJson);
        });
        result.set(INTERFACES, interfaces);
        return result;
    }
    @Override
    public VplsData decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }
        List<String> names = new ArrayList<>();
        json.findValues(NAME).forEach(jsonNode -> {
            names.add(jsonNode.asText());
        });
        Collection<Interface> interfaceList = new ArrayList<>();
        JsonNode interfacesJeson = json.findValue(INTERFACES);
        JsonCodec<Interface> interfaceCodec = context.codec(Interface.class);
        if (interfacesJeson != null) {
            IntStream.range(0, interfacesJeson.size())
                    .forEach(i -> interfaceList.add(
                            interfaceCodec.decode(get(interfacesJeson, i),
                                    context)));

        }
        interfaceList.forEach(interf -> {
            names.remove(interf.name());
        });
        String vplsName = names.get(0);
        EncapsulationType encap =  json.findValue(ENCAPSULATION_TYPE) == null ?
                null : EncapsulationType.enumFromString(json.findValue(ENCAPSULATION_TYPE).asText());
        VplsData vplsData = VplsData.of(vplsName, encap);
        vplsData.addInterfaces(interfaceList);
        return vplsData;
    }

}

