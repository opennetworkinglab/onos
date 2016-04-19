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
package org.onosproject.cpman;

import com.google.common.base.MoreObjects;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * A container class that is used to wrap the control metric response.
 */
public class ControlLoadSnapshot {

    private final long latest;
    private final long average;
    private final long time;

    /**
     * Instantiates a new control metric response with given latest, average, time.
     *
     * @param latest latest value of control metric
     * @param average average value of control metric
     * @param time last logging time fo control metric
     */
    public ControlLoadSnapshot(long latest, long average, long time) {
        this.latest = latest;
        this.average = average;
        this.time = time;
    }

    /**
     * Returns latest value of control metric.
     *
     * @return latest value of control metric
     */
    public long latest() {
        return latest;
    }

    /**
     * Returns last logging time of control metric.
     *
     * @return last logging time of control metric
     */
    public long time() {
        return time;
    }

    /**
     * Returns average value of control metric.
     *
     * @return average value of control metric
     */
    public long average() {
        return average;
    }

    @Override
    public int hashCode() {
        return Objects.hash(latest, average, time);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ControlLoadSnapshot) {
            final ControlLoadSnapshot other = (ControlLoadSnapshot) obj;
            return Objects.equals(this.latest, other.latest) &&
                    Objects.equals(this.average, other.average) &&
                    Objects.equals(this.time, other.time);
        }
        return false;
    }

    @Override
    public String toString() {
        MoreObjects.ToStringHelper helper;
        helper = toStringHelper(this)
                .add("latest", latest)
                .add("average", average)
                .add("time", time);
        return helper.toString();
    }
}
