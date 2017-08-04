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
package org.onosproject.codec.impl;

import com.fasterxml.jackson.databind.JsonNode;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.onosproject.net.flowobjective.ForwardingObjective;

/**
 * Hamcrest matcher for forwardingObjective.
 */
public final class ForwardingObjectiveJsonMatcher extends TypeSafeDiagnosingMatcher<JsonNode> {

    final ForwardingObjective forwardingObjective;

    private ForwardingObjectiveJsonMatcher(ForwardingObjective forwardingObjective) {
        this.forwardingObjective = forwardingObjective;
    }

    @Override
    protected boolean matchesSafely(JsonNode jsonForwardingObj, Description description) {

        ObjectiveJsonMatcher.matchesObjective(forwardingObjective).matchesSafely(jsonForwardingObj);

        // check id
        int jsonId = jsonForwardingObj.get("id").asInt();
        int id = forwardingObjective.id();
        if (jsonId != id) {
            description.appendText("id was " + jsonId);
            return false;
        }

        // check nextId
        int jsonNextId = jsonForwardingObj.get("nextId").asInt();
        int nextId = forwardingObjective.nextId();
        if (jsonNextId != nextId) {
            description.appendText("nextId was " + jsonNextId);
            return false;
        }

        // check flag
        String jsonFlag = jsonForwardingObj.get("flag").asText();
        String flag = forwardingObjective.flag().toString();
        if (!jsonFlag.equals(flag)) {
            description.appendText("flag was " + jsonFlag);
            return false;
        }

        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(forwardingObjective.toString());
    }

    /**
     * Factory to allocate a forwardingObjective matcher.
     *
     * @param forwardingObjective forwardingObjective object we are looking for
     * @return matcher
     */
    public static ForwardingObjectiveJsonMatcher matchesForwardingObjective(ForwardingObjective forwardingObjective) {
        return new ForwardingObjectiveJsonMatcher(forwardingObjective);
    }
}
