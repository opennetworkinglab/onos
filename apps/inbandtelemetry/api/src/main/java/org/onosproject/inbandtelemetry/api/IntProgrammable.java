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

import com.google.common.annotations.Beta;
import org.onosproject.net.driver.HandlerBehaviour;

import java.util.concurrent.CompletableFuture;

@Beta
public interface IntProgrammable extends HandlerBehaviour {

    /**
     * Initializes the pipeline, by installing required flow rules
     * not relevant to specific watchlist, report and event.
     */
    void init();

    /**
     * Adds a given IntObjective to the device.
     *
     * @param obj an IntObjective
     * @return true if the objective is successfully added; false otherwise.
     */
    CompletableFuture<Boolean> addIntObjective(IntObjective obj);

    /**
     * Removes a given IntObjective entry from the device.
     *
     * @param obj an IntObjective
     * @return true if the objective is successfully removed; false otherwise.
     */
    CompletableFuture<Boolean> removeIntObjective(IntObjective obj);

    /**
     * Set up report-related configuration.
     *
     * @param config a configuration regarding to the collector
     * @return true if the objective is successfully added; false otherwise.
     */
    CompletableFuture<Boolean> setupIntConfig(IntConfig config);

    //TODO: [ONOS-7616] Design IntEvent and related APIs
}
