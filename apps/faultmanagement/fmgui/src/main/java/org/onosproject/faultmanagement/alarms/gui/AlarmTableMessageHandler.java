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
package org.onosproject.faultmanagement.alarms.gui;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.table.TableModel;
import org.onosproject.ui.table.TableRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Set;
import org.joda.time.DateTime;
import org.onosproject.incubator.net.faultmanagement.alarm.Alarm;
import org.onosproject.incubator.net.faultmanagement.alarm.AlarmId;
import org.onosproject.net.DeviceId;
import org.onosproject.ui.table.cell.TimeFormatter;

/**
 * Skeletal ONOS UI Table-View message handler.
 */
public class AlarmTableMessageHandler extends UiMessageHandler {

    private static final String ALARM_TABLE_DATA_REQ = "alarmTableDataRequest";
    private static final String ALARM_TABLE_DATA_RESP = "alarmTableDataResponse";
    private static final String ALARM_TABLES = "alarmTables";

    private static final String ALARM_TABLE_DETAIL_REQ = "alarmTableDetailsRequest";
    private static final String ALARM_TABLE_DETAIL_RESP = "alarmTableDetailsResponse";
    private static final String DETAILS = "details";

    private static final String ID = "id";
    private static final String DEVICE_ID_STR = "alarmDeviceId";
    private static final String DESCRIPTION = "alarmDesc";
    private static final String SOURCE = "alarmSource";
    private static final String TIME_RAISED = "alarmTimeRaised";
    private static final String TIME_UPDATED = "alarmTimeUpdated";
    private static final String TIME_CLEARED = "alarmTimeCleared";
    private static final String SEVERITY = "alarmSeverity";
    private static final String RESULT = "result";

    // TODO No need to show id column in ONOS-GUI

    // TODO Replace SEVERITY column by color-coding of row depending on severity ie. red=critical, green=cleared etc
    private static final String[] COLUMN_IDS = {ID, DEVICE_ID_STR, DESCRIPTION, SOURCE, TIME_RAISED, SEVERITY};

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(
                new AlarmTableDataRequestHandler(),
                new AlarmTableDetailRequestHandler()
        );
    }

    // handler for alarm table requests
    private final class AlarmTableDataRequestHandler extends TableRequestHandler {

        private static final String NO_ROWS_MESSAGE = "No alarms found";

        private AlarmTableDataRequestHandler() {
            super(ALARM_TABLE_DATA_REQ, ALARM_TABLE_DATA_RESP, ALARM_TABLES);
        }

        @Override
        protected String defaultColumnId() {
            // if necessary, override defaultColumnId() -- if it isn't "id"
            return ID;
        }

        @Override
        protected String[] getColumnIds() {
            return COLUMN_IDS;
        }

        @Override
        protected String noRowsMessage(ObjectNode payload) {
            return NO_ROWS_MESSAGE;
        }

        @Override
        protected TableModel createTableModel() {
            // if required, override createTableModel() to set column formatters / comparators
            TableModel tm = super.createTableModel();
            tm.setFormatter(TIME_RAISED, new TimeFormatter());
            return tm;
        }

        @Override
        protected void populateTable(TableModel tm, ObjectNode payload) {
            log.debug(" populateTable tm={} payload ={}", tm, payload);
            String devId = string(payload, "devId");

            Set<Alarm> alarms = Strings.isNullOrEmpty(devId) ?
                    AlarmServiceUtil.lookUpAlarms() :
                    AlarmServiceUtil.lookUpAlarms(DeviceId.deviceId(devId));

            alarms.forEach((alarm) -> {
                populateRow(tm.addRow(), alarm);
            });

        }

        private void populateRow(TableModel.Row row, Alarm alarm) {
            log.debug("populate table Row row={} item ={}", row, alarm);

            row.cell(ID, alarm.id().fingerprint())
                    .cell(DEVICE_ID_STR, alarm.deviceId())
                    .cell(DESCRIPTION, alarm.description())
                    .cell(SOURCE, alarm.source())
                    .cell(TIME_RAISED, new DateTime(alarm.timeRaised()))
                    .cell(SEVERITY, alarm.severity());
        }
    }

    // handler for alarm details requests
    private final class AlarmTableDetailRequestHandler extends RequestHandler {

        private AlarmTableDetailRequestHandler() {
            super(ALARM_TABLE_DETAIL_REQ);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            log.debug("sid={}, payload ={}", sid, payload);

            String id = string(payload, ID, "(none)");
            Alarm alarm = AlarmServiceUtil.lookupAlarm(AlarmId.alarmId(Long.parseLong(id)));
            ObjectNode rootNode = objectNode();
            ObjectNode data = objectNode();
            rootNode.set(DETAILS, data);

            if (alarm == null) {
                rootNode.put(RESULT, "Item with id '" + id + "' not found");
                log.warn("attempted to get item detail for id '{}'", id);

            } else {
                rootNode.put(RESULT, "Found item with id '" + id + "'");

                data.put(ID, alarm.id().fingerprint());
                data.put(DESCRIPTION, alarm.description());
                data.put(DEVICE_ID_STR, alarm.deviceId().toString());
                data.put(SOURCE, alarm.source().toString());
                long timeRaised = alarm.timeRaised();
                data.put(TIME_RAISED,
                        formatTime(timeRaised)
                );
                data.put(TIME_UPDATED, formatTime(alarm.timeUpdated()));
                data.put(TIME_CLEARED, formatTime(alarm.timeCleared()));
                data.put(SEVERITY, alarm.severity().toString());
            }
            log.debug("send ={}", rootNode);

            sendMessage(ALARM_TABLE_DETAIL_RESP, 0, rootNode);
        }
    }

    private static String formatTime(Long msSinceStartOfEpoch) {
        if (msSinceStartOfEpoch == null) {
            return "-";
        }
        return new TimeFormatter().format(new DateTime(msSinceStartOfEpoch));
    }


}
