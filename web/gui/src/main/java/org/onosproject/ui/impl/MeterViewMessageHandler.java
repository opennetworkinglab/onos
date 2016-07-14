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
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.meter.Band;
import org.onosproject.net.meter.Meter;
import org.onosproject.net.meter.MeterService;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.table.CellFormatter;
import org.onosproject.ui.table.TableModel;
import org.onosproject.ui.table.TableRequestHandler;
import org.onosproject.ui.table.cell.HexLongFormatter;
import org.onosproject.ui.table.cell.NumberFormatter;

import java.util.Collection;
import java.util.Set;

/**
 * Message handler for meter view related messages.
 */
public class MeterViewMessageHandler extends UiMessageHandler {

    private static final String METER_DATA_REQ = "meterDataRequest";
    private static final String METER_DATA_RESP = "meterDataResponse";
    private static final String METERS = "meters";

    private static final String PROTOCOL = "protocol";
    private static final String OF_10 = "OF_10";
    private static final String OF_11 = "OF_11";
    private static final String OF_12 = "OF_12";
    private static final Set<String> UNSUPPORTED_PROTOCOLS =
            ImmutableSet.of(OF_10, OF_11, OF_12);

    private static final String ID = "id";
    private static final String APP_ID = "app_id";
    private static final String STATE = "state";
    private static final String PACKETS = "packets";
    private static final String BYTES = "bytes";
    private static final String BANDS = "bands";

    private static final String[] COL_IDS = {
            ID, APP_ID, STATE, PACKETS, BYTES, BANDS
    };

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(new MeterDataRequest());
    }

    // handler for meter table requests
    private final class MeterDataRequest extends TableRequestHandler {

        private static final String NO_ROWS_MESSAGE = "No meters found";
        private static final String NOT_SUPPORT_MESSAGE = "Meters not supported";

        private MeterDataRequest() {
            super(METER_DATA_REQ, METER_DATA_RESP, METERS);
        }

        @Override
        protected String[] getColumnIds() {
            return COL_IDS;
        }

        @Override
        protected String noRowsMessage(ObjectNode payload) {
            String uri = string(payload, "devId");
            if (!Strings.isNullOrEmpty(uri)) {
                DeviceService ds = get(DeviceService.class);
                Device dev = ds.getDevice(DeviceId.deviceId(uri));

                if (meterNotSupported(dev)) {
                    return NOT_SUPPORT_MESSAGE;
                }
            }
            return NO_ROWS_MESSAGE;
        }

        @Override
        protected TableModel createTableModel() {
            TableModel tm = super.createTableModel();
            tm.setFormatter(ID, HexLongFormatter.INSTANCE);
            tm.setFormatter(PACKETS, NumberFormatter.INTEGER);
            tm.setFormatter(BYTES, NumberFormatter.INTEGER);
            tm.setFormatter(BANDS, new BandFormatter());
            return tm;
        }

        @Override
        protected void populateTable(TableModel tm, ObjectNode payload) {
            String uri = string(payload, "devId");
            if (!Strings.isNullOrEmpty(uri)) {
                DeviceId deviceId = DeviceId.deviceId(uri);
                MeterService ms = get(MeterService.class);
                for (Meter meter : ms.getMeters(deviceId)) {
                    populateRow(tm.addRow(), meter);
                }
            }
        }

        private void populateRow(TableModel.Row row, Meter m) {
            row.cell(ID, m.id().id())
                    .cell(APP_ID, m.appId().name())
                    .cell(STATE, m.state())
                    .cell(PACKETS, m.packetsSeen())
                    .cell(BYTES, m.bytesSeen())
                    .cell(BANDS, m.bands());
        }

        private boolean meterNotSupported(Device dev) {
            String protocol = dev.annotations().value(PROTOCOL);
            return UNSUPPORTED_PROTOCOLS.contains(protocol);
        }

        private final class BandFormatter implements CellFormatter {
            private static final String BREAK = "<br>";

            @Override
            public String format(Object value) {
                StringBuilder sb = new StringBuilder();
                Collection<Band> bands = (Collection<Band>) value;

                if (bands.isEmpty()) {
                    return "(No bands for this meter)";
                }

                // TODO: re-arrange band properties based on band type
                for (Band b : bands) {
                    sb.append("Bytes: ")
                            .append(b.bytes())
                            .append(" Packets: ")
                            .append(b.packets())
                            .append(" Type: ")
                            .append(b.type())
                            .append(BREAK);
                }

                return sb.toString();
            }
        }
    }
}
