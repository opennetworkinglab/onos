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
package org.onosproject.l2lb.api;


import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Represents L2 load balancer event data.
 */
public class L2LbData {

    // We exchange only id and nextid in the events
    private L2LbId l2LbId;
    private int nextId;

    /**
     * Constructs a L2 load balancer data.
     *
     * @param l2LbId L2 load balancer ID
     */
    public L2LbData(L2LbId l2LbId) {
        this.l2LbId = l2LbId;
        this.nextId = -1;
    }

    /**
     * Constructs a L2 load balancer data.
     *
     * @param l2LbId L2 load balancer ID
     * @param nextId L2 load balancer next id
     */
    public L2LbData(L2LbId l2LbId, int nextId) {
        this.l2LbId = l2LbId;
        this.nextId = nextId;
    }

    /**
     * Gets L2 load balancer ID.
     *
     * @return L2 load balancer ID
     */
    public L2LbId l2LbId() {
        return l2LbId;
    }

    /**
     * Gets L2 load balancer next id.
     *
     * @return L2 load balancer next id
     */
    public int nextId() {
        return nextId;
    }

    /**
     * Sets L2 load balancer next id.
     *
     * @param nextId L2 load balancer next id
     */
    public void setNextId(int nextId) {
        this.nextId = nextId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(l2LbId, nextId);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof L2LbData)) {
            return false;
        }
        final L2LbData other = (L2LbData) obj;

        return Objects.equals(this.l2LbId, other.l2LbId) &&
                this.nextId == other.nextId;
    }

    @Override
    public String toString() {
        return toStringHelper(getClass())
                .add("l2LbId", l2LbId)
                .add("nextId", nextId)
                .toString();
    }
}
