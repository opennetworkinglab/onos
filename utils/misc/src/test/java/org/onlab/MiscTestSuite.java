/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onlab;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.onlab.graph.AbstractEdgeTest;
import org.onlab.graph.AdjacencyListsGraphTest;
import org.onlab.graph.BellmanFordGraphSearchTest;
import org.onlab.graph.BreadthFirstSearchTest;
import org.onlab.graph.DefaultMutablePathTest;
import org.onlab.graph.DefaultPathTest;
import org.onlab.graph.DepthFirstSearchTest;
import org.onlab.graph.DijkstraGraphSearchTest;
import org.onlab.graph.DisjointPathPairTest;
import org.onlab.graph.HeapTest;
import org.onlab.graph.KShortestPathsSearchTest;
import org.onlab.graph.LazyKShortestPathsSearchTest;
import org.onlab.graph.SrlgGraphSearchTest;
import org.onlab.graph.SuurballeGraphSearchTest;
import org.onlab.graph.TarjanGraphSearchTest;
import org.onlab.util.ImmutableByteSequenceTest;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        AbstractEdgeTest.class,
        AdjacencyListsGraphTest.class,
        BellmanFordGraphSearchTest.class,
        BreadthFirstSearchTest.class,
        DefaultMutablePathTest.class,
        DefaultPathTest.class,
        DepthFirstSearchTest.class,
        DijkstraGraphSearchTest.class,
        DisjointPathPairTest.class,
        HeapTest.class,
        KShortestPathsSearchTest.class,
        LazyKShortestPathsSearchTest.class,
        SrlgGraphSearchTest.class,
        SuurballeGraphSearchTest.class,
        TarjanGraphSearchTest.class,
        ImmutableByteSequenceTest.class,
})

public class MiscTestSuite {

}
