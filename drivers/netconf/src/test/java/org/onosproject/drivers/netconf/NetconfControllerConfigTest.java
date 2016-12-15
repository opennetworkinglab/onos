/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.drivers.netconf;

import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.net.behaviour.ControllerConfig;

public class NetconfControllerConfigTest {

    private ControllerConfig netconfCtlConfig;

    @Before
    public void setUp() throws Exception {
        netconfCtlConfig = new NetconfControllerConfig();
        netconfCtlConfig.setHandler(new MockDriverHandler());
//        netconfCtlConfig.setControllers(Arrays.asList(new ControllerInfo[]{}));
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testGetControllers() {
        assertNotNull(netconfCtlConfig.getControllers());

    }

}
