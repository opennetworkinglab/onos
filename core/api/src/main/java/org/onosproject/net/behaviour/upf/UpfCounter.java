/*
 * Copyright 2021-present Open Networking Foundation
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

package org.onosproject.net.behaviour.upf;


import com.google.common.annotations.Beta;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A structure for compactly passing UPF counter values for a given counter ID.
 * Contains four counts: Ingress Packets, Ingress Bytes, Egress Packets, Egress Bytes.
 * UpfCounter can be used ONLY on {@code apply} and {@code readAll} calls in the
 * {@link UpfDevice} interface.
 */
@Beta
public final class UpfCounter implements UpfEntity {
    private final int cellId;
    private final long ingressPkts;
    private final long ingressBytes;
    private final long egressPkts;
    private final long egressBytes;

    private UpfCounter(int cellId, long ingressPkts, long ingressBytes,
                       long egressPkts, long egressBytes) {
        this.cellId = cellId;
        this.ingressPkts = ingressPkts;
        this.ingressBytes = ingressBytes;
        this.egressPkts = egressPkts;
        this.egressBytes = egressBytes;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return String.format("UpfStats(cell_id=%d, ingress=(%dpkts,%dbytes), egress=(%dpkts,%dbytes))",
                cellId, ingressPkts, ingressBytes, egressPkts, egressBytes);
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (object == null) {
            return false;
        }
        if (getClass() != object.getClass()) {
            return false;
        }
        UpfCounter that = (UpfCounter) object;
        return this.cellId == that.cellId;
    }

    /**
     * Returns whether this UpfCounter is exactly equal to the given UpfCounter,
     * including their packets and bytes values.
     *
     * @param that other {@link UpfCounter} instance to compare
     * @return true if exactly equals, false otherwise
     */
    public boolean exactlyEquals(UpfCounter that) {
        return this.equals(that) &&
                this.ingressPkts == that.ingressPkts &&
                this.ingressBytes == that.ingressBytes &&
                this.egressPkts == that.egressPkts &&
                this.egressBytes == that.egressBytes;
    }


    @Override
    public int hashCode() {
        return Objects.hash(cellId);
    }

    /**
     * Get the cell ID (index) of the dataplane counter that produced this set of stats.
     *
     * @return counter cell ID
     */
    public int getCellId() {
        return cellId;
    }

    /**
     * Get the number of packets that hit this counter in the dataplane ingress pipeline.
     *
     * @return ingress packet count
     */
    public long getIngressPkts() {
        return ingressPkts;
    }

    /**
     * Get the number of packets that hit this counter in the dataplane egress pipeline.
     *
     * @return egress packet count
     */
    public long getEgressPkts() {
        return egressPkts;
    }

    /**
     * Get the number of packet bytes that hit this counter in the dataplane ingress pipeline.
     *
     * @return ingress byte count
     */
    public long getIngressBytes() {
        return ingressBytes;
    }

    /**
     * Get the number of packet bytes that hit this counter in the dataplane egress pipeline.
     *
     * @return egress byte count
     */
    public long getEgressBytes() {
        return egressBytes;
    }

    @Override
    public UpfEntityType type() {
        return UpfEntityType.COUNTER;
    }

    public static class Builder {
        private Integer cellId;
        private long ingressPkts;
        private long ingressBytes;
        private long egressPkts;
        private long egressBytes;

        public Builder() {
            this.ingressPkts = 0;
            this.ingressBytes = 0;
            this.egressPkts = 0;
            this.egressBytes = 0;
        }

        /**
         * Set the Cell ID (index) of the datalane counter that produced this set of stats.
         *
         * @param cellId the counter cell ID
         * @return This builder
         */
        public Builder withCellId(int cellId) {
            this.cellId = cellId;
            return this;
        }

        /**
         * Set the number of packets and bytes that hit the counter in the dataplane ingress pipeline.
         *
         * @param ingressPkts  ingress packet count
         * @param ingressBytes egress packet count
         * @return This builder
         */
        public Builder setIngress(long ingressPkts, long ingressBytes) {
            this.ingressPkts = ingressPkts;
            this.ingressBytes = ingressBytes;
            return this;
        }

        /**
         * Set the number of packets and bytes that hit the counter in the dataplane egress pipeline.
         *
         * @param egressPkts  egress packet count
         * @param egressBytes egress byte count
         * @return This builder
         */
        public Builder setEgress(long egressPkts, long egressBytes) {
            this.egressPkts = egressPkts;
            this.egressBytes = egressBytes;
            return this;
        }

        public UpfCounter build() {
            checkNotNull(cellId, "CellID must be provided");
            return new UpfCounter(cellId, ingressPkts, ingressBytes, egressPkts, egressBytes);
        }
    }
}
