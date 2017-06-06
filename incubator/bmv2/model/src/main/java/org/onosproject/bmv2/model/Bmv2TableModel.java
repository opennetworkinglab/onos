/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.bmv2.model;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import org.onosproject.net.pi.model.PiActionModel;
import org.onosproject.net.pi.model.PiTableMatchFieldModel;
import org.onosproject.net.pi.model.PiTableModel;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

import static com.google.common.base.Preconditions.*;

/**
 * BMv2 table model.
 */
@Beta
public final class Bmv2TableModel implements PiTableModel {
    private final String name;
    private final int id;
    private final int maxSize;
    private final boolean hasCounters;
    private final boolean supportAging;
    private final Set<PiTableMatchFieldModel> matchFields;
    private final Set<PiActionModel> actions;

    /**
     * Creates new BMv2 table model.
     *
     * @param name the name of table model
     * @param id the table id
     * @param maxSize the max size of table model
     * @param hasCounters true if the table model has counter; null otherwise
     * @param supportAging true if the table model supports aging; null otherwise
     * @param matchFields the match fields of table model
     * @param actions the actions of table model
     */
    public Bmv2TableModel(String name, int id,
                          int maxSize, boolean hasCounters,
                          boolean supportAging,
                          Set<Bmv2TableMatchFieldModel> matchFields,
                          Set<Bmv2ActionModel> actions) {
        checkNotNull(name, "Model name can't be null");
        checkArgument(maxSize >= 0, "Max size should more than 0");
        checkNotNull(matchFields, "Match fields can't be null");
        checkNotNull(actions, "Actions can't be null");
        this.name = name;
        this.id = id;
        this.maxSize = maxSize;
        this.hasCounters = hasCounters;
        this.supportAging = supportAging;
        this.matchFields = ImmutableSet.copyOf(matchFields);
        this.actions = ImmutableSet.copyOf(actions);
    }

    /**
     * Gets table model id.
     *
     * @return table model id
     */
    public int id() {
        return id;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public int maxSize() {
        return maxSize;
    }

    @Override
    public boolean hasCounters() {
        return hasCounters;
    }

    @Override
    public boolean supportsAging() {
        return supportAging;
    }

    @Override
    public Collection<PiTableMatchFieldModel> matchFields() {
        return matchFields;
    }

    @Override
    public Collection<PiActionModel> actions() {
        return actions;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, id, maxSize, hasCounters, supportAging,
                            matchFields, actions);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Bmv2TableModel)) {
            return false;
        }
        Bmv2TableModel that = (Bmv2TableModel) obj;
        return Objects.equals(name, that.name) &&
                Objects.equals(id, that.id) &&
                Objects.equals(maxSize, that.maxSize) &&
                Objects.equals(hasCounters, that.hasCounters) &&
                Objects.equals(supportAging, that.supportAging) &&
                Objects.equals(matchFields, that.matchFields) &&
                Objects.equals(actions, that.actions);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("name", name)
                .add("id", id)
                .add("maxSize", maxSize)
                .add("hasCounters", hasCounters)
                .add("supportAging", supportAging)
                .add("matchFields", matchFields)
                .add("actions", actions)
                .toString();
    }
}