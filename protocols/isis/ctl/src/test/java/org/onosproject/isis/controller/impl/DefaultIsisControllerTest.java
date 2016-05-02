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
 * Unit test case for DefaultIsisController.
 */
public class DefaultIsisControllerTest {
    private DefaultIsisController defaultIsisController;

    @Before
    public void setUp() throws Exception {
        defaultIsisController = new DefaultIsisController();
    }

    @After
    public void tearDown() throws Exception {
        defaultIsisController = null;
    }

    /**
     * Tests activate() method.
     */
    @Test
    public void testActivate() throws Exception {
        defaultIsisController.activate();
        assertThat(defaultIsisController, is(notNullValue()));
    }

    /**
     * Tests deactivate() method.
     */
    @Test(expected = Exception.class)
    public void testDeactivate() throws Exception {
        defaultIsisController.activate();
        defaultIsisController.deactivate();
        assertThat(defaultIsisController, is(notNullValue()));
    }

    /**
     * Tests allConfiguredProcesses() method.
     */
    @Test
    public void testAllConfiguredProcesses() throws Exception {
        defaultIsisController.allConfiguredProcesses();
        assertThat(defaultIsisController, is(notNullValue()));
    }
}
