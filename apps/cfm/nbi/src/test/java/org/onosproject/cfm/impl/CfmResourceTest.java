/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.cfm.impl;

import org.glassfish.jersey.server.ResourceConfig;
import org.onosproject.cfm.rest.CfmWebApplication;
import org.onosproject.rest.resources.ResourceTest;

/**
 * Base class for CFM REST API tests.  Performs common configuration operations.
 */
public class CfmResourceTest extends ResourceTest {

    /**
     * Creates a new web-resource test.
     */
    public CfmResourceTest() {
        super(ResourceConfig.forApplicationClass(CfmWebApplication.class));
    }
}
