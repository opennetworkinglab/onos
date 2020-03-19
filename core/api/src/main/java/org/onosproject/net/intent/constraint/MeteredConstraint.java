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
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.Link;
import org.onosproject.net.intent.ResourceContext;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Constraint that evaluates links based on the metered flag.
 */
@Beta
public class MeteredConstraint extends BooleanConstraint {

    private final boolean useMetered;

    /**
     * Creates a new constraint for requesting connectivity using or avoiding
     * the metered links.
     *
     * @param metered   indicates whether a metered link can be used.
     */
    public MeteredConstraint(boolean metered) {
        this.useMetered = metered;
    }

    // Constructor for serialization
    private MeteredConstraint() {
        this.useMetered = false;
    }

    // doesn't use LinkResourceService
    @Override
    public boolean isValid(Link link, ResourceContext context) {
        // explicitly call a method not depending on LinkResourceService
        return isValid(link);
    }

    private boolean isValid(Link link) {
        return !isMeteredLink(link) || useMetered;
    }

    private boolean isMeteredLink(Link link) {
        return link.annotations().keys().contains(AnnotationKeys.METERED)
                && Boolean.valueOf(link.annotations().value(AnnotationKeys.METERED));
    }

    /**
     * Indicates if the constraint is metered or not.
     *
     * @return true if metered
     */
    public boolean isUseMetered() {
        return useMetered;
    }

    @Override
    public int hashCode() {
        return Objects.hash(useMetered);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final MeteredConstraint other = (MeteredConstraint) obj;
        return Objects.equals(this.useMetered, other.useMetered);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("metered", useMetered)
                .toString();
    }
}
