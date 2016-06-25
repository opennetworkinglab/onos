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
package org.onosproject.dhcp.impl;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.dhcp.DhcpService;
import org.onosproject.dhcp.IpAssignment;
import org.onosproject.net.HostId;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.table.TableModel;
import org.onosproject.ui.table.TableRequestHandler;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

/**
 * DHCPViewMessageHandler class implementation.
 */
public class DhcpViewMessageHandler extends UiMessageHandler {

    private static final String DHCP_DATA_REQ = "dhcpDataRequest";
    private static final String DHCP_DATA_RESP = "dhcpDataResponse";
    private static final String DHCP = "dhcps";

    private static final String HOST = "host";
    private static final String IP = "ip";
    private static final String LEASE = "lease";

    private static final String[] COL_IDS = {
            HOST, IP, LEASE
    };

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(
                new DataRequestHandler()
        );
    }

    // handler for dhcp table requests
    private final class DataRequestHandler extends TableRequestHandler {

        private static final String NO_ROWS_MESSAGE = "No mappings found";

        private DataRequestHandler() {
            super(DHCP_DATA_REQ, DHCP_DATA_RESP, DHCP);
        }

        @Override
        protected String defaultColumnId() {
            return HOST;
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
        protected void populateTable(TableModel tm, ObjectNode payload) {
            DhcpService dhcpService = AbstractShellCommand.get(DhcpService.class);
            Map<HostId, IpAssignment> allocationMap = dhcpService.listMapping();

            for (Map.Entry<HostId, IpAssignment> entry : allocationMap.entrySet()) {
                populateRow(tm.addRow(), entry);
            }
        }

        private void populateRow(TableModel.Row row, Map.Entry<HostId, IpAssignment> entry) {
            if (entry.getValue().leasePeriod() > 0) {
                Date now = new Date(entry.getValue().timestamp().getTime() + entry.getValue().leasePeriod());
                row.cell(HOST, entry.getKey())
                        .cell(IP, entry.getValue().ipAddress())
                        .cell(LEASE, now.toString());
            } else {
                row.cell(HOST, entry.getKey())
                        .cell(IP, entry.getValue().ipAddress())
                        .cell(LEASE, "Infinite Static Lease");
            }
        }
    }
}
