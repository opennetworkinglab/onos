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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiMessageHandlerTwo;
import org.onosproject.ui.table.AbstractTableRow;
import org.onosproject.ui.table.RowComparator;
import org.onosproject.ui.table.TableRow;
import org.onosproject.ui.table.TableUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Message handler for cluster view related messages.
 */
public class ClusterViewMessageHandler extends UiMessageHandlerTwo {

    private static final String CLUSTER_DATA_REQ = "clusterDataRequest";

    @Override
    protected Collection<RequestHandler> getHandlers() {
        return ImmutableSet.of(new ClusterDataRequest());
    }

    // ======================================================================

    private final class ClusterDataRequest extends RequestHandler {

        private ClusterDataRequest() {
            super(CLUSTER_DATA_REQ);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            RowComparator rc = TableUtils.createRowComparator(payload);

            ClusterService service = get(ClusterService.class);
            TableRow[] rows = generateTableRows(service);
            Arrays.sort(rows, rc);
            ObjectNode rootNode = MAPPER.createObjectNode();
            rootNode.set("clusters", TableUtils.generateArrayNode(rows));

            sendMessage("clusterDataResponse", 0, rootNode);
        }

        private TableRow[] generateTableRows(ClusterService service) {
            List<TableRow> list = service.getNodes().stream()
                    .map(node -> new ControllerNodeTableRow(service, node))
                    .collect(Collectors.toList());
            return list.toArray(new TableRow[list.size()]);
        }
    }

    // ======================================================================

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
