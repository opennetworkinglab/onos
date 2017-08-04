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
package org.onosproject.ui.impl;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import org.onlab.packet.IpAddress;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.BasicHostConfig;
import org.onosproject.net.host.HostService;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.table.CellFormatter;
import org.onosproject.ui.table.TableModel;
import org.onosproject.ui.table.TableRequestHandler;
import org.onosproject.ui.table.cell.HostLocationFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Strings.emptyToNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onosproject.net.HostId.hostId;

/**
 * Message handler for host view related messages.
 */
public class HostViewMessageHandler extends UiMessageHandler {

    private static final String HOST_DATA_REQ = "hostDataRequest";
    private static final String HOST_DATA_RESP = "hostDataResponse";
    private static final String HOSTS = "hosts";

    private static final String HOST_DETAILS_REQ = "hostDetailsRequest";
    private static final String HOST_DETAILS_RESP = "hostDetailsResponse";
    private static final String DETAILS = "details";

    private static final String HOST_NAME_CHANGE_REQ = "hostNameChangeRequest";
    private static final String HOST_NAME_CHANGE_RESP = "hostNameChangeResponse";

    private static final String TYPE_IID = "_iconid_type";
    private static final String NAME = "name";
    private static final String ID = "id";
    private static final String MAC = "mac";
    private static final String VLAN = "vlan";
    private static final String IP = "ip";
    private static final String IPS = "ips";
    private static final String LOCATION = "location";
    private static final String LOCATIONS = "locations";
    private static final String CONFIGURED = "configured";

    private static final String DASH = "-";

    private static final String HOST_ICON_PREFIX = "hostIcon_";

    private static final String[] COL_IDS = {
            TYPE_IID, NAME, ID, MAC, VLAN, CONFIGURED, IPS, LOCATION
    };

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(
                new HostDataRequest(),
                new DetailRequestHandler(),
                new NameChangeHandler()
        );
    }

    private String getTypeIconId(Host host) {
        String hostType = host.annotations().value(AnnotationKeys.TYPE);
        return HOST_ICON_PREFIX +
                (isNullOrEmpty(hostType) ? "endstation" : hostType);
    }

    // Returns the first of the given set of IP addresses as a string.
    private String ip(Set<IpAddress> ipAddresses) {
        Iterator<IpAddress> it = ipAddresses.iterator();
        return it.hasNext() ? it.next().toString() : "unknown";
    }

    private boolean useDefaultName(String nameAnnotated) {
        return isNullOrEmpty(nameAnnotated) || DASH.equals(nameAnnotated);
    }

    // returns the "friendly name" for the host
    private String getHostName(Host host) {
        String name = host.annotations().value(AnnotationKeys.NAME);
        return useDefaultName(name) ? ip(host.ipAddresses()) : name;
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
                    .cell(NAME, getHostName(host))
                    .cell(ID, host.id())
                    .cell(MAC, host.mac())
                    .cell(VLAN, host.vlan())
                    .cell(CONFIGURED, host.configured())
                    .cell(IPS, host.ipAddresses())
                    .cell(LOCATION, host.location());
            // Note: leave complete list of all LOCATIONS to the details panel
        }

        private final class IpSetFormatter implements CellFormatter {
            private static final String COMMA = ", ";

            @Override
            public String format(Object value) {
                Set<IpAddress> ips = (Set<IpAddress>) value;
                if (ips.isEmpty()) {
                    return "(No IP Addresses)";
                }
                StringBuilder sb = new StringBuilder();
                for (IpAddress ip : ips) {
                    sb.append(ip.toString())
                            .append(COMMA);
                }
                return removeTrailingComma(sb).toString();
            }

            private StringBuilder removeTrailingComma(StringBuilder sb) {
                int pos = sb.lastIndexOf(COMMA);
                sb.delete(pos, sb.length());
                return sb;
            }
        }
    }


    private final class DetailRequestHandler extends RequestHandler {
        private DetailRequestHandler() {
            super(HOST_DETAILS_REQ);
        }

        @Override
        public void process(ObjectNode payload) {
            String id = string(payload, ID, "");

            HostId hostId = hostId(id);
            HostService service = get(HostService.class);
            Host host = service.getHost(hostId);
            ObjectNode data = objectNode();

            data.put(TYPE_IID, getTypeIconId(host))
                    .put(NAME, getHostName(host))
                    .put(ID, hostId.toString())
                    .put(IP, ip(host.ipAddresses()))
                    .put(MAC, host.mac().toString())
                    .put(VLAN, host.vlan().toString())
                    .put(CONFIGURED, host.configured())
                    .put(LOCATION, host.location().toString());

            List<IpAddress> sortedIps = new ArrayList<>(host.ipAddresses());
            Collections.sort(sortedIps);
            ArrayNode ips = arrayNode();
            for (IpAddress ip : sortedIps) {
                ips.add(ip.toString());
            }
            data.set(IPS, ips);

            List<HostLocation> sortedLocs = new ArrayList<>(host.locations());
            Collections.sort(sortedLocs);
            ArrayNode locs = arrayNode();
            for (HostLocation hl : sortedLocs) {
                locs.add(hl.toString());
            }
            data.set(LOCATIONS, locs);

            ObjectNode root = objectNode();
            root.set(DETAILS, data);

            sendMessage(HOST_DETAILS_RESP, root);
        }
    }

    private final class NameChangeHandler extends RequestHandler {
        private NameChangeHandler() {
            super(HOST_NAME_CHANGE_REQ);
        }

        @Override
        public void process(ObjectNode payload) {
            HostId hostId = hostId(string(payload, ID, ""));
            String name = emptyToNull(string(payload, NAME, null));
            log.debug("Name change request: {} -- '{}'", hostId, name);

            NetworkConfigService service = get(NetworkConfigService.class);
            BasicHostConfig cfg =
                    service.addConfig(hostId, BasicHostConfig.class);

            // Name attribute missing from the payload (or empty string)
            // means that the friendly name should be unset.
            cfg.name(name);
            cfg.apply();
            sendMessage(HOST_NAME_CHANGE_RESP, payload);
        }
    }
}
