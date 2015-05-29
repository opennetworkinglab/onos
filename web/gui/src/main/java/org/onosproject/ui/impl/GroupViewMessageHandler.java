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
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import org.onosproject.net.DeviceId;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupService;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.table.CellFormatter;
import org.onosproject.ui.table.TableModel;
import org.onosproject.ui.table.TableRequestHandler;
import org.onosproject.ui.table.cell.EnumFormatter;

import java.util.Collection;
import java.util.List;


/**
 * Message handler for group view related messages.
 */
public class GroupViewMessageHandler extends UiMessageHandler {

    private static final String GROUP_DATA_REQ = "groupDataRequest";
    private static final String GROUP_DATA_RESP = "groupDataResponse";
    private static final String GROUPS = "groups";

    private static final String ID = "id";
    private static final String APP_ID = "app_id";
    private static final String STATE = "state";
    private static final String TYPE = "type";
    private static final String PACKETS = "packets";
    private static final String BYTES = "bytes";
    private static final String BUCKETS = "buckets";

    private static final String[] COL_IDS = {
            ID, APP_ID, STATE, TYPE, PACKETS, BYTES, BUCKETS
    };

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(new GroupDataRequest());
    }

    // handler for group table requests
    private final class GroupDataRequest extends TableRequestHandler {

        private GroupDataRequest() {
            super(GROUP_DATA_REQ, GROUP_DATA_RESP, GROUPS);
        }

        @Override
        protected String[] getColumnIds() {
            return COL_IDS;
        }

        @Override
        protected TableModel createTableModel() {
            TableModel tm = super.createTableModel();
            tm.setFormatter(TYPE, EnumFormatter.INSTANCE);
            tm.setFormatter(BUCKETS, new BucketFormatter());
            return tm;
        }

        @Override
        protected void populateTable(TableModel tm, ObjectNode payload) {
            String uri = string(payload, "devId");
            if (!Strings.isNullOrEmpty(uri)) {
                DeviceId deviceId = DeviceId.deviceId(uri);
                GroupService gs = get(GroupService.class);
                for (Group group : gs.getGroups(deviceId)) {
                    populateRow(tm.addRow(), group);
                }
            }
        }

        private void populateRow(TableModel.Row row, Group g) {
            row.cell(ID, g.id().id())
                .cell(APP_ID, g.appId().name())
                .cell(STATE, g.state())
                .cell(TYPE, g.type())
                .cell(PACKETS, g.packets())
                .cell(BYTES, g.bytes())
                .cell(BUCKETS, g.buckets().buckets());
        }

        private final class BucketFormatter implements CellFormatter {
            private static final String BREAK = "<br>";

            @Override
            public String format(Object value) {
                StringBuilder sb = new StringBuilder();
                List<GroupBucket> buckets = (List<GroupBucket>) value;

                if (buckets.isEmpty()) {
                    return "(No buckets for this group)";
                }

                for (GroupBucket b : buckets) {
                    sb.append("Bytes: ")
                            .append(b.bytes())
                            .append(" Packets: ")
                            .append(b.packets())
                            .append(" Actions: ")
                            .append(b.treatment().allInstructions())
                            .append(BREAK);
                }

                return sb.toString();
            }
        }
    }
}
