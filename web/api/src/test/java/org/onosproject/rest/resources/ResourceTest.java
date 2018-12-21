/*
 * Copyright 2016-present Open Networking Foundation
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
import org.glassfish.jersey.test.TestProperties;
import org.glassfish.jersey.test.jetty.JettyTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.onlab.junit.TestUtils;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.rest.AuthorizationFilter;
import org.onlab.rest.BaseResource;

/**
 * Base class for REST API tests.
 * Performs common configuration operations.
 */
public class ResourceTest extends JerseyTest {

    /**
     * Creates a new web-resource test.
     */
    public ResourceTest() {
        super(ResourceConfig.forApplicationClass(CoreWebApplication.class));
        configureProperties();
    }

    /**
     * Creates a new web-resource test.
     */
    public ResourceTest(ResourceConfig config) {
        super(config);
        configureProperties();
    }

    private void configureProperties() {
        set(TestProperties.CONTAINER_PORT, 0);
        AuthorizationFilter.disableForTests();
        AuditFilter.disableForTests();
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

    /**
     * Sets up the test services directory in the base resource environment.
     *
     * @param testDirectory new test directory
     */
    protected void setServiceDirectory(ServiceDirectory testDirectory) {
        TestUtils.setField(BaseResource.class, "services", testDirectory);
    }
}
