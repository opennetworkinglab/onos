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
package org.onosproject.cpman.impl;

import org.apache.commons.lang3.ArrayUtils;
import org.onosproject.cpman.MetricsDatabase;
import org.rrd4j.ConsolFun;
import org.rrd4j.DsType;
import org.rrd4j.core.ArcDef;
import org.rrd4j.core.DsDef;
import org.rrd4j.core.FetchRequest;
import org.rrd4j.core.RrdBackendFactory;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;
import org.rrd4j.core.Sample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An implementation of control plane metrics back-end database.
 */
public final class DefaultMetricsDatabase implements MetricsDatabase {
    private static final Logger log = LoggerFactory.getLogger(DefaultMetricsDatabase.class);

    private String metricName;
    private String resourceName;
    private RrdDb rrdDb;
    private Sample sample;
    private static final long SECONDS_OF_DAY = 60L * 60L * 24L;
    private static final long SECONDS_OF_MINUTE = 60L;
    private static final ConsolFun CONSOL_FUNCTION = ConsolFun.LAST;
    private static final String NON_EXIST_METRIC = "Non-existing metric type.";
    private static final String INSUFFICIENT_DURATION = "Given duration less than one minute.";
    private static final String EXCEEDED_DURATION = "Given duration exceeds a day time.";

    /**
     * Constructs a metrics database using the given metric name and
     * round robin database.
     *
     * @param metricName  metric name
     * @param rrdDb       round robin database
     */
    private DefaultMetricsDatabase(String metricName, String resourceName, RrdDb rrdDb) {
        this.metricName = metricName;
        this.resourceName = resourceName;
        this.rrdDb = rrdDb;
    }

    @Override
    public String metricName() {
        return this.metricName;
    }

    @Override
    public String resourceName() {
        return this.resourceName;
    }

    @Override
    public void updateMetric(String metricType, double value) {
        updateMetric(metricType, value, System.currentTimeMillis() / 1000L);
    }

    @Override
    public void updateMetric(String metricType, double value, long time) {
        try {
            checkArgument(rrdDb.containsDs(metricType), NON_EXIST_METRIC);
            sample = rrdDb.createSample(time);
            sample.setValue(metricType, value);
            sample.update();
        } catch (IOException e) {
            log.error("Failed to update metric value due to {}", e);
        }
    }

    @Override
    public void updateMetrics(Map<String, Double> metrics) {
        updateMetrics(metrics, System.currentTimeMillis() / 1000L);
    }

    @Override
    public void updateMetrics(Map<String, Double> metrics, long time) {
        try {
            sample = rrdDb.createSample(time);
            metrics.forEach((k, v) -> {
                try {
                    checkArgument(rrdDb.containsDs(k), NON_EXIST_METRIC);
                    sample.setValue(k, v);
                } catch (IOException e) {
                    log.error("Failed to update metric value due to {}", e);
                }
            });
            sample.update();
        } catch (IOException e) {
            log.error("Failed to update metric values due to {}", e);
        }
    }

    @Override
    public double recentMetric(String metricType) {
        try {
            checkArgument(rrdDb.containsDs(metricType), NON_EXIST_METRIC);
            return rrdDb.getDatasource(metricType).getLastValue();
        } catch (IOException e) {
            log.error("Failed to obtain metric value due to {}", e);
            return 0D;
        }
    }

    @Override
    public double[] recentMetrics(String metricType, int duration, TimeUnit unit) {
        try {
            checkArgument(rrdDb.containsDs(metricType), NON_EXIST_METRIC);
            long endTime = rrdDb.getLastUpdateTime();
            long startTime = endTime - TimeUnit.SECONDS.convert(duration, unit);
            if (checkTimeRange(startTime, endTime)) {
                FetchRequest fr = rrdDb.createFetchRequest(CONSOL_FUNCTION, startTime, endTime);
                return arrangeDataPoints(fr.fetchData().getValues(metricType));
            } else {
                log.warn("Data projection is out-of-range");
                return new double[0];
            }
        } catch (IOException e) {
            log.error("Failed to obtain metric values due to {}", e);
            return new double[0];
        }
    }

    @Override
    public double minMetric(String metricType) {
        try {
            checkArgument(rrdDb.containsDs(metricType), NON_EXIST_METRIC);
            long endTime = rrdDb.getLastUpdateTime() - 1;
            long startTime = endTime - SECONDS_OF_DAY + 1;
            return minMetric(metricType, startTime, endTime);
        } catch (IOException e) {
            log.error("Failed to obtain metric value due to {}", e);
            return 0D;
        }
    }

    @Override
    public double maxMetric(String metricType) {
        try {
            checkArgument(rrdDb.containsDs(metricType), NON_EXIST_METRIC);
            long endTime = rrdDb.getLastUpdateTime();
            long startTime = endTime - SECONDS_OF_DAY;
            return maxMetric(metricType, startTime, endTime);
        } catch (IOException e) {
            log.error("Failed to obtain metric value due to {}", e);
            return 0D;
        }
    }

    @Override
    public double[] metrics(String metricType) {
        try {
            checkArgument(rrdDb.containsDs(metricType), NON_EXIST_METRIC);
            long endTime = rrdDb.getLastUpdateTime();
            long startTime = endTime - SECONDS_OF_DAY;
            return metrics(metricType, startTime, endTime);
        } catch (IOException e) {
            log.error("Failed to obtain metric values due to {}", e);
            return new double[0];
        }
    }

    @Override
    public double[] metrics(String metricType, long startTime, long endTime) {
        try {
            checkArgument(rrdDb.containsDs(metricType), NON_EXIST_METRIC);
            if (checkTimeRange(startTime, endTime)) {
                FetchRequest fr = rrdDb.createFetchRequest(CONSOL_FUNCTION, startTime, endTime);
                return arrangeDataPoints(fr.fetchData().getValues(metricType));
            } else {
                log.warn("Data projection is out-of-range");
                return new double[0];
            }
        } catch (IOException e) {
            log.error("Failed to obtain metric values due to {}", e);
            return new double[0];
        }
    }

    @Override
    public long lastUpdate(String metricType) {
        try {
            checkArgument(rrdDb.containsDs(metricType), NON_EXIST_METRIC);
            return rrdDb.getLastUpdateTime();
        } catch (IOException e) {
            log.error("Failed to obtain last update time due to {}", e);
            return 0L;
        }
    }

    // try to check whether projected time range is within a day
    private boolean checkTimeRange(long startTime, long endTime) {
        // check whether the given startTime and endTime larger than 1 minute
        checkArgument(endTime - startTime >= SECONDS_OF_MINUTE, INSUFFICIENT_DURATION);

        // check whether the given start time and endTime smaller than 1 day
        checkArgument(endTime - startTime <= SECONDS_OF_DAY, EXCEEDED_DURATION);
        return true;
    }

    // try to remove first and last data points
    private double[] arrangeDataPoints(double[] data) {
        return Arrays.copyOfRange(data, 1, data.length - 1);
    }

    // obtains maximum metric value among projected range
    private double maxMetric(String metricType, long startTime, long endTime) {
        double[] all = metrics(metricType, startTime, endTime);
        List list = Arrays.asList(ArrayUtils.toObject(all));
        return (double) Collections.max(list);
    }

    // obtains minimum metric value among projected range
    private double minMetric(String metricType, long startTime, long endTime) {
        double[] all = metrics(metricType, startTime, endTime);
        List list = Arrays.asList(ArrayUtils.toObject(all));
        return (double) Collections.min(list);
    }

    public static final class Builder implements MetricsDatabase.Builder {
        private static final int RESOLUTION_IN_SECOND = 60;
        private static final String STORING_METHOD = "MEMORY";
        private static final DsType SOURCE_TYPE = DsType.GAUGE;
        private static final String DB_PATH = "CPMAN";
        private static final ConsolFun CONSOL_FUNCTION = ConsolFun.LAST;
        private static final double MIN_VALUE = 0;
        private static final double MAX_VALUE = Double.NaN;
        private static final double XFF_VALUE = 0.2;
        private static final int STEP_VALUE = 1;
        private static final int ROW_VALUE = 60 * 24;
        private static final String METRIC_NAME_MSG = "Must specify a metric name.";
        private static final String RESOURCE_NAME_MSG = "Must specify a resource name.";
        private static final String METRIC_TYPE_MSG = "Must supply at least a metric type.";
        private static final String SPLITTER = "_";

        private RrdDb rrdDb;
        private RrdDef rrdDef;
        private List<DsDef> dsDefs;
        private String metricName;
        private String resourceName;

        public Builder() {
            // initialize data source definition list
            dsDefs = new ArrayList<>();
        }

        @Override
        public Builder withMetricName(String metric) {
            this.metricName = metric;
            return this;
        }

        @Override
        public MetricsDatabase.Builder withResourceName(String resource) {
            this.resourceName = resource;
            return this;
        }

        @Override
        public Builder addMetricType(String metricType) {
            dsDefs.add(defineSchema(metricType));
            return this;
        }

        @Override
        public MetricsDatabase build() {
            checkNotNull(metricName, METRIC_NAME_MSG);
            checkNotNull(resourceName, RESOURCE_NAME_MSG);
            checkArgument(!dsDefs.isEmpty(), METRIC_TYPE_MSG);

            // define the resolution of monitored metrics
            rrdDef = new RrdDef(DB_PATH + SPLITTER + metricName +
                     SPLITTER + resourceName, RESOLUTION_IN_SECOND);

            try {
                DsDef[] dsDefArray = new DsDef[dsDefs.size()];
                IntStream.range(0, dsDefs.size()).forEach(i -> dsDefArray[i] = dsDefs.get(i));

                rrdDef.addDatasource(dsDefArray);
                rrdDef.setStep(RESOLUTION_IN_SECOND);

                // raw archive, no aggregation is required
                ArcDef rawArchive = new ArcDef(CONSOL_FUNCTION, XFF_VALUE,
                        STEP_VALUE, ROW_VALUE);
                rrdDef.addArchive(rawArchive);

                // always store the metric data in memory...
                rrdDb = new RrdDb(rrdDef, RrdBackendFactory.getFactory(STORING_METHOD));
            } catch (IOException e) {
                log.warn("Failed to create a new round-robin database due to {}", e);
            }

            return new DefaultMetricsDatabase(metricName, resourceName, rrdDb);
        }

        private DsDef defineSchema(String metricType) {
            return new DsDef(metricType, SOURCE_TYPE, RESOLUTION_IN_SECOND,
                    MIN_VALUE, MAX_VALUE);
        }
    }
}
