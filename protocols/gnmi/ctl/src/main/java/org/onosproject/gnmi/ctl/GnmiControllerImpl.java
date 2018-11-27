/*
 * Copyright 2018-present Open Networking Foundation
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
package protocols.gnmi.ctl.java.org.onosproject.gnmi.ctl;

import io.grpc.ManagedChannel;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.gnmi.api.GnmiEvent;
import org.onosproject.gnmi.api.GnmiEventListener;
import org.onosproject.grpc.ctl.AbstractGrpcClientController;
import org.onosproject.gnmi.api.GnmiClient;
import org.onosproject.gnmi.api.GnmiClientKey;
import org.onosproject.gnmi.api.GnmiController;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of gNMI controller.
 */
@Component(immediate = true)
@Service
public class GnmiControllerImpl
        extends AbstractGrpcClientController<GnmiClientKey, GnmiClient, GnmiEvent, GnmiEventListener>
        implements GnmiController {
    private final Logger log = getLogger(getClass());

    @Activate
    public void activate() {
        super.activate();
        eventDispatcher.addSink(GnmiEvent.class, listenerRegistry);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        super.deactivate();
        log.info("Stopped");
    }

    @Override
    protected GnmiClient createClientInstance(GnmiClientKey clientKey, ManagedChannel channel) {
        return new GnmiClientImpl(clientKey, channel, this);
    }

    /**
     * Handles event from gNMI client.
     *
     * @param event the gNMI event
     */
    void postEvent(GnmiEvent event) {
        post(event);
    }
}
