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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.NodeId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Message handler for cluster view related messages.
 */
public class ClusterViewMessageHandler extends AbstractTabularViewMessageHandler {

    /**
     * Creates a new message handler for the cluster messages.
     */
    protected ClusterViewMessageHandler() {
        super(ImmutableSet.of("clusterDataRequest"));
    }

    @Override
    public void process(ObjectNode message) {
        ObjectNode payload = payload(message);
        String sortCol = string(payload, "sortCol", "id");
        String sortDir = string(payload, "sortDir", "asc");

        ClusterService service = get(ClusterService.class);
        TableRow[] rows = generateTableRows(service);
        RowComparator rc =
                new RowComparator(sortCol, RowComparator.direction(sortDir));
        Arrays.sort(rows, rc);
        ArrayNode clusterNodes = generateArrayNode(rows);
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.set("clusters", clusterNodes);

        connection().sendMessage("clusterDataResponse", 0, rootNode);
    }

    private TableRow[] generateTableRows(ClusterService service) {
        List<TableRow> list = new ArrayList<>();
        for (ControllerNode node : service.getNodes()) {
            list.add(new ControllerNodeTableRow(service, node));
        }
        return list.toArray(new TableRow[list.size()]);
    }

    /**
     * TableRow implementation for {@link ControllerNode controller nodes}.
     */
    private static class ControllerNodeTableRow extends AbstractTableRow {

        private static final String ID = "id";
        private static final String IP = "ip";
        private static final String TCP_PORT = "tcp";
        private static final String STATE_IID = "_iconid_state";
        private static final String UPDATED = "updated";

        private static final String[] COL_IDS = {
                ID, IP, TCP_PORT, STATE_IID, UPDATED
        };

        private static final String ICON_ID_ONLINE = "active";
        private static final String ICON_ID_OFFLINE = "inactive";

        public ControllerNodeTableRow(ClusterService service, ControllerNode n) {
            NodeId id = n.id();
            DateTime lastUpdated = service.getLastUpdated(id);
            org.joda.time.format.DateTimeFormatter format = DateTimeFormat.longTime();
            String iconId = (service.getState(id) == ControllerNode.State.ACTIVE) ?
                    ICON_ID_ONLINE : ICON_ID_OFFLINE;

            add(ID, id.toString());
            add(IP, n.ip().toString());
            add(TCP_PORT, Integer.toString(n.tcpPort()));
            add(STATE_IID, iconId);
            add(UPDATED, format.print(lastUpdated));
        }

        @Override
        protected String[] columnIds() {
            return COL_IDS;
        }
    }

}
