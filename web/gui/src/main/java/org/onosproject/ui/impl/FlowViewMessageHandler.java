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
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.table.CellFormatter;
import org.onosproject.ui.table.TableModel;
import org.onosproject.ui.table.TableRequestHandler;
import org.onosproject.ui.table.cell.EnumFormatter;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Message handler for flow view related messages.
 */
public class FlowViewMessageHandler extends UiMessageHandler {

    private static final String FLOW_DATA_REQ = "flowDataRequest";
    private static final String FLOW_DATA_RESP = "flowDataResponse";
    private static final String FLOWS = "flows";

    private static final String ID = "id";
    private static final String APP_ID = "appId";
    private static final String GROUP_ID = "groupId";
    private static final String TABLE_ID = "tableId";
    private static final String PRIORITY = "priority";
    private static final String SELECTOR = "selector";
    private static final String TREATMENT = "treatment";
    private static final String TIMEOUT = "timeout";
    private static final String PERMANENT = "permanent";
    private static final String STATE = "state";
    private static final String PACKETS = "packets";
    private static final String BYTES = "bytes";

    private static final String COMMA = ", ";

    private static final String[] COL_IDS = {
            ID, APP_ID, GROUP_ID, TABLE_ID, PRIORITY, SELECTOR,
            TREATMENT, TIMEOUT, PERMANENT, STATE, PACKETS, BYTES
    };

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(new FlowDataRequest());
    }

    // handler for flow table requests
    private final class FlowDataRequest extends TableRequestHandler {

        private FlowDataRequest() {
            super(FLOW_DATA_REQ, FLOW_DATA_RESP, FLOWS);
        }

        @Override
        protected String[] getColumnIds() {
            return COL_IDS;
        }

        @Override
        protected TableModel createTableModel() {
            TableModel tm = super.createTableModel();
            tm.setFormatter(SELECTOR, new SelectorFormatter());
            tm.setFormatter(TREATMENT, new TreatmentFormatter());
            tm.setFormatter(STATE, EnumFormatter.INSTANCE);
            return tm;
        }

        @Override
        protected void populateTable(TableModel tm, ObjectNode payload) {
            String uri = string(payload, "devId");
            if (!Strings.isNullOrEmpty(uri)) {
                DeviceId deviceId = DeviceId.deviceId(uri);
                FlowRuleService frs = get(FlowRuleService.class);
                for (FlowEntry flow : frs.getFlowEntries(deviceId)) {
                    populateRow(tm.addRow(), flow);
                }
            }
        }

        private void populateRow(TableModel.Row row, FlowEntry flow) {
            row.cell(ID, flow.id().value())
                .cell(APP_ID, flow.appId())
                .cell(GROUP_ID, flow.groupId().id())
                .cell(TABLE_ID, flow.tableId())
                .cell(PRIORITY, flow.priority())
                .cell(SELECTOR, flow)
                .cell(TREATMENT, flow)
                .cell(TIMEOUT, flow.timeout())
                .cell(PERMANENT, flow.isPermanent())
                .cell(STATE, flow.state())
                .cell(PACKETS, flow.packets())
                .cell(BYTES, flow.bytes());
        }

        private final class SelectorFormatter implements CellFormatter {
            @Override
            public String format(Object value) {
                FlowEntry flow = (FlowEntry) value;
                Set<Criterion> criteria = flow.selector().criteria();

                if (criteria.isEmpty()) {
                    return "(No traffic selector criteria for this flow)";
                }
                StringBuilder sb = new StringBuilder("Criteria: ");
                for (Criterion c : criteria) {
                    sb.append(c).append(COMMA);
                }
                removeTrailingComma(sb);

                return sb.toString();
            }
        }

        private final class TreatmentFormatter implements CellFormatter {
            @Override
            public String format(Object value) {
                FlowEntry flow = (FlowEntry) value;
                List<Instruction> instructions = flow.treatment().allInstructions();

                if (instructions.isEmpty()) {
                    return "(No traffic treatment instructions for this flow)";
                }
                StringBuilder sb = new StringBuilder("Treatment Instructions: ");
                for (Instruction i : instructions) {
                    sb.append(i).append(COMMA);
                }
                removeTrailingComma(sb);

                return sb.toString();
            }
        }

        private StringBuilder removeTrailingComma(StringBuilder sb) {
            int pos = sb.lastIndexOf(COMMA);
            sb.delete(pos, sb.length());
            return sb;
        }
    }
}
