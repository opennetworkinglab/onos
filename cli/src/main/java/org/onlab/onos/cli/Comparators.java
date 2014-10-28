/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.onlab.onos.cli;

import org.onlab.onos.core.ApplicationId;
import org.onlab.onos.cluster.ControllerNode;
import org.onlab.onos.net.Element;
import org.onlab.onos.net.ElementId;
import org.onlab.onos.net.Port;
import org.onlab.onos.net.flow.FlowRule;
import org.onlab.onos.net.topology.TopologyCluster;

import java.util.Comparator;

/**
 * Various comparators.
 */
public final class Comparators {

    // Ban construction
    private Comparators() {
    }

    public static final Comparator<ApplicationId> APP_ID_COMPARATOR = new Comparator<ApplicationId>() {
        @Override
        public int compare(ApplicationId id1, ApplicationId id2) {
            return id1.id() - id2.id();
        }
    };

    public static final Comparator<ElementId> ELEMENT_ID_COMPARATOR = new Comparator<ElementId>() {
        @Override
        public int compare(ElementId id1, ElementId id2) {
            return id1.toString().compareTo(id2.toString());
        }
    };

    public static final Comparator<Element> ELEMENT_COMPARATOR = new Comparator<Element>() {
        @Override
        public int compare(Element e1, Element e2) {
            return e1.id().toString().compareTo(e2.id().toString());
        }
    };

    public static final Comparator<FlowRule> FLOW_RULE_COMPARATOR = new Comparator<FlowRule>() {
        @Override
        public int compare(FlowRule f1, FlowRule f2) {
            return Long.valueOf(f1.id().value()).compareTo(f2.id().value());
        }
    };

    public static final Comparator<Port> PORT_COMPARATOR = new Comparator<Port>() {
        @Override
        public int compare(Port p1, Port p2) {
            long delta = p1.number().toLong() - p2.number().toLong();
            return delta == 0 ? 0 : (delta < 0 ? -1 : +1);
        }
    };

    public static final Comparator<TopologyCluster> CLUSTER_COMPARATOR = new Comparator<TopologyCluster>() {
        @Override
        public int compare(TopologyCluster c1, TopologyCluster c2) {
            return c1.id().index() - c2.id().index();
        }
    };

    public static final Comparator<ControllerNode> NODE_COMPARATOR = new Comparator<ControllerNode>() {
        @Override
        public int compare(ControllerNode ci1, ControllerNode ci2) {
            return ci1.id().toString().compareTo(ci2.id().toString());
        }
    };

}
