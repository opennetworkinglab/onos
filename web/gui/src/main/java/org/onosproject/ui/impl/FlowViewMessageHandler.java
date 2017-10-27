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
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.onosproject.app.ApplicationService;
import org.onosproject.core.Application;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.table.CellFormatter;
import org.onosproject.ui.table.TableModel;
import org.onosproject.ui.table.TableRequestHandler;
import org.onosproject.ui.table.cell.DefaultCellFormatter;
import org.onosproject.ui.table.cell.EnumFormatter;
import org.onosproject.ui.table.cell.HexLongFormatter;
import org.onosproject.ui.table.cell.NumberFormatter;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Message handler for flow view related messages.
 */
public class FlowViewMessageHandler extends UiMessageHandler {

    private static final String FLOW_DATA_REQ = "flowDataRequest";
    private static final String FLOW_DATA_RESP = "flowDataResponse";
    private static final String FLOWS = "flows";

    private static final String FLOW_DETAILS_REQ = "flowDetailsRequest";
    private static final String FLOW_DETAILS_RESP = "flowDetailsResponse";
    private static final String DETAILS = "details";
    private static final String FLOW_PRIORITY = "priority";

    private static final String DEV_ID = "devId";

    private static final String ID = "id";
    private static final String FLOW_ID = "flowId";
    private static final String APP_ID = "appId";
    private static final String APP_NAME = "appName";
    private static final String GROUP_ID = "groupId";
    private static final String TABLE_NAME = "tableName";
    private static final String PRIORITY = "priority";
    private static final String SELECTOR_C = "selector_c"; // for table column
    private static final String SELECTOR = "selector";
    private static final String TREATMENT_C = "treatment_c"; // for table column
    private static final String TREATMENT = "treatment";
    private static final String IDLE_TIMEOUT = "idleTimeout";
    private static final String HARD_TIMEOUT = "hardTimeout";
    private static final String PERMANENT = "permanent";
    private static final String STATE = "state";
    private static final String PACKETS = "packets";
    private static final String DURATION = "duration";
    private static final String BYTES = "bytes";

    private static final String COMMA = ", ";
    private static final String OX = "0x";
    private static final String EMPTY = "";
    private static final String SPACE = " ";
    private static final String COLON = ":";
    private static final String ANGLE_O = "<";
    private static final String ANGLE_C = ">";
    private static final String SQUARE_O = "[";
    private static final String SQUARE_C = "]";

    private static final String ONOS_PREFIX = "org.onosproject.";
    private static final String ONOS_MARKER = "*";

    // json structure keys
    private static final String IMMED = "immed";
    private static final String DEFER = "defer";
    private static final String METER = "meter";
    private static final String TABLE = "table";
    private static final String META = "meta";
    private static final String CLEARDEF = "clearDef";

    // TODO: replace the use of the following constants with localized text
    private static final String MSG_NO_SELECTOR =
            "(No traffic selector criteria for this flow)";
    private static final String MSG_NO_TREATMENT =
            "(No traffic treatment instructions for this flow)";
    private static final String NO_ROWS_NO_FLOWS = "No flows found";

    private static final String CRITERIA = "Criteria";
    private static final String TREATMENT_INSTRUCTIONS = "Treatment Instructions";
    private static final String IMM = "imm";
    private static final String DEF = "def";
    private static final String METERED = "metered";
    private static final String TRANSITION = "transition";
    private static final String METADATA = "metadata";
    private static final String CLEARED = "cleared";
    private static final String UNKNOWN = "Unknown";


    private static final String[] COL_IDS = {
            ID,
            STATE,
            BYTES,
            PACKETS,
            DURATION,
            PRIORITY,
            TABLE_NAME,
            APP_ID,
            APP_NAME,

            GROUP_ID,
            IDLE_TIMEOUT,
            PERMANENT,

            SELECTOR_C,
            SELECTOR,
            TREATMENT_C,
            TREATMENT,
    };

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(
                new FlowDataRequest(),
                new DetailRequestHandler()
        );
    }

    private void removeTrailingComma(StringBuilder sb) {
        int pos = sb.lastIndexOf(COMMA);
        sb.delete(pos, sb.length());
    }

    // Generate a map of shorts->application IDs
    // (working around deficiency(?) in Application Service API)
    private Map<Short, ApplicationId> appShortMap() {
        Set<Application> apps =
                get(ApplicationService.class).getApplications();

        return apps.stream()
                .collect(Collectors.toMap(a -> a.id().id(), Application::id));
    }

    // Return an application name, based on a lookup of the internal short ID
    private String makeAppName(short id, Map<Short, ApplicationId> lookup) {
        ApplicationId appId = lookup.get(id);
        if (appId == null) {
            appId = get(CoreService.class).getAppId(id);
            if (appId == null) {
                return UNKNOWN + SPACE + ANGLE_O + id + ANGLE_C;
            }
            lookup.put(id, appId);
        }
        String appName = appId.name();
        return appName.startsWith(ONOS_PREFIX)
                ? appName.replaceFirst(ONOS_PREFIX, ONOS_MARKER) : appName;
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
        protected String noRowsMessage(ObjectNode payload) {
            return NO_ROWS_NO_FLOWS;
        }

        @Override
        protected TableModel createTableModel() {
            TableModel tm = super.createTableModel();
            tm.setFormatter(ID, HexLongFormatter.INSTANCE);
            tm.setFormatter(STATE, EnumFormatter.INSTANCE);
            tm.setFormatter(BYTES, NumberFormatter.INTEGER);
            tm.setFormatter(PACKETS, NumberFormatter.INTEGER);
            tm.setFormatter(DURATION, NumberFormatter.INTEGER);

            tm.setFormatter(SELECTOR_C, new SelectorShortFormatter());
            tm.setFormatter(SELECTOR, new SelectorFormatter());
            tm.setFormatter(TREATMENT_C, new TreatmentShortFormatter());
            tm.setFormatter(TREATMENT, new TreatmentFormatter());
            tm.setFormatter(TABLE_NAME, DefaultCellFormatter.INSTANCE);
            return tm;
        }

        @Override
        protected void populateTable(TableModel tm, ObjectNode payload) {
            String uri = string(payload, DEV_ID);
            if (!Strings.isNullOrEmpty(uri)) {
                DeviceId deviceId = DeviceId.deviceId(uri);
                Map<Short, ApplicationId> lookup = appShortMap();
                FlowRuleService frs = get(FlowRuleService.class);

                for (FlowEntry flow : frs.getFlowEntries(deviceId)) {
                    populateRow(tm.addRow(), flow, lookup);
                }
            }
        }

        private void populateRow(TableModel.Row row, FlowEntry flow,
                                 Map<Short, ApplicationId> lookup) {
            row.cell(ID, flow.id().value())
                    .cell(STATE, flow.state())
                    .cell(BYTES, flow.bytes())
                    .cell(PACKETS, flow.packets())
                    .cell(DURATION, flow.life())
                    .cell(PRIORITY, flow.priority())
                    .cell(TABLE_NAME, flow.table())
                    .cell(APP_ID, flow.appId())
                    .cell(APP_NAME, makeAppName(flow.appId(), lookup))

                    .cell(GROUP_ID, flow.groupId().id())
                    .cell(IDLE_TIMEOUT, flow.timeout())
                    .cell(PERMANENT, flow.isPermanent())

                    .cell(SELECTOR_C, flow)
                    .cell(SELECTOR, flow)
                    .cell(TREATMENT_C, flow)
                    .cell(TREATMENT, flow);
        }

        private class InternalSelectorFormatter implements CellFormatter {
            private final boolean shortFormat;

            InternalSelectorFormatter(boolean shortFormat) {
                this.shortFormat = shortFormat;
            }

            @Override
            public String format(Object value) {
                FlowEntry flow = (FlowEntry) value;
                Set<Criterion> criteria = flow.selector().criteria();

                if (criteria.isEmpty()) {
                    return MSG_NO_SELECTOR;
                }

                StringBuilder sb = new StringBuilder();
                if (!shortFormat) {
                    sb.append(CRITERIA).append(COLON).append(SPACE);
                }

                for (Criterion c : criteria) {
                    sb.append(c).append(COMMA);
                }
                removeTrailingComma(sb);

                return sb.toString();
            }
        }

        private final class SelectorShortFormatter extends InternalSelectorFormatter {
            SelectorShortFormatter() {
                super(true);
            }
        }

        private final class SelectorFormatter extends InternalSelectorFormatter {
            SelectorFormatter() {
                super(false);
            }
        }

        private class InternalTreatmentFormatter implements CellFormatter {
            private final boolean shortFormat;

            InternalTreatmentFormatter(boolean shortFormat) {
                this.shortFormat = shortFormat;
            }

            @Override
            public String format(Object value) {
                FlowEntry flow = (FlowEntry) value;
                TrafficTreatment treatment = flow.treatment();
                List<Instruction> imm = treatment.immediate();
                List<Instruction> def = treatment.deferred();
                if (imm.isEmpty() &&
                        def.isEmpty() &&
                        treatment.metered() == null &&
                        treatment.tableTransition() == null &&
                        treatment.writeMetadata() == null) {
                    return MSG_NO_TREATMENT;
                }

                StringBuilder sb = new StringBuilder();

                if (!shortFormat) {
                    sb.append(TREATMENT_INSTRUCTIONS).append(COLON).append(SPACE);
                }

                formatInstructs(sb, imm, IMM);
                formatInstructs(sb, def, DEF);

                treatment.meters().forEach(meterInstruction ->
                        addLabVal(sb, METERED, meterInstruction)
                );
                addLabVal(sb, TRANSITION, treatment.tableTransition());
                addLabVal(sb, METADATA, treatment.writeMetadata());

                sb.append(CLEARED).append(COLON)
                        .append(treatment.clearedDeferred());

                return sb.toString();
            }

            private void addLabVal(StringBuilder sb, String label, Instruction value) {
                if (value != null) {
                    sb.append(label).append(COLON).append(value).append(COMMA);
                }
            }
        }

        private final class TreatmentShortFormatter extends InternalTreatmentFormatter {
            TreatmentShortFormatter() {
                super(true);
            }
        }

        private final class TreatmentFormatter extends InternalTreatmentFormatter {
            TreatmentFormatter() {
                super(false);
            }
        }

        private void formatInstructs(StringBuilder sb,
                                     List<Instruction> instructs,
                                     String type) {
            if (!instructs.isEmpty()) {
                sb.append(type).append(SQUARE_O);
                for (Instruction i : instructs) {
                    sb.append(renderInstructionForDisplay(i)).append(COMMA);
                }
                removeTrailingComma(sb);
                sb.append(SQUARE_C).append(COMMA);
            }
        }
    }

    private final class DetailRequestHandler extends RequestHandler {
        private DetailRequestHandler() {
            super(FLOW_DETAILS_REQ);
        }

        private FlowEntry findFlowById(String appIdText, String flowId) {
            String strippedFlowId = flowId.replaceAll(OX, EMPTY);
            FlowRuleService fs = get(FlowRuleService.class);
            int appIdInt = Integer.parseInt(appIdText);
            ApplicationId appId = new DefaultApplicationId(appIdInt, DETAILS);
            Iterable<FlowEntry> entries = fs.getFlowEntriesById(appId);

            for (FlowEntry entry : entries) {
                if (entry.id().toString().equals(strippedFlowId)) {
                    return entry;
                }
            }

            return null;
        }

        private String decorateFlowId(FlowRule flow) {
            return OX + flow.id();
        }

        private String decorateGroupId(FlowRule flow) {
            return OX + flow.groupId().id();
        }

        @Override
        public void process(ObjectNode payload) {

            String flowId = string(payload, FLOW_ID);
            String appId = string(payload, APP_ID);

            FlowEntry flow = findFlowById(appId, flowId);
            if (flow != null) {
                ObjectNode data = objectNode();

                data.put(FLOW_ID, decorateFlowId(flow));

                data.put(STATE, EnumFormatter.INSTANCE.format(flow.state()));
                data.put(BYTES, NumberFormatter.INTEGER.format(flow.bytes()));
                data.put(PACKETS, NumberFormatter.INTEGER.format(flow.packets()));
                data.put(DURATION, NumberFormatter.INTEGER.format(flow.life()));

                data.put(FLOW_PRIORITY, flow.priority());
                data.put(TABLE_NAME, flow.table().toString());
                data.put(APP_ID, flow.appId());
                // NOTE: horribly inefficient... make a map and retrieve a single value...
                data.put(APP_NAME, makeAppName(flow.appId(), appShortMap()));

                data.put(GROUP_ID, decorateGroupId(flow));
                data.put(IDLE_TIMEOUT, flow.timeout());
                data.put(HARD_TIMEOUT, flow.hardTimeout());
                data.put(PERMANENT, flow.isPermanent());

                data.set(SELECTOR, jsonCriteria(flow));
                data.set(TREATMENT, jsonTreatment(flow));

                ObjectNode rootNode = objectNode();
                rootNode.set(DETAILS, data);
                sendMessage(FLOW_DETAILS_RESP, rootNode);
            }
        }

        private ArrayNode jsonCriteria(FlowEntry flow) {
            ArrayNode crits = arrayNode();
            for (Criterion c : flow.selector().criteria()) {
                crits.add(c.toString());
            }
            return crits;
        }

        private ObjectNode jsonTreatment(FlowEntry flow) {
            ObjectNode treat = objectNode();
            TrafficTreatment treatment = flow.treatment();
            List<Instruction> imm = Lists.newArrayList(treatment.immediate());
            List<Instruction> def = treatment.deferred();
            Set<Instructions.MeterInstruction> meter = treatment.meters();
            Instructions.TableTypeTransition table = treatment.tableTransition();
            Instructions.MetadataInstruction meta = treatment.writeMetadata();

            if (!imm.isEmpty()) {
                treat.set(IMMED, jsonInstrList(imm));
            }
            if (!def.isEmpty()) {
                treat.set(DEFER, jsonInstrList(def));
            }
            if (!meter.isEmpty()) {
                treat.set(METER, jsonInstrList(Lists.newArrayList(meter)));
            }
            if (table != null) {
                treat.put(TABLE, table.toString());
            }
            if (meta != null) {
                treat.put(META, meta.toString());
            }
            treat.put(CLEARDEF, treatment.clearedDeferred());
            return treat;
        }

        private ArrayNode jsonInstrList(List<Instruction> instructions) {
            ArrayNode array = arrayNode();
            for (Instruction i : instructions) {
                array.add(renderInstructionForDisplay(i));
            }
            return array;
        }
    }

    // package private to allow unit test access...
    String renderInstructionForDisplay(Instruction instr) {

        // special handling for Extension Instruction Wrappers...
        if (instr instanceof Instructions.ExtensionInstructionWrapper) {
            Instructions.ExtensionInstructionWrapper wrap =
                    (Instructions.ExtensionInstructionWrapper) instr;
            return wrap.type() + COLON + wrap.extensionInstruction();
        }

        // special handling of other instruction classes could be placed here

        // default to the natural string representation otherwise
        return instr.toString();
    }
}
