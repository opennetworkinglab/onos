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
import org.onlab.graph.AbstractEdge;

import java.util.Objects;

/**
 * Representation of a dependency from one step on completion of another.
 */
public class Dependency extends AbstractEdge<Step> {

    private boolean isSoft;

    /**
     * Creates a new edge between the specified source and destination vertexes.
     *
     * @param src    source vertex
     * @param dst    destination vertex
     * @param isSoft indicates whether this is a hard or soft dependency
     */
    public Dependency(Step src, Step dst, boolean isSoft) {
        super(src, dst);
        this.isSoft = isSoft;
    }

    /**
     * Indicates whether this is a soft or hard dependency, i.e. one that
     * requires successful completion of the dependency or just any completion.
     *
     * @return true if dependency is a soft one
     */
    public boolean isSoft() {
        return isSoft;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(isSoft);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Dependency) {
            final Dependency other = (Dependency) obj;
            return super.equals(other) && Objects.equals(this.isSoft, other.isSoft);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", src().name())
                .add("requires", dst().name())
                .add("isSoft", isSoft)
                .toString();
    }
}
