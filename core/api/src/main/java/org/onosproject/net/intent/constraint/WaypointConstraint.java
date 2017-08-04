/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.net.intent.constraint;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.intent.ResourceContext;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Constraint that evaluates elements passed through in order.
 */
@Beta
public final class WaypointConstraint extends PathViabilityConstraint {

    private final List<DeviceId> waypoints;

    /**
     * Creates a new waypoint constraint.
     *
     * @param waypoints waypoints
     */
    public WaypointConstraint(DeviceId... waypoints) {
        checkNotNull(waypoints, "waypoints cannot be null");
        checkArgument(waypoints.length > 0, "length of waypoints should be more than 0");
        this.waypoints = ImmutableList.copyOf(waypoints);
    }

    // Constructor for serialization
    private WaypointConstraint() {
        this.waypoints = Collections.emptyList();
    }

    public List<DeviceId> waypoints() {
        return waypoints;
    }

    // doesn't use LinkResourceService
    @Override
    public boolean validate(Path path, ResourceContext context) {
        // explicitly call a method not depending on LinkResourceService
        return validate(path);
    }

    private boolean validate(Path path) {
        LinkedList<DeviceId> waypoints = new LinkedList<>(this.waypoints);
        DeviceId current = waypoints.poll();
        // This is safe because Path class ensures the number of links are more than 0
        Link firstLink = path.links().get(0);
        if (firstLink.src().elementId().equals(current)) {
            current = waypoints.poll();
        }

        for (Link link : path.links()) {
            if (link.dst().elementId().equals(current)) {
                current = waypoints.poll();
                // Empty waypoints means passing through all waypoints in the specified order
                if (current == null) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public int hashCode() {
        return waypoints.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof WaypointConstraint)) {
            return false;
        }

        final WaypointConstraint that = (WaypointConstraint) obj;
        return Objects.equals(this.waypoints, that.waypoints);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("waypoints", waypoints)
                .toString();
    }
}
