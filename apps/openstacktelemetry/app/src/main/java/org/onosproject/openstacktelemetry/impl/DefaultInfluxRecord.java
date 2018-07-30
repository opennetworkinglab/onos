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
package org.onosproject.openstacktelemetry.impl;

import com.google.common.base.Strings;
import org.onosproject.openstacktelemetry.api.InfluxRecord;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.onosproject.openstacktelemetry.api.Constants.DEFAULT_INFLUXDB_MEASUREMENT;

/**
 * A default implementation of influx record.
 *
 * @param <K> key of influx record
 * @param <V> value of influx record
 */
public final class DefaultInfluxRecord<K, V> implements InfluxRecord<K, V> {
    public static final String MEASUREMENT_NAME = DEFAULT_INFLUXDB_MEASUREMENT;
    private final K measurement;
    private final V flowInfos;

    protected DefaultInfluxRecord(K measurement, V flowInfos) {
        if (Strings.isNullOrEmpty((String) measurement)) {
            this.measurement = (K) MEASUREMENT_NAME;
        } else {
            this.measurement = measurement;
        }
        this.flowInfos = flowInfos;
    }

    @Override
    public K measurement() {
        return measurement;
    }

    @Override
    public V flowInfos() {
        return flowInfos;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof  DefaultInfluxRecord) {
            final DefaultInfluxRecord other = (DefaultInfluxRecord) obj;
            return Objects.equals(this.measurement, other.measurement) &&
                    Objects.equals(this.flowInfos, other.flowInfos);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(measurement, flowInfos);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("measurement", measurement)
                .add("flowInfos", flowInfos)
                .toString();
    }
}
