/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.net;

/**
 * Abstraction of wavelength. Currently, this is just a marker interface
 */
public interface Lambda {
    /**
     * Creates a Lambda instance with the specified arguments.
     *
     * @param gridType          grid type
     * @param channelSpacing    channel spacing
     * @param spacingMultiplier channel spacing multiplier
     * @param slotGranularity   slot width granularity
     * @return new lambda with specified arguments
     */
    static Lambda ochSignal(GridType gridType, ChannelSpacing channelSpacing,
                            int spacingMultiplier, int slotGranularity) {
        return new OchSignal(gridType, channelSpacing, spacingMultiplier, slotGranularity);
    }
}
