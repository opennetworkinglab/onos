/*
 *  Copyright 2016-present Open Networking Laboratory
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.onosproject.ui;

import org.onosproject.ui.model.topo.UiTopoLayout;
import org.onosproject.ui.model.topo.UiTopoLayoutId;

import java.util.Set;

/**
 * Service for managing {@link UiTopoLayout} instances.
 */
public interface UiTopoLayoutService {

    /**
     * Returns the set of available layouts.
     *
     * @return set of available layouts
     */
    Set<UiTopoLayout> getLayouts();

    /**
     * Adds a layout to the system or updates an existing one.
     *
     * @param layout the layout to add or update
     * @return an indication of success
     */
    boolean addLayout(UiTopoLayout layout);


    /**
     * Returns the layout with the specified identifier.
     * @param layoutId layout identifier
     * @return layout or null if no such layout is found
     */
    UiTopoLayout getLayout(UiTopoLayoutId layoutId);

    /**
     * Removes a layout from the system.
     *
     * @param layout the layout to remove
     * @return an indication of success
     */
    boolean removeLayout(UiTopoLayout layout);

}
