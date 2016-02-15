/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.fnl.base;

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.HostId;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.criteria.Criterion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Common utility functions and constants.
 */
public final class NetworkDiagnosticUtils {

    private NetworkDiagnosticUtils() {
        // no instantiation
    }

    /**
     * Returns a list of flow entries sorted by priority in descending order.
     *
     * @param flowEntries flow entries to be sorted
     * @return flow entries in descending order
     */
    public static List<FlowEntry> sortFlowTable(Iterable<FlowEntry> flowEntries) {

        List<FlowEntry> flows = new ArrayList<>();
        flowEntries.forEach(flows::add);

        Collections.sort(flows,
                (f1, f2) -> f2.priority() - f1.priority());
        return flows;
    }

    /**
     * Returns a list of match fields sorted by their types in ascending order.
     *
     * @param criterionSet the criteria to be sorted
     * @return the list of criteria in ascending order
     */
    public static List<Criterion> sortCriteria(Set<Criterion> criterionSet) {

        List<Criterion> array = new ArrayList<>(criterionSet);
        Collections.sort(array,
                (c1, c2) -> c1.type().compareTo(c2.type()));
        return array;
    }

    /**
     * Returns true if the given connect point is a device point.
     *
     * @param connectPoint the connect point to be checked
     * @return true if the connect point is a device point
     */
    public static boolean isDevice(ConnectPoint connectPoint) {
        return connectPoint.elementId() instanceof DeviceId;
    }

    /**
     * Returns true if the given connect point is a host point.
     *
     * @param connectPoint the connect point to be checked
     * @return true if the connect point is a host point
     */
    public static boolean isHost(ConnectPoint connectPoint) {
        // TODO - not debug yet
        return connectPoint.elementId() instanceof HostId;
    }
}
