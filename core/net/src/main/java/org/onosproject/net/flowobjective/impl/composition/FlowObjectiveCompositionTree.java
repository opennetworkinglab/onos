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
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjective;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Provides a policy tree to store all flow tables for each device.
 *
 * Note: This class uses in-memory structures and is not yet distributed.
 */
public class FlowObjectiveCompositionTree {

    FlowObjectiveCompositionManager.PolicyOperator operator;
    FlowObjectiveCompositionTree leftChild;
    FlowObjectiveCompositionTree rightChild;
    protected short applicationId;
    protected FilterTable filterTable;
    protected ForwardTable forwardTable;
    protected NextTable nextTable;

    protected int priorityMultiplier;
    protected int priorityAddend;

    public FlowObjectiveCompositionTree(short applicationId) {
        this.operator = FlowObjectiveCompositionManager.PolicyOperator.Application;
        this.leftChild = null;
        this.rightChild = null;
        this.applicationId = applicationId;
        this.filterTable = new FilterTable();
        this.forwardTable = new ForwardTable();
        this.nextTable = new NextTable();
        this.priorityMultiplier = 10;
        this.priorityAddend = 10;
    }

    public FlowObjectiveCompositionTree(Character ch) {
        switch (ch) {
            case '+':
                this.operator = FlowObjectiveCompositionManager.PolicyOperator.Parallel;
                break;
            case '>':
                this.operator = FlowObjectiveCompositionManager.PolicyOperator.Sequential;
                break;
            case '/':
                this.operator = FlowObjectiveCompositionManager.PolicyOperator.Override;
                break;
            default:
                this.operator = FlowObjectiveCompositionManager.PolicyOperator.Application;
                break;
        }
        this.leftChild = null;
        this.rightChild = null;
        this.applicationId = (short) -1;
        this.filterTable = new FilterTable();
        this.forwardTable = new ForwardTable();
        this.nextTable = new NextTable();
        this.priorityMultiplier = 10;
        this.priorityAddend = 10;
    }

    protected List<FilteringObjective> updateFilter(FilteringObjective filteringObjective) {
        switch (this.operator) {
            case Parallel:
                return updateFilterParallel(filteringObjective);
            case Sequential:
                return updateFilterSequential(filteringObjective);
            case Override:
                return updateFilterOverride(filteringObjective);
            case Application:
                if (filteringObjective.appId().id() == this.applicationId) {
                    return this.filterTable.updateFilter(filteringObjective);
                } else {
                    return new ArrayList<>();
                }
            default:
                    return new ArrayList<>();
        }
    }

    // Parallel composition: the filter set is the union of the children
    protected List<FilteringObjective> updateFilterParallel(FilteringObjective filteringObjective) {
        List<FilteringObjective> leftUpdates = this.leftChild.updateFilter(filteringObjective);
        List<FilteringObjective> rightUpdates = this.rightChild.updateFilter(filteringObjective);

        List<FilteringObjective> updates = new ArrayList<>();
        updates.addAll(leftUpdates);
        updates.addAll(rightUpdates);

        return this.filterTable.updateFilter(updates);
    }

    // Sequential composition: the filter set is the filter set of the left child
    protected List<FilteringObjective> updateFilterSequential(FilteringObjective filteringObjective) {
        List<FilteringObjective> leftUpdates = this.leftChild.updateFilter(filteringObjective);
        List<FilteringObjective> rightUpdates = this.rightChild.updateFilter(filteringObjective);
        return this.filterTable.updateFilter(leftUpdates);
    }

    // Override composition: the filter set is the filter set of the left child
    protected List<FilteringObjective> updateFilterOverride(FilteringObjective filteringObjective) {
        List<FilteringObjective> leftUpdates = this.leftChild.updateFilter(filteringObjective);
        List<FilteringObjective> rightUpdates = this.rightChild.updateFilter(filteringObjective);
        return this.filterTable.updateFilter(leftUpdates);
    }

    public List<ForwardingObjective> updateForward(ForwardingObjective forwardingObjective) {
        return this.updateForwardNode(forwardingObjective).toForwardingObjectiveList();
    }

    public ForwardUpdateTable updateForwardNode(ForwardingObjective forwardingObjective) {
        switch (this.operator) {
            case Parallel:
            case Sequential:
            case Override:
                return updateForwardComposition(forwardingObjective);
            case Application:
                if (forwardingObjective.appId().id() == this.applicationId) {
                    return this.forwardTable.updateForward(forwardingObjective);
                } else {
                    return (new ForwardUpdateTable());
                }
            default:
                return (new ForwardUpdateTable());
        }
    }

    protected ForwardUpdateTable updateForwardComposition(ForwardingObjective forwardingObjective) {
        ForwardUpdateTable leftUpdates = this.leftChild.updateForwardNode(forwardingObjective);
        ForwardUpdateTable rightUpdates = this.rightChild.updateForwardNode(forwardingObjective);

        List<ForwardingObjective> addUpdates = new ArrayList<>();
        List<ForwardingObjective> removeUpdates = new ArrayList<>();
        // Handle ADD
        if (this.operator == FlowObjectiveCompositionManager.PolicyOperator.Parallel
                || this.operator == FlowObjectiveCompositionManager.PolicyOperator.Sequential) {
            for (ForwardingObjective fo1 : leftUpdates.addObjectives) {
                for (ForwardingObjective fo2 : this.rightChild.forwardTable.getForwardingObjectives()) {
                    ForwardingObjective composedFo = null;
                    if (this.operator == FlowObjectiveCompositionManager.PolicyOperator.Parallel) {
                        composedFo = FlowObjectiveCompositionUtil.composeParallel(fo1, fo2);
                    } else {
                        composedFo = FlowObjectiveCompositionUtil.composeSequential(fo1, fo2, this.priorityMultiplier);
                    }
                    if (composedFo != null) {
                        addUpdates.add(composedFo);
                        this.leftChild.forwardTable.addGeneratedParentForwardingObjective(fo1, composedFo);
                        this.rightChild.forwardTable.addGeneratedParentForwardingObjective(fo2, composedFo);
                    }
                }
            }
            Collection<ForwardingObjective> leftTableWithoutAdd = FlowObjectiveCompositionUtil
                    .minusForwardingObjectives(this.leftChild.forwardTable.getForwardingObjectives(),
                            leftUpdates.addObjectives);
            for (ForwardingObjective fo1 : leftTableWithoutAdd) {
                for (ForwardingObjective fo2 : rightUpdates.addObjectives) {
                    ForwardingObjective composedFo = null;
                    if (this.operator == FlowObjectiveCompositionManager.PolicyOperator.Parallel) {
                        composedFo = FlowObjectiveCompositionUtil.composeParallel(fo1, fo2);
                    } else {
                        composedFo = FlowObjectiveCompositionUtil.composeSequential(fo1, fo2, this.priorityMultiplier);
                    }
                    if (composedFo != null) {
                        addUpdates.add(composedFo);
                        this.leftChild.forwardTable.addGeneratedParentForwardingObjective(fo1, composedFo);
                        this.rightChild.forwardTable.addGeneratedParentForwardingObjective(fo2, composedFo);
                    }
                }
            }
        } else {
            for (ForwardingObjective fo : leftUpdates.addObjectives) {
                ForwardingObjective composedFo = FlowObjectiveCompositionUtil.composeOverride(fo, this.priorityAddend);
                addUpdates.add(composedFo);
                this.leftChild.forwardTable.addGeneratedParentForwardingObjective(fo, composedFo);
            }
            for (ForwardingObjective fo : rightUpdates.addObjectives) {
                ForwardingObjective composedFo = FlowObjectiveCompositionUtil.composeOverride(fo, 0);
                addUpdates.add(composedFo);
                this.rightChild.forwardTable.addGeneratedParentForwardingObjective(fo, composedFo);
            }
        }

        // Handle REMOVE
        for (ForwardingObjective fo : leftUpdates.removeObjectives) {
            List<ForwardingObjective> fos = this.leftChild.forwardTable
                    .getGeneratedParentForwardingObjectiveForRemove(fo);
            removeUpdates.addAll(fos);
        }
        this.leftChild.forwardTable.deleteGeneratedParentForwardingObjective(leftUpdates.removeObjectives);
        for (ForwardingObjective fo : rightUpdates.removeObjectives) {
            List<ForwardingObjective> fos = this.rightChild.forwardTable
                    .getGeneratedParentForwardingObjectiveForRemove(fo);
            removeUpdates.addAll(fos);
        }
        this.rightChild.forwardTable.deleteGeneratedParentForwardingObjective(rightUpdates.removeObjectives);

        ForwardUpdateTable updates = new ForwardUpdateTable();
        updates.addUpdateTable(this.forwardTable.updateForward(addUpdates));
        updates.addUpdateTable(this.forwardTable.updateForward(removeUpdates));
        return updates;
    }

    public List<NextObjective> updateNext(NextObjective nextObjective) {
        switch (this.operator) {
            case Parallel:
            case Sequential:
            case Override:
                return updateNextComposition(nextObjective);
            case Application:
                if (nextObjective.appId().id() == this.applicationId) {
                    return this.nextTable.updateNext(nextObjective);
                } else {
                    return new ArrayList<>();
                }
            default:
                return new ArrayList<>();
        }
    }

    // Next: the union of the children
    protected List<NextObjective> updateNextComposition(NextObjective nextObjective) {
        List<NextObjective> leftUpdates = this.leftChild.updateNext(nextObjective);
        List<NextObjective> rightUpdates = this.rightChild.updateNext(nextObjective);

        List<NextObjective> updates = new ArrayList<>();
        updates.addAll(leftUpdates);
        updates.addAll(rightUpdates);

        return this.nextTable.updateNext(updates);
    }

    @Override
    public String toString() {
        String str = null;
        switch (this.operator) {
            case Parallel:
                str = "(" + this.leftChild + "+" + this.rightChild + ")";
                break;
            case Sequential:
                str = "(" + this.leftChild + ">" + this.rightChild + ")";
                break;
            case Override:
                str = "(" + this.leftChild + "/" + this.rightChild + ")";
                break;
            default:
                str = " " + applicationId + " ";
                break;
        }
        return str;
    }

}
