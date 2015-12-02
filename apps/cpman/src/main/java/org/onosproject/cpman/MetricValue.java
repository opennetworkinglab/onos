/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.cpman;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Primitive Metric Value.
 */
public class MetricValue {

    private final long rate;
    private final long load;
    private final long count;

    /**
     * Constructor.
     *
     * @param rate rate
     * @param load load
     * @param count count
     */
    MetricValue(long rate, long load, long count) {
        this.rate = rate;
        this.load = load;
        this.count = count;
    }

    public long getRate() {
        return rate;
    }

    public long getLoad() {
        return load;
    }

    public long getCount() {
        return count;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof MetricValue) {
            MetricValue other = (MetricValue) obj;
            if (this.rate == other.rate &&
                    this.load == other.load &&
                    this.count == other.count) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        final int prime = 1004;
        int result = super.hashCode();
        result = prime * result + (int) this.rate;
        result = prime * result + (int) this.load;
        result = prime * result + (int) this.count;
        return result;
    }

    @Override
    public String toString() {
        return toStringHelper(getClass())
                .add("rate", Long.toHexString(rate))
                .add("load", Long.toHexString(load))
                .add("count", Long.toHexString(count)).toString();
    }
}
