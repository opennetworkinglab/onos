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

package org.onosproject.grpc.utils;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.onosproject.grpc.api.GrpcClient;
import org.onosproject.grpc.api.GrpcClientController;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.BasicDeviceConfig;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ConcurrentMap;

/**
 * Abstract implementation of HandlerBehaviour for gNMI-based devices.
 *
 * @param <CLIENT> gRPC client class
 * @param <CTRL>   gRPC controller class
 */
public class AbstractGrpcHandlerBehaviour
        <CLIENT extends GrpcClient, CTRL extends GrpcClientController<CLIENT>>
        extends AbstractHandlerBehaviour {

    static final ConcurrentMap<DeviceId, URI> CHANNEL_URIS = Maps.newConcurrentMap();

    protected final Logger log = LoggerFactory.getLogger(getClass());

    final Class<CTRL> controllerClass;
    protected DeviceId deviceId;
    protected DeviceService deviceService;
    protected CLIENT client;

    public AbstractGrpcHandlerBehaviour(Class<CTRL> controllerClass) {
        this.controllerClass = controllerClass;
    }

    protected boolean setupBehaviour(String opName) {
        deviceId = handler().data().deviceId();
        deviceService = handler().get(DeviceService.class);
        client = getClientByNetcfg();
        if (client == null) {
            log.warn("Missing client for {}, aborting {}", deviceId, opName);
            return false;
        }

        return true;
    }

    private CLIENT getClientByNetcfg() {
        // Check if there's a channel for this device and if we created it with
        // the same URI of that derived from the current netcfg. This makes sure
        // we return null if the netcfg changed after we created the channel.
        if (!CHANNEL_URIS.containsKey(data().deviceId()) ||
                !CHANNEL_URIS.get(data().deviceId()).equals(mgmtUriFromNetcfg())) {
            return null;
        }
        return handler().get(controllerClass).get(data().deviceId());
    }

    protected URI mgmtUriFromNetcfg() {
        deviceId = handler().data().deviceId();

        final BasicDeviceConfig cfg = handler().get(NetworkConfigService.class)
                .getConfig(deviceId, BasicDeviceConfig.class);
        if (cfg == null || Strings.isNullOrEmpty(cfg.managementAddress())) {
            log.error("Missing or invalid config for {}, cannot derive " +
                              "gRPC server endpoints", deviceId);
            return null;
        }

        try {
            return new URI(cfg.managementAddress());
        } catch (URISyntaxException e) {
            log.error("Management address of {} is not a valid URI: {}",
                      deviceId, cfg.managementAddress());
            return null;
        }
    }
}
