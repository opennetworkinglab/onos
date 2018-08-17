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
package org.onosproject.influxdbmetrics;

import com.google.common.collect.BiMap;
import com.google.common.collect.EnumHashBiMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.CoreService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * A Metric retriever implementation for querying metrics from influxDB server.
 */
@Component(immediate = true, service = InfluxDbMetricsRetriever.class)
public class DefaultInfluxDbMetricsRetriever implements InfluxDbMetricsRetriever {

    private final Logger log = getLogger(getClass());

    private static final String DEFAULT_PROTOCOL = "http";
    private static final String DEFAULT_POLICY = "default";
    private static final String COLON_SEPARATOR = ":";
    private static final String SLASH_SEPARATOR = "//";
    private static final String BRACKET_START = "[";
    private static final String BRACKET_END = "]";
    private static final String METRIC_DELIMITER = ".";
    private static final int NUB_OF_DELIMITER = 3;

    private static final BiMap<TimeUnit, String> TIME_UNIT_MAP =
            EnumHashBiMap.create(TimeUnit.class);

    static {
        // key is TimeUnit enumeration type
        // value is influx database time unit keyword
        TIME_UNIT_MAP.put(TimeUnit.DAYS, "d");
        TIME_UNIT_MAP.put(TimeUnit.HOURS, "h");
        TIME_UNIT_MAP.put(TimeUnit.MINUTES, "m");
        TIME_UNIT_MAP.put(TimeUnit.SECONDS, "s");
    }

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    protected String database;

    InfluxDB influxDB;

    @Activate
    public void activate() {
        coreService.registerApplication("org.onosproject.influxdbmetrics");

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public void config(String address, int port, String database, String username, String password) {
        StringBuilder url = new StringBuilder();
        url.append(DEFAULT_PROTOCOL);
        url.append(COLON_SEPARATOR + SLASH_SEPARATOR);
        url.append(address);
        url.append(COLON_SEPARATOR);
        url.append(port);

        this.influxDB = InfluxDBFactory.connect(url.toString(), username, password);
        this.database = database;
    }

    @Override
    public Map<NodeId, Map<String, List<InfluxMetric>>> allMetrics(int period,
                                                             TimeUnit unit) {
        Map<NodeId, Set<String>> nameMap = allMetricNames();
        Map<NodeId, Map<String, List<InfluxMetric>>> metricsMap = Maps.newHashMap();

        nameMap.forEach((nodeId, metricNames) ->
                metricsMap.putIfAbsent(nodeId, metricsByNodeId(nodeId, period, unit))
        );

        return metricsMap;
    }

    @Override
    public Map<String, List<InfluxMetric>> metricsByNodeId(NodeId nodeId, int period,
                                                     TimeUnit unit) {
        Map<NodeId, Set<String>> nameMap = allMetricNames();
        Map<String, List<InfluxMetric>> map = Maps.newHashMap();

        nameMap.get(nodeId).forEach(metricName -> {
            List<InfluxMetric> value = metric(nodeId, metricName, period, unit);
            if (value != null) {
                map.putIfAbsent(metricName, value);
            }
        });

        return map;
    }

    @Override
    public Map<NodeId, List<InfluxMetric>> metricsByName(String metricName, int period,
                                                   TimeUnit unit) {
        Map<NodeId, List<InfluxMetric>> map = Maps.newHashMap();
        List<InfluxMetric> metrics = Lists.newArrayList();
        String queryPrefix = new StringBuilder()
                .append("SELECT m1_rate FROM")
                .append(database)
                .append(METRIC_DELIMITER)
                .append(quote(DEFAULT_POLICY))
                .append(METRIC_DELIMITER)
                .toString();
        String querySuffix = new StringBuilder()
                .append(" WHERE time > now() - ")
                .append(period)
                .append(unitString(unit))
                .toString();

        allMetricNames().keySet().forEach(nodeId -> {
            String queryString = new StringBuilder()
                    .append(queryPrefix)
                    .append(quote(nodeId + METRIC_DELIMITER + metricName))
                    .append(querySuffix)
                    .toString();
            Query query = new Query(queryString, database);
            List<QueryResult.Result> results = influxDB.query(query).getResults();

            if (results != null && results.get(0) != null
                    && results.get(0).getSeries() != null) {

                results.get(0).getSeries().get(0).getValues().forEach(value ->
                    metrics.add(new DefaultInfluxMetric.Builder()
                            .time((String) value.get(0))
                            .oneMinRate((Double) value.get(1))
                            .build()));
                map.putIfAbsent(nodeId, metrics);
            }
        });

        return map;
    }

    @Override
    public List<InfluxMetric> metric(NodeId nodeId, String metricName,
                               int period, TimeUnit unit) {
        List<InfluxMetric> metrics = Lists.newArrayList();
        String queryString = new StringBuilder()
                .append("SELECT m1_rate FROM ")
                .append(database)
                .append(METRIC_DELIMITER)
                .append(quote(DEFAULT_POLICY))
                .append(METRIC_DELIMITER)
                .append(quote(nodeId + METRIC_DELIMITER + metricName))
                .append(" WHERE time > now() - ")
                .append(period)
                .append(unitString(unit))
                .toString();

        Query query = new Query(queryString, database);
        List<QueryResult.Result> results = influxDB.query(query).getResults();

        if (results != null && results.get(0) != null
                && results.get(0).getSeries() != null) {

            results.get(0).getSeries().get(0).getValues().forEach(value ->
                    metrics.add(new DefaultInfluxMetric.Builder()
                            .time((String) value.get(0))
                            .oneMinRate((Double) value.get(1))
                            .build()));
            return metrics;
        }

        return null;
    }

    @Override
    public Map<NodeId, Map<String, InfluxMetric>> allMetrics() {
        Map<NodeId, Set<String>> nameMap = allMetricNames();
        Map<NodeId, Map<String, InfluxMetric>> metricsMap = Maps.newHashMap();

        nameMap.forEach((nodeId, metricNames) ->
            metricsMap.putIfAbsent(nodeId, metricsByNodeId(nodeId))
        );

        return metricsMap;
    }

    @Override
    public Map<String, InfluxMetric> metricsByNodeId(NodeId nodeId) {
        Map<NodeId, Set<String>> nameMap = allMetricNames();
        Map<String, InfluxMetric> map = Maps.newHashMap();

        nameMap.get(nodeId).forEach(metricName -> {
            InfluxMetric value = metric(nodeId, metricName);
            if (value != null) {
                map.putIfAbsent(metricName, value);
            }
        });

        return map;
    }

    @Override
    public Map<NodeId, InfluxMetric> metricsByName(String metricName) {
        Map<NodeId, InfluxMetric> map = Maps.newHashMap();
        String queryPrefix = new StringBuilder()
                .append("SELECT m1_rate FROM")
                .append(database)
                .append(METRIC_DELIMITER)
                .append(quote(DEFAULT_POLICY))
                .append(METRIC_DELIMITER)
                .toString();
        String querySuffix = new StringBuilder()
                .append(" LIMIT 1")
                .toString();

        allMetricNames().keySet().forEach(nodeId -> {
            String queryString = new StringBuilder()
                    .append(queryPrefix)
                    .append(quote(nodeId + METRIC_DELIMITER + metricName))
                    .append(querySuffix)
                    .toString();
            Query query = new Query(queryString, database);
            List<QueryResult.Result> results = influxDB.query(query).getResults();

            if (results != null && results.get(0) != null
                    && results.get(0).getSeries() != null) {
                InfluxMetric metric = new DefaultInfluxMetric.Builder()
                        .time((String) results.get(0).getSeries().get(0).getValues().get(0).get(0))
                        .oneMinRate((Double) results.get(0).getSeries().get(0)
                                .getValues().get(0).get(1)).build();
                map.putIfAbsent(nodeId, metric);
            }
        });

        return map;
    }

    @Override
    public InfluxMetric metric(NodeId nodeId, String metricName) {
        String queryString = new StringBuilder()
                .append("SELECT m1_rate FROM ")
                .append(database)
                .append(METRIC_DELIMITER)
                .append(quote(DEFAULT_POLICY))
                .append(METRIC_DELIMITER)
                .append(quote(nodeId + METRIC_DELIMITER + metricName))
                .append(" LIMIT 1")
                .toString();

        Query query = new Query(queryString, database);
        List<QueryResult.Result> results = influxDB.query(query).getResults();

        if (results != null && results.get(0) != null
                && results.get(0).getSeries() != null) {
            return new DefaultInfluxMetric.Builder()
                    .time((String) results.get(0).getSeries().get(0).getValues().get(0).get(0))
                    .oneMinRate((Double) results.get(0).getSeries().get(0)
                            .getValues().get(0).get(1)).build();
        }

        return null;
    }

    private String unitString(TimeUnit unit) {
        return TIME_UNIT_MAP.get(unit) == null ? "h" : TIME_UNIT_MAP.get(unit);
    }

    private String quote(String str) {
        return "\"" + str + "\"";
    }

    /**
     * Returns all metric names that bound with node identification.
     *
     * @return all metric names
     */
    protected Map<NodeId, Set<String>> allMetricNames() {
        Map<NodeId, Set<String>> metricNameMap = Maps.newHashMap();
        Query query = new Query("SHOW MEASUREMENTS", database);
        List<QueryResult.Result> results = influxDB.query(query).getResults();
        List<List<Object>> rawMetricNames = results.get(0).getSeries().get(0).getValues();

        rawMetricNames.forEach(rawMetricName -> {
            String nodeIdStr = getNodeId(strip(rawMetricName.toString()));

            if (nodeIdStr != null) {
                NodeId nodeId = NodeId.nodeId(nodeIdStr);
                String metricName = getMetricName(strip(rawMetricName.toString()));

                if (!metricNameMap.containsKey(nodeId)) {
                    metricNameMap.putIfAbsent(nodeId, Sets.newHashSet());
                }

                if (metricName != null) {
                    metricNameMap.get(nodeId).add(metricName);
                }
            }
        });

        return metricNameMap;
    }

    /**
     * Strips special bracket from the full name.
     *
     * @param fullName full name
     * @return bracket stripped string
     */
    private String strip(String fullName) {
        return StringUtils.strip(StringUtils.strip(fullName, BRACKET_START), BRACKET_END);
    }

    /**
     * Returns metric name from full name.
     * The elements in full name is split by using '.';
     * We assume that the metric name always comes after the last three '.'
     *
     * @param fullName full name
     * @return metric name
     */
    private String getMetricName(String fullName) {
        int index = StringUtils.lastOrdinalIndexOf(fullName,
                METRIC_DELIMITER, NUB_OF_DELIMITER);
        if (index != -1) {
            return StringUtils.substring(fullName, index + 1);
        } else {
            log.warn("Database {} contains malformed metric name.", database);
            return null;
        }
    }

    /**
     * Returns node id from full name.
     * The elements in full name is split by using '.';
     * We assume that the node id always comes before the last three '.'
     *
     * @param fullName full name
     * @return node id
     */
    private String getNodeId(String fullName) {
        int index = StringUtils.lastOrdinalIndexOf(fullName,
                METRIC_DELIMITER, NUB_OF_DELIMITER);
        if (index != -1) {
            return StringUtils.substring(fullName, 0, index);
        } else {
            log.warn("Database {} contains malformed node id.", database);
            return null;
        }
    }
}
