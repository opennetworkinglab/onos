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

import org.junit.Test;

import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Unit tests for bad REST requests.
 */
public class BadRequestTest extends ResourceTest {

    /**
     * Tests the response for an invalid URL.
     */
    @Test
    public void badUrl() {
        WebResource rs = resource();
        try {
            rs.path("ThisIsABadURL").get(String.class);
            fail("Fetch of non-existent URL did not throw an exception");
        } catch (UniformInterfaceException ex) {
            assertThat(ex.getMessage(),
                    containsString("returned a response status of 404 Not Found"));
        }
    }

    /**
     * Tests the response for a request with a bad method.
     */
    @Test
    public void badMethod() {
        WebResource rs = resource();
        try {
            rs.path("hosts").delete();
            fail("Fetch of non-existent URL did not throw an exception");
        } catch (UniformInterfaceException ex) {
            assertThat(ex.getMessage(),
                    containsString("returned a response status of 405 Method Not Allowed"));
        }
    }
}
