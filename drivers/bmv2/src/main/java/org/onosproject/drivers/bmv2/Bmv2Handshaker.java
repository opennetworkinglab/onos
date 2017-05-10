/*
 * Copyright 2017-present Open Networking Laboratory
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

import org.onosproject.net.MastershipRole;
import org.onosproject.net.device.DeviceHandshaker;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.driver.DriverData;
import org.onosproject.net.key.DeviceKeyId;
import org.onosproject.net.key.DeviceKeyService;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of the DeviceHandshaker for the bmv2 softswitch.
 */
//TODO consider abstract class with empty connect method and
//the implementation into a protected one for reusability.
//FIXME fill method bodies, used for testing.
public class Bmv2Handshaker extends AbstractHandlerBehaviour
        implements DeviceHandshaker {

    private final Logger log = getLogger(getClass());

    @Override
    public CompletableFuture<Boolean> connect() {
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        DeviceKeyService deviceKeyService = handler().get(DeviceKeyService.class);
        DriverData data = data();
        //Retrieving information from the driver data.
        log.info("protocol bmv2, ip {}, port {}, key {}", data.value("p4runtime_ip"),
                data.value("p4runtime_port"),
                deviceKeyService.getDeviceKey(DeviceKeyId.deviceKeyId(data.value("p4runtime_key")))
                        .asUsernamePassword().username());

        log.info("protocol gnmi, ip {}, port {}, key {}", data.value("gnmi_ip"), data.value("gnmi_port"),
                deviceKeyService.getDeviceKey(DeviceKeyId.deviceKeyId(data.value("gnmi_key")))
                        .asUsernamePassword().username());
        result.complete(true);
        return result;
    }

    @Override
    public CompletableFuture<Boolean> disconnect() {
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        result.complete(true);
        return result;
    }

    @Override
    public CompletableFuture<Boolean> isReachable() {
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        result.complete(true);
        return result;
    }

    @Override
    public CompletableFuture<MastershipRole> roleChanged(MastershipRole newRole) {
        CompletableFuture<MastershipRole> result = new CompletableFuture<>();
        result.complete(MastershipRole.MASTER);
        return result;
    }

}
