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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.onosproject.net.pi.model.PiActionModel;
import org.onosproject.net.pi.model.PiHeaderModel;
import org.onosproject.net.pi.model.PiHeaderTypeModel;
import org.onosproject.net.pi.model.PiPipelineModel;
import org.onosproject.net.pi.model.PiTableModel;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * BMv2 pipeline model.
 */
@Beta
public class Bmv2PipelineModel implements PiPipelineModel {
    private final Map<String, PiHeaderTypeModel> headerTypeModels;
    private final Map<String, PiHeaderModel> headerModels;
    private final Map<String, PiActionModel> actionModels;
    private final Map<String, PiTableModel> tableModels;

    /**
     * Constructs a BMv2 pipeline model by given information.
     *
     * @param headerTypeModels the header type models for this pipeline model
     * @param headerModels the header models for this pipeline model
     * @param actionModels the action models for this pipeline model
     * @param tableModels the table models for this pipeline model
     */
    Bmv2PipelineModel(List<Bmv2HeaderTypeModel> headerTypeModels,
                      List<Bmv2HeaderModel> headerModels,
                      List<Bmv2ActionModel> actionModels,
                      List<Bmv2TableModel> tableModels) {
        checkNotNull(headerTypeModels, "Header type models can't be null");
        checkNotNull(headerModels, "Header models can't be null");
        checkNotNull(actionModels, "Action models can't be null");
        checkNotNull(tableModels, "Table models can't be null");

        Map<String, PiHeaderTypeModel> headerTypeModelMap = Maps.newHashMap();
        headerTypeModels.stream()
                .filter(Objects::nonNull)
                .forEach(htm -> headerTypeModelMap.put(htm.name(), htm));
        this.headerTypeModels = ImmutableMap.copyOf(headerTypeModelMap);

        Map<String, PiHeaderModel> headerModelMap = Maps.newHashMap();
        headerModels.stream()
                .filter(Objects::nonNull)
                .forEach(hm -> headerModelMap.put(hm.type().name(), hm));
        this.headerModels = ImmutableMap.copyOf(headerModelMap);

        Map<String, PiActionModel> actionModelMap = Maps.newHashMap();
        actionModels.stream()
                .filter(Objects::nonNull)
                .forEach(am -> actionModelMap.put(am.name(), am));
        this.actionModels = ImmutableMap.copyOf(actionModelMap);

        Map<String, PiTableModel> tableModelMap = Maps.newHashMap();
        tableModels.stream()
                .filter(Objects::nonNull)
                .forEach(tm -> tableModelMap.put(tm.name(), tm));
        this.tableModels = ImmutableMap.copyOf(tableModelMap);
    }

    @Override
    public Optional<PiHeaderTypeModel> headerType(String name) {
        return Optional.ofNullable(headerTypeModels.get(name));
    }

    @Override
    public Collection<PiHeaderTypeModel> headerTypes() {
        return headerTypeModels.values();
    }

    @Override
    public Optional<PiHeaderModel> header(String name) {
        return Optional.ofNullable(headerModels.get(name));
    }

    @Override
    public Collection<PiHeaderModel> headers() {
        return headerModels.values();
    }

    @Override
    public Optional<PiActionModel> action(String name) {
        return Optional.ofNullable(actionModels.get(name));
    }

    @Override
    public Collection<PiActionModel> actions() {
        return actionModels.values();
    }

    @Override
    public Optional<PiTableModel> table(String name) {
        return Optional.ofNullable(tableModels.get(name));
    }

    @Override
    public Collection<PiTableModel> tables() {
        return tableModels.values();
    }

    @Override
    public int hashCode() {
        return Objects.hash(headerTypeModels, headerModels, actionModels, tableModels);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Bmv2PipelineModel)) {
            return false;
        }
        Bmv2PipelineModel that = (Bmv2PipelineModel) obj;
        return Objects.equals(this.headerTypeModels, that.headerTypeModels) &&
                Objects.equals(this.headerModels, that.headerModels) &&
                Objects.equals(this.actionModels, that.actionModels) &&
                Objects.equals(this.tableModels, that.tableModels);
    }
}
