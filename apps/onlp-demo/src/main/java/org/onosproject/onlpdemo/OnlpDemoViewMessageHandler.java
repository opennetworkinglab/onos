/*
 * Copyright 2019-present Open Networking Foundation
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

package org.onosproject.onlpdemo;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import org.onosproject.net.DeviceId;
import org.onosproject.onlpdemo.OnlpDemoManager.OnlpData;
import org.onosproject.onlpdemo.OnlpDemoManager.OnlpDataSource;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.table.TableModel;
import org.onosproject.ui.table.TableRequestHandler;
import org.onosproject.ui.table.cell.AbstractCellComparator;

import java.util.Collection;


/**
 * Message handler for ONLP view related messages.
 */
public class OnlpDemoViewMessageHandler extends UiMessageHandler {

    private static final String ONLP_DATA_REQ = "onlpDataRequest";
    private static final String ONLP_DATA_RESP = "onlpDataResponse";
    private static final String ONLPS = "onlps";

    private static final String ID = "id";
    private static final String PRESENCE = "presence";
    private static final String VENDOR = "vendor";
    private static final String SERIAL_NO = "serial_number";
    private static final String MODEL_NO = "model_number";
    private static final String FORM_FACTOR = "form_factor";


    private static final String[] COL_IDS = {
            ID, PRESENCE, VENDOR, MODEL_NO, SERIAL_NO, FORM_FACTOR,
    };

    private final OnlpDataSource onlpDataSource;

    public OnlpDemoViewMessageHandler(OnlpDataSource onlpDataSource) {
        super();
        this.onlpDataSource = onlpDataSource;
    }

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(
                new OnlpDataRequest()
        );
    }

    // handler for port table requests
    private final class OnlpDataRequest extends TableRequestHandler {

        private static final String NO_ROWS_MESSAGE = "No data available yet";

        private OnlpDataRequest() {
            super(ONLP_DATA_REQ, ONLP_DATA_RESP, ONLPS);
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
            tm.setComparator(ID, new SfpIdCellComparator());
            return tm;
        }

        @Override
        protected void populateTable(TableModel tm, ObjectNode payload) {
            String uri = string(payload, "devId");
            if (!Strings.isNullOrEmpty(uri)) {
                for (OnlpData data : onlpDataSource.getData(DeviceId.deviceId(uri))) {
                    populateRow(tm.addRow(), data);
                }
            }
        }

        private void populateRow(TableModel.Row row, OnlpData data) {
            row.cell(ID, data.id)
                .cell(PRESENCE, data.presence)
                .cell(VENDOR, data.vendor)
                .cell(MODEL_NO, data.modelNumber)
                .cell(SERIAL_NO, data.serialNumber)
                .cell(FORM_FACTOR, data.formFactor);
        }
    }

    private static class SfpIdCellComparator extends AbstractCellComparator {
        @Override
        protected int nonNullCompare(Object o1, Object o2) {
            return index((String) o2) - index((String) o1);
        }

        private int index(String s) {
            int i = s.indexOf("-");
            try {
                return i > 0 ? Integer.parseInt(s.substring(i)) : 0;
            } catch (NumberFormatException e) {
                return Integer.MAX_VALUE;
            }
        }
    }

}
