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
package org.onosproject.inbandtelemetry.api;

import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;

import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.inbandtelemetry.api.IntIntent.IntMetadataType;
import static org.onosproject.inbandtelemetry.api.IntIntent.IntHeaderType;

public final class IntObjective {

    private static final int DEFAULT_PRIORITY = 10;

    // TrafficSelector to describe target flows to monitor
    private final TrafficSelector selector;
    // Set of metadata types to collect
    private final Set<IntMetadataType> metadataTypes;
    // Type of header (either hop-by-hop or destination)
    private final IntHeaderType headerType;

    /**
     * Creates an IntObjective.
     *
     * @param selector      the traffic selector that identifies traffic to enable INT
     * @param metadataTypes a set of metadata types to collect
     * @param headerType    the type of INT header
     */
    private IntObjective(TrafficSelector selector, Set<IntMetadataType> metadataTypes,
                         IntHeaderType headerType) {
        this.selector = selector;
        this.metadataTypes = metadataTypes;
        this.headerType = headerType;
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
     * Returns a INT header type specified in this objective.
     *
     * @return INT header type
     */
    public IntHeaderType headerType() {
        return headerType;
    }

    /**
     * An IntObjective builder.
     */
    public static final class Builder {
        private TrafficSelector selector = DefaultTrafficSelector.emptySelector();
        private Set<IntMetadataType> metadataTypes = new HashSet<>();
        private IntHeaderType headerType = IntHeaderType.HOP_BY_HOP;

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
         * Assigns a header type to the IntObjective.
         *
         * @param headerType a header type
         * @return an IntObjective builder
         */
        public IntObjective.Builder withHeaderType(IntHeaderType headerType) {
            this.headerType = headerType;
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
            checkNotNull(headerType, "Header type cannot be null.");

            return new IntObjective(selector, metadataTypes, headerType);
        }
    }
}
