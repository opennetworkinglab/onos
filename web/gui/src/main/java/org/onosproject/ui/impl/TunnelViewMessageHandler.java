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
package org.onosproject.ui.impl;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import org.onosproject.incubator.net.tunnel.Tunnel;
import org.onosproject.incubator.net.tunnel.TunnelEndPointFormatter;
import org.onosproject.incubator.net.tunnel.TunnelService;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.table.TableModel;
import org.onosproject.ui.table.TableRequestHandler;
import org.onosproject.ui.table.cell.EnumFormatter;

import java.util.Collection;

public class TunnelViewMessageHandler extends UiMessageHandler {
    private static final String TUNNEL_DATA_REQ = "tunnelDataRequest";
    private static final String TUNNEL_DATA_RESP = "tunnelDataResponse";
    private static final String TUNNELS = "tunnels";
    private static final String ID = "id";
    private static final String NAME = "name";
    private static final String ONE = "one";
    private static final String TWO = "two";
    private static final String TYPE = "type";
    private static final String GROUP_ID = "group_id";

    private static final String BANDWIDTH = "bandwidth";
    private static final String PATH = "path";


    private static final String[] COL_IDS = {
            ID, NAME, ONE, TWO, TYPE, GROUP_ID,
            BANDWIDTH, PATH
    };

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(new TunnelDataRequestHandler());
    }

    private final class TunnelDataRequestHandler extends TableRequestHandler {

        private static final String NO_ROWS_MESSAGE = "No tunnels found";

        public TunnelDataRequestHandler() {
            super(TUNNEL_DATA_REQ, TUNNEL_DATA_RESP, TUNNELS);
        }

        @Override
        protected String[] getColumnIds() {
            return COL_IDS;
        }

        @Override
        protected String noRowsMessage(ObjectNode payload) {
            return NO_ROWS_MESSAGE;
        }

        @Override
        protected TableModel createTableModel() {
            TableModel tm = super.createTableModel();
            //TODO add more formater class so that we can get a more readable table
            tm.setFormatter(ONE, TunnelEndPointFormatter.INSTANCE);
            tm.setFormatter(TWO, TunnelEndPointFormatter.INSTANCE);
            tm.setFormatter(TYPE, EnumFormatter.INSTANCE);
            return tm;
        }

        @Override
        protected void populateTable(TableModel tm, ObjectNode payload) {
            TunnelService ts = get(TunnelService.class);
            ts.queryAllTunnels().forEach(tunnel -> populateRow(tm.addRow(), tunnel));
        }

    }

    private void populateRow(TableModel.Row row, Tunnel tunnel) {
        row.cell(ID, tunnel.tunnelId().id())
                .cell(NAME, tunnel.tunnelName().value())
                .cell(ONE, tunnel.src())
                .cell(TWO, tunnel.dst())
                .cell(TYPE, tunnel.type())
                .cell(GROUP_ID, tunnel.groupId().id())
                .cell(BANDWIDTH, tunnel.annotations().value(BANDWIDTH))
                .cell(PATH, tunnel.path());
    }
}
