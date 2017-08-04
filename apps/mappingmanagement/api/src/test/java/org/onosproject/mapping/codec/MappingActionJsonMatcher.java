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
package org.onosproject.mapping.codec;

import com.fasterxml.jackson.databind.JsonNode;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.onosproject.mapping.actions.MappingAction;
import org.onosproject.mapping.actions.NoMappingAction;
import org.onosproject.mapping.actions.DropMappingAction;
import org.onosproject.mapping.actions.ForwardMappingAction;
import org.onosproject.mapping.actions.NativeForwardMappingAction;
/**
 * Hamcrest matcher for mapping actions.
 */
public final class MappingActionJsonMatcher
        extends TypeSafeDiagnosingMatcher<JsonNode> {

    private final MappingAction action;

    /**
     * A default constructor.
     *
     * @param action mapping action
     */
    private MappingActionJsonMatcher(MappingAction action) {
        this.action = action;
    }

    /**
     * Matches the contents of a no mapping action.
     *
     * @param node        JSON action to match
     * @param description object used for recording errors
     * @return true if contents match, false otherwise
     */
    private boolean matchNoAction(JsonNode node, Description description) {
        NoMappingAction actionToMatch = (NoMappingAction) action;
        final String jsonType = node.get(MappingActionCodec.TYPE).textValue();
        if (!actionToMatch.type().name().equals(jsonType)) {
            description.appendText("type was " + jsonType);
            return false;
        }
        return true;
    }

    /**
     * Matches the contents of a drop mapping action.
     *
     * @param node        JSON action to match
     * @param description object used for recording errors
     * @return true if the contents match, false otherwise
     */
    private boolean matchDropAction(JsonNode node, Description description) {
        DropMappingAction actionToMatch = (DropMappingAction) action;
        final String jsonType = node.get(MappingActionCodec.TYPE).textValue();
        if (!actionToMatch.type().name().equals(jsonType)) {
            description.appendText("type was " + jsonType);
            return false;
        }
        return true;
    }

    /**
     * Matches the contents of a forward mapping action.
     *
     * @param node        JSON action to match
     * @param description object used for recording errors
     * @return true if the contents match, false otherwise
     */
    private boolean matchForwardAction(JsonNode node, Description description) {
        ForwardMappingAction actionToMatch = (ForwardMappingAction) action;
        final String jsonType = node.get(MappingActionCodec.TYPE).textValue();
        if (!actionToMatch.type().name().equals(jsonType)) {
            description.appendText("type was " + jsonType);
            return false;
        }
        return true;
    }

    /**
     * Matches the contents of a native forward mapping action.
     *
     * @param node        JSON action to match
     * @param description object used for recording errors
     * @return true if the contents match, false otherwise
     */
    private boolean matchNativeForwardAction(JsonNode node, Description description) {
        NativeForwardMappingAction actionToMatch = (NativeForwardMappingAction) action;
        final String jsonType = node.get(MappingActionCodec.TYPE).textValue();
        if (!actionToMatch.type().name().equals(jsonType)) {
            description.appendText("type was " + jsonType);
            return false;
        }
        return true;
    }

    @Override
    protected boolean matchesSafely(JsonNode jsonNode, Description description) {

        // check type
        final JsonNode jsonTypeNode = jsonNode.get(MappingActionCodec.TYPE);
        final String jsonType = jsonTypeNode.textValue();
        final String type = action.type().name();
        if (!jsonType.equals(type)) {
            description.appendText("type was " + type);
            return false;
        }

        if (action instanceof NoMappingAction) {
            return matchNoAction(jsonNode, description);
        } else if (action instanceof DropMappingAction) {
            return matchDropAction(jsonNode, description);
        } else if (action instanceof ForwardMappingAction) {
            return matchForwardAction(jsonNode, description);
        } else if (action instanceof NativeForwardMappingAction) {
            return matchNativeForwardAction(jsonNode, description);
        }

        return false;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(action.toString());
    }

    /**
     * Factory to allocate a mapping action matcher.
     *
     * @param action action object we are looking for
     * @return matcher
     */
    public static MappingActionJsonMatcher matchesAction(MappingAction action) {
        return new MappingActionJsonMatcher(action);
    }
}
