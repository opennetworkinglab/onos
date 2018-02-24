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
package org.onosproject.cfm.web;

import static org.onlab.util.Tools.nullIsIllegal;

import java.util.ArrayList;
import java.util.List;

import org.onlab.packet.VlanId;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.incubator.net.l2monitoring.cfm.Component;
import org.onosproject.incubator.net.l2monitoring.cfm.DefaultComponent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Encode and decode to/from JSON to Component object.
 */
public class ComponentCodec extends JsonCodec<Component> {

    private static final String COMPONENT_ID = "component-id";
    private static final String COMPONENT = "component";
    private static final String VID_LIST = "vid-list";
    private static final String TAG_TYPE = "tag-type";
    private static final String MHF_CREATION_TYPE = "mhf-creation-type";
    private static final String ID_PERMISSION = "id-permission";

    /**
     * Encodes the Component entity into JSON.
     *
     * @param component  Component to encode
     * @param context encoding context
     * @return JSON node
     * @throws java.lang.UnsupportedOperationException if the codec does not
     *                                                 support encode operations
     */
    @Override
    public ObjectNode encode(Component component, CodecContext context) {

        ObjectNode node = context.mapper().createObjectNode()
                .put(COMPONENT_ID, component.componentId());

        node.set(VID_LIST, new VidCodec().encode(component.vidList(), context));

        if (component.mhfCreationType() != null) {
            node.put(MHF_CREATION_TYPE, component.mhfCreationType().name());
        }
        if (component.idPermission() != null) {
            node.put(ID_PERMISSION, component.idPermission().name());
        }
        if (component.tagType() != null) {
            node.put(TAG_TYPE, component.tagType().name());
        }

        return (ObjectNode) context.mapper().createObjectNode().set(COMPONENT, node);
    }

    /**
     * Encodes the collection of the Component entities.
     *
     * @param components collection of Component to encode
     * @param context  encoding context
     * @return JSON array
     * @throws java.lang.UnsupportedOperationException if the codec does not
     *                                                 support encode operations
     */
    @Override
    public ArrayNode encode(Iterable<Component> components, CodecContext context) {
        ArrayNode an = context.mapper().createArrayNode();
        components.forEach(component -> an.add(encode(component, context)));
        return an;
    }

    /**
     * Decodes the Component entity from JSON.
     *
     * @param json    JSON to decode
     * @param context decoding context
     * @return decoded Component
     * @throws java.lang.UnsupportedOperationException if the codec does not
     *                                                 support decode operations
     */
    @Override
    public Component decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        JsonNode componentNode = json.get(COMPONENT);

        int componentId = nullIsIllegal(componentNode.get(COMPONENT_ID),
                "component-id is required").asInt();
        Component.ComponentBuilder componentBuilder =
                DefaultComponent.builder(componentId);

        List<VlanId> vidList = (new VidCodec()).decode((ArrayNode)
                nullIsIllegal(componentNode.get(VID_LIST), "vid-list is required"), context);
        if (vidList == null || vidList.isEmpty()) {
            throw new IllegalArgumentException("A least one VID is required in component: " + componentId);
        }
        for (VlanId vid:vidList) {
            componentBuilder = componentBuilder.addToVidList(vid);
        }

        if (componentNode.get(TAG_TYPE) != null) {
            componentBuilder = componentBuilder
                    .tagType(Component.TagType.valueOf(
                            componentNode.get(TAG_TYPE).asText()));
        }

        if (componentNode.get(MHF_CREATION_TYPE) != null) {
            componentBuilder = componentBuilder
                    .mhfCreationType(Component.MhfCreationType.valueOf(
                            componentNode.get(MHF_CREATION_TYPE).asText()));
        }
        if (componentNode.get(ID_PERMISSION) != null) {
            componentBuilder = componentBuilder
                    .idPermission(Component.IdPermissionType.valueOf(
                            componentNode.get(ID_PERMISSION).asText()));
        }

        return componentBuilder.build();
    }

    @Override
    public List<Component> decode(ArrayNode json, CodecContext context) {
        List<Component> componentList = new ArrayList<>();
        json.forEach(node -> componentList.add(decode((ObjectNode) node, context)));
        return componentList;
    }
}
