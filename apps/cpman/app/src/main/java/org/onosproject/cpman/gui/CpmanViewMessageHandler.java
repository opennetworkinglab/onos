/*
 * Copyright 2016-present Open Networking Laboratory
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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.LocalDateTime;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cpman.ControlLoadSnapshot;
import org.onosproject.cpman.ControlMetricType;
import org.onosproject.cpman.ControlPlaneMonitorService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.chart.ChartModel;
import org.onosproject.ui.chart.ChartRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.onosproject.cpman.ControlResource.CONTROL_MESSAGE_METRICS;
import static org.onosproject.cpman.ControlResource.Type.CONTROL_MESSAGE;

/**
 * CpmanViewMessageHandler class implementation.
 */
public class CpmanViewMessageHandler extends UiMessageHandler {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String CPMAN_DATA_REQ = "cpmanDataRequest";
    private static final String CPMAN_DATA_RESP = "cpmanDataResponse";
    private static final String CPMANS = "cpmans";

    // TODO: we assume that server side always returns 60 data points
    // to feed 1 hour time slots, later this should make to be configurable
    private static final int NUM_OF_DATA_POINTS = 60;

    private static final int MILLI_CONV_UNIT = 1000;

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
            if (!Strings.isNullOrEmpty(uri)) {
                Map<ControlMetricType, Long[]> data = Maps.newHashMap();
                DeviceId deviceId = DeviceId.deviceId(uri);
                ClusterService cs = get(ClusterService.class);
                ControlPlaneMonitorService cpms = get(ControlPlaneMonitorService.class);

                if (cpms.availableResources(CONTROL_MESSAGE).contains(deviceId.toString())) {
                    LocalDateTime ldt = null;

                    try {
                        for (ControlMetricType cmt : CONTROL_MESSAGE_METRICS) {
                            ControlLoadSnapshot cls = cpms.getLoad(cs.getLocalNode().id(),
                                    cmt, NUM_OF_DATA_POINTS, TimeUnit.MINUTES,
                                    Optional.of(deviceId)).get();
                            data.put(cmt, ArrayUtils.toObject(cls.recent()));
                            if (ldt == null) {
                                ldt = new LocalDateTime(cls.time() * MILLI_CONV_UNIT);
                            }
                        }

                        for (int i = 0; i < NUM_OF_DATA_POINTS; i++) {
                            Map<String, Long> local = Maps.newHashMap();
                            for (ControlMetricType cmt : CONTROL_MESSAGE_METRICS) {
                                local.put(StringUtils.lowerCase(cmt.name()), data.get(cmt)[i]);
                            }

                            local.put(LABEL, ldt.minusMinutes(NUM_OF_DATA_POINTS - i).toDateTime().getMillis());

                            populateMetric(cm.addDataPoint(ldt.minusMinutes(NUM_OF_DATA_POINTS - i)
                                    .toDateTime().getMillis()), local);
                        }

                    } catch (InterruptedException | ExecutionException e) {
                        log.warn(e.getMessage());
                    }
                }
            } else {
                DeviceService ds = get(DeviceService.class);
                ds.getAvailableDevices();
            }
        }

        private void populateAllDevs(ChartModel.DataPoint dataPoint, Map<String, Long> data) {

        }

        private void populateMetric(ChartModel.DataPoint dataPoint,
                                    Map<String, Long> data) {
            data.forEach((k, v) -> dataPoint.data(k, v.doubleValue()));
        }
    }
}
