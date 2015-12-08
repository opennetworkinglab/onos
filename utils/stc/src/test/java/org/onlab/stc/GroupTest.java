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
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * Test of the test scenario entity.
 */
public class GroupTest extends StepTest {

    @Test
    public void basics() {
        Group group = new Group(NAME, CMD, ENV, CWD, parent, 1);
        assertEquals("incorrect name", NAME, group.name());
        assertEquals("incorrect command", CMD, group.command());
        assertEquals("incorrect env", ENV, group.env());
        assertEquals("incorrect cwd", CWD, group.cwd());
        assertSame("incorrect group", parent, group.group());
        assertEquals("incorrect delay", 1, group.delay());

        Step step = new Step("step", null, null, null, group, 0);
        group.addChild(step);
        assertSame("incorrect child", step, group.children().iterator().next());
    }

    @Test
    public void equality() {
        Group g1 = new Group(NAME, CMD, null, null, parent, 0);
        Group g2 = new Group(NAME, CMD, ENV, CWD, null, 0);
        Group g3 = new Group("foo", null, null, null, parent, 0);
        new EqualsTester()
                .addEqualityGroup(g1, g2)
                .addEqualityGroup(g3)
                .testEquals();
    }

}