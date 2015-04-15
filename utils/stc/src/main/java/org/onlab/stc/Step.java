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
    protected final Group group;

    /**
     * Creates a new test step.
     *
     * @param name     step name
     * @param command  step command to execute
     * @param group    optional group to which this step belongs
     */
    public Step(String name, String command, Group group) {
        this.name = checkNotNull(name, "Name cannot be null");
        this.group = group;

        // Set the command; if one is not given default to the enclosing group
        this.command = command != null ? command :
                group != null && group.command != null ? group.command : null;
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
     * Returns the enclosing group; null if none.
     *
     * @return enclosing group or null
     */
    public Group group() {
        return group;
    }


    @Override
    public int hashCode() {
        return Objects.hash(name);
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
                .add("group", group)
                .toString();
    }
}
