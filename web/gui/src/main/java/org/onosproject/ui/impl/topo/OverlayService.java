/*
 * Copyright 2015 Open Networking Laboratory
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
 *
 */

package org.onosproject.ui.impl.topo;

import org.onosproject.ui.impl.topo.overlay.SummaryGenerator;

/**
 * Provides the API for external agents to inject topology overlay behavior.
 */
// TODO: move to core-api module
public interface OverlayService {

    /**
     * Registers a custom summary generator for the specified overlay.
     *
     * @param overlayId overlay identifier
     * @param generator generator to register
     */
    void addSummaryGenerator(String overlayId, SummaryGenerator generator);

    /**
     * Unregisters the generator associated with the specified overlay.
     *
     * @param overlayId overlay identifier
     */
    void removeSummaryGenerator(String overlayId);
}
