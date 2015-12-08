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

import static org.junit.Assert.*;

/**
 * Test of the test step dependency.
 */
public class DependencyTest extends StepTest {

    protected Step step1, step2;

    @Before
    public void setUp() throws ConfigurationException {
        super.setUp();
        step1 = new Step("step1", CMD, null, null, null, 0);
        step2 = new Step("step2", CMD, null, null, null, 0);
    }

    @Test
    public void hard() {
        Dependency hard = new Dependency(step1, step2, false);
        assertSame("incorrect src", step1, hard.src());
        assertSame("incorrect dst", step2, hard.dst());
        assertFalse("incorrect isSoft", hard.isSoft());
    }

    @Test
    public void soft() {
        Dependency soft = new Dependency(step2, step1, true);
        assertSame("incorrect src", step2, soft.src());
        assertSame("incorrect dst", step1, soft.dst());
        assertTrue("incorrect isSoft", soft.isSoft());
    }

    @Test
    public void equality() {
        Dependency d1 = new Dependency(step1, step2, false);
        Dependency d2 = new Dependency(step1, step2, false);
        Dependency d3 = new Dependency(step1, step2, true);
        Dependency d4 = new Dependency(step2, step1, true);
        new EqualsTester()
                .addEqualityGroup(d1, d2)
                .addEqualityGroup(d3)
                .addEqualityGroup(d4)
                .testEquals();
    }

}