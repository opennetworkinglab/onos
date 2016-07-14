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
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.onosproject.net.flowobjective.NextObjective;

/**
 * Hamcrest matcher for nextObjective.
 */
public final class NextObjectiveJsonMatcher extends TypeSafeDiagnosingMatcher<JsonNode> {

    final NextObjective nextObjective;

    private NextObjectiveJsonMatcher(NextObjective nextObjective) {
        this.nextObjective = nextObjective;
    }

    @Override
    protected boolean matchesSafely(JsonNode jsonNextObj, Description description) {
        ObjectiveJsonMatcher.matchesObjective(nextObjective).matchesSafely(jsonNextObj);

        // check id
        int jsonId = jsonNextObj.get("id").asInt();
        int id = nextObjective.id();
        if (jsonId != id) {
            description.appendText("id was " + jsonId);
            return false;
        }

        // check type
        String jsonType = jsonNextObj.get("type").asText();
        String type = nextObjective.type().toString();
        if (!jsonType.equals(type)) {
            description.appendText("type was " + jsonType);
            return false;
        }

        // check size of treatment array
        JsonNode jsonTreatments = jsonNextObj.get("treatments");
        if (jsonTreatments.size() != nextObjective.next().size()) {
            description.appendText("treatments size was " + jsonTreatments.size());
            return false;
        }

        // TODO: need to check the content of treatment collection

        // TODO: need to check the content of selector instance

        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(nextObjective.toString());
    }

    /**
     * Factory to allocate a nextObjective matcher.
     *
     * @param nextObjective nextObjective object we are looking for
     * @return matcher
     */
    public static NextObjectiveJsonMatcher matchesNextObjective(NextObjective nextObjective) {
        return new NextObjectiveJsonMatcher(nextObjective);
    }
}
