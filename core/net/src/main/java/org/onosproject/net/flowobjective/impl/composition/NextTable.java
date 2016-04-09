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

import org.onosproject.net.flowobjective.NextObjective;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides a table to store Next.
 */
public class NextTable {

    protected Map<Integer, NextObjective> nextMap;

    public NextTable() {
        this.nextMap = new HashMap<>();
    }

    public List<NextObjective> updateNext(NextObjective nextObjective) {
        List<NextObjective> updates = new ArrayList<>();
        switch (nextObjective.op()) {
            case ADD:
                this.nextMap.put(nextObjective.id(), nextObjective);
                updates.add(nextObjective);
                break;
            case REMOVE:
                this.nextMap.remove(nextObjective.id());
                updates.add(nextObjective);
                break;
            default:
                break;
        }
        return updates;
    }

    public List<NextObjective> updateNext(List<NextObjective> nextObjectives) {
        List<NextObjective> updates = new ArrayList<>();
        for (NextObjective nextObjective : nextObjectives) {
            updates.addAll(this.updateNext(nextObjective));
        }
        return updates;
    }

}
