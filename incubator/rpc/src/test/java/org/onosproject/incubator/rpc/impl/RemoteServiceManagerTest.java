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

import static org.junit.Assert.*;

import java.net.URI;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.incubator.rpc.RemoteServiceContext;
import org.onosproject.incubator.rpc.RemoteServiceDirectory;
import org.onosproject.incubator.rpc.impl.LocalRemoteServiceProvider.SomeOtherService;

/**
 * Set of tests of the RemoteServiceManager component.
 */
public class RemoteServiceManagerTest {

    private static final URI LOCAL_URI = URI.create("local://whateverIgnored");

    private RemoteServiceManager rpcManager;
    private RemoteServiceDirectory rpcDirectory;

    private LocalRemoteServiceProvider rpcProvider;

    @Before
    public void setUp() {
        rpcManager = new RemoteServiceManager();
        rpcManager.activate();
        rpcDirectory = rpcManager;

        rpcProvider = new LocalRemoteServiceProvider();
        rpcProvider.rpcRegistry = rpcManager;
        rpcProvider.activate();

    }

    @After
    public void tearDown() {
        rpcProvider.deactivate();

        rpcManager.deactivate();
    }

    @Test
    public void basics() {
        RemoteServiceContext remoteServiceContext = rpcDirectory.get(LOCAL_URI);
        assertNotNull("Expecting valid RPC context", remoteServiceContext);

        SomeOtherService someService = remoteServiceContext.get(SomeOtherService.class);
        assertNotNull("Expecting reference to sample service", someService);

        assertEquals("Goodbye", someService.hello());
    }

}
