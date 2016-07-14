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
import org.onlab.packet.IpAddress;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.Host;
import org.onosproject.net.host.HostService;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.table.CellFormatter;
import org.onosproject.ui.table.TableModel;
import org.onosproject.ui.table.TableRequestHandler;
import org.onosproject.ui.table.cell.HostLocationFormatter;

import java.util.Collection;
import java.util.Set;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Message handler for host view related messages.
 */
public class HostViewMessageHandler extends UiMessageHandler {

    private static final String HOST_DATA_REQ = "hostDataRequest";
    private static final String HOST_DATA_RESP = "hostDataResponse";
    private static final String HOSTS = "hosts";

    private static final String TYPE_IID = "_iconid_type";
    private static final String ID = "id";
    private static final String MAC = "mac";
    private static final String VLAN = "vlan";
    private static final String IPS = "ips";
    private static final String LOCATION = "location";

    private static final String HOST_ICON_PREFIX = "hostIcon_";

    private static final String[] COL_IDS = {
            TYPE_IID, ID, MAC, VLAN, IPS, LOCATION
    };

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(new HostDataRequest());
    }

    // handler for host table requests
    private final class HostDataRequest extends TableRequestHandler {
        private static final String NO_ROWS_MESSAGE = "No hosts found";

        private HostDataRequest() {
            super(HOST_DATA_REQ, HOST_DATA_RESP, HOSTS);
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
            tm.setFormatter(LOCATION, HostLocationFormatter.INSTANCE);
            tm.setFormatter(IPS, new IpSetFormatter());
            return tm;
        }

        @Override
        protected void populateTable(TableModel tm, ObjectNode payload) {
            HostService hs = get(HostService.class);
            for (Host host : hs.getHosts()) {
                populateRow(tm.addRow(), host);
            }
        }

        private void populateRow(TableModel.Row row, Host host) {
            row.cell(TYPE_IID, getTypeIconId(host))
                    .cell(ID, host.id())
                    .cell(MAC, host.mac())
                    .cell(VLAN, host.vlan())
                    .cell(IPS, host.ipAddresses())
                    .cell(LOCATION, host.location());
        }

        private String getTypeIconId(Host host) {
            String hostType = host.annotations().value(AnnotationKeys.TYPE);
            return HOST_ICON_PREFIX +
                    (isNullOrEmpty(hostType) ? "endstation" : hostType);
        }

        private final class IpSetFormatter implements CellFormatter {
            private static final String COMMA = ", ";

            @Override
            public String format(Object value) {
                Set<IpAddress> ips = (Set<IpAddress>) value;
                if (ips.isEmpty()) {
                    return "(No IP Addresses for this host)";
                }
                StringBuilder sb = new StringBuilder();
                for (IpAddress ip : ips) {
                    sb.append(ip.toString())
                            .append(COMMA);
                }
                removeTrailingComma(sb);
                return sb.toString();
            }

            private StringBuilder removeTrailingComma(StringBuilder sb) {
                int pos = sb.lastIndexOf(COMMA);
                sb.delete(pos, sb.length());
                return sb;
            }
        }
    }
}
