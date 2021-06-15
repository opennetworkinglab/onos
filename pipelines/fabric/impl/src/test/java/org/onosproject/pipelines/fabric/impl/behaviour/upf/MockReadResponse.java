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

import org.onosproject.net.pi.runtime.PiCounterCell;
import org.onosproject.net.pi.runtime.PiCounterCellData;
import org.onosproject.net.pi.runtime.PiCounterCellHandle;
import org.onosproject.net.pi.runtime.PiEntity;
import org.onosproject.net.pi.runtime.PiEntityType;
import org.onosproject.net.pi.runtime.PiHandle;
import org.onosproject.p4runtime.api.P4RuntimeReadClient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * For faking reads to a p4runtime client. Currently only used for testing
 * UP4-specific counter reads, because all other P4 entities that UP4 reads can
 * be read via other ONOS services.
 */
public class MockReadResponse implements P4RuntimeReadClient.ReadResponse {
    List<PiEntity> entities;
    long packets;
    long bytes;

    public MockReadResponse(Iterable<? extends PiHandle> handles, long packets, long bytes) {
        this.entities = new ArrayList<>();
        this.packets = packets;
        this.bytes = bytes;
        checkNotNull(handles);
        handles.forEach(this::handle);
    }

    @Override
    public boolean isSuccess() {
        return true;
    }

    public MockReadResponse handle(PiHandle handle) {
        if (handle.entityType().equals(PiEntityType.COUNTER_CELL)) {
            PiCounterCellHandle counterHandle = (PiCounterCellHandle) handle;
            PiCounterCellData data =
                    new PiCounterCellData(this.packets, this.bytes);
            PiEntity entity = new PiCounterCell(counterHandle.cellId(), data);
            this.entities.add(entity);
        }
        // Only handles counter cell so far

        return this;
    }

    @Override
    public Collection<PiEntity> all() {
        return this.entities;
    }

    @Override
    public <E extends PiEntity> Collection<E> all(Class<E> clazz) {
        List<E> results = new ArrayList<>();
        this.entities.forEach(ent -> {
            if (ent.getClass().equals(clazz)) {
                results.add(clazz.cast(ent));
            }
        });
        return results;
    }

    @Override
    public String explanation() {
        return null;
    }

    @Override
    public Throwable throwable() {
        return null;
    }
}
