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

import java.net.URI;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.incubator.rpc.RemoteServiceContext;
import org.onosproject.incubator.rpc.RemoteServiceDirectory;
import org.onosproject.incubator.rpc.RemoteServiceContextProvider;
import org.onosproject.incubator.rpc.RemoteServiceContextProviderService;
import org.onosproject.incubator.rpc.RemoteServiceProviderRegistry;
import org.onosproject.net.provider.AbstractProviderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.Beta;

/**
 * Provides RemoteService related APIs.
 */
@Beta
@Component(immediate = true)
@Service
public class RemoteServiceManager extends AbstractProviderRegistry
    implements RemoteServiceDirectory, RemoteServiceProviderRegistry {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Activate
    protected void activate() {
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
    }

    @Override
    public RemoteServiceContext get(URI uri) {
        RemoteServiceContextProvider factory = getProvider(uri.getScheme());
        if (factory != null) {
            return factory.get(uri);
        }
        throw new UnsupportedOperationException(uri.getScheme() + " not supported");
    }

    private final class InternalRemoteServiceContextProviderService
            extends AbstractProviderService<RemoteServiceContextProvider>
            implements RemoteServiceContextProviderService {

        public InternalRemoteServiceContextProviderService(RemoteServiceContextProvider provider) {
            super(provider);
        }
    }

    // make this abstract method if slicing out
    @Override
    protected RemoteServiceContextProviderService createProviderService(RemoteServiceContextProvider provider) {
        return new InternalRemoteServiceContextProviderService(provider);
    }

}
