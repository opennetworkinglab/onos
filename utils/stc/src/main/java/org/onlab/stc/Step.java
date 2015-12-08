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

import com.google.common.base.MoreObjects;
import org.onlab.graph.Vertex;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Representation of a test step.
 */
public class Step implements Vertex {

    protected final String name;
    protected final String command;
    protected final String env;
    protected final String cwd;
    protected final Group group;
    protected final int delay;

    /**
     * Creates a new test step.
     *
     * @param name    step name
     * @param command step command to execute
     * @param env     path to file to be sourced into the environment
     * @param cwd     path to current working directory for the step
     * @param group   optional group to which this step belongs
     * @param delay   seconds to delay before executing
     */
    public Step(String name, String command, String env, String cwd, Group group, int delay) {
        this.name = checkNotNull(name, "Name cannot be null");
        this.group = group;
        this.delay = delay;

        // Set the command, environment and cwd
        // If one is not given use the value from the enclosing group
        this.command = command != null ? command : group != null && group.command != null ? group.command : null;
        this.env = env != null ? env : group != null && group.env != null ? group.env : null;
        this.cwd = cwd != null ? cwd : group != null && group.cwd != null ? group.cwd : null;
    }

    /**
     * Returns the step name.
     *
     * @return step name
     */
    public String name() {
        return name;
    }

    /**
     * Returns the step command string.
     *
     * @return command string
     */
    public String command() {
        return command;
    }

    /**
     * Returns the step environment script path.
     *
     * @return env script path
     */
    public String env() {
        return env;
    }

    /**
     * Returns the step current working directory path.
     *
     * @return current working dir path
     */
    public String cwd() {
        return cwd;
    }

    /**
     * Returns the enclosing group; null if none.
     *
     * @return enclosing group or null
     */
    public Group group() {
        return group;
    }

    /**
     * Returns the start delay in seconds.
     *
     * @return number of seconds
     */
    public int delay() {
        return delay;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Step) {
            final Step other = (Step) obj;
            return Objects.equals(this.name, other.name);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("command", command)
                .add("env", env)
                .add("cwd", cwd)
                .add("group", group)
                .add("delay", delay)
                .toString();
    }
}
