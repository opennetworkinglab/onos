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

import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.ForwardingObjective;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Provides a table to store Forward.
 */
public class ForwardTable {

    protected Map<Integer, ForwardingObjective> forwardMap;
    protected Map<Integer, List<ForwardingObjective>> generatedParentForwardingObjectiveMap;

    public ForwardTable() {
        this.forwardMap = new HashMap<>();
        this.generatedParentForwardingObjectiveMap = new HashMap<>();
    }

    public ForwardUpdateTable updateForward(ForwardingObjective forwardingObjective) {
        ForwardUpdateTable updates = new ForwardUpdateTable();
        switch (forwardingObjective.op()) {
            case ADD:
                this.forwardMap.put(forwardingObjectiveHash(forwardingObjective), forwardingObjective);
                this.generatedParentForwardingObjectiveMap
                        .put(forwardingObjectiveHash(forwardingObjective), new ArrayList<>());
                updates.addObjectives.add(forwardingObjective);
                break;
            case REMOVE:
                if (this.forwardMap.remove(forwardingObjectiveHash(forwardingObjective)) != null) {
                    updates.removeObjectives.add(forwardingObjective);
                }
                break;
            default:
                break;
        }
        return updates;
    }

    public ForwardUpdateTable updateForward(List<ForwardingObjective> forwardingObjectives) {
        ForwardUpdateTable updates = new ForwardUpdateTable();
        for (ForwardingObjective forwardingObjective : forwardingObjectives) {
            updates.addUpdateTable(this.updateForward(forwardingObjective));
        }
        return updates;
    }

    public void addGeneratedParentForwardingObjective(ForwardingObjective child, ForwardingObjective parent) {
        this.generatedParentForwardingObjectiveMap.get(forwardingObjectiveHash(child)).add(parent);
    }

    public void deleteGeneratedParentForwardingObjective(List<ForwardingObjective> children) {
        for (ForwardingObjective fo : children) {
            this.generatedParentForwardingObjectiveMap.remove(forwardingObjectiveHash(fo));
        }
    }

    private List<ForwardingObjective> getGeneratedParentForwardingObjective(ForwardingObjective child) {
        return this.generatedParentForwardingObjectiveMap.get(forwardingObjectiveHash(child));
    }

    public List<ForwardingObjective> getGeneratedParentForwardingObjectiveForRemove(ForwardingObjective child) {
        List<ForwardingObjective> fos = this.generatedParentForwardingObjectiveMap.get(forwardingObjectiveHash(child));
        List<ForwardingObjective> removeFos = new ArrayList<>();
        for (ForwardingObjective fo : fos) {
            removeFos.add(DefaultForwardingObjective.builder()
                    .fromApp(fo.appId())
                    .makePermanent()
                    .withFlag(fo.flag())
                    .withPriority(fo.priority())
                    .withSelector(fo.selector())
                    .withTreatment(fo.treatment())
                    .remove());
        }
        return removeFos;
    }

    public Collection<ForwardingObjective> getForwardingObjectives() {
        return this.forwardMap.values();
    }

    public static int forwardingObjectiveHash(ForwardingObjective forwardingObjective) {
        return Objects.hash(forwardingObjective.selector(), forwardingObjective.flag(),
                forwardingObjective.permanent(), forwardingObjective.timeout(),
                forwardingObjective.appId(), forwardingObjective.priority(),
                forwardingObjective.nextId(), forwardingObjective.treatment());
    }
}
