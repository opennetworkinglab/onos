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

import org.junit.Test;
import org.onlab.stc.MonitorLayout.Box;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.onlab.stc.CompilerTest.getStream;
import static org.onlab.stc.CompilerTest.stageTestResource;
import static org.onlab.stc.MonitorLayout.SLOT_WIDTH;
import static org.onlab.stc.Scenario.loadScenario;

/**
 * Tests of the monitor layout functionality.
 */
public class MonitorLayoutTest {

    private MonitorLayout layout;

    private Compiler getCompiler(String name) throws IOException {
        stageTestResource(name);
        Scenario scenario = loadScenario(getStream(name));
        Compiler compiler = new Compiler(scenario);
        compiler.compile();
        return compiler;
    }

    @Test
    public void basic() throws IOException {
        layout = new MonitorLayout(getCompiler("layout-basic.xml"));
        validate(layout, null, 0, 1, 5, 2);
        validate(layout, "a", 1, 1, 1, 1, 1, -SLOT_WIDTH / 2);
        validate(layout, "b", 2, 2, 1, 1, 0, 0);
        validate(layout, "f", 3, 3, 1);

        validate(layout, "g", 1, 1, 4, 1, 1, SLOT_WIDTH / 2);
        validate(layout, "c", 2, 1, 1);
        validate(layout, "d", 3, 2, 1);
        validate(layout, "e", 4, 3, 1);
    }

    @Test
    public void basicNest() throws IOException {
        layout = new MonitorLayout(getCompiler("layout-basic-nest.xml"));
        validate(layout, null, 0, 1, 6, 2);
        validate(layout, "a", 1, 1, 1, 1, 1, -SLOT_WIDTH / 2);
        validate(layout, "b", 2, 2, 1);
        validate(layout, "f", 3, 3, 1);

        validate(layout, "g", 1, 1, 5, 1);
        validate(layout, "c", 2, 1, 1);

        validate(layout, "gg", 3, 2, 3, 1);
        validate(layout, "d", 4, 1, 1);
        validate(layout, "e", 5, 2, 1);
    }

    @Test
    public void staggeredDependencies() throws IOException {
        layout = new MonitorLayout(getCompiler("layout-staggered-dependencies.xml"));
        validate(layout, null, 0, 1, 7, 4);
        validate(layout, "a", 1, 1, 1, 1, 1, -SLOT_WIDTH - SLOT_WIDTH / 2);
        validate(layout, "aa", 1, 1, 1, 1, 1, -SLOT_WIDTH / 2);
        validate(layout, "b", 2, 2, 1);
        validate(layout, "f", 3, 3, 1);

        validate(layout, "g", 1, 1, 5, 2, 1, +SLOT_WIDTH / 2);
        validate(layout, "c", 2, 1, 1);

        validate(layout, "gg", 3, 2, 3, 2);
        validate(layout, "d", 4, 1, 1);
        validate(layout, "dd", 4, 1, 1);
        validate(layout, "e", 5, 2, 1);

        validate(layout, "i", 6, 6, 1);
    }

    @Test
    public void deepNext() throws IOException {
        layout = new MonitorLayout(getCompiler("layout-deep-nest.xml"));
        validate(layout, null, 0, 1, 7, 6);
        validate(layout, "a", 1, 1, 1);
        validate(layout, "aa", 1, 1, 1);
        validate(layout, "b", 2, 2, 1);
        validate(layout, "f", 3, 3, 1);

        validate(layout, "g", 1, 1, 5, 2);
        validate(layout, "c", 2, 1, 1);

        validate(layout, "gg", 3, 2, 3, 2);
        validate(layout, "d", 4, 1, 1);
        validate(layout, "dd", 4, 1, 1);
        validate(layout, "e", 5, 2, 1);

        validate(layout, "i", 6, 6, 1);

        validate(layout, "g1", 1, 1, 6, 2);
        validate(layout, "g2", 2, 1, 5, 2);
        validate(layout, "g3", 3, 1, 4, 2);
        validate(layout, "u", 4, 1, 1);
        validate(layout, "v", 4, 1, 1);
        validate(layout, "w", 5, 2, 1);
        validate(layout, "z", 6, 3, 1);
    }


    private void validate(MonitorLayout layout, String name,
                          int absoluteTier, int tier, int depth, int breadth) {
        Box b = layout.get(name);
        assertEquals("incorrect absolute tier", absoluteTier, b.absoluteTier());
        assertEquals("incorrect tier", tier, b.tier());
        assertEquals("incorrect depth", depth, b.depth());
        assertEquals("incorrect breadth", breadth, b.breadth());
    }

    private void validate(MonitorLayout layout, String name,
                          int absoluteTier, int tier, int depth, int breadth,
                          int top, int center) {
        validate(layout, name, absoluteTier, tier, depth, breadth);
        Box b = layout.get(name);
        assertEquals("incorrect top", top, b.top());
        assertEquals("incorrect center", center, b.center());
    }

    private void validate(MonitorLayout layout, String name,
                          int absoluteTier, int tier, int depth) {
        validate(layout, name, absoluteTier, tier, depth, 1);
    }

}