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

package org.onosproject.drivers.bmv2;

import org.onosproject.drivers.bmv2.api.Bmv2PreController;
import org.onosproject.drivers.p4runtime.P4RuntimeHandshaker;
import org.onosproject.net.DeviceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class Bmv2DeviceHandshaker extends P4RuntimeHandshaker {

    public static final String THRIFT_SERVER_ADDRESS_KEY = "bmv2-thrift_ip";
    public static final String THRIFT_SERVER_PORT_KEY = "bmv2-thrift_port";
    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public CompletableFuture<Boolean> connect() {
        //connect to both GRPC and Thrift servers in parallel
        CompletableFuture<Boolean> futureP4Runtime = super.connect();
        CompletableFuture<Boolean> futureBmv2Pre = connectToBmv2Pre();
        //combine futures and asses the overall result by using P4Runtime connection status
        return futureP4Runtime.thenCombine(futureBmv2Pre,
                                           (p4RuntimeConnected, bmv2PreConnected) -> p4RuntimeConnected);
    }

    @Override
    public CompletableFuture<Boolean> disconnect() {
        //disconnect from both GRPC and Thrift servers in parallel
        CompletableFuture<Boolean> futureP4Runtime = super.disconnect();
        CompletableFuture<Boolean> futureBmv2Pre = disconnectFromBmv2Pre();
        //combine futures and asses the overall result by using P4Runtime disconnection status
        return futureP4Runtime.thenCombine(futureBmv2Pre,
                                           (p4RuntimeDisconnected, bmv2PreDisconnected) -> p4RuntimeDisconnected);
    }

    private CompletableFuture<Boolean> connectToBmv2Pre() {
        return CompletableFuture.supplyAsync(this::createPreClient);
    }

    private CompletableFuture<Boolean> disconnectFromBmv2Pre() {
        return CompletableFuture.supplyAsync(() -> {
            removePreClient();
            return true;
        });
    }

    /**
     * Creates a BMv2 PRE client for this device.
     *
     * @return true if successful; false otherwise
     */
    private boolean createPreClient() {
        DeviceId deviceId = handler().data().deviceId();

        String serverAddress = data().value(THRIFT_SERVER_ADDRESS_KEY);
        String serverPortString = data().value(THRIFT_SERVER_PORT_KEY);

        if (serverAddress == null || serverPortString == null) {
            log.warn("Unable to create PRE client for {}, missing driver data key (required is {}, and {})",
                     deviceId, THRIFT_SERVER_ADDRESS_KEY, THRIFT_SERVER_PORT_KEY);
            return false;
        }
        Bmv2PreController bmv2PreController = handler().get(Bmv2PreController.class);
        try {
            return bmv2PreController.createPreClient(deviceId,
                                                     serverAddress,
                                                     Integer.parseUnsignedInt(serverPortString));
        } catch (RuntimeException e) {
            log.warn("Unable to create BMv2 PRE client for {} due to {}", deviceId, e.toString());
            return false;
        }
    }

    private void removePreClient() {
        DeviceId deviceId = handler().data().deviceId();
        handler().get(Bmv2PreController.class).removePreClient(deviceId);
    }
}
