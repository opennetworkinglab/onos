/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.net.device;

import org.onosproject.net.driver.HandlerBehaviour;

import java.util.Collection;

/**
 * Handler behaviour capable of collecting and updating port statistics.
 */
public interface PortStatisticsDiscovery extends HandlerBehaviour {

    /**
     * Returns a list of port statistics descriptions appropriately annotated
     * to support downstream model extension via projections of their parent
     * device.
     *
     * @return annotated port statistics
     */
    Collection<PortStatistics> discoverPortStatistics();
}
