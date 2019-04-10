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
package org.onosproject.gnoi.ctl;

import io.grpc.ManagedChannel;
import org.onosproject.event.AbstractEvent;
import org.onosproject.event.EventListener;
import org.onosproject.gnoi.api.GnoiClient;
import org.onosproject.gnoi.api.GnoiController;
import org.onosproject.grpc.ctl.AbstractGrpcClientController;
import org.onosproject.net.DeviceId;
import org.osgi.service.component.annotations.Component;

/**
 * Implementation of gNOI controller.
 */
@Component(immediate = true, service = GnoiController.class)
public class GnoiControllerImpl
        extends AbstractGrpcClientController<GnoiClient, AbstractEvent, EventListener<AbstractEvent>>
        implements GnoiController {

    public GnoiControllerImpl() {
        super(AbstractEvent.class, "gNOI");
    }

    @Override
    protected GnoiClient createClientInstance(DeviceId deviceId, ManagedChannel channel) {
        return new GnoiClientImpl(deviceId, channel, this);
    }
}
