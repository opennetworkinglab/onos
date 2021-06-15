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

import org.onosproject.net.DeviceId;
import org.onosproject.net.pi.model.PiCounterModel;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.model.PiPipeconfId;
import org.onosproject.net.pi.model.PiTableModel;
import org.onosproject.net.pi.service.PiPipeconfListener;
import org.onosproject.net.pi.service.PiPipeconfService;

import java.util.Collection;
import java.util.Optional;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

public class MockPiPipeconfService implements PiPipeconfService {

    private final PiPipeconf mockPiPipeconf;

    public MockPiPipeconfService(Collection<PiTableModel> tables,
                                 Collection<PiCounterModel> counters) {
        mockPiPipeconf = createMock(PiPipeconf.class);
        expect(mockPiPipeconf.pipelineModel())
                .andReturn(new MockPiPipelineModel(tables, counters))
                .anyTimes();
        replay(mockPiPipeconf);
    }

    @Override
    public Optional<PiPipeconf> getPipeconf(PiPipeconfId id) {
        return Optional.of(mockPiPipeconf);
    }

    @Override
    public Optional<PiPipeconf> getPipeconf(DeviceId deviceId) {
        return Optional.of(mockPiPipeconf);
    }

    @Override
    public void register(PiPipeconf pipeconf) throws IllegalStateException {

    }

    @Override
    public void unregister(PiPipeconfId pipeconfId) throws IllegalStateException {

    }

    @Override
    public Iterable<PiPipeconf> getPipeconfs() {
        return null;
    }

    @Override
    public void bindToDevice(PiPipeconfId pipeconfId, DeviceId deviceId) {

    }

    @Override
    public String getMergedDriver(DeviceId deviceId, PiPipeconfId pipeconfId) {
        return null;
    }

    @Override
    public Optional<PiPipeconfId> ofDevice(DeviceId deviceId) {
        return Optional.empty();
    }

    @Override
    public void addListener(PiPipeconfListener listener) {

    }

    @Override
    public void removeListener(PiPipeconfListener listener) {

    }
}
