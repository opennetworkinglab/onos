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

import org.onosproject.cpman.ControlLoad;
import org.onosproject.cpman.ControlMetricType;
import org.onosproject.cpman.MetricsDatabase;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * An implementation of control plane load.
 */
public class DefaultControlLoad implements ControlLoad {

    private final MetricsDatabase mdb;
    private final ControlMetricType type;

    /**
     * Constructs a control load using the given metrics database and
     * control metric type.
     *
     * @param mdb  metrics database
     * @param type control metric type
     */
    public DefaultControlLoad(MetricsDatabase mdb, ControlMetricType type) {
        this.mdb = mdb;
        this.type = type;
    }

    @Override
    public long average(int duration, TimeUnit unit) {
        return (long) Arrays.stream(recent(duration, unit)).average().getAsDouble();
    }

    @Override
    public long average() {
        return (long) Arrays.stream(all()).average().getAsDouble();
    }

    @Override
    public long rate() {
        return 0;
    }

    @Override
    public long latest() {
        return (long) mdb.recentMetric(type.toString());
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public long time() {
        return mdb.lastUpdate(type.toString());
    }

    @Override
    public long[] recent(int duration, TimeUnit unit) {
        return doubleToLong(mdb.recentMetrics(type.toString(), duration, unit));
    }

    @Override
    public long[] all() {
        return doubleToLong(mdb.metrics(type.toString()));
    }

    private double nanToZero(double d) {
        return Double.isNaN(d) ? 0D : d;
    }

    private long[] doubleToLong(double[] array) {
        final long[] longArray = new long[array.length];
        IntStream.range(0, array.length).forEach(i ->
                longArray[i] = (long) nanToZero(array[i]));

        return longArray;
    }
}
