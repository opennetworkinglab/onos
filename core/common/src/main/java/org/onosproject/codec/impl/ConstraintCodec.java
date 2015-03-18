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

import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.constraint.AnnotationConstraint;
import org.onosproject.net.intent.constraint.BandwidthConstraint;
import org.onosproject.net.intent.constraint.LambdaConstraint;
import org.onosproject.net.intent.constraint.LatencyConstraint;
import org.onosproject.net.intent.constraint.LinkTypeConstraint;
import org.onosproject.net.intent.constraint.ObstacleConstraint;
import org.onosproject.net.intent.constraint.WaypointConstraint;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Constraint JSON codec.
 */
public final class ConstraintCodec extends JsonCodec<Constraint> {

    /**
     * Encodes a latency constraint.
     *
     * @param constraint latency constraint to encode
     * @param context code context
     * @return JSON ObjectNode representing the constraint
     */
    private ObjectNode encodeLatencyConstraint(Constraint constraint,
                                                CodecContext context) {
        checkNotNull(constraint, "Duration constraint cannot be null");
        final LatencyConstraint latencyConstraint =
                (LatencyConstraint) constraint;
        return context.mapper().createObjectNode()
                .put("latencyMillis", latencyConstraint.latency().toMillis());
    }

    /**
     * Encodes an obstacle constraint.
     *
     * @param constraint obstacle constraint to encode
     * @param context code context
     * @return JSON ObjectNode representing the constraint
     */
    private ObjectNode encodeObstacleConstraint(Constraint constraint,
                                                CodecContext context) {
        checkNotNull(constraint, "Obstacle constraint cannot be null");
        final ObstacleConstraint obstacleConstraint =
                (ObstacleConstraint) constraint;

        final ObjectNode result = context.mapper().createObjectNode();
        final ArrayNode jsonObstacles = result.putArray("obstacles");

        for (DeviceId did : obstacleConstraint.obstacles()) {
            jsonObstacles.add(did.toString());
        }

        return result;
    }

    /**
     * Encodes a waypoint constraint.
     *
     * @param constraint waypoint constraint to encode
     * @param context code context
     * @return JSON ObjectNode representing the constraint
     */
    private ObjectNode encodeWaypointConstraint(Constraint constraint,
                                                CodecContext context) {
        checkNotNull(constraint, "Waypoint constraint cannot be null");
        final WaypointConstraint waypointConstraint =
                (WaypointConstraint) constraint;

        final ObjectNode result = context.mapper().createObjectNode();
        final ArrayNode jsonWaypoints = result.putArray("waypoints");

        for (DeviceId did : waypointConstraint.waypoints()) {
            jsonWaypoints.add(did.toString());
        }

        return result;
    }

    /**
     * Encodes a annotation constraint.
     *
     * @param constraint annotation constraint to encode
     * @param context code context
     * @return JSON ObjectNode representing the constraint
     */
    private ObjectNode encodeAnnotationConstraint(Constraint constraint,
                                                  CodecContext context) {
        checkNotNull(constraint, "Annotation constraint cannot be null");
        final AnnotationConstraint annotationConstraint =
                (AnnotationConstraint) constraint;
        return context.mapper().createObjectNode()
                .put("key", annotationConstraint.key())
                .put("threshold", annotationConstraint.threshold());
    }

    /**
     * Encodes a bandwidth constraint.
     *
     * @param constraint bandwidth constraint to encode
     * @param context code context
     * @return JSON ObjectNode representing the constraint
     */
    private ObjectNode encodeBandwidthConstraint(Constraint constraint,
                                                 CodecContext context) {
        checkNotNull(constraint, "Bandwidth constraint cannot be null");
        final BandwidthConstraint bandwidthConstraint =
                (BandwidthConstraint) constraint;
        return context.mapper().createObjectNode()
                .put("bandwidth", bandwidthConstraint.bandwidth().toDouble());
    }

    /**
     * Encodes a lambda constraint.
     *
     * @param constraint lambda constraint to encode
     * @param context code context
     * @return JSON ObjectNode representing the constraint
     */
    private ObjectNode encodeLambdaConstraint(Constraint constraint,
                                              CodecContext context) {
        checkNotNull(constraint, "Lambda constraint cannot be null");
        final LambdaConstraint lambdaConstraint =
                (LambdaConstraint) constraint;

        return context.mapper().createObjectNode()
                .put("lambda", lambdaConstraint.lambda().toInt());
    }

    /**
     * Encodes a link type constraint.
     *
     * @param constraint link type constraint to encode
     * @param context code context
     * @return JSON ObjectNode representing the constraint
     */
    private ObjectNode encodeLinkTypeConstraint(Constraint constraint,
                                                        CodecContext context) {
        checkNotNull(constraint, "Link type constraint cannot be null");

        final LinkTypeConstraint linkTypeConstraint =
                (LinkTypeConstraint) constraint;

        final ObjectNode result = context.mapper().createObjectNode()
                .put("inclusive", linkTypeConstraint.isInclusive());

        final ArrayNode jsonTypes = result.putArray("types");

        if (linkTypeConstraint.types() != null) {
            for (Link.Type type : linkTypeConstraint.types()) {
                jsonTypes.add(type.name());
            }
        }

        return result;
    }

    @Override
    public ObjectNode encode(Constraint constraint, CodecContext context) {
        checkNotNull(constraint, "Constraint cannot be null");

        final ObjectNode result;
        if (constraint instanceof BandwidthConstraint) {
            result = encodeBandwidthConstraint(constraint, context);
        } else if (constraint instanceof LambdaConstraint) {
            result = encodeLambdaConstraint(constraint, context);
        } else if (constraint instanceof LinkTypeConstraint) {
            result = encodeLinkTypeConstraint(constraint, context);
        } else if (constraint instanceof AnnotationConstraint) {
            result = encodeAnnotationConstraint(constraint, context);
        } else if (constraint instanceof LatencyConstraint) {
            result = encodeLatencyConstraint(constraint, context);
        } else if (constraint instanceof ObstacleConstraint) {
            result = encodeObstacleConstraint(constraint, context);
        } else if (constraint instanceof WaypointConstraint) {
            result = encodeWaypointConstraint(constraint, context);
        } else {
            result = context.mapper().createObjectNode();
        }

        result.put("type", constraint.getClass().getSimpleName());
        return result;
    }
}
