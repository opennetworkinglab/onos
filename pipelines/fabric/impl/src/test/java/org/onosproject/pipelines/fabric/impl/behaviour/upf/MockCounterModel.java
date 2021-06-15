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

import org.onosproject.net.pi.model.PiCounterId;
import org.onosproject.net.pi.model.PiCounterModel;
import org.onosproject.net.pi.model.PiCounterType;
import org.onosproject.net.pi.model.PiTableId;

public class MockCounterModel implements PiCounterModel {
    PiCounterId id;
    int size;

    public MockCounterModel(PiCounterId id, int size) {
        this.id = id;
        this.size = size;
    }

    @Override
    public PiCounterId id() {
        return this.id;
    }

    @Override
    public PiCounterType counterType() {
        return null;
    }

    @Override
    public Unit unit() {
        return null;
    }

    @Override
    public PiTableId table() {
        return null;
    }

    @Override
    public long size() {
        return this.size;
    }
}
