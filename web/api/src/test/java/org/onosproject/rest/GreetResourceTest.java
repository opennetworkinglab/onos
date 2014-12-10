/*
 * Copyright 2014 Open Networking Laboratory
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

import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.test.framework.JerseyTest;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Simple example on how to write a JAX-RS unit test using Jersey test framework.
 * A base class should/will be created to provide further assistance for testing.
 */
public class GreetResourceTest extends JerseyTest {

    public GreetResourceTest() {
        super("org.onosproject.rest");
    }

    @BeforeClass
    public static void classSetUp() {
//        ServiceDirectory testDirectory =
//                new TestServiceDirectory().add(GreetService.class, new GreetManager());
//        GreetResource.setServiceDirectory(testDirectory);
    }

    @Ignore
    @Test
    public void basics() {
        WebResource rs = resource();
        String response = rs.path("greet").get(String.class);
        assertTrue("incorrect response", response.contains("greeting"));
    }

}
