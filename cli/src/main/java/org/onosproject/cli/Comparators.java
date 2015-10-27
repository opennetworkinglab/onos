/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.cli;

import org.onosproject.cluster.ControllerNode;
import org.onosproject.core.Application;
import org.onosproject.core.ApplicationId;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Element;
import org.onosproject.net.ElementId;
import org.onosproject.net.Port;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.group.Group;

import org.onosproject.net.statistic.TypedFlowEntryWithLoad;
import org.onosproject.net.topology.TopologyCluster;

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

    public static final Comparator<Application> APP_COMPARATOR = new Comparator<Application>() {
        @Override
        public int compare(Application app1, Application app2) {
            return app1.id().id() - app2.id().id();
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
            int tableCompare = Integer.valueOf(f1.tableId()).compareTo(f2.tableId());
            return (tableCompare == 0)
                    ? Long.valueOf(f1.id().value()).compareTo(f2.id().value())
                    : tableCompare;
        }
    };

    public static final Comparator<Group> GROUP_COMPARATOR = new Comparator<Group>() {
        @Override
        public int compare(Group g1, Group g2) {
            return Long.valueOf(g1.id().id()).compareTo(Long.valueOf(g2.id().id()));
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

    public static final Comparator<ConnectPoint> CONNECT_POINT_COMPARATOR = new Comparator<ConnectPoint>() {
        @Override
        public int compare(ConnectPoint o1, ConnectPoint o2) {
            int compareId = ELEMENT_ID_COMPARATOR.compare(o1.elementId(), o2.elementId());
            return (compareId != 0) ?
                compareId :
                Long.signum(o1.port().toLong() - o2.port().toLong());
        }
    };

    public static final Comparator<Interface> INTERFACES_COMPARATOR = (intf1, intf2) ->
            CONNECT_POINT_COMPARATOR.compare(intf1.connectPoint(), intf2.connectPoint());

    public static final Comparator<TypedFlowEntryWithLoad> TYPEFLOWENTRY_WITHLOAD_COMPARATOR =
            new Comparator<TypedFlowEntryWithLoad>() {
                @Override
                public int compare(TypedFlowEntryWithLoad fe1, TypedFlowEntryWithLoad fe2) {
                    long delta = fe1.load().rate() -  fe2.load().rate();
                    return delta == 0 ? 0 : (delta > 0 ? -1 : +1);
                }
            };
}
