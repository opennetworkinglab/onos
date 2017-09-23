/*
 * Copyright 2017-present Open Networking Foundation
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
import org.onosproject.store.primitives.PartitionAdminService;
import org.onosproject.store.service.PartitionInfo;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.table.CellFormatter;
import org.onosproject.ui.table.TableModel;
import org.onosproject.ui.table.TableRequestHandler;

import java.util.Collection;
import java.util.List;

/**
 * Message handler for partition view related messages.
 */
public class PartitionViewMessageHandler extends UiMessageHandler {
    private static final String PARTITION_DATA_REQ = "partitionDataRequest";
    private static final String PARTITION_DATA_RESP = "partitionDataResponse";
    private static final String PARTITIONS = "partitions";

    private static final String NAME = "name";
    private static final String TERM = "term";
    private static final String LEADER = "leader";
    private static final String MEMBERS = "members";

    private static final String[] COL_IDS = {NAME, TERM, LEADER, MEMBERS};

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(new PartitionDataHandler());
    }

    private final class PartitionDataHandler extends TableRequestHandler {
        private static final String NO_ROWS_MESSAGE = "No partitions found";

        private PartitionDataHandler() {
            super(PARTITION_DATA_REQ, PARTITION_DATA_RESP, PARTITIONS);
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
        protected String defaultColumnId() {
            return NAME;
        }

        @Override
        protected TableModel createTableModel() {
            TableModel tm = super.createTableModel();
            tm.setFormatter(MEMBERS, new MembersFormatter());
            return tm;
        }

        @Override
        protected void populateTable(TableModel tm, ObjectNode payload) {
            PartitionAdminService ps = get(PartitionAdminService.class);
            for (PartitionInfo partition : ps.partitionInfo()) {
                populateRow(tm.addRow(), partition);
            }
        }

        private void populateRow(TableModel.Row row, PartitionInfo p) {
            row.cell(NAME, p.id())
                    .cell(TERM, p.term())
                    .cell(LEADER, p.leader())
                    .cell(MEMBERS, p.members());
        }

        private final class MembersFormatter implements CellFormatter {
            private static final String COMMA = ", ";

            @Override
            public String format(Object value) {
                List<String> members = (List<String>) value;
                if (members.isEmpty()) {
                    return "(No members for this partition)";
                }
                StringBuilder sb = new StringBuilder();
                for (String m : members) {
                    sb.append(m).append(COMMA);
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
