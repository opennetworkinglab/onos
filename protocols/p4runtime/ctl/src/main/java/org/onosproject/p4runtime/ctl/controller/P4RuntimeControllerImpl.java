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

package org.onosproject.p4runtime.ctl.controller;

import io.grpc.ManagedChannel;
import org.onosproject.grpc.ctl.AbstractGrpcClientController;
import org.onosproject.net.DeviceId;
import org.onosproject.net.pi.service.PiPipeconfService;
import org.onosproject.p4runtime.api.P4RuntimeClient;
import org.onosproject.p4runtime.api.P4RuntimeController;
import org.onosproject.p4runtime.api.P4RuntimeEvent;
import org.onosproject.p4runtime.api.P4RuntimeEventListener;
import org.onosproject.p4runtime.ctl.client.P4RuntimeClientImpl;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

/**
 * P4Runtime controller implementation.
 */
@Component(immediate = true, service = P4RuntimeController.class)
public class P4RuntimeControllerImpl
        extends AbstractGrpcClientController
        <P4RuntimeClient, P4RuntimeEvent, P4RuntimeEventListener>
        implements P4RuntimeController {

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private PiPipeconfService pipeconfService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private MasterElectionIdStore masterElectionIdStore;

    public P4RuntimeControllerImpl() {
        super(P4RuntimeEvent.class, "P4Runtime");
    }

    @Override
    public void remove(DeviceId deviceId) {
        super.remove(deviceId);
        // Assuming that when a client is removed, it is done so by all nodes,
        // this is the best place to clear master election ID state.
        masterElectionIdStore.removeAll(deviceId);
    }

    @Override
    protected P4RuntimeClient createClientInstance(
            DeviceId deviceId, ManagedChannel channel) {
        return new P4RuntimeClientImpl(deviceId, channel, this,
                                       pipeconfService, masterElectionIdStore);
    }
}
