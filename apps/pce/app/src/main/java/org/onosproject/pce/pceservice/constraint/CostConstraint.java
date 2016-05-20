/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.pce.pceservice.constraint;

import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.intent.ResourceContext;
import org.onosproject.net.intent.Constraint;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Constraint that evaluates whether cost for a link is available, if yes return cost for that link.
 */
public final class CostConstraint implements Constraint {

    /**
     * Represents about cost types.
     */
    public enum Type {
        /**
         * Signifies that cost is IGP cost.
         */
        COST(1),

        /**
         * Signifies that cost is TE cost.
         */
        TE_COST(2);

        int value;

        /**
         * Assign val with the value as the Cost type.
         *
         * @param val Cost type
         */
        Type(int val) {
            value = val;
        }

        /**
         * Returns value of Cost type.
         *
         * @return Cost type
         */
        public byte type() {
            return (byte) value;
        }
    }

    private final Type type;
    public static final String TE_COST = "teCost";
    public static final String COST = "cost";

    // Constructor for serialization
    private CostConstraint() {
        this.type = null;
    }

    /**
     * Creates a new cost constraint.
     *
     * @param type of a link
     */
    public CostConstraint(Type type) {
        this.type = checkNotNull(type, "Type cannot be null");
    }

    /**
     * Creates new CostConstraint with specified cost type.
     *
     * @param type of cost
     * @return instance of CostConstraint
     */
    public static CostConstraint of(Type type) {
        return new CostConstraint(type);
    }

    /**
     * Returns the type of a cost specified in a constraint.
     *
     * @return required cost type
     */
    public Type type() {
        return type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof CostConstraint) {
            CostConstraint other = (CostConstraint) obj;
            return Objects.equals(this.type, other.type);
        }

        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("type", type)
                .toString();
    }

    @Override
    public double cost(Link link, ResourceContext context) {
        //TODO: Usage of annotations are for transient solution. In future will be replaces with the
        // network config service / Projection model.
        switch (type) {
        case COST:
            if (link.annotations().value(COST) != null) {
                return Double.parseDouble(link.annotations().value(COST));
            }

            //If cost annotations absent return -1[It is not L3 device]
            return -1;
        case TE_COST:
            if (link.annotations().value(TE_COST) != null) {
                return Double.parseDouble(link.annotations().value(TE_COST));
            }

            //If TE cost annotations absent return -1[It is not L3 device]
            return -1;
        default:
            return -1;
        }
    }

    @Override
    public boolean validate(Path path, ResourceContext context) {
        // TODO Auto-generated method stub
        return false;
    }
}