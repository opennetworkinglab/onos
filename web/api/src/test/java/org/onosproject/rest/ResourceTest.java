/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.rest;

import java.io.IOException;
import java.net.ServerSocket;

import com.sun.jersey.spi.container.servlet.ServletContainer;
import com.sun.jersey.test.framework.AppDescriptor;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.WebAppDescriptor;

/**
 * Base class for REST API tests.  Performs common configuration operations.
 */
public class ResourceTest extends JerseyTest {

    /**
     * Creates a new web-resource test.
     */
    public ResourceTest() {
        super();
    }

    /**
     * Creates a new web-resource test initialized according to the specified
     * web application class.
     */
    protected ResourceTest(Class<?> webAppClass) {
        super(new WebAppDescriptor.Builder("javax.ws.rs.Application",
                                           webAppClass.getCanonicalName())
                      .servletClass(ServletContainer.class).build());
    }

    /**
     * Assigns an available port for the test.
     *
     * @param defaultPort If a port cannot be determined, this one is used.
     * @return free port
     */
    @Override
    public int getPort(int defaultPort) {
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

    @Override
    public AppDescriptor configure() {
        return new WebAppDescriptor.Builder("org.onosproject.rest").build();
    }

}
