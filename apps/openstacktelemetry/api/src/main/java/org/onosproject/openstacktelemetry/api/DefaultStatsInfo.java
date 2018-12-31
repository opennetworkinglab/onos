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
package org.onosproject.openstacktelemetry.api;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Implementation class of StatsInfo.
 */
public final class DefaultStatsInfo implements StatsInfo {

    private final long startupTime;
    private final long fstPktArrTime;
    private final int lstPktOffset;
    private final long prevAccBytes;
    private final int prevAccPkts;
    private final long currAccBytes;
    private final int currAccPkts;
    private final short errorPkts;
    private final short dropPkts;

    private DefaultStatsInfo(long startupTime, long fstPktArrTime, int lstPktOffset,
                             long prevAccBytes, int prevAccPkts, long currAccBytes,
                             int currAccPkts, short errorPkts, short dropPkts) {
        this.startupTime = startupTime;
        this.fstPktArrTime = fstPktArrTime;
        this.lstPktOffset = lstPktOffset;
        this.prevAccBytes = prevAccBytes;
        this.prevAccPkts = prevAccPkts;
        this.currAccBytes = currAccBytes;
        this.currAccPkts = currAccPkts;
        this.errorPkts = errorPkts;
        this.dropPkts = dropPkts;
    }

    @Override
    public long startupTime() {
        return startupTime;
    }

    @Override
    public long fstPktArrTime() {
        return fstPktArrTime;
    }

    @Override
    public int lstPktOffset() {
        return lstPktOffset;
    }

    @Override
    public long prevAccBytes() {
        return prevAccBytes;
    }

    @Override
    public int prevAccPkts() {
        return prevAccPkts;
    }

    @Override
    public long currAccBytes() {
        return currAccBytes;
    }

    @Override
    public int currAccPkts() {
        return currAccPkts;
    }

    @Override
    public short errorPkts() {
        return errorPkts;
    }

    @Override
    public short dropPkts() {
        return dropPkts;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof DefaultStatsInfo) {
            final DefaultStatsInfo other = (DefaultStatsInfo) obj;
            return Objects.equals(this.startupTime, other.startupTime) &&
                    Objects.equals(this.fstPktArrTime, other.fstPktArrTime) &&
                    Objects.equals(this.lstPktOffset, other.lstPktOffset) &&
                    Objects.equals(this.prevAccBytes, other.prevAccBytes) &&
                    Objects.equals(this.prevAccPkts, other.prevAccPkts) &&
                    Objects.equals(this.currAccBytes, other.currAccBytes) &&
                    Objects.equals(this.currAccPkts, other.currAccPkts) &&
                    Objects.equals(this.errorPkts, other.errorPkts) &&
                    Objects.equals(this.dropPkts, other.dropPkts);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(startupTime, fstPktArrTime, lstPktOffset,
                prevAccBytes, prevAccPkts, currAccBytes, currAccPkts,
                errorPkts, dropPkts);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("startupTime", startupTime)
                .add("fstPktArrTime", fstPktArrTime)
                .add("lstPktOffset", lstPktOffset)
                .add("prevAccBytes", prevAccBytes)
                .add("prevAccPkts", prevAccPkts)
                .add("currAccBytes", currAccBytes)
                .add("currAccPkts", currAccPkts)
                .add("errorPkts", errorPkts)
                .add("dropPkts", dropPkts)
                .toString();
    }

    /**
     * Builder class of DefaultStatsInfo.
     */
    public static final class DefaultBuilder implements StatsInfo.Builder {
        private long startupTime;
        private long fstPktArrTime;
        private int lstPktOffset;
        private long prevAccBytes;
        private int prevAccPkts;
        private long currAccBytes;
        private int currAccPkts;
        private short errorPkts;
        private short dropPkts;

        @Override
        public Builder withStartupTime(long startupTime) {
            this.startupTime = startupTime;
            return this;
        }

        @Override
        public Builder withFstPktArrTime(long fstPktArrTime) {
            this.fstPktArrTime = fstPktArrTime;
            return this;
        }

        @Override
        public Builder withLstPktOffset(int lstPktOffset) {
            this.lstPktOffset = lstPktOffset;
            return this;
        }

        @Override
        public Builder withPrevAccBytes(long prevAccBytes) {
            this.prevAccBytes = prevAccBytes;
            return this;
        }

        @Override
        public Builder withPrevAccPkts(int prevAccPkts) {
            this.prevAccPkts = prevAccPkts;
            return this;
        }

        @Override
        public Builder withCurrAccBytes(long currAccBytes) {
            this.currAccBytes = currAccBytes;
            return this;
        }

        @Override
        public Builder withCurrAccPkts(int currAccPkts) {
            this.currAccPkts = currAccPkts;
            return this;
        }

        @Override
        public Builder withErrorPkts(short errorPkts) {
            this.errorPkts = errorPkts;
            return this;
        }

        @Override
        public Builder withDropPkts(short dropPkts) {
            this.dropPkts = dropPkts;
            return this;
        }

        @Override
        public StatsInfo build() {

            return new DefaultStatsInfo(startupTime, fstPktArrTime, lstPktOffset,
                    prevAccBytes, prevAccPkts, currAccBytes, currAccPkts,
                    errorPkts, dropPkts);
        }
    }
}
