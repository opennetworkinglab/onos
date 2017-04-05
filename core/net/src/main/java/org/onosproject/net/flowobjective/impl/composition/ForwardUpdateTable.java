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

import org.onosproject.net.flowobjective.ForwardingObjective;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides an update table for Forward.
 */
public class ForwardUpdateTable {
    List<ForwardingObjective> addObjectives;
    List<ForwardingObjective> removeObjectives;

    public ForwardUpdateTable() {
        this.addObjectives = new ArrayList<>();
        this.removeObjectives = new ArrayList<>();
    }

    public void addUpdateTable(ForwardUpdateTable updateTable) {
        this.addObjectives.addAll(updateTable.addObjectives);
        this.removeObjectives.addAll(updateTable.removeObjectives);
    }

    public List<ForwardingObjective> toForwardingObjectiveList() {
        List<ForwardingObjective> forwardingObjectives = new ArrayList<>();
        forwardingObjectives.addAll(this.addObjectives);
        forwardingObjectives.addAll(this.removeObjectives);
        return forwardingObjectives;
    }
}
