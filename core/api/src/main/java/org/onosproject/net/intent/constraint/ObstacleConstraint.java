/*
 * Copyright 2014-present Open Networking Laboratory
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
import com.google.common.collect.ImmutableSet;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.intent.ResourceContext;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * Constraint that evaluates elements not passed through.
 */
@Beta
public class ObstacleConstraint extends BooleanConstraint {

    private final Set<DeviceId> obstacles;

    /**
     * Creates a new constraint that the specified device are not passed through.
     * @param obstacles devices not to be passed
     */
    public ObstacleConstraint(DeviceId... obstacles) {
        this.obstacles = ImmutableSet.copyOf(obstacles);
    }

    // Constructor for serialization
    private ObstacleConstraint() {
        this.obstacles = Collections.emptySet();
    }

    /**
     * Returns the obstacle device ids.
     *
     * @return Set of obstacle device ids
     */
    public Set<DeviceId> obstacles() {
        return obstacles;
    }

    // doesn't use LinkResourceService
    @Override
    public boolean isValid(Link link, ResourceContext context) {
        // explicitly call a method not depending on LinkResourceService
        return isValid(link);
    }

    private boolean isValid(Link link) {
        if (link.type() != Link.Type.EDGE) {
            DeviceId src = link.src().deviceId();
            DeviceId dst = link.dst().deviceId();

            return !(obstacles.contains(src) || obstacles.contains(dst));

        } else {

            boolean isSrc = true;
            if (link.src().elementId() instanceof DeviceId) {

                DeviceId src = link.src().deviceId();
                isSrc = !(obstacles.contains(src));
            }

            boolean isDst = true;
            if (link.dst().elementId() instanceof DeviceId) {
                DeviceId dst = link.dst().deviceId();
                isDst = !(obstacles.contains(dst));
            }

            return isSrc || isDst;
        }
    }

    @Override
    public int hashCode() {
        return obstacles.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof ObstacleConstraint)) {
            return false;
        }

        final ObstacleConstraint that = (ObstacleConstraint) obj;
        return Objects.equals(this.obstacles, that.obstacles);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("obstacles", obstacles)
                .toString();
    }
}
