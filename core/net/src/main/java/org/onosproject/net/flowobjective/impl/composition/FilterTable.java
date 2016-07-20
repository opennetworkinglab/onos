/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.net.flowobjective.impl.composition;

import org.onosproject.net.flowobjective.FilteringObjective;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides a table to store Fitler.
 */
public class FilterTable {

    protected Map<Integer, FilteringObjective> filterMap;

    public FilterTable() {
        this.filterMap = new HashMap<>();
    }

    public List<FilteringObjective> updateFilter(FilteringObjective filteringObjective) {
        List<FilteringObjective> updates = new ArrayList<>();
        switch (filteringObjective.op()) {
            case ADD:
                this.filterMap.put(filteringObjective.id(), filteringObjective);
                updates.add(filteringObjective);
                break;
            case REMOVE:
                this.filterMap.remove(filteringObjective.id());
                updates.add(filteringObjective);
                break;
            default:
                break;
        }
        return updates;
    }

    public List<FilteringObjective> updateFilter(List<FilteringObjective> filteringObjectives) {
        List<FilteringObjective> updates = new ArrayList<>();
        for (FilteringObjective filteringObjective : filteringObjectives) {
            updates.addAll(this.updateFilter(filteringObjective));
        }
        return updates;
    }

}
