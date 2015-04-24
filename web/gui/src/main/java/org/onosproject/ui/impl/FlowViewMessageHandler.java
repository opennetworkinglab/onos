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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang.WordUtils;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.instructions.Instruction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;


/**
 * Message handler for flow view related messages.
 */
public class FlowViewMessageHandler extends AbstractTabularViewMessageHandler {

    private static final String NO_DEV = "none";

    /**
     * Creates a new message handler for the flow messages.
     */
    protected FlowViewMessageHandler() {
        super(ImmutableSet.of("flowDataRequest"));
    }

    @Override
    public void process(ObjectNode message) {
        ObjectNode payload = payload(message);
        String uri = string(payload, "devId", NO_DEV);
        String sortCol = string(payload, "sortCol", "id");
        String sortDir = string(payload, "sortDir", "asc");

        ObjectNode rootNode;
        if (uri.equals(NO_DEV)) {
            rootNode = mapper.createObjectNode();
            rootNode.set("flows", mapper.createArrayNode());
        } else {
            DeviceId deviceId = DeviceId.deviceId(uri);

            FlowRuleService service = get(FlowRuleService.class);
            TableRow[] rows = generateTableRows(service, deviceId);
            RowComparator rc =
                    new RowComparator(sortCol, RowComparator.direction(sortDir));
            Arrays.sort(rows, rc);
            ArrayNode flows = generateArrayNode(rows);

            rootNode = mapper.createObjectNode();
            rootNode.set("flows", flows);
        }

        connection().sendMessage("flowDataResponse", 0, rootNode);
    }

    private TableRow[] generateTableRows(FlowRuleService service,
                                         DeviceId deviceId) {
        List<TableRow> list = new ArrayList<>();
        for (FlowEntry flow : service.getFlowEntries(deviceId)) {
            list.add(new FlowTableRow(flow));
        }
        return list.toArray(new TableRow[list.size()]);
    }

    /**
     * TableRow implementation for {@link org.onosproject.net.flow.FlowRule flows}.
     */
    private static class FlowTableRow extends AbstractTableRow {

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

        private static final String[] COL_IDS = {
            ID, APP_ID, GROUP_ID, TABLE_ID, PRIORITY, SELECTOR,
                TREATMENT, TIMEOUT, PERMANENT, STATE
        };

        public FlowTableRow(FlowEntry f) {
            add(ID, Long.toString(f.id().value()));
            add(APP_ID, Short.toString(f.appId()));
            add(GROUP_ID, Integer.toString(f.groupId().id()));
            add(TABLE_ID, Integer.toString(f.tableId()));
            add(PRIORITY, Integer.toString(f.priority()));
            add(SELECTOR, getSelectorString(f));
            add(TREATMENT, getTreatmentString(f));
            add(TIMEOUT, Integer.toString(f.timeout()));
            add(PERMANENT, Boolean.toString(f.isPermanent()));
            add(STATE, WordUtils.capitalizeFully(f.state().toString()));
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
                    sb.append(WordUtils.capitalizeFully(c.type().toString()))
                            .append(COMMA);
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
                    sb.append(WordUtils.capitalizeFully(i.type().toString()))
                            .append(COMMA);
                }
                removeTrailingComma(sb);
            }
        }

        private void addImmediate(StringBuilder sb, List<Instruction> immediate) {
            if (!immediate.isEmpty()) {
                sb.append("Immediate instructions = ");
                for (Instruction i : immediate) {
                    sb.append(WordUtils.capitalizeFully(i.type().toString()))
                            .append(COMMA);
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
