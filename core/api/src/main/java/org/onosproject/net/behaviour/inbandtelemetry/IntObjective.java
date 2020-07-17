/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.net.behaviour.inbandtelemetry;

import com.google.common.collect.ImmutableSet;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;

import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Represents a device-level objective to collect INT metadata for packets
 * identified by a traffic selector.
 */
public final class IntObjective {

    private static final int DEFAULT_PRIORITY = 10;

    // TrafficSelector to describe target flows to monitor
    private final TrafficSelector selector;
    // Set of metadata types to collect
    private final ImmutableSet<IntMetadataType> metadataTypes;

    /**
     * Creates an IntObjective.
     *
     * @param selector      the traffic selector that identifies traffic to enable INT
     * @param metadataTypes a set of metadata types to collect
     */
    private IntObjective(TrafficSelector selector, Set<IntMetadataType> metadataTypes) {
        this.selector = selector;
        this.metadataTypes = ImmutableSet.copyOf(metadataTypes);
    }

    /**
     * Returns traffic selector of this objective.
     *
     * @return traffic selector
     */
    public TrafficSelector selector() {
        return selector;
    }

    /**
     * Returns a set of metadata types specified in this objective.
     *
     * @return instruction bitmap
     */
    public Set<IntMetadataType> metadataTypes() {
        return metadataTypes;
    }

    /**
     * An IntObjective builder.
     */
    public static final class Builder {
        private TrafficSelector selector = DefaultTrafficSelector.emptySelector();
        private final Set<IntMetadataType> metadataTypes = new HashSet<>();

        /**
         * Assigns a selector to the IntObjective.
         *
         * @param selector a traffic selector
         * @return an IntObjective builder
         */
        public IntObjective.Builder withSelector(TrafficSelector selector) {
            this.selector = selector;
            return this;
        }

        /**
         * Add a metadata type to the IntObjective.
         *
         * @param metadataTypes a set of metadata types
         * @return an IntObjective builder
         */
        public IntObjective.Builder withMetadataTypes(Set<IntMetadataType> metadataTypes) {
            this.metadataTypes.addAll(metadataTypes);
            return this;
        }

        /**
         * Builds the IntObjective.
         *
         * @return an IntObjective
         */
        public IntObjective build() {
            checkArgument(!selector.criteria().isEmpty(), "Empty selector cannot match any flow.");
            checkArgument(!metadataTypes.isEmpty(), "Metadata types cannot be empty");

            return new IntObjective(selector, metadataTypes);
        }
    }
}
