/*
 * Copyright 2021-present Open Networking Foundation
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

package org.onosproject.pipelines.fabric.impl.behaviour.upf;

import org.onosproject.net.pi.model.PiActionId;
import org.onosproject.net.pi.model.PiActionModel;
import org.onosproject.net.pi.model.PiActionProfileModel;
import org.onosproject.net.pi.model.PiCounterModel;
import org.onosproject.net.pi.model.PiMatchFieldId;
import org.onosproject.net.pi.model.PiMatchFieldModel;
import org.onosproject.net.pi.model.PiMeterModel;
import org.onosproject.net.pi.model.PiTableId;
import org.onosproject.net.pi.model.PiTableModel;
import org.onosproject.net.pi.model.PiTableType;

import java.util.Collection;
import java.util.Optional;

public class MockTableModel implements PiTableModel {
    PiTableId id;
    int size;

    public MockTableModel(PiTableId id, int size) {
        this.id = id;
        this.size = size;
    }

    @Override
    public PiTableId id() {
        return this.id;
    }

    @Override
    public PiTableType tableType() {
        return null;
    }

    @Override
    public PiActionProfileModel actionProfile() {
        return null;
    }

    @Override
    public long maxSize() {
        return size;
    }

    @Override
    public Collection<PiCounterModel> counters() {
        return null;
    }

    @Override
    public Collection<PiMeterModel> meters() {
        return null;
    }

    @Override
    public boolean supportsAging() {
        return false;
    }

    @Override
    public Collection<PiMatchFieldModel> matchFields() {
        return null;
    }

    @Override
    public Collection<PiActionModel> actions() {
        return null;
    }

    @Override
    public Optional<PiActionModel> constDefaultAction() {
        return Optional.empty();
    }

    @Override
    public boolean isConstantTable() {
        return false;
    }

    @Override
    public boolean oneShotOnly() {
        return false;
    }

    @Override
    public Optional<PiActionModel> action(PiActionId actionId) {
        return Optional.empty();
    }

    @Override
    public Optional<PiMatchFieldModel> matchField(PiMatchFieldId matchFieldId) {
        return Optional.empty();
    }
}
