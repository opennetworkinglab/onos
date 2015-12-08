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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import java.util.Set;

/**
 * Represenation of a related group of steps.
 */
public class Group extends Step {

    private final Set<Step> children = Sets.newHashSet();

    /**
     * Creates a new test step.
     *
     * @param name    group name
     * @param command default command
     * @param env     default path to file to be sourced into the environment
     * @param cwd     default path to current working directory for the step
     * @param group   optional group to which this step belongs
     * @param delay   seconds to delay before executing
     */
    public Group(String name, String command, String env, String cwd, Group group, int delay) {
        super(name, command, env, cwd, group, delay);
    }

    /**
     * Returns the set of child steps and groups contained within this group.
     *
     * @return set of children
     */
    public Set<Step> children() {
        return ImmutableSet.copyOf(children);
    }

    /**
     * Adds the specified step or group as a child of this group.
     *
     * @param child child step or group to add
     */
    public void addChild(Step child) {
        children.add(child);
    }
}
