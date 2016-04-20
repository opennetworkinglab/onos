/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.rest.resources;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.jetty.JettyTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Base class for REST API tests.
 * Performs common configuration operations.
 */
public class ResourceTest extends JerseyTest {
    private static final int DEFAULT_PORT = 9998;

    /**
     * Creates a new web-resource test.
     */
    public ResourceTest() {
        super(ResourceConfig.forApplicationClass(CoreWebApplication.class));
        this.set("jersey.config.test.container.port", getRandomPort(DEFAULT_PORT));
    }

    /**
     * Creates a new web-resource test.
     */
    public ResourceTest(ResourceConfig config) {
        super(config);
        this.set("jersey.config.test.container.port", getRandomPort(DEFAULT_PORT));
    }

    /**
     * Returns an unused port number to make sure that each unit test runs in
     * different port number.
     *
     * @param defaultPort default port number
     * @return a randomized unique port number
     */
    private int getRandomPort(int defaultPort) {
        try {
            ServerSocket socket = new ServerSocket(0);
            socket.setReuseAddress(true);
            int port = socket.getLocalPort();
            socket.close();
            return port;
        } catch (IOException ioe) {
            return defaultPort;
        }
    }

    /**
     * Configures the jetty test container as default test container.
     *
     * @return test container factory
     * @throws TestContainerException
     */
    @Override
    protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
        return new JettyTestContainerFactory();
    }
}
