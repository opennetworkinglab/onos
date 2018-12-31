/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.openstacktelemetry.gui;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.TpPort;
import org.onosproject.net.Host;
import org.onosproject.net.host.HostService;
import org.onosproject.openstacktelemetry.api.FlowInfo;
import org.onosproject.openstacktelemetry.api.StatsFlowRule;
import org.onosproject.openstacktelemetry.api.StatsFlowRuleAdminService;
import org.onosproject.openstacktelemetry.api.DefaultStatsFlowRule;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiConnection;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.chart.ChartModel;
import org.onosproject.ui.chart.ChartRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import static org.onosproject.net.HostId.hostId;
import static org.onosproject.openstacktelemetry.util.OpenstackTelemetryUtil.getProtocolTypeFromString;

/**
 * Message handler for Openstack Telemetry view related messages.
 */
public class OpensteckTelemetryViewMessageHandler extends UiMessageHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    // Chart View
    private static final String OST_DATA_REQ = "openstacktelemetryDataRequest";
    private static final String OST_DATA_RESP = "openstacktelemetryDataResponse";

    // Network Topology
    private static final String OST_IS_ACTIVATED_REQ = "openstackFlowStatsIsActivatedRequest";
    private static final String OST_IS_ACTIVATED_RESP = "openstackFlowStatsIsActivatedResponse";
    private static final String OST_FLOW_STATS_CREATE_REQ = "openstackFlowStatsCreateRequest";
    private static final String OST_FLOW_STATS_CREATE_RESP = "openstackFlowStatsCreateResponse";

    private static final String OSTS = "openstacktelemetrys";
    private static final String ANNOT_FLOW_IDS = "flowIds";
    private static final String ANNOT_PERIOD_OPTIONS = "periodOptions";

    private static final String SOURCE = "src";
    private static final String DESTINATION = "dst";
    private static final String SOURCE_IP = "srcIp";
    private static final String DESTINATION_IP = "dstIp";
    private static final String SOURCE_TRANSPORT_PORT = "srcPort";
    private static final String DESTINATION_TRANSPORT_PORT = "dstPort";
    private static final String SOURCE_HOST_NAME = "srcName";
    private static final String DESTINATION_HOST_NAME = "dstName";
    private static final String IP_PROTOCOL = "ipProto";
    private static final String IP_PROTOCOL_LIST = "ipProtoList";

    private static final String RESULT = "result";
    private static final String RESULT_OK = "OK";
    private static final String VALUE = "value";
    private static final String SUCCESS = "Success";
    private static final String FAILED = "Failed";
    private static final String FAILED_TO_CREATE_FLOW_STATS = "Failed to create a flow rule for statistics";

    private static final String[] IP_PROTOCOL_ARRAY = {"TCP", "UDP", "ANY"};

    private static final String STAT_CURR_ACC_PACKET = "curr_acc_packet";
    private static final String STAT_PREV_ACC_PACKET = "prev_acc_packet";
    private static final String STAT_CURR_ACC_BYTE = "curr_acc_byte";
    private static final String STAT_PREV_ACC_BYTE = "prev_acc_byte";
    private static final String STAT_ERROR_PACKET = "error_packet";
    private static final String STAT_DROP_PACKET = "drop_packet";

    private static final String CHART_TIME_FORMAT = "HH:mm:ss";

    // JSON node name
    private static final String JSON_NODE_FLOW = "flowOpt";
    private static final String JSON_NODE_PERIOD = "periodOpt";

    private static final String DEFAULT_PERIOD_OPTION_1MIN = "1 MIN";

    // Statistics period of statistics chart
    private static final Map<String, Integer> PERIOD_OPTION_MAP =
                            ImmutableMap.<String, Integer>builder()
                            .put(DEFAULT_PERIOD_OPTION_1MIN, 12)
                            .put("3 MIN", 36)
                            .put("5 MIN", 60)
                            .put("30 MIN", 360)
                            .put("1 HOUR", 720)
                            .put("2 HOUR", 1440)
                            .put("6 HOUR", 4320)
                            .put("24 HOUR", 17280)
                            .build();

    private HostService hostService;
    private StatsFlowRuleAdminService statsFlowRuleService;

    @Override
    public void init(UiConnection connection, ServiceDirectory directory) {
        super.init(connection, directory);

        hostService = directory.get(HostService.class);
        statsFlowRuleService = directory.get(StatsFlowRuleAdminService.class);
    }

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(
                new ChartControlMessageRequest(),
                new FlowStatsIsActivatedRequestHandler(),
                new FlowStatsCreateRequestHandler()
        );
    }

    /* Handler for Line-Chart View */
    private final class ChartControlMessageRequest extends ChartRequestHandler {
        private Map<String, Queue<FlowInfo>> flowInfoMap = null;
        private String currentFlowKey = null;
        private String currentPeriod = DEFAULT_PERIOD_OPTION_1MIN;

        private ChartControlMessageRequest() {
            super(OST_DATA_REQ, OST_DATA_RESP, OSTS);
        }

        @Override
        protected String[] getSeries() {
            String[] series = {STAT_CURR_ACC_PACKET, STAT_PREV_ACC_PACKET,
                    STAT_CURR_ACC_BYTE, STAT_PREV_ACC_BYTE, STAT_ERROR_PACKET, STAT_DROP_PACKET};
            return series;
        }

        @Override
        protected void populateChart(ChartModel cm, ObjectNode payload) {
            if (statsFlowRuleService == null) {
                statsFlowRuleService = get(StatsFlowRuleAdminService.class);
            }
            if (flowInfoMap == null) {
                flowInfoMap = statsFlowRuleService.getFlowInfoMap();
            }

            String flowKey = string(payload, JSON_NODE_FLOW);
            String period = string(payload, JSON_NODE_PERIOD);

            if (!Strings.isNullOrEmpty(flowKey) || !Strings.isNullOrEmpty(period)) {
                if (!Strings.isNullOrEmpty(flowKey)) {
                    currentFlowKey = flowKey;
                }
                if (!Strings.isNullOrEmpty(period)) {
                    currentPeriod = period;
                }

                Queue<FlowInfo> flowInfoQ = flowInfoMap.get(currentFlowKey);

                if (flowInfoQ == null) {
                    log.warn("No such flow key {}", currentFlowKey);
                    return;
                } else {
                    populateMetrics(cm, flowInfoQ);
                    attachFlowList(cm);
                    attachPeriodList(cm);
                }
            } else {
                flowInfoMap.keySet().forEach(key -> {
                    Queue<FlowInfo> flowInfoQ = flowInfoMap.get(key);
                    if (flowInfoQ == null) {
                        log.warn("Key {} is not found in FlowInfoMap", key);
                        return;
                    }
                    FlowInfo flowInfo = getLatestFlowInfo(flowInfoQ);

                    Map<String, Object> local = Maps.newHashMap();
                    local.put(LABEL, key);
                    local.put(STAT_CURR_ACC_PACKET, flowInfo.statsInfo().currAccPkts());
                    local.put(STAT_PREV_ACC_PACKET, flowInfo.statsInfo().prevAccPkts());
                    local.put(STAT_CURR_ACC_BYTE, flowInfo.statsInfo().currAccBytes());
                    local.put(STAT_PREV_ACC_BYTE, flowInfo.statsInfo().prevAccBytes());
                    local.put(STAT_ERROR_PACKET, flowInfo.statsInfo().errorPkts());
                    local.put(STAT_DROP_PACKET, flowInfo.statsInfo().dropPkts());

                    populateMetric(cm.addDataPoint(key), local);
                });
            }
        }

        private void populateMetrics(ChartModel cm, Queue<FlowInfo> flowInfoQ) {
            FlowInfo[] flowInfos = flowInfoQ.toArray(new FlowInfo[flowInfoQ.size()]);
            SimpleDateFormat form = new SimpleDateFormat(CHART_TIME_FORMAT);
            int timeOffset = 0;
            Integer dataPointCount = PERIOD_OPTION_MAP.get(currentPeriod);
            if (dataPointCount != null) {
                timeOffset = flowInfos.length - (int) dataPointCount;
                if (timeOffset < 0) {
                    timeOffset = 0;
                }
            }

            for (int idx = timeOffset; idx < flowInfos.length; idx++) {
                Map<String, Object> local = Maps.newHashMap();
                local.put(LABEL, form.format(new Date(flowInfos[idx].statsInfo().fstPktArrTime())));
                local.put(STAT_CURR_ACC_PACKET, flowInfos[idx].statsInfo().currAccPkts());
                local.put(STAT_PREV_ACC_PACKET, flowInfos[idx].statsInfo().prevAccPkts());
                local.put(STAT_CURR_ACC_BYTE, flowInfos[idx].statsInfo().currAccBytes());
                local.put(STAT_PREV_ACC_BYTE, flowInfos[idx].statsInfo().prevAccBytes());
                local.put(STAT_ERROR_PACKET, flowInfos[idx].statsInfo().errorPkts());
                local.put(STAT_DROP_PACKET, flowInfos[idx].statsInfo().dropPkts());
                populateMetric(cm.addDataPoint(flowInfos[idx].uniqueFlowInfoKey()), local);
            }
        }

        private void populateMetric(ChartModel.DataPoint dataPoint,
                                    Map<String, Object> data) {
            data.forEach(dataPoint::data);
        }

        private void attachFlowList(ChartModel cm) {
            ArrayNode array = arrayNode();
            flowInfoMap.keySet().forEach(key -> {
                array.add(key);
            });
            cm.addAnnotation(ANNOT_FLOW_IDS, array);
        }

        private void attachPeriodList(ChartModel cm) {
            ArrayNode array = arrayNode();
            PERIOD_OPTION_MAP.keySet().forEach(period -> {
                array.add(period);
            });
            cm.addAnnotation(ANNOT_PERIOD_OPTIONS, array);
        }

        private FlowInfo getLatestFlowInfo(Queue<FlowInfo> flowInfoQ) {
            FlowInfo[] flowInfos = flowInfoQ.toArray(new FlowInfo[flowInfoQ.size()]);
            return flowInfos[flowInfos.length - 1];
        }
    }

    /* Handler for Network Topology View */
    private final class FlowStatsIsActivatedRequestHandler extends RequestHandler {

        private FlowStatsIsActivatedRequestHandler() {
            super(OST_IS_ACTIVATED_REQ);
        }

        @Override
        public void process(ObjectNode payload) {
            String srcId = string(payload, SOURCE, null);
            String dstId = string(payload, DESTINATION, null);

            log.debug("Input from GUI: {}", payload.toString());
            if (srcId != null && dstId != null) {
                Host srcHost = hostService.getHost(hostId(srcId));
                Host dstHost = hostService.getHost(hostId(dstId));

                if (srcHost != null && dstHost != null) {
                    ArrayNode ipProtos = arrayNode();
                    String srcHostName = ipForHost(srcHost);
                    String dstHostName = ipForHost(dstHost);

                    for (String proto : IP_PROTOCOL_ARRAY) {
                        ipProtos.add(proto);
                    }

                    payload.put(SOURCE_HOST_NAME, srcHostName);
                    payload.put(DESTINATION_HOST_NAME, dstHostName);
                    payload.put(IP_PROTOCOL_LIST, ipProtos);

                    log.debug("Output to GUI: {}", payload.toString());

                    sendMessage(OST_IS_ACTIVATED_RESP, payload);
                }
            }
        }

        // Returns the first of the given host's set of IP addresses as a string.
        private String ipForHost(Host host) {
            Set<IpAddress> ipAddresses = host.ipAddresses();
            Iterator<IpAddress> it = ipAddresses.iterator();
            return it.hasNext() ? it.next().toString() + "/32" : "unknown";
        }
    }

    private final class FlowStatsCreateRequestHandler extends RequestHandler {
        private String srcIp;
        private String dstIp;
        private String ipProto;
        private String srcTpPort;
        private String dstTpPort;
        private ObjectNode result = objectNode();

        private FlowStatsCreateRequestHandler() {
            super(OST_FLOW_STATS_CREATE_REQ);
        }

        @Override
        public void process(ObjectNode payload) {
            srcIp = string(payload, SOURCE_IP, null);
            dstIp = string(payload, DESTINATION_IP, null);
            ipProto = string(payload, IP_PROTOCOL, null);
            srcTpPort = string(payload, SOURCE_TRANSPORT_PORT, null);
            dstTpPort = string(payload, DESTINATION_TRANSPORT_PORT, null);

            log.debug("[GUI input] srcIp:{} dstIp:{} ipPro:{} srcTpPort:{} dstTpPort:{}",
                     srcIp, dstIp, ipProto, srcTpPort, dstTpPort);

            try {
                StatsFlowRule statsFlowRule = DefaultStatsFlowRule.builder()
                        .srcIpPrefix(IpPrefix.valueOf(srcIp))
                        .dstIpPrefix(IpPrefix.valueOf(dstIp))
                        .srcTpPort(TpPort.tpPort(Integer.valueOf(srcTpPort)))
                        .dstTpPort(TpPort.tpPort(Integer.valueOf(dstTpPort)))
                        .ipProtocol(getProtocolTypeFromString(ipProto))
                        .build();
                statsFlowRuleService.createStatFlowRule(statsFlowRule);
                result.put(RESULT, SUCCESS);
                result.put(VALUE, RESULT_OK);
            } catch (Exception ex) {
                log.warn(FAILED_TO_CREATE_FLOW_STATS + " : " + ex.toString());
                result.put(RESULT, FAILED);
                result.put(VALUE, FAILED_TO_CREATE_FLOW_STATS);
            }
            sendMessage(OST_FLOW_STATS_CREATE_RESP, result);
        }
    }
}
