/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.cli.net;

import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.edge.EdgePortService;
import org.onosproject.utils.Comparators;

import java.util.Collections;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.onosproject.net.DeviceId.deviceId;

/**
 * Lists all edge ports.
 */
@Service
@Command(scope = "onos", name = "edge-ports",
        description = "Lists all edge ports.")
public class EdgePortsListCommand extends AbstractShellCommand {

    private static final String FMT = "%s/%s";

    @Argument(index = 0, name = "uri", description = "Device ID",
            required = false, multiValued = false)
    @Completion(DeviceIdCompleter.class)
    String uri = null;

    @Override
    protected void doExecute() {
        EdgePortService service = get(EdgePortService.class);
        if (uri == null) {
            printEdgePoints(service.getEdgePoints());
        } else {
            printEdgePoints(service.getEdgePoints(deviceId(uri)));
        }
    }

    private void printEdgePoints(Iterable<ConnectPoint> edgePoints) {
        List<ConnectPoint> sorted = sort(edgePoints);
        if (outputJson()) {
            ArrayNode result = mapper().createObjectNode().putArray(null);
            sorted.forEach(e -> {
                result.add(mapper().createObjectNode()
                        .put(e.deviceId().toString(), e.port().toString()));
            });
            print("%s", result.toString());
        } else {
            sorted.forEach(e -> print(FMT, e.deviceId(), e.port()));
        }
    }

    private static List<ConnectPoint> sort(Iterable<ConnectPoint> connectPoints) {
        List<ConnectPoint> edgePoints = newArrayList(connectPoints);
        Collections.sort(edgePoints, Comparators.CONNECT_POINT_COMPARATOR);
        return edgePoints;
    }

}
