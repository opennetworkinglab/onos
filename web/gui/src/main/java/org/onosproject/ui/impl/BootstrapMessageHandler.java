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
package org.onosproject.ui.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import org.onlab.osgi.ServiceDirectory;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.ui.UiConnection;
import org.onosproject.ui.UiMessageHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


/**
 * Facility for creating and sending initial bootstrap message to the GUI.
 */
public class BootstrapMessageHandler extends UiMessageHandler {

    private static final String BOOTSTRAP = "bootstrap";

    private static final Comparator<? super ControllerNode> NODE_COMPARATOR =
            new Comparator<ControllerNode>() {
                @Override
                public int compare(ControllerNode o1, ControllerNode o2) {
                    return o1.id().toString().compareTo(o2.id().toString());
                }
            };

    private final ObjectMapper mapper = new ObjectMapper();

    private ClusterService clusterService;

    // TODO: ClusterEventListener - update GUI when instances come or go...

    /**
     * Creates a new bootstrap message handler for bootstrapping the GUI.
     */
    protected BootstrapMessageHandler() {
        super(ImmutableSet.of(BOOTSTRAP));
    }

    @Override
    public void init(UiConnection connection, ServiceDirectory directory) {
        super.init(connection, directory);
        clusterService = directory.get(ClusterService.class);

        // Obtain list of cluster nodes and send bootstrap message to GUI
        List<ControllerNode> nodes = new ArrayList<>(clusterService.getNodes());
        Collections.sort(nodes, NODE_COMPARATOR);

        ObjectNode payload = mapper.createObjectNode();

        ArrayNode anode = mapper.createArrayNode();
        for (ControllerNode node : nodes) {
            anode.add(clusterNodeData(node));
        }
        payload.set("instances", anode);
        connection.sendMessage(envelope(BOOTSTRAP, 0, payload));
    }

    private ObjectNode clusterNodeData(ControllerNode node) {
        return mapper.createObjectNode()
                .put("id", node.id().toString())
                .put("ip", node.ip().toString())
                .put("uiAttached", node.equals(clusterService.getLocalNode()));
    }


    @Override
    public void process(ObjectNode message) {
        // We registered for "bootstrap" events, but we don't expect
        // the GUI to send us any; it was just that we had to define a
        // non-empty set of events that we handle, in the constructor.
    }

}
