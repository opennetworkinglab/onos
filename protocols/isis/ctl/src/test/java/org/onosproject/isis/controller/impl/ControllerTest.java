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

package org.onosproject.isis.controller.impl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Unit test class for ControllerTest.
 */
public class ControllerTest {

    private Controller controller;

    @Before
    public void setUp() throws Exception {
        controller = new Controller();
    }

    @After
    public void tearDown() throws Exception {
        controller = null;
    }

    /**
     * Tests isisDeactivate() method.
     */
    @Test(expected = Exception.class)
    public void testIsisDeactivate() throws Exception {
        controller.isisDeactivate();
        assertThat(controller, is(notNullValue()));
    }

    /**
     * Tests getAllConfiguredProcesses() method.
     */
    @Test
    public void testGetAllConfiguredProcesses() throws Exception {
        controller.getAllConfiguredProcesses();
        assertThat(controller, is(notNullValue()));
    }
}