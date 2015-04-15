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
package org.onlab.stc;

import com.google.common.testing.EqualsTester;
import org.apache.commons.configuration.ConfigurationException;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * Test of the test step entity.
 */
public class StepTest {

    protected static final String NAME = "step";
    protected static final String CMD = "command";
    protected Group parent;

    @Before
    public void setUp() throws ConfigurationException {
        parent = new Group("parent", null, null);
    }

    @Test
    public void basics() {
        Step step = new Step(NAME, CMD, parent);
        assertEquals("incorrect name", NAME, step.name());
        assertEquals("incorrect command", CMD, step.command());
        assertSame("incorrect group", parent, step.group());
    }

    @Test
    public void equality() {
        Step s1 = new Step(NAME, CMD, parent);
        Step s2 = new Step(NAME, CMD, null);
        Step s3 = new Step("foo", null, parent);
        new EqualsTester()
                .addEqualityGroup(s1, s2)
                .addEqualityGroup(s3)
                .testEquals();
    }
}