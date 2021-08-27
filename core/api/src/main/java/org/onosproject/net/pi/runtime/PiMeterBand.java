/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.net.pi.runtime;

import com.google.common.annotations.Beta;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Represents a band used within a meter.
 */
@Beta
public final class PiMeterBand {
    private final PiMeterBandType type;
    private final long rate;
    private final long burst;

    /**
     * Creates a band with rate and burst.
     *
     * @param type type of this band
     * @param rate  rate of this band
     * @param burst burst of this band
     */
    public PiMeterBand(PiMeterBandType type, long rate, long burst) {
        this.type = type;
        this.rate = rate;
        this.burst = burst;
    }

    /**
     * Returns the type of this band.
     *
     * @return type of this band
     */
    public PiMeterBandType type() {
        return type;
    }

    /**
     * Returns the rate of this band.
     *
     * @return rate of this band
     */
    public long rate() {
        return rate;
    }

    /**
     * Returns the burst of this band.
     *
     * @return burst of this band
     */
    public long burst() {
        return burst;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, rate, burst);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof PiMeterBand) {
            PiMeterBand that = (PiMeterBand) obj;
            return Objects.equals(type, that.type) &&
                    Objects.equals(rate, that.rate) &&
                    Objects.equals(burst, that.burst);

        }
        return false;
    }

    public String toString() {
        return toStringHelper(this)
                .add("type", type)
                .add("rate", rate)
                .add("burst", burst).toString();
    }
}
