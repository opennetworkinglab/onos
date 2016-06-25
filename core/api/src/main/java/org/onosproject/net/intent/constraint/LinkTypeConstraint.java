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
import com.google.common.collect.ImmutableSet;
import org.onosproject.net.Link;
import org.onosproject.net.intent.ResourceContext;

import java.util.Objects;
import java.util.Set;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Constraint that evaluates links based on their type.
 */
@Beta
public class LinkTypeConstraint extends BooleanConstraint {

    private final Set<Link.Type> types;
    private final boolean isInclusive;

    /**
     * Creates a new constraint for requesting connectivity using or avoiding
     * the specified link types.
     *
     * @param inclusive indicates whether the given link types are to be
     *                  permitted or avoided
     * @param types     link types
     */
    public LinkTypeConstraint(boolean inclusive, Link.Type... types) {
        checkNotNull(types, "Link types cannot be null");
        checkArgument(types.length > 0, "There must be more than one type");
        this.types = ImmutableSet.copyOf(types);
        this.isInclusive = inclusive;
    }

    // Constructor for serialization
    private LinkTypeConstraint() {
        this.types = null;
        this.isInclusive = false;
    }

    // doesn't use LinkResourceService
    @Override
    public boolean isValid(Link link, ResourceContext context) {
        // explicitly call a method not depending on LinkResourceService
        return isValid(link);
    }

    private boolean isValid(Link link) {
        boolean contains = types.contains(link.type());
        return isInclusive == contains;
    }

    /**
     * Returns the set of link types.
     *
     * @return set of link types
     */
    public Set<Link.Type> types() {
        return types;
    }

    /**
     * Indicates if the constraint is inclusive or exclusive.
     *
     * @return true if inclusive
     */
    public boolean isInclusive() {
        return isInclusive;
    }

    @Override
    public int hashCode() {
        return Objects.hash(types, isInclusive);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final LinkTypeConstraint other = (LinkTypeConstraint) obj;
        return Objects.equals(this.types, other.types) && Objects.equals(this.isInclusive, other.isInclusive);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("inclusive", isInclusive)
                .add("types", types)
                .toString();
    }
}
