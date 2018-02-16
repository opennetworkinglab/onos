/*
 * Copyright 2015-present Open Networking Foundation
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
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
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
import org.onosproject.net.intent.constraint.AnnotationConstraint;
import org.onosproject.net.intent.constraint.BandwidthConstraint;
import org.onosproject.net.intent.constraint.LatencyConstraint;
import org.onosproject.net.intent.constraint.LinkTypeConstraint;
import org.onosproject.net.intent.constraint.ObstacleConstraint;
import org.onosproject.net.intent.constraint.WaypointConstraint;

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
        final ConnectPoint ingress = pointToPointIntent.filteredIngressPoint().connectPoint();
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
        final ConnectPoint egress = pointToPointIntent.filteredEgressPoint().connectPoint();
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
     * Matches a bandwidth constraint against a JSON representation of the
     * constraint.
     *
     * @param bandwidthConstraint constraint object to match
     * @param constraintJson JSON representation of the constraint
     * @return true if the constraint and JSON match, false otherwise.
     */
    private boolean matchBandwidthConstraint(BandwidthConstraint bandwidthConstraint,
                                             JsonNode constraintJson) {
        final JsonNode bandwidthJson = constraintJson.get("bandwidth");
        return bandwidthJson != null
                && constraintJson.get("bandwidth").asDouble()
                == bandwidthConstraint.bandwidth().bps();
    }

    /**
     * Matches a link type constraint against a JSON representation of the
     * constraint.
     *
     * @param linkTypeConstraint constraint object to match
     * @param constraintJson JSON representation of the constraint
     * @return true if the constraint and JSON match, false otherwise.
     */
    private boolean matchLinkTypeConstraint(LinkTypeConstraint linkTypeConstraint,
                                            JsonNode constraintJson) {
        final JsonNode inclusiveJson = constraintJson.get("inclusive");
        final JsonNode typesJson = constraintJson.get("types");

        if (typesJson.size() != linkTypeConstraint.types().size()) {
            return false;
        }

        int foundType = 0;
        for (Link.Type type : linkTypeConstraint.types()) {
            for (int jsonIndex = 0; jsonIndex < typesJson.size(); jsonIndex++) {
                if (type.name().equals(typesJson.get(jsonIndex).asText())) {
                    foundType++;
                    break;
                }
            }
        }
        return (inclusiveJson != null &&
                inclusiveJson.asBoolean() == linkTypeConstraint.isInclusive()) &&
                foundType == typesJson.size();
    }

    /**
     * Matches an annotation constraint against a JSON representation of the
     * constraint.
     *
     * @param annotationConstraint constraint object to match
     * @param constraintJson JSON representation of the constraint
     * @return true if the constraint and JSON match, false otherwise.
     */
    private boolean matchAnnotationConstraint(AnnotationConstraint annotationConstraint,
                                              JsonNode constraintJson) {
        final JsonNode keyJson = constraintJson.get("key");
        final JsonNode thresholdJson = constraintJson.get("threshold");
        return (keyJson != null
                && keyJson.asText().equals(annotationConstraint.key())) &&
               (thresholdJson != null
                && thresholdJson.asDouble() == annotationConstraint.threshold());
    }

    /**
     * Matches a latency constraint against a JSON representation of the
     * constraint.
     *
     * @param latencyConstraint constraint object to match
     * @param constraintJson JSON representation of the constraint
     * @return true if the constraint and JSON match, false otherwise.
     */
    private boolean matchLatencyConstraint(LatencyConstraint latencyConstraint,
                                           JsonNode constraintJson) {
        final JsonNode latencyJson = constraintJson.get("latencyMillis");
        return (latencyJson != null
                && latencyJson.asInt() == latencyConstraint.latency().toMillis());
    }

    /**
     * Matches an obstacle constraint against a JSON representation of the
     * constraint.
     *
     * @param obstacleConstraint constraint object to match
     * @param constraintJson JSON representation of the constraint
     * @return true if the constraint and JSON match, false otherwise.
     */
    private boolean matchObstacleConstraint(ObstacleConstraint obstacleConstraint,
                                            JsonNode constraintJson) {
        final JsonNode obstaclesJson = constraintJson.get("obstacles");

        if (obstaclesJson.size() != obstacleConstraint.obstacles().size()) {
            return false;
        }

        for (int obstaclesIndex = 0; obstaclesIndex < obstaclesJson.size();
                 obstaclesIndex++) {
            boolean obstacleFound = false;
            final String obstacleJson = obstaclesJson.get(obstaclesIndex)
                    .asText();
            for (DeviceId obstacle : obstacleConstraint.obstacles()) {
                if (obstacle.toString().equals(obstacleJson)) {
                    obstacleFound = true;
                }
            }
            if (!obstacleFound) {
                return false;
            }
        }
        return true;
    }

    /**
     * Matches a waypoint constraint against a JSON representation of the
     * constraint.
     *
     * @param waypointConstraint constraint object to match
     * @param constraintJson JSON representation of the constraint
     * @return true if the constraint and JSON match, false otherwise.
     */
    private boolean matchWaypointConstraint(WaypointConstraint waypointConstraint,
                                            JsonNode constraintJson) {
        final JsonNode waypointsJson = constraintJson.get("waypoints");

        if (waypointsJson.size() != waypointConstraint.waypoints().size()) {
            return false;
        }

        for (int waypointsIndex = 0; waypointsIndex < waypointsJson.size();
             waypointsIndex++) {
            boolean waypointFound = false;
            final String waypointJson = waypointsJson.get(waypointsIndex)
                    .asText();
            for (DeviceId waypoint : waypointConstraint.waypoints()) {
                if (waypoint.toString().equals(waypointJson)) {
                    waypointFound = true;
                }
            }
            if (!waypointFound) {
                return false;
            }
        }
        return true;
    }


    /**
     * Matches a constraint against a JSON representation of the
     * constraint.
     *
     * @param constraint constraint object to match
     * @param constraintJson JSON representation of the constraint
     * @return true if the constraint and JSON match, false otherwise.
     */
    private boolean matchConstraint(Constraint constraint, JsonNode constraintJson) {
        final JsonNode typeJson = constraintJson.get("type");
        if (!typeJson.asText().equals(constraint.getClass().getSimpleName())) {
            return false;
        }
        if (constraint instanceof BandwidthConstraint) {
            return matchBandwidthConstraint((BandwidthConstraint) constraint,
                    constraintJson);
        } else if (constraint instanceof LinkTypeConstraint) {
            return matchLinkTypeConstraint((LinkTypeConstraint) constraint,
                    constraintJson);
        } else if (constraint instanceof AnnotationConstraint) {
            return matchAnnotationConstraint((AnnotationConstraint) constraint,
                    constraintJson);
        } else if (constraint instanceof LatencyConstraint) {
            return matchLatencyConstraint((LatencyConstraint) constraint,
                    constraintJson);
        } else if (constraint instanceof ObstacleConstraint) {
            return matchObstacleConstraint((ObstacleConstraint) constraint,
                    constraintJson);
        } else if (constraint instanceof WaypointConstraint) {
            return matchWaypointConstraint((WaypointConstraint) constraint,
                    constraintJson);
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
        final List<Instruction> instructions = treatment.immediate();
        final JsonNode jsonInstructions = jsonTreatment.get("instructions");
        if (jsonInstructions.size() != instructions.size()) {
            description.appendText("size of instructions array is "
                    + Integer.toString(jsonInstructions.size()));
            return false;
        }

        for (Instruction instruction : instructions) {
            boolean instructionFound = false;
            for (int instructionIndex = 0; instructionIndex < jsonInstructions.size(); instructionIndex++) {
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
                for (int constraintIndex = 0; constraintIndex < jsonConstraints.size();
                     constraintIndex++) {
                    final JsonNode value = jsonConstraints.get(constraintIndex);
                    if (matchConstraint(constraint, value)) {
                        constraintFound = true;
                    }
                }
                if (!constraintFound) {
                    final String constraintString = constraint.toString();
                    description.appendText("constraint missing " + constraintString);
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
        final JsonNode jsonAppIdNode = jsonIntent.get("appId");

        final String jsonAppId = jsonAppIdNode.asText();
        final String appId = intent.appId().name();
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
