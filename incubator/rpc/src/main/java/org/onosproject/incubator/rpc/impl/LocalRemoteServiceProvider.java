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
package org.onosproject.incubator.rpc.impl;

import static com.google.common.base.Preconditions.checkArgument;

import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.osgi.ServiceDirectory;
import org.onosproject.incubator.rpc.RemoteServiceContext;
import org.onosproject.incubator.rpc.RemoteServiceContextProvider;
import org.onosproject.incubator.rpc.RemoteServiceContextProviderService;
import org.onosproject.incubator.rpc.RemoteServiceProviderRegistry;
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.Beta;

/**
 * Sample implementation of RemoteServiceContextProvider.
 *
 * Scheme: "local", calling corresponding local service on request.
 * Only expected for testing until real RPC implementation is ready.
 *
 * Note: This is expected to be removed or separated out as separate bundle
 * once other RPC implementaion became available.
 */
@Beta
@Component(immediate = true)
public class LocalRemoteServiceProvider implements RemoteServiceContextProvider {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private RemoteServiceContext theOne = new LocalServiceContext();

    private static final ProviderId PID = new ProviderId("local", "org.onosproject.rpc.provider.local");

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected RemoteServiceProviderRegistry rpcRegistry;

    private final Map<Class<? extends Object>, Object> services = new ConcurrentHashMap<>();

    private RemoteServiceContextProviderService providerService;

    @Activate
    protected void activate() {

        services.put(SomeOtherService.class, new SomeOtherServiceImpl());

        providerService = rpcRegistry.register(this);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        rpcRegistry.unregister(this);
        log.info("Stopped");
    }

    @Override
    public ProviderId id() {
        return PID;
    }

    @Override
    public RemoteServiceContext get(URI uri) {
        checkArgument(Objects.equals(uri.getScheme(), "local"));
        return theOne;
    }

    private final class LocalServiceContext implements RemoteServiceContext {

        private final ServiceDirectory directory = new DefaultServiceDirectory();

        @Override
        public <T> T get(Class<T> serviceClass) {
            @SuppressWarnings("unchecked")
            T service = (T) services.get(serviceClass);
            if (service != null) {
                return service;
            }
            // look up OSGi services on this host.
            // provided to unblock development depending on RPC.
            return directory.get(serviceClass);
        }
    }

    // Service provided by RPC can be non-OSGi Service
    public static interface SomeOtherService {
        String hello();
    }

    public static class SomeOtherServiceImpl implements SomeOtherService {

        @Override
        public String hello() {
            return "Goodbye";
        }
    }

}
