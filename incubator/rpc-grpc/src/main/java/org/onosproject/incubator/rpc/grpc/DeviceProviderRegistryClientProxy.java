/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.incubator.rpc.grpc;

import java.util.Map;

import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.provider.AbstractProviderRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

import io.grpc.Channel;
import io.grpc.ManagedChannel;

// gRPC Client side
/**
 * Proxy object to handle DeviceProviderRegistry calls.
 *
 * RPC wise, this will start/stop bidirectional streaming service sessions.
 */
final class DeviceProviderRegistryClientProxy
        extends AbstractProviderRegistry<DeviceProvider, DeviceProviderService>
        implements DeviceProviderRegistry {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Channel channel;

    private final Map<DeviceProvider, DeviceProviderServiceClientProxy> pServices;

    DeviceProviderRegistryClientProxy(ManagedChannel channel) {
        this.channel = channel;
        pServices = Maps.newIdentityHashMap();
    }

    @Override
    protected synchronized DeviceProviderService createProviderService(DeviceProvider provider) {

        // Create session
        DeviceProviderServiceClientProxy pService = new DeviceProviderServiceClientProxy(provider, channel);
        log.debug("Created DeviceProviderServiceClientProxy {}", pService);

        DeviceProviderServiceClientProxy old = pServices.put(provider, pService);
        if (old != null) {
            // sanity check, can go away
            log.warn("Duplicate registration detected for {}", provider.id());
        }
        return pService;
    }

    @Override
    public synchronized void unregister(DeviceProvider provider) {
        DeviceProviderServiceClientProxy pService = pServices.remove(provider);
        log.debug("Unregistering DeviceProviderServiceClientProxy {}", pService);
        super.unregister(provider);
        if (pService != null) {
            pService.shutdown();
        }
    }
}
