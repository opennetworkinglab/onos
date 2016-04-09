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
package org.onosproject.iptopology.api;

import static com.google.common.base.MoreObjects.toStringHelper;

import java.util.Objects;

/**
 * Represents Traffic engineering metrics.
  */
public class Metric {
    private final Integer metric;

    /**
     * Constructor to initialize its metric.
     *
     * @param metric can be TE metric or IGP metric or Prefix metric
     */
    public Metric(Integer metric) {
        this.metric = metric;
    }

    /**
     * Obtains traffic engineering metric.
     *
     * @return traffic engineering metric
     */
    public Integer metric() {
        return metric;
    }

    @Override
    public int hashCode() {
        return Objects.hash(metric);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof Metric) {
            Metric other = (Metric) obj;
            return Objects.equals(metric, other.metric);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("metric", metric)
                .toString();
    }
}