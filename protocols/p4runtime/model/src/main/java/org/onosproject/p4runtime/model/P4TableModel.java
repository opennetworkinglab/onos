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

package org.onosproject.p4runtime.model;

import com.google.common.collect.ImmutableMap;
import org.onosproject.net.pi.model.PiActionId;
import org.onosproject.net.pi.model.PiActionModel;
import org.onosproject.net.pi.model.PiActionProfileModel;
import org.onosproject.net.pi.model.PiCounterId;
import org.onosproject.net.pi.model.PiCounterModel;
import org.onosproject.net.pi.model.PiMatchFieldId;
import org.onosproject.net.pi.model.PiMatchFieldModel;
import org.onosproject.net.pi.model.PiMeterId;
import org.onosproject.net.pi.model.PiMeterModel;
import org.onosproject.net.pi.model.PiTableId;
import org.onosproject.net.pi.model.PiTableModel;
import org.onosproject.net.pi.model.PiTableType;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Implementation of PiTableModel for P4Runtime.
 */
final class P4TableModel implements PiTableModel {

    private final PiTableId id;
    private final PiTableType tableType;
    private final PiActionProfileModel actionProfile;
    private final long maxSize;
    private final ImmutableMap<PiCounterId, PiCounterModel> counters;
    private final ImmutableMap<PiMeterId, PiMeterModel> meters;
    private final boolean supportAging;
    private final ImmutableMap<PiMatchFieldId, PiMatchFieldModel> matchFields;
    private final ImmutableMap<PiActionId, PiActionModel> actions;
    private final PiActionModel constDefaultAction;
    private final boolean isConstTable;
    private final boolean oneShotOnly;

    P4TableModel(PiTableId id, PiTableType tableType,
                 PiActionProfileModel actionProfile, long maxSize,
                 ImmutableMap<PiCounterId, PiCounterModel> counters,
                 ImmutableMap<PiMeterId, PiMeterModel> meters, boolean supportAging,
                 ImmutableMap<PiMatchFieldId, PiMatchFieldModel> matchFields,
                 ImmutableMap<PiActionId, PiActionModel> actions,
                 PiActionModel constDefaultAction,
                 boolean isConstTable, boolean oneShotOnly) {
        this.id = id;
        this.tableType = tableType;
        this.actionProfile = actionProfile;
        this.maxSize = maxSize;
        this.counters = counters;
        this.meters = meters;
        this.supportAging = supportAging;
        this.matchFields = matchFields;
        this.actions = actions;
        this.constDefaultAction = constDefaultAction;
        this.isConstTable = isConstTable;
        this.oneShotOnly = oneShotOnly;
    }

    @Override
    public PiTableId id() {
        return id;
    }

    @Override
    public PiTableType tableType() {
        return tableType;
    }

    @Override
    public PiActionProfileModel actionProfile() {
        return actionProfile;
    }

    @Override
    public long maxSize() {
        return maxSize;
    }

    @Override
    public Collection<PiCounterModel> counters() {
        return counters.values();
    }

    @Override
    public Collection<PiMeterModel> meters() {
        return meters.values();
    }

    @Override
    public boolean supportsAging() {
        return supportAging;
    }

    @Override
    public Collection<PiMatchFieldModel> matchFields() {
        return matchFields.values();
    }

    @Override
    public Collection<PiActionModel> actions() {
        return actions.values();
    }

    @Override
    public Optional<PiActionModel> constDefaultAction() {
        return Optional.ofNullable(constDefaultAction);
    }

    @Override
    public boolean isConstantTable() {
        return isConstTable;
    }

    public boolean oneShotOnly() {
        return oneShotOnly;
    }

    @Override
    public Optional<PiActionModel> action(PiActionId actionId) {
        return Optional.ofNullable(actions.get(actionId));
    }

    @Override
    public Optional<PiMatchFieldModel> matchField(PiMatchFieldId matchFieldId) {
        return Optional.ofNullable(matchFields.get(matchFieldId));
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, tableType, actionProfile, maxSize, counters,
                            meters, supportAging, matchFields, actions,
                            constDefaultAction);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final P4TableModel other = (P4TableModel) obj;
        return Objects.equals(this.id, other.id)
                && Objects.equals(this.tableType, other.tableType)
                && Objects.equals(this.actionProfile, other.actionProfile)
                && Objects.equals(this.maxSize, other.maxSize)
                && Objects.equals(this.counters, other.counters)
                && Objects.equals(this.meters, other.meters)
                && Objects.equals(this.supportAging, other.supportAging)
                && Objects.equals(this.matchFields, other.matchFields)
                && Objects.equals(this.actions, other.actions)
                && Objects.equals(this.constDefaultAction, other.constDefaultAction)
                && Objects.equals(this.oneShotOnly, other.oneShotOnly);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("id", id)
                .add("tableType", tableType)
                .add("actionProfile", actionProfile)
                .add("maxSize", maxSize)
                .add("counters", counters)
                .add("meters", meters)
                .add("supportAging", supportAging)
                .add("matchFields", matchFields)
                .add("actions", actions)
                .add("constDefaultAction", constDefaultAction)
                .add("isConstTable", isConstTable)
                .toString();
    }
}
