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
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.table.AbstractTableRow;
import org.onosproject.ui.table.TableRequestHandler;
import org.onosproject.ui.table.TableRow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.apache.commons.lang.WordUtils.capitalizeFully;


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

    private static final String COMMA = ", ";

    @Override
    protected Collection<RequestHandler> getHandlers() {
        return ImmutableSet.of(new FlowDataRequest());
    }

    // handler for flow table requests
    private final class FlowDataRequest extends TableRequestHandler {

        private FlowDataRequest() {
            super(FLOW_DATA_REQ, FLOW_DATA_RESP, FLOWS);
        }

        @Override
        protected TableRow[] generateTableRows(ObjectNode payload) {
            String uri = string(payload, "devId");
            if (Strings.isNullOrEmpty(uri)) {
                return new TableRow[0];
            }
            DeviceId deviceId = DeviceId.deviceId(uri);
            FlowRuleService service = get(FlowRuleService.class);
            List<TableRow> list = new ArrayList<>();
            for (FlowEntry flow : service.getFlowEntries(deviceId)) {
                list.add(new FlowTableRow(flow));
            }
            return list.toArray(new TableRow[list.size()]);
        }
    }

    /**
     * TableRow implementation for
     * {@link org.onosproject.net.flow.FlowRule flows}.
     */
    private static class FlowTableRow extends AbstractTableRow {

        private static final String[] COL_IDS = {
                ID, APP_ID, GROUP_ID, TABLE_ID, PRIORITY, SELECTOR,
                TREATMENT, TIMEOUT, PERMANENT, STATE
        };

        public FlowTableRow(FlowEntry f) {
            add(ID, f.id().value());
            add(APP_ID, f.appId());
            add(GROUP_ID, f.groupId().id());
            add(TABLE_ID, f.tableId());
            add(PRIORITY, f.priority());
            add(SELECTOR, getSelectorString(f));
            add(TREATMENT, getTreatmentString(f));
            add(TIMEOUT, f.timeout());
            add(PERMANENT, f.isPermanent());
            add(STATE, capitalizeFully(f.state().toString()));
        }

        private String getSelectorString(FlowEntry f) {
            String result;
            TrafficSelector selector = f.selector();
            Set<Criterion> criteria = selector.criteria();

            if (criteria.isEmpty()) {
                result = "(No traffic selectors for this flow)";
            } else {
                StringBuilder sb = new StringBuilder("Criteria = ");
                for (Criterion c : criteria) {
                    sb.append(capitalizeFully(c.type().toString())).append(COMMA);
                }
                result = removeTrailingComma(sb).toString();
            }
            return result;
        }

        private String getTreatmentString(FlowEntry f) {
            TrafficTreatment treatment = f.treatment();
            List<Instruction> deferred = treatment.deferred();
            List<Instruction> immediate = treatment.immediate();
            boolean haveDef = !deferred.isEmpty();
            boolean haveImm = !immediate.isEmpty();
            boolean both = haveDef && haveImm;
            boolean neither = !haveDef && !haveImm;
            String result;

            if (neither) {
                result = "(No traffic treatment instructions for this flow)";
            } else {
                StringBuilder sb = new StringBuilder();
                addDeferred(sb, deferred);
                if (both) {
                    sb.append(COMMA);
                }
                addImmediate(sb, immediate);
                result = sb.toString();
            }
            return result;
        }

        private void addDeferred(StringBuilder sb, List<Instruction> deferred) {
            if (!deferred.isEmpty()) {
                sb.append("Deferred instructions = ");
                for (Instruction i : deferred) {
                    sb.append(capitalizeFully(i.type().toString())).append(COMMA);
                }
                removeTrailingComma(sb);
            }
        }

        private void addImmediate(StringBuilder sb, List<Instruction> immediate) {
            if (!immediate.isEmpty()) {
                sb.append("Immediate instructions = ");
                for (Instruction i : immediate) {
                    sb.append(capitalizeFully(i.type().toString())).append(COMMA);
                }
                removeTrailingComma(sb);
            }
        }

        private StringBuilder removeTrailingComma(StringBuilder sb) {
            int pos = sb.lastIndexOf(COMMA);
            sb.delete(pos, sb.length());
            return sb;
        }

        @Override
        protected String[] columnIds() {
            return COL_IDS;
        }
    }

}
