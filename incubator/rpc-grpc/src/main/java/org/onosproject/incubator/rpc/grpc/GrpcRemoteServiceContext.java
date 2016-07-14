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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

import org.onosproject.incubator.rpc.RemoteServiceContext;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.link.LinkProviderRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

import io.grpc.ManagedChannel;

// gRPC Client side
// Probably there should be plug-in mechanism in the future.
/**
 * RemoteServiceContext based on gRPC.
 *
 * <p>
 * Currently it supports {@link DeviceProviderRegistry}.
 */
public class GrpcRemoteServiceContext implements RemoteServiceContext {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Map<Class<? extends Object>, Object> services = new ConcurrentHashMap<>();

    private final ManagedChannel channel;

    public GrpcRemoteServiceContext(ManagedChannel channel) {
        this.channel = checkNotNull(channel);
        services.put(DeviceProviderRegistry.class, new DeviceProviderRegistryClientProxy(channel));
        services.put(LinkProviderRegistry.class, new LinkProviderRegistryClientProxy(channel));
    }


    @Override
    public <T> T get(Class<T> serviceClass) {
        @SuppressWarnings("unchecked")
        T service = (T) services.get(serviceClass);
        if (service != null) {
            return service;
        }
        log.error("{} not supported", serviceClass);
        throw new NoSuchElementException(serviceClass.getTypeName() + " not supported");
    }


    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("services", services.keySet())
                .add("channel", channel.authority())
                .toString();
    }

}
