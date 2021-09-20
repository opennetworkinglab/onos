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
import org.onosproject.net.intent.ResourceContext;

import java.util.Objects;

import static org.onosproject.net.AnnotationKeys.getAnnotatedValue;

/**
 * Constraint that evaluates an arbitrary link annotated value is under the specified threshold.
 */
@Beta
public class AnnotationConstraint extends BooleanConstraint {

    private final String key;
    private final double threshold;

    /**
     * Creates a new constraint to keep the value for the specified key
     * of link annotation under the threshold.
     *
     * @param key key of link annotation
     * @param threshold threshold value of the specified link annotation
     */
    public AnnotationConstraint(String key, double threshold) {
        this.key = key;
        this.threshold = threshold;
    }

    // Constructor for serialization
    private AnnotationConstraint() {
        this.key = "";
        this.threshold = 0;
    }

    /**
     * Returns the key of link annotation this constraint designates.
     * @return key of link annotation
     */
    public String key() {
        return key;
    }

    /**
     * Returns the threshold this constraint ensures as link annotated value.
     *
     * @return threshold as link annotated value
     */
    public double threshold() {
        return threshold;
    }

    // doesn't use LinkResourceService
    @Override
    public boolean isValid(Link link, ResourceContext context) {
        // explicitly call a method not depending on LinkResourceService
        return isValid(link);
    }

    private boolean isValid(Link link) {
        if (link.annotations().value(key) != null) {
            return getAnnotatedValue(link, key) <= threshold;
        } else {
            return false;
        }
    }

    // doesn't use LinkResourceService
    @Override
    public double cost(Link link, ResourceContext context) {
        // explicitly call a method not depending on LinkResourceService
        return cost(link);
    }

    private double cost(Link link) {
        if (isValid(link)) {
            return getAnnotatedValue(link, key);
        } else {
            return -1;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, threshold);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof AnnotationConstraint)) {
            return false;
        }

        final AnnotationConstraint other = (AnnotationConstraint) obj;
        return Objects.equals(this.key, other.key) && Objects.equals(this.threshold, other.threshold);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("key", key)
                .add("threshold", threshold)
                .toString();
    }
}
