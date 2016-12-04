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
package org.onosproject.net.behaviour;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import org.onlab.packet.DscpClass;
import org.onlab.packet.IPPrecedence;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

/**
 * Represents an action to be taken by a marker/policer.
 */
@Beta
public final class BandwidthProfileAction {

    /**
     * Denotes the type of action to be taken.
     */
    public enum Action {
        /**
         * Traffic is allowed to pass unmodified.
         */
        PASS,

        /**
         * Traffic is allowed to pass after being appropriately remarked.
         */
        REMARK,

        /**
         * Traffic is dropped.
         */
        DISCARD
    }

    private final Action action;
    private final DscpClass dscpClass;
    private final IPPrecedence ipPrecedence;
    private final Short dropPrecedence;

    private BandwidthProfileAction(Action action,
                                   DscpClass dscpClass,
                                   IPPrecedence ipPrecedence,
                                   Short dropPrecedence) {
        this.action = action;
        this.dscpClass = dscpClass;
        this.ipPrecedence = ipPrecedence;
        this.dropPrecedence = dropPrecedence;
    }

    /**
     * Obtains the type of this bandwidth profile action object.
     *
     * @return the bandwidth profile action type
     */
    public Action getAction() {
        return this.action;
    }

    /**
     * Obtains the DSCP class corresponding to the REMARK action.
     * If this is not a REMARK action or if another field is remarked
     * null is returned.
     *
     * @return the DSCP class for the action; may be null
     */
    public DscpClass getDscpClass() {
        return this.dscpClass;
    }

    /**
     * Obtains the IP precedence corresponding to the REMARK action.
     * If this is not a REMARK action or if another field is remarked
     * null is returned.
     *
     * @return the IP precedence for the action; may be null
     */
    public IPPrecedence getIpPrecedence() {
        return this.ipPrecedence;
    }

    /**
     * Obtains the drop precedence corresponding to the REMARK action.
     * If this is not a REMARK action or if another field is remarked
     * null is returned.
     *
     * @return the drop precedence for the action; may be null
     */
    public Short getDropPrecedence() {
        return this.dropPrecedence;
    }

    /**
     * Returns a new builder.
     *
     * @return new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder of BandwidthProfileAction entities.
     */
    public static final class Builder {

        private Action action;
        private DscpClass dscpClass;
        private IPPrecedence ipPrecedence;
        private Short dropPrecedence;

        /**
         * Sets the type of this builder.
         *
         * @param action the builder type to set
         * @return this builder instance
         */
        public Builder action(Action action) {
            this.action = action;
            return this;
        }

        /**
         * Sets the DSCP class of this builder.
         *
         * @param dscpClass the builder DSCP class to set
         * @return this builder instance
         */
        public Builder dscpClass(DscpClass dscpClass) {
            this.dscpClass = dscpClass;
            return this;
        }

        /**
         * Sets the IP precedence of this builder.
         *
         * @param ipPrecedence the builder IP precedence to set
         * @return this builder instance
         */
        public Builder ipPrecedence(IPPrecedence ipPrecedence) {
            this.ipPrecedence = ipPrecedence;
            return this;
        }

        /**
         * Sets the drop precedence of this builder.
         *
         * @param dropPrecedence the drop IP precedence to set
         * @return this builder instance
         */
        public Builder dropPrecedence(Short dropPrecedence) {
            this.dropPrecedence = dropPrecedence;
            return this;
        }

        /**
         * Builds a new BandwidthProfileAction based on builder's parameters.
         *
         * @return a new BandwidthProfileAction instance
         */
        public BandwidthProfileAction build() {
            checkNotNull(action);
            checkArgument(!action.equals(Action.REMARK) ||
                                  (dscpClass != null ^
                                          ipPrecedence != null ^
                                          dropPrecedence != null),
                          "Exactly one remark type must be defined");
            return new BandwidthProfileAction(action,
                                              dscpClass,
                                              ipPrecedence,
                                              dropPrecedence);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(action, dscpClass, ipPrecedence, dropPrecedence);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof BandwidthProfileAction) {
            final BandwidthProfileAction that = (BandwidthProfileAction) obj;
            return this.getClass() == that.getClass() &&
                    Objects.equals(this.action, that.action) &&
                    Objects.equals(this.dscpClass, that.dscpClass) &&
                    Objects.equals(this.ipPrecedence, that.ipPrecedence) &&
                    Objects.equals(this.dropPrecedence, that.dropPrecedence);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("action", action == null ? null : action.name())
                .add("dscpClass", dscpClass == null ? null : dscpClass.name())
                .add("ipPrecedence", ipPrecedence)
                .add("dropPrecedence", dropPrecedence)
                .toString();
    }
}
