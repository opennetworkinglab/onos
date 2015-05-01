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
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.Host;
import org.onosproject.net.HostLocation;
import org.onosproject.net.host.HostService;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.table.AbstractTableRow;
import org.onosproject.ui.table.TableRequestHandler;
import org.onosproject.ui.table.TableRow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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


    @Override
    protected Collection<RequestHandler> getHandlers() {
        return ImmutableSet.of(new HostDataRequest());
    }

    // handler for host table requests
    private final class HostDataRequest extends TableRequestHandler {
        private HostDataRequest() {
            super(HOST_DATA_REQ, HOST_DATA_RESP, HOSTS);
        }

        @Override
        protected TableRow[] generateTableRows(ObjectNode payload) {
            HostService service = get(HostService.class);
            List<TableRow> list = new ArrayList<>();
            for (Host host : service.getHosts()) {
                list.add(new HostTableRow(host));
            }
            return list.toArray(new TableRow[list.size()]);
        }
    }

    /**
     * TableRow implementation for {@link Host hosts}.
     */
    private static class HostTableRow extends AbstractTableRow {

        private static final String[] COL_IDS = {
                TYPE_IID, ID, MAC, VLAN, IPS, LOCATION
        };

        public HostTableRow(Host h) {
            HostLocation location = h.location();

            add(TYPE_IID, getTypeIconId(h));
            add(ID, h.id());
            add(MAC, h.mac());
            add(VLAN, h.vlan());
            add(IPS, h.ipAddresses());
            add(LOCATION, concat(location.deviceId(), "/", location.port()));
        }

        private String getTypeIconId(Host host) {
            String hostType = host.annotations().value(AnnotationKeys.TYPE);
            return HOST_ICON_PREFIX +
                    (isNullOrEmpty(hostType) ? "endstation" : hostType);
        }

        @Override
        protected String[] columnIds() {
            return COL_IDS;
        }
    }

}
