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
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.table.TableModel;
import org.onosproject.ui.table.TableRequestHandler;
import org.onosproject.ui.table.cell.TimeFormatter;

import java.util.Collection;


/**
 * Message handler for cluster view related messages.
 */
public class ClusterViewMessageHandler extends UiMessageHandler {

    private static final String CLUSTER_DATA_REQ = "clusterDataRequest";
    private static final String CLUSTER_DATA_RESP = "clusterDataResponse";
    private static final String CLUSTERS = "clusters";

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

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(new ClusterDataRequest());
    }

    // handler for cluster table requests
    private final class ClusterDataRequest extends TableRequestHandler {
        private ClusterDataRequest() {
            super(CLUSTER_DATA_REQ, CLUSTER_DATA_RESP, CLUSTERS);
        }

        @Override
        protected String[] getColumnIds() {
            return COL_IDS;
        }

        @Override
        protected TableModel createTableModel() {
            TableModel tm = super.createTableModel();
            tm.setFormatter(UPDATED, new TimeFormatter());
            return tm;
        }

        @Override
        protected void populateTable(TableModel tm, ObjectNode payload) {
            ClusterService cs = get(ClusterService.class);
            for (ControllerNode node : cs.getNodes()) {
                populateRow(tm.addRow(), node, cs);
            }
        }

        private void populateRow(TableModel.Row row, ControllerNode node,
                                 ClusterService cs) {
            NodeId id = node.id();
            DateTime lastUpdated = cs.getLastUpdated(id);
            String iconId = (cs.getState(id) == ControllerNode.State.ACTIVE) ?
                    ICON_ID_ONLINE : ICON_ID_OFFLINE;

            row.cell(ID, id)
                .cell(IP, node.ip())
                .cell(TCP_PORT, node.tcpPort())
                .cell(STATE_IID, iconId)
                .cell(UPDATED, lastUpdated);
        }
    }
}
