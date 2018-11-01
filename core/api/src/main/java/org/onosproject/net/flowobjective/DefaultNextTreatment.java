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
package org.onosproject.net.flowobjective;

import org.onosproject.net.flow.TrafficTreatment;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Represents a next action specified by traffic treatment.
 */
public final class DefaultNextTreatment implements NextTreatment {
    private final TrafficTreatment treatment;

    private DefaultNextTreatment(TrafficTreatment treatment) {
        this.treatment = treatment;
    }

    /**
     * Returns traffic treatment.
     *
     * @return traffic treatment.
     */
    public TrafficTreatment treatment() {
        return treatment;
    }

    /**
     * Returns an instance of DefaultNextTreatment with given traffic treatment.
     *
     * @param treatment traffic treatment
     * @return an instance of DefaultNextTreatment
     */
    public static DefaultNextTreatment of(TrafficTreatment treatment) {
        return new DefaultNextTreatment(treatment);
    }

    @Override
    public Type type() {
        return Type.TREATMENT;
    }

    @Override
    public int hashCode() {
        return Objects.hash(treatment);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultNextTreatment) {
            final DefaultNextTreatment other = (DefaultNextTreatment) obj;
            return Objects.equals(this.treatment, other.treatment);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("treatment", treatment)
                .toString();
    }
}
