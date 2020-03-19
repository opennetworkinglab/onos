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
import com.google.common.collect.ImmutableSet;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.Link;
import org.onosproject.net.intent.ResourceContext;

import java.util.Objects;
import java.util.List;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Constraint that evaluates links based on their type.
 */
@Beta
public class TierConstraint extends BooleanConstraint {

    public enum CostType {
        /**
         * Configures the constraint to return the same cost (1.0) for any
         * link that has a valid tier value.
         */
        VALID,
        /**
         * Configures the constraint to return the tier value as the cost.
         */
        TIER,
        /**
         * Configures the constraint to return the order the tier value was
         * added to the list of included tiers on the constraint.
         */
        ORDER;
    }

    private final List<Integer> tiers;
    private final boolean isInclusive;
    private final CostType costType;

    /**
     * Creates a new constraint for requesting connectivity using or avoiding
     * the specified link tiers.
     *
     * @param inclusive indicates whether the given link tiers are to be
     *                  permitted or avoided
     * @param costType  defines the model used to calculate the link cost.
     * @param tiers     link tiers
     */
    public TierConstraint(boolean inclusive, CostType costType, Integer... tiers) {
        checkNotNull(tiers, "Link tiers cannot be null");
        checkArgument(tiers.length > 0, "There must at least one tier");
        if (costType == CostType.ORDER) {
            checkArgument(inclusive, "Order is only valid when inclusive=true");
        }
        this.tiers = ImmutableSet.copyOf(tiers).asList();
        this.isInclusive = inclusive;
        this.costType = costType;
    }

    /**
     * Creates a new constraint for requesting connectivity using or avoiding
     * the specified link tiers. The VALID cost type is used by default.
     *
     * @param inclusive indicates whether the given link tiers are to be
     *                  permitted or avoided
     * @param tiers     link tiers
     */
    public TierConstraint(boolean inclusive, Integer... tiers) {
        this(inclusive, CostType.VALID, tiers);
    }

    // Constructor for serialization
    private TierConstraint() {
        this.tiers = null;
        this.isInclusive = false;
        this.costType = CostType.VALID;
    }

    // doesn't use LinkResourceService
    @Override
    public boolean isValid(Link link, ResourceContext context) {
        // explicitly call a method not depending on LinkResourceService
        return isValid(link);
    }

    private boolean isValid(Link link) {
        boolean contains = link.annotations().keys().contains(AnnotationKeys.TIER)
                            && tiers.contains(Integer.valueOf(
                                   link.annotations().value(AnnotationKeys.TIER)));
        return isInclusive == contains;
    }

    // doesn't use LinkResourceService
    @Override
    public double cost(Link link, ResourceContext context) {
        // explicitly call a method not depending on LinkResourceService
        return cost(link);
    }

    private double cost(Link link) {
        double cost = -1.0;

        if (isValid(link)) {
            Integer tier = new Integer(link.annotations().value(AnnotationKeys.TIER));
            if (costType == CostType.ORDER) {
                cost = tiers.indexOf(tier) + 1;
            } else if (costType == CostType.TIER) {
                cost = tier;
            } else {
                cost = 1.0;
            }
        }

        return cost;
    }

    /**
     * Returns the set of link tiers.
     *
     * @return set of link tiers
     */
    public List<Integer> tiers() {
        return tiers;
    }

    /**
     * Indicates if the constraint is inclusive or exclusive.
     *
     * @return true if inclusive
     */
    public boolean isInclusive() {
        return isInclusive;
    }

    /**
     * Return the cost model used by this constraint.
     *
     * @return true if inclusive
     */
    public CostType costType() {
        return costType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tiers, isInclusive);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final TierConstraint other = (TierConstraint) obj;
        return Objects.equals(this.tiers, other.tiers) && Objects.equals(this.isInclusive, other.isInclusive)
                && Objects.equals(this.costType, other.costType);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("inclusive", isInclusive)
                .add("costType", costType)
                .add("tiers", tiers)
                .toString();
    }
}
