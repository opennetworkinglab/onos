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
import org.onosproject.net.packet.PacketProcessorEntry;
import org.onosproject.net.packet.PacketService;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.table.TableModel;
import org.onosproject.ui.table.TableRequestHandler;
import org.onosproject.ui.table.cell.NumberFormatter;

import java.util.Collection;

import static org.onosproject.net.packet.PacketProcessor.ADVISOR_MAX;
import static org.onosproject.net.packet.PacketProcessor.DIRECTOR_MAX;

/**
 * Message handler for packet processor view related messages.
 */
public class ProcessorViewMessageHandler extends UiMessageHandler {

    private static final String PROCESSOR_DATA_REQ = "processorDataRequest";
    private static final String PROCESSOR_DATA_RESP = "processorDataResponse";
    private static final String PROCESSORS = "processors";

    private static final String OBSERVER = "observer";
    private static final String DIRECTOR = "director";
    private static final String ADVISOR = "advisor";

    private static final String ID = "id";
    private static final String TYPE = "type";
    private static final String PRIORITY = "priority";
    private static final String PROCESSOR = "processor";
    private static final String PACKETS = "packets";
    private static final String AVG_MS = "avgMillis";

    private static final long NANOS_IN_MS = 1_000_000;

    private static final String[] COL_IDS = {
            ID, TYPE, PRIORITY, PROCESSOR, PACKETS, AVG_MS
    };

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(new ProcessorDataRequest());
    }

    // handler for packet processor table requests
    private final class ProcessorDataRequest extends TableRequestHandler {
        private static final String NO_ROWS_MESSAGE = "No packet processors found";

        private ProcessorDataRequest() {
            super(PROCESSOR_DATA_REQ, PROCESSOR_DATA_RESP, PROCESSORS);
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
            tm.setFormatter(AVG_MS, NumberFormatter.TO_5DP);
            return tm;
        }

        @Override
        protected void populateTable(TableModel tm, ObjectNode payload) {
            PacketService ps = get(PacketService.class);
            ps.getProcessors().forEach(entry -> populateRow(tm.addRow(), entry));
        }

        private void populateRow(TableModel.Row row, PacketProcessorEntry entry) {
            row.cell(ID, entry.priority())
                    .cell(TYPE, processorType(entry.priority()))
                    .cell(PRIORITY, processorPriority(entry.priority()))
                    .cell(PROCESSOR, entry.processor().getClass().getName())
                    .cell(PACKETS, entry.invocations())
                    .cell(AVG_MS, (double) entry.averageNanos() / NANOS_IN_MS);
        }

        private String processorType(int p) {
            return p > DIRECTOR_MAX ? OBSERVER : p > ADVISOR_MAX ? DIRECTOR : ADVISOR;
        }

        private int processorPriority(int p) {
            return p > DIRECTOR_MAX ? (p - DIRECTOR_MAX - 1) :
                    p > ADVISOR_MAX ? (p - ADVISOR_MAX - 1) : (p - 1);
        }

    }
}
