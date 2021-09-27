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

package org.onosproject.drivers.gnmi;

import gnmi.Gnmi;
import org.onosproject.gnmi.api.GnmiClient;
import org.onosproject.gnmi.api.GnmiController;
import org.onosproject.grpc.utils.AbstractGrpcHandlerBehaviour;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.PortAdmin;

import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * Implementation of PortAdmin for gNMI devices with OpenConfig support.
 */
public class OpenConfigGnmiPortAdminBehaviour
        extends AbstractGrpcHandlerBehaviour<GnmiClient, GnmiController>
        implements PortAdmin {

    public OpenConfigGnmiPortAdminBehaviour() {
        super(GnmiController.class);
    }

    @Override
    public CompletableFuture<Boolean> enable(PortNumber number) {
        if (!setupBehaviour("enable()")) {
            return completedFuture(false);
        }
        doEnable(number, true);
        // Always returning true is OK assuming this is used only by the
        // GeneralDeviceProvider, which ignores the return value and instead
        // waits for a gNMI Update over the Subscribe RPC.
        return completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> disable(PortNumber number) {
        if (!setupBehaviour("disable()")) {
            return completedFuture(false);
        }
        doEnable(number, false);
        return completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> isEnabled(PortNumber number) {
        throw new UnsupportedOperationException("Not implemented");
    }

    private void doEnable(PortNumber portNumber, boolean enabled) {
        if (portNumber.isLogical()) {
            log.warn("Cannot update port status for logical port {} on {}",
                     portNumber, deviceId);
            return;
        }

        /* Requests coming from the north may come without name.
           When this happens port name equals to port number */
        if (!portNumber.hasName()) {
            if (deviceService == null) {
                log.warn("Cannot update port status of port {} on {} because the device " +
                                "service is null", portNumber, deviceId);
                return;
            }
            Port devicePort = deviceService.getPort(deviceId, portNumber);
            if (devicePort != null) {
                // Some devices may reject the config, make sure we act on the reported port
                portNumber = devicePort.number();
            }
        }

        final Gnmi.Path path = Gnmi.Path.newBuilder()
                .addElem(Gnmi.PathElem.newBuilder().setName("interfaces").build())
                .addElem(Gnmi.PathElem.newBuilder().setName("interface")
                                 .putKey("name", portNumber.name()).build())
                .addElem(Gnmi.PathElem.newBuilder().setName("config").build())
                .addElem(Gnmi.PathElem.newBuilder().setName("enabled").build())
                .build();
        final Gnmi.TypedValue value = Gnmi.TypedValue.newBuilder()
                .setBoolVal(enabled)
                .build();
        final Gnmi.SetRequest request = Gnmi.SetRequest.newBuilder()
                .addUpdate(Gnmi.Update.newBuilder()
                                   .setPath(path)
                                   .setVal(value)
                                   .build())
                .build();

        // Async submit request and forget about it. In case of errors, the
        // client will log them. In case of success, we should receive a gNMI
        // Update over the Subscribe RPC with the new oper status.
        client.set(request);
    }
}
