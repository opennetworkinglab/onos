/*
 * Copyright 2015 Open Networking Laboratory
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

import java.util.List;
import java.util.Set;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.NetworkResource;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.intent.ConnectivityIntent;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.HostToHostIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.PointToPointIntent;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Hamcrest matcher to check that an intent representation in JSON matches
 * the actual intent.
 */
public final class IntentJsonMatcher extends TypeSafeDiagnosingMatcher<JsonNode> {

    private final Intent intent;

    /**
     * Constructor is private, use factory method.
     *
     * @param intentValue the intent object to compare against
     */
    private IntentJsonMatcher(Intent intentValue) {
        intent = intentValue;
    }

    /**
     * Matches the JSON representation of a host to host intent.
     *
     * @param jsonIntent JSON representation of the intent
     * @param description Description object used for recording errors
     * @return true if the JSON matches the intent, false otherwise
     */
    private boolean matchHostToHostIntent(JsonNode jsonIntent, Description description) {
        final HostToHostIntent hostToHostIntent = (HostToHostIntent) intent;

        // check host one
        final String host1 = hostToHostIntent.one().toString();
        final String jsonHost1 = jsonIntent.get("one").asText();
        if (!host1.equals(jsonHost1)) {
            description.appendText("host one was " + jsonHost1);
            return false;
        }

        // check host 2
        final String host2 = hostToHostIntent.two().toString();
        final String jsonHost2 = jsonIntent.get("two").asText();
        if (!host2.equals(jsonHost2)) {
            description.appendText("host two was " + jsonHost2);
            return false;
        }
        return true;
    }

    /**
     * Matches the JSON representation of a point to point intent.
     *
     * @param jsonIntent JSON representation of the intent
     * @param description Description object used for recording errors
     * @return true if the JSON matches the intent, false otherwise
     */
    private boolean matchPointToPointIntent(JsonNode jsonIntent, Description description) {
        final PointToPointIntent pointToPointIntent = (PointToPointIntent) intent;

        // check ingress connection
        final ConnectPoint ingress = pointToPointIntent.ingressPoint();
        final ConnectPointJsonMatcher ingressMatcher =
                ConnectPointJsonMatcher.matchesConnectPoint(ingress);
        final JsonNode jsonIngress = jsonIntent.get("ingressPoint");
        final boolean ingressMatches =
                ingressMatcher.matchesSafely(jsonIngress, description);

        if (!ingressMatches) {
            description.appendText("ingress was " + jsonIngress);
            return false;
        }

        // check egress connection
        final ConnectPoint egress = pointToPointIntent.egressPoint();
        final ConnectPointJsonMatcher egressMatcher =
                ConnectPointJsonMatcher.matchesConnectPoint(egress);
        final JsonNode jsonEgress = jsonIntent.get("egressPoint");
        final boolean egressMatches =
                egressMatcher.matchesSafely(jsonEgress, description);

        if (!egressMatches) {
            description.appendText("egress was " + jsonEgress);
            return false;
        }

        return true;
    }

    /**
     * Matches the JSON representation of a connectivity intent. Calls the
     * matcher for the connectivity intent subtype.
     *
     * @param jsonIntent JSON representation of the intent
     * @param description Description object used for recording errors
     * @return true if the JSON matches the intent, false otherwise
     */
    private boolean matchConnectivityIntent(JsonNode jsonIntent, Description description) {
        final ConnectivityIntent connectivityIntent = (ConnectivityIntent) intent;

        // check selector
        final JsonNode jsonSelector = jsonIntent.get("selector");
        final TrafficSelector selector = connectivityIntent.selector();
        final Set<Criterion> criteria = selector.criteria();
        final JsonNode jsonCriteria = jsonSelector.get("criteria");
        if (jsonCriteria.size() != criteria.size()) {
            description.appendText("size of criteria array is "
                    + Integer.toString(jsonCriteria.size()));
            return false;
        }

        for (Criterion criterion : criteria) {
            boolean criterionFound = false;
            for (int criterionIndex = 0; criterionIndex < jsonCriteria.size(); criterionIndex++) {
                final CriterionJsonMatcher criterionMatcher =
                        CriterionJsonMatcher.matchesCriterion(criterion);
                if (criterionMatcher.matches(jsonCriteria.get(criterionIndex))) {
                    criterionFound = true;
                    break;
                }
            }
            if (!criterionFound) {
                description.appendText("criterion not found " + criterion.toString());
                return false;
            }
        }

        // check treatment
        final JsonNode jsonTreatment = jsonIntent.get("treatment");
        final TrafficTreatment treatment = connectivityIntent.treatment();
        final List<Instruction> instructions = treatment.instructions();
        final JsonNode jsonInstructions = jsonTreatment.get("instructions");
        if (jsonInstructions.size() != instructions.size()) {
            description.appendText("size of instructions array is "
                    + Integer.toString(jsonInstructions.size()));
            return false;
        }

        for (Instruction instruction : instructions) {
            boolean instructionFound = false;
            for (int instructionIndex = 0; instructionIndex < jsonCriteria.size(); instructionIndex++) {
                final InstructionJsonMatcher instructionMatcher =
                        InstructionJsonMatcher.matchesInstruction(instruction);
                if (instructionMatcher.matches(jsonInstructions.get(instructionIndex))) {
                    instructionFound = true;
                    break;
                }
            }
            if (!instructionFound) {
                description.appendText("instruction not found " + instruction.toString());
                return false;
            }
        }

        // Check constraints
        final JsonNode jsonConstraints = jsonIntent.get("constraints");
        if (connectivityIntent.constraints() != null) {
            if (connectivityIntent.constraints().size() != jsonConstraints.size()) {
                description.appendText("constraints array size was "
                        + Integer.toString(jsonConstraints.size()));
                return false;
            }
            for (final Constraint constraint : connectivityIntent.constraints()) {
                boolean constraintFound = false;
                final String constraintString = constraint.toString();
                for (int constraintIndex = 0; constraintIndex < jsonConstraints.size();
                     constraintIndex++) {
                    final JsonNode value = jsonConstraints.get(constraintIndex);
                    if (value.asText().equals(constraintString)) {
                        constraintFound = true;
                    }
                }
                if (!constraintFound) {
                    description.appendText("resource missing " + constraintString);
                    return false;
                }
            }
        } else if (jsonConstraints.size() != 0) {
            description.appendText("constraint array not empty");
            return false;
        }

        if (connectivityIntent instanceof HostToHostIntent) {
            return matchHostToHostIntent(jsonIntent, description);
        } else if (connectivityIntent instanceof PointToPointIntent) {
            return matchPointToPointIntent(jsonIntent, description);
        } else {
            description.appendText("class of connectivity intent is unknown");
            return false;
        }
    }

    @Override
    public boolean matchesSafely(JsonNode jsonIntent, Description description) {
        // check id
        final String jsonId = jsonIntent.get("id").asText();
        final String id = intent.id().toString();
        if (!jsonId.equals(id)) {
            description.appendText("id was " + jsonId);
            return false;
        }

        // check application id
        final String jsonAppId = jsonIntent.get("appId").asText();
        final String appId = intent.appId().toString();
        if (!jsonAppId.equals(appId)) {
            description.appendText("appId was " + jsonAppId);
            return false;
        }

        // check intent type
        final String jsonType = jsonIntent.get("type").asText();
        final String type = intent.getClass().getSimpleName();
        if (!jsonType.equals(type)) {
            description.appendText("type was " + jsonType);
            return false;
        }

        // check details field
        final String jsonDetails = jsonIntent.get("details").asText();
        final String details = intent.toString();
        if (!jsonDetails.equals(details)) {
            description.appendText("details were " + jsonDetails);
            return false;
        }

        // check resources array
        final JsonNode jsonResources = jsonIntent.get("resources");
        if (intent.resources() != null) {
            if (intent.resources().size() != jsonResources.size()) {
                description.appendText("resources array size was "
                                       + Integer.toString(jsonResources.size()));
                return false;
            }
            for (final NetworkResource resource : intent.resources()) {
                boolean resourceFound = false;
                final String resourceString = resource.toString();
                for (int resourceIndex = 0; resourceIndex < jsonResources.size(); resourceIndex++) {
                    final JsonNode value = jsonResources.get(resourceIndex);
                    if (value.asText().equals(resourceString)) {
                        resourceFound = true;
                    }
                }
                if (!resourceFound) {
                    description.appendText("resource missing " + resourceString);
                    return false;
                }
            }
        } else if (jsonResources.size() != 0) {
            description.appendText("resources array empty");
            return false;
        }

        if (intent instanceof ConnectivityIntent) {
            return matchConnectivityIntent(jsonIntent, description);
        } else {
            description.appendText("class of intent is unknown");
            return false;
        }
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(intent.toString());
    }

    /**
     * Factory to allocate an intent matcher.
     *
     * @param intent intent object we are looking for
     * @return matcher
     */
    public static IntentJsonMatcher matchesIntent(Intent intent) {
        return new IntentJsonMatcher(intent);
    }
}
