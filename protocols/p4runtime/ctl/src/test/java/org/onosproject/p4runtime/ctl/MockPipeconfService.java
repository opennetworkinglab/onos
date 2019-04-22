/*
 * Copyright 2019-present Open Networking Foundation
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

package org.onosproject.p4runtime.ctl;

import org.onosproject.net.DeviceId;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.model.PiPipeconfId;
import org.onosproject.net.pi.service.PiPipeconfListener;
import org.onosproject.net.pi.service.PiPipeconfService;

import java.util.Optional;

public class MockPipeconfService implements PiPipeconfService {
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
    public Optional<PiPipeconf> getPipeconf(PiPipeconfId id) {
        return Optional.empty();
    }

    @Override
    public Optional<PiPipeconf> getPipeconf(DeviceId deviceId) {
        return Optional.empty();
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
