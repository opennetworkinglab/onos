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
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.ResourceContext;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import static org.onosproject.net.AnnotationKeys.LATENCY;
import static org.onosproject.net.AnnotationKeys.getAnnotatedValue;

/**
 * Constraint that evaluates the latency through a path.
 */
@Beta
public class LatencyConstraint implements Constraint {

    private final Duration latency;

    /**
     * Creates a new constraint to keep under specified latency through a path.
     * @param latency latency to be kept
     */
    public LatencyConstraint(Duration latency) {
        this.latency = latency;
    }

    // Constructor for serialization
    private LatencyConstraint() {
        this.latency = Duration.ZERO;
    }

    public Duration latency() {
        return latency;
    }

    // doesn't use LinkResourceService
    @Override
    public double cost(Link link, ResourceContext context) {
        // explicitly call a method not depending on LinkResourceService
        return cost(link);
    }

    private double cost(Link link) {
        //Check only links, not EdgeLinks
        if (link.type() != Link.Type.EDGE) {
            return link.annotations().value(LATENCY) != null ? getAnnotatedValue(link, LATENCY) : 0;
        } else {
            return 0;
        }
    }

    // doesn't use LinkResourceService
    @Override
    public boolean validate(Path path, ResourceContext context) {
        // explicitly call a method not depending on LinkResourceService
        return validate(path);
    }

    private boolean validate(Path path) {
        //Guarantee all the latency units in ONOS is nanoseconds.
        double pathLatency = path.links().stream().mapToDouble(this::cost).sum();
        return Duration.of((long) pathLatency, ChronoUnit.NANOS).compareTo(latency) <= 0;
    }

    @Override
    public int hashCode() {
        return latency.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof LatencyConstraint)) {
            return false;
        }

        final LatencyConstraint that = (LatencyConstraint) obj;
        return Objects.equals(this.latency, that.latency);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("latency", latency)
                .toString();
    }
}
