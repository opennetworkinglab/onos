/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.cpman.gui;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.cpman.ControlLoadSnapshot;
import org.onosproject.cpman.ControlMetricType;
import org.onosproject.cpman.ControlPlaneMonitorService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.chart.ChartModel;
import org.onosproject.ui.chart.ChartRequestHandler;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;

import static org.onosproject.cpman.ControlResource.CONTROL_MESSAGE_METRICS;
import static org.onosproject.cpman.ControlResource.Type.CONTROL_MESSAGE;

/**
 * Message handler for control plane monitoring view related messages.
 */
public class CpmanViewMessageHandler extends UiMessageHandler {

    private static final String CPMAN_DATA_REQ = "cpmanDataRequest";
    private static final String CPMAN_DATA_RESP = "cpmanDataResponse";
    private static final String CPMANS = "cpmans";

    private static final String DEVICE_IDS = "deviceIds";

    // TODO: we assume that server side always returns 20 data points
    // to feed 20 minutes time slots, later this should make to be configurable
    private static final int NUM_OF_DATA_POINTS = 20;

    private static final int MILLI_CONV_UNIT = 1000;

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ISO_LOCAL_TIME;

    private long timestamp = 0L;

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(
                new ControlMessageRequest()
        );
    }

    private final class ControlMessageRequest extends ChartRequestHandler {

        private ControlMessageRequest() {
            super(CPMAN_DATA_REQ, CPMAN_DATA_RESP, CPMANS);
        }

        @Override
        protected String[] getSeries() {
            return CONTROL_MESSAGE_METRICS.stream().map(type ->
                    StringUtils.lowerCase(type.name())).toArray(String[]::new);
        }

        @Override
        protected void populateChart(ChartModel cm, ObjectNode payload) {
            String uri = string(payload, "devId");
            ControlPlaneMonitorService cpms = get(ControlPlaneMonitorService.class);
            ClusterService cs = get(ClusterService.class);
            DeviceService ds = get(DeviceService.class);
            NodeId localNodeId = cs.getLocalNode().id();

            if (!Strings.isNullOrEmpty(uri)) {
                DeviceId deviceId = DeviceId.deviceId(uri);
                if (cpms.availableResourcesSync(localNodeId, CONTROL_MESSAGE).contains(deviceId.toString())) {
                    Map<ControlMetricType, Long[]> data = generateMatrix(cpms, cs, deviceId);
                    @SuppressWarnings("FromTemporalAccessor")
                    LocalDateTime ldt = LocalDateTime.from(Instant.ofEpochMilli(timestamp * MILLI_CONV_UNIT));

                    populateMetrics(cm, data, ldt, NUM_OF_DATA_POINTS);

                    Set<DeviceId> deviceIds = Sets.newHashSet();
                    ds.getAvailableDevices().forEach(device -> deviceIds.add(device.id()));
                    attachDeviceList(cm, deviceIds);
                }
            } else {
                Set<String> deviceIds = cpms.availableResourcesSync(localNodeId, CONTROL_MESSAGE);
                for (String deviceId : deviceIds) {
                    Map<ControlMetricType, Long> data =
                            populateDeviceMetrics(cpms, cs, DeviceId.deviceId(deviceId));
                    Map<String, Object> local = Maps.newHashMap();
                    for (ControlMetricType cmt : CONTROL_MESSAGE_METRICS) {
                        local.put(StringUtils.lowerCase(cmt.name()), data.get(cmt));
                    }

                    local.put(LABEL, deviceId);
                    populateMetric(cm.addDataPoint(deviceId), local);
                }
            }
        }

        private Map<ControlMetricType, Long> populateDeviceMetrics(ControlPlaneMonitorService cpms,
                                                                   ClusterService cs, DeviceId deviceId) {
            Map<ControlMetricType, Long> data = Maps.newHashMap();
            for (ControlMetricType cmt : CONTROL_MESSAGE_METRICS) {
                ControlLoadSnapshot cls = cpms.getLoadSync(cs.getLocalNode().id(),
                        cmt, NUM_OF_DATA_POINTS, TimeUnit.MINUTES, Optional.of(deviceId));
                data.put(cmt, Math.round(LongStream.of(cls.recent()).average().getAsDouble()));
                timestamp = cls.time();
            }
            return data;
        }

        private Map<ControlMetricType, Long[]> generateMatrix(ControlPlaneMonitorService cpms,
                                                              ClusterService cs, DeviceId deviceId) {
            Map<ControlMetricType, Long[]> data = Maps.newHashMap();
            for (ControlMetricType cmt : CONTROL_MESSAGE_METRICS) {
                ControlLoadSnapshot cls = cpms.getLoadSync(cs.getLocalNode().id(),
                        cmt, NUM_OF_DATA_POINTS, TimeUnit.MINUTES, Optional.of(deviceId));

                // TODO: in some cases, the number of returned data set is
                // less than what we expected (expected -1)
                // As a workaround, we simply fill the slot with 0 values,
                // such a bug should be fixed with updated RRD4J lib...
                data.put(cmt, ArrayUtils.toObject(fillData(cls.recent(), NUM_OF_DATA_POINTS)));
                timestamp = cls.time();
            }
            return data;
        }

        private long[] fillData(long[] origin, int expected) {
            if (origin.length == expected) {
                return origin;
            } else {
                long[] filled = new long[expected];
                for (int i = 0; i < expected; i++) {
                    if (i == 0) {
                        filled[i] = 0;
                    } else {
                        filled[i] = origin[i - 1];
                    }
                }
                return filled;
            }
        }

        // FIXME using local time in timestamps likely to be sign of problem
        private void populateMetrics(ChartModel cm,
                                     Map<ControlMetricType, Long[]> data,
                                     LocalDateTime time, int numOfDp) {
            for (int i = 0; i < numOfDp; i++) {
                Map<String, Object> local = Maps.newHashMap();
                for (ControlMetricType cmt : CONTROL_MESSAGE_METRICS) {
                    local.put(StringUtils.lowerCase(cmt.name()), data.get(cmt)[i]);
                }

                String calculated = time.minusMinutes((long) numOfDp - i).format(TIME_FORMAT);

                local.put(LABEL, calculated);
                populateMetric(cm.addDataPoint(calculated), local);
            }
        }

        private void populateMetric(ChartModel.DataPoint dataPoint,
                                    Map<String, Object> data) {
            data.forEach(dataPoint::data);
        }

        private void attachDeviceList(ChartModel cm, Set<DeviceId> deviceIds) {
            ArrayNode array = arrayNode();
            deviceIds.forEach(id -> array.add(id.toString()));
            cm.addAnnotation(DEVICE_IDS, array);
        }
    }
}
