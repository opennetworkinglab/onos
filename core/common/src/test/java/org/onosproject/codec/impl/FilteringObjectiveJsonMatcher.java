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
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flowobjective.FilteringObjective;

/**
 * Hamcrest matcher for filteringObjective.
 */
public final class FilteringObjectiveJsonMatcher extends TypeSafeDiagnosingMatcher<JsonNode> {

    final FilteringObjective filteringObj;

    private FilteringObjectiveJsonMatcher(FilteringObjective filteringObjective) {
        this.filteringObj = filteringObjective;
    }

    @Override
    protected boolean matchesSafely(JsonNode jsonFilteringObj, Description description) {

        ObjectiveJsonMatcher.matchesObjective(filteringObj).matchesSafely(jsonFilteringObj);

        // check id
        int jsonId = jsonFilteringObj.get("id").asInt();
        int id = filteringObj.id();
        if (jsonId != id) {
            description.appendText("id was " + jsonId);
            return false;
        }

        // check type
        String jsonType = jsonFilteringObj.get("type").asText();
        String type = filteringObj.type().toString();
        if (!jsonType.equals(type)) {
            description.appendText("type was " + jsonType);
            return false;
        }

        // check size of condition array
        JsonNode jsonConditions = jsonFilteringObj.get("conditions");
        if (jsonConditions.size() != filteringObj.conditions().size()) {
            description.appendText("conditions size was " + jsonConditions.size());
            return false;
        }

        // check conditions
        for (Criterion c : filteringObj.conditions()) {
            boolean conditionFound = false;
            for (int cIndex = 0; cIndex < jsonConditions.size(); cIndex++) {
                CriterionJsonMatcher criterionMatcher = CriterionJsonMatcher.matchesCriterion(c);
                if (criterionMatcher.matches(jsonConditions.get(cIndex))) {
                    conditionFound = true;
                    break;
                }
            }
            if (!conditionFound) {
                description.appendText("condition not found " + c.toString());
                return false;
            }
        }

        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(filteringObj.toString());
    }

    /**
     * Factory to allocate a filteringObjective matcher.
     *
     * @param filteringObj filteringObjective object we are looking for
     * @return matcher
     */
    public static FilteringObjectiveJsonMatcher matchesFilteringObjective(FilteringObjective filteringObj) {
        return new FilteringObjectiveJsonMatcher(filteringObj);
    }
}
