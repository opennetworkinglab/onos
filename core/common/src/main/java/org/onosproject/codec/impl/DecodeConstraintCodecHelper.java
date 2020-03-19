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

import java.time.Duration;
import java.util.ArrayList;
import java.util.stream.IntStream;

import org.onlab.util.Bandwidth;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.constraint.AnnotationConstraint;
import org.onosproject.net.intent.constraint.AsymmetricPathConstraint;
import org.onosproject.net.intent.constraint.BandwidthConstraint;
import org.onosproject.net.intent.constraint.DomainConstraint;
import org.onosproject.net.intent.constraint.LatencyConstraint;
import org.onosproject.net.intent.constraint.LinkTypeConstraint;
import org.onosproject.net.intent.constraint.MeteredConstraint;
import org.onosproject.net.intent.constraint.NonDisruptiveConstraint;
import org.onosproject.net.intent.constraint.ObstacleConstraint;
import org.onosproject.net.intent.constraint.TierConstraint;
import org.onosproject.net.intent.constraint.WaypointConstraint;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.onlab.util.Tools.nullIsIllegal;
import static org.onosproject.net.intent.constraint.NonDisruptiveConstraint.nonDisruptive;

/**
 * Constraint JSON decoder.
 */
public final class DecodeConstraintCodecHelper {
    private final ObjectNode json;

    /**
     * Constructs a constraint decoder.
     *
     * @param json object node to decode
     */
    public DecodeConstraintCodecHelper(ObjectNode json) {
        this.json = json;
    }

    /**
     * Decodes a link type constraint.
     *
     * @return link type constraint object.
     */
    private Constraint decodeLinkTypeConstraint() {
        boolean inclusive = nullIsIllegal(json.get(ConstraintCodec.INCLUSIVE),
                ConstraintCodec.INCLUSIVE + ConstraintCodec.MISSING_MEMBER_MESSAGE).asBoolean();

        JsonNode types = nullIsIllegal(json.get(ConstraintCodec.TYPES),
                ConstraintCodec.TYPES + ConstraintCodec.MISSING_MEMBER_MESSAGE);
        if (types.size() < 1) {
            throw new IllegalArgumentException(
                    "types array in link constraint must have at least one value");
        }

        ArrayList<Link.Type> typesEntries = new ArrayList<>(types.size());
        IntStream.range(0, types.size())
                .forEach(index ->
                        typesEntries.add(Link.Type.valueOf(types.get(index).asText())));

        return new LinkTypeConstraint(inclusive,
                typesEntries.toArray(new Link.Type[types.size()]));
    }

    /**
     * Decodes an annotation constraint.
     *
     * @return annotation constraint object.
     */
    private Constraint decodeAnnotationConstraint() {
        String key = nullIsIllegal(json.get(ConstraintCodec.KEY),
                ConstraintCodec.KEY + ConstraintCodec.MISSING_MEMBER_MESSAGE)
                .asText();
        double threshold = nullIsIllegal(json.get(ConstraintCodec.THRESHOLD),
                ConstraintCodec.THRESHOLD + ConstraintCodec.MISSING_MEMBER_MESSAGE)
                .asDouble();

        return new AnnotationConstraint(key, threshold);
    }

    /**
     * Decodes a latency constraint.
     *
     * @return latency constraint object.
     */
    private Constraint decodeLatencyConstraint() {
        long latencyMillis = nullIsIllegal(json.get(ConstraintCodec.LATENCY_MILLIS),
                ConstraintCodec.LATENCY_MILLIS + ConstraintCodec.MISSING_MEMBER_MESSAGE)
                .asLong();

        return new LatencyConstraint(Duration.ofMillis(latencyMillis));
    }

    /**
     * Decodes an obstacle constraint.
     *
     * @return obstacle constraint object.
     */
    private Constraint decodeObstacleConstraint() {
        JsonNode obstacles = nullIsIllegal(json.get(ConstraintCodec.OBSTACLES),
                ConstraintCodec.OBSTACLES + ConstraintCodec.MISSING_MEMBER_MESSAGE);
        if (obstacles.size() < 1) {
            throw new IllegalArgumentException(
                    "obstacles array in obstacles constraint must have at least one value");
        }

        ArrayList<DeviceId> obstacleEntries = new ArrayList<>(obstacles.size());
        IntStream.range(0, obstacles.size())
                .forEach(index ->
                        obstacleEntries.add(DeviceId.deviceId(obstacles.get(index).asText())));

        return new ObstacleConstraint(
                obstacleEntries.toArray(new DeviceId[obstacles.size()]));
    }

    /**
     * Decodes a waypoint constraint.
     *
     * @return waypoint constraint object.
     */
    private Constraint decodeWaypointConstraint() {
        JsonNode waypoints = nullIsIllegal(json.get(ConstraintCodec.WAYPOINTS),
                ConstraintCodec.WAYPOINTS + ConstraintCodec.MISSING_MEMBER_MESSAGE);
        if (waypoints.size() < 1) {
            throw new IllegalArgumentException(
                    "obstacles array in obstacles constraint must have at least one value");
        }

        ArrayList<DeviceId> waypointEntries = new ArrayList<>(waypoints.size());
        IntStream.range(0, waypoints.size())
                .forEach(index ->
                        waypointEntries.add(DeviceId.deviceId(waypoints.get(index).asText())));

        return new WaypointConstraint(
                waypointEntries.toArray(new DeviceId[waypoints.size()]));
    }

    /**
     * Decodes an asymmetric path constraint.
     *
     * @return asymmetric path constraint object.
     */
    private Constraint decodeAsymmetricPathConstraint() {
        return new AsymmetricPathConstraint();
    }

    /**
     * Decodes a domain constraint.
     *
     * @return domain constraint object.
     */
    private Constraint decodeDomainConstraint() {
        return DomainConstraint.domain();
    }


    /**
     * Decodes a bandwidth constraint.
     *
     * @return bandwidth constraint object.
     */
    private Constraint decodeBandwidthConstraint() {
        double bandwidth = nullIsIllegal(json.get(ConstraintCodec.BANDWIDTH),
                ConstraintCodec.BANDWIDTH + ConstraintCodec.MISSING_MEMBER_MESSAGE)
                .asDouble();

        return new BandwidthConstraint(Bandwidth.bps(bandwidth));
    }

    /**
     * Decodes a non-disruptive reallocation constraint.
     *
     * @return non-disruptive reallocation constraint object.
     */
    private Constraint decodeNonDisruptiveConstraint() {
        return nonDisruptive();
    }

    private Constraint decodeMeteredConstraint() {
        boolean metered = nullIsIllegal(json.get(ConstraintCodec.METERED),
                ConstraintCodec.METERED + ConstraintCodec.MISSING_MEMBER_MESSAGE).asBoolean();
        return new MeteredConstraint(metered);
    }

    /**
     * Decodes a link type constraint.
     *
     * @return link type constraint object.
     */
    private Constraint decodeTierConstraint() {
        boolean inclusive = nullIsIllegal(json.get(ConstraintCodec.INCLUSIVE),
                ConstraintCodec.INCLUSIVE + ConstraintCodec.MISSING_MEMBER_MESSAGE).asBoolean();

        TierConstraint.CostType costType = TierConstraint.CostType.valueOf(nullIsIllegal(
                json.get(ConstraintCodec.COST_TYPE), ConstraintCodec.COST_TYPE + ConstraintCodec.MISSING_MEMBER_MESSAGE
            ).asText());

        JsonNode tiers = nullIsIllegal(json.get(ConstraintCodec.TIERS),
                ConstraintCodec.TIERS + ConstraintCodec.MISSING_MEMBER_MESSAGE);
        if (tiers.size() < 1) {
            throw new IllegalArgumentException(
                    ConstraintCodec.TIERS + " array in tier constraint must have at least one value");
        }

        ArrayList<Integer> tierEntries = new ArrayList<>(tiers.size());
        IntStream.range(0, tiers.size())
                .forEach(index ->
                        tierEntries.add(new Integer(tiers.get(index).asText())));

        return new TierConstraint(inclusive, costType,
                tierEntries.toArray(new Integer[tiers.size()]));
    }

    /**
     * Decodes the given constraint.
     *
     * @return constraint object.
     */
    public Constraint decode() {
        final String type = nullIsIllegal(json.get(ConstraintCodec.TYPE),
                ConstraintCodec.TYPE + ConstraintCodec.MISSING_MEMBER_MESSAGE)
                .asText();

        if (type.equals(BandwidthConstraint.class.getSimpleName())) {
            return decodeBandwidthConstraint();
        } else if (type.equals(LinkTypeConstraint.class.getSimpleName())) {
            return decodeLinkTypeConstraint();
        } else if (type.equals(AnnotationConstraint.class.getSimpleName())) {
            return decodeAnnotationConstraint();
        } else if (type.equals(LatencyConstraint.class.getSimpleName())) {
            return decodeLatencyConstraint();
        } else if (type.equals(ObstacleConstraint.class.getSimpleName())) {
            return decodeObstacleConstraint();
        } else if (type.equals(WaypointConstraint.class.getSimpleName())) {
            return decodeWaypointConstraint();
        } else if (type.equals(AsymmetricPathConstraint.class.getSimpleName())) {
            return decodeAsymmetricPathConstraint();
        } else if (type.equals(DomainConstraint.class.getSimpleName())) {
            return decodeDomainConstraint();
        } else if (type.equals(NonDisruptiveConstraint.class.getSimpleName())) {
            return decodeNonDisruptiveConstraint();
        } else if (type.equals(MeteredConstraint.class.getSimpleName())) {
            return decodeMeteredConstraint();
        } else if (type.equals(TierConstraint.class.getSimpleName())) {
            return decodeTierConstraint();
        }

        throw new IllegalArgumentException("Instruction type "
                + type + " is not supported");
    }
}
