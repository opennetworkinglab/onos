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

import org.onlab.graph.MutableAdjacencyListsGraph;

import java.util.Set;

/**
 * Graph representation of a test process flow.
 */
public class ProcessFlow extends MutableAdjacencyListsGraph<Step, Dependency> {

    /**
     * Creates a graph comprising of the specified vertexes and edges.
     *
     * @param vertexes set of graph vertexes
     * @param edges    set of graph edges
     */
    public ProcessFlow(Set<Step> vertexes, Set<Dependency> edges) {
        super(vertexes, edges);
    }

}
