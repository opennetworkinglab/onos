/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.utils;

import org.onosproject.cluster.ControllerNode;
import org.onosproject.core.Application;
import org.onosproject.core.ApplicationId;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.incubator.net.virtual.TenantId;
import org.onosproject.incubator.net.virtual.VirtualDevice;
import org.onosproject.incubator.net.virtual.VirtualNetwork;
import org.onosproject.incubator.net.virtual.VirtualPort;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Element;
import org.onosproject.net.ElementId;
import org.onosproject.net.Port;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.group.Group;
import org.onosproject.net.key.DeviceKey;
import org.onosproject.net.region.Region;
import org.onosproject.net.statistic.TypedFlowEntryWithLoad;
import org.onosproject.net.topology.TopologyCluster;
import org.onosproject.ui.model.topo.UiTopoLayout;

import java.util.Comparator;

/**
 * Various comparators.
 */
public final class Comparators {

    // Ban construction
    private Comparators() {
    }

    public static final Comparator<ApplicationId> APP_ID_COMPARATOR =
            (id1, id2) -> id1.id() - id2.id();

    public static final Comparator<Application> APP_COMPARATOR =
            (app1, app2) -> app1.id().id() - app2.id().id();

    public static final Comparator<ElementId> ELEMENT_ID_COMPARATOR =
            (id1, id2) -> id1.toString().compareTo(id2.toString());

    public static final Comparator<Element> ELEMENT_COMPARATOR =
            (e1, e2) -> e1.id().toString().compareTo(e2.id().toString());

    public static final Comparator<FlowRule> FLOW_RULE_COMPARATOR =
            (f1, f2) -> {
                // Compare table IDs in ascending order
                int tableCompare = f1.tableId() - f2.tableId();
                if (tableCompare != 0) {
                    return tableCompare;
                }
                // Compare priorities in descending order
                int priorityCompare = f2.priority() - f1.priority();
                return (priorityCompare == 0)
                        ? Long.valueOf(f1.id().value()).compareTo(f2.id().value())
                        : priorityCompare;
            };

    public static final Comparator<Group> GROUP_COMPARATOR =
            (g1, g2) -> Long.valueOf(g1.id().id()).compareTo((long) g2.id().id());

    public static final Comparator<Port> PORT_COMPARATOR =
            (p1, p2) -> {
                long delta = p1.number().toLong() - p2.number().toLong();
                return delta == 0 ? 0 : (delta < 0 ? -1 : +1);
            };

    public static final Comparator<TopologyCluster> CLUSTER_COMPARATOR =
            (c1, c2) -> c1.id().index() - c2.id().index();

    public static final Comparator<ControllerNode> NODE_COMPARATOR =
            (ci1, ci2) -> ci1.id().toString().compareTo(ci2.id().toString());

    public static final Comparator<ConnectPoint> CONNECT_POINT_COMPARATOR =
            (o1, o2) -> {
                int compareId = ELEMENT_ID_COMPARATOR.compare(o1.elementId(), o2.elementId());
                return (compareId != 0) ?
                        compareId :
                        Long.signum(o1.port().toLong() - o2.port().toLong());
            };

    public static final Comparator<Interface> INTERFACES_COMPARATOR =
            (intf1, intf2) ->
                    CONNECT_POINT_COMPARATOR.compare(intf1.connectPoint(), intf2.connectPoint());

    public static final Comparator<TypedFlowEntryWithLoad> TYPEFLOWENTRY_WITHLOAD_COMPARATOR =
            (fe1, fe2) -> {
                long delta = fe1.load().rate() - fe2.load().rate();
                return delta == 0 ? 0 : (delta > 0 ? -1 : +1);
            };

    public static final Comparator<DeviceKey> DEVICE_KEY_COMPARATOR =
            (dk1, dk2) -> dk1.deviceKeyId().id().compareTo(dk2.deviceKeyId().id());

    public static final Comparator<Region> REGION_COMPARATOR =
            (r1, r2) -> r1.id().toString().compareTo(r2.id().toString());

    public static final Comparator<UiTopoLayout> LAYOUT_COMPARATOR =
            (l1, l2) -> l1.id().toString().compareTo(l2.id().toString());

    public static final Comparator<TenantId> TENANT_ID_COMPARATOR =
            (t1, t2) -> t1.id().compareTo(t2.id());

    public static final Comparator<VirtualNetwork> VIRTUAL_NETWORK_COMPARATOR =
            (v1, v2) -> v1.tenantId().toString().compareTo(v2.tenantId().toString());

    public static final Comparator<VirtualDevice> VIRTUAL_DEVICE_COMPARATOR =
            (v1, v2) -> v1.id().toString().compareTo(v2.id().toString());

    public static final Comparator<VirtualPort> VIRTUAL_PORT_COMPARATOR =
            (v1, v2) -> v1.number().toString().compareTo(v2.number().toString());
}
