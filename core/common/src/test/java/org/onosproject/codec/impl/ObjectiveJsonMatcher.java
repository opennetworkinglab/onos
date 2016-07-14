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
package org.onosproject.codec.impl;

import com.fasterxml.jackson.databind.JsonNode;
import org.onosproject.net.flowobjective.Objective;

/**
 * Hamcrest matcher for instructions.
 */
public final class ObjectiveJsonMatcher {

    final Objective objective;

    private ObjectiveJsonMatcher(Objective objective) {
        this.objective = objective;
    }

    protected boolean matchesSafely(JsonNode jsonObjective) {

        // check operation
        String jsonOp = jsonObjective.get("operation").asText();
        String op = objective.op().toString();
        if (!jsonOp.equals(op)) {
            return false;
        }

        // check permanent
        boolean jsonPermanent = jsonObjective.get("isPermanent").asBoolean();
        boolean permanent = objective.permanent();
        if (jsonPermanent != permanent) {
            return false;
        }

        // check priority
        int jsonPriority = jsonObjective.get("priority").asInt();
        int priority = objective.priority();
        if (jsonPriority != priority) {
            return false;
        }

        // check timeout
        int jsonTimeout = jsonObjective.get("timeout").asInt();
        int timeout = objective.timeout();
        if (jsonTimeout != timeout) {
            return false;
        }

        return true;
    }

    /**
     * Factory to allocate a ObjectiveJsonMatcher.
     *
     * @param objective objective object we are looking for
     * @return matcher
     */
    public static ObjectiveJsonMatcher matchesObjective(Objective objective) {
        return new ObjectiveJsonMatcher(objective);
    }
}
