/*
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.tetopology.management.api.link;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * Represents the common attributes of a TE path or segment.
 */
public class TePathAttributes {
    private final Long cost;
    private final Long delay;
    private final List<Long> srlgs;

    /**
     * Creates an instance of TE path attributes.
     *
     * @param cost  the path's cost
     * @param delay the path's delay
     * @param srlgs the path's shared risk link groups (SRLGs)
     */
    public TePathAttributes(Long cost, Long delay, List<Long> srlgs) {
        this.cost = cost;
        this.delay = delay;
        this.srlgs = srlgs != null ? Lists.newArrayList(srlgs) : null;
    }

    /**
     * Creates an instance of TE path attributes based on a given TE link.
     *
     * @param link the TE link
     */
    public TePathAttributes(TeLink link) {
        this.cost = link.cost();
        this.delay = link.delay();
        this.srlgs = link.srlgs() != null ?
                Lists.newArrayList(link.srlgs()) : null;
    }

    /**
     * Returns the path cost.
     *
     * @return the cost
     */
    public Long cost() {
        return cost;
    }

    /**
     * Returns the path delay.
     *
     * @return the delay
     */
    public Long delay() {
        return delay;
    }

    /**
     * Returns the path SRLG values.
     *
     * @return the srlgs
     */
    public List<Long> srlgs() {
        if (srlgs == null) {
            return null;
        }
        return ImmutableList.copyOf(srlgs);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(cost, delay, srlgs);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof TePathAttributes) {
            TePathAttributes that = (TePathAttributes) object;
            return Objects.equal(cost, that.cost) &&
                    Objects.equal(delay, that.delay) &&
                    Objects.equal(srlgs, that.srlgs);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("cost", cost)
                .add("delay", delay)
                .add("srlgs", srlgs)
                .toString();
    }

}
