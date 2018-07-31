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
package org.onosproject.openstacktroubleshoot.impl;

import com.google.common.base.MoreObjects;
import org.onlab.packet.IpAddress;
import org.onosproject.openstacktroubleshoot.api.Reachability;

import java.util.Objects;

/**
 * Implementation of reachability.
 */
public final class DefaultReachability implements Reachability {

    private final IpAddress srcIp;
    private final IpAddress dstIp;
    private final boolean isReachable;

    private DefaultReachability(IpAddress srcIp, IpAddress dstIp, boolean isReachable) {
        this.srcIp = srcIp;
        this.dstIp = dstIp;
        this.isReachable = isReachable;
    }

    @Override
    public IpAddress srcIp() {
        return srcIp;
    }

    @Override
    public IpAddress dstIp() {
        return dstIp;
    }

    @Override
    public boolean isReachable() {
        return isReachable;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof DefaultReachability) {
            DefaultReachability that = (DefaultReachability) obj;
            return Objects.equals(srcIp, that.srcIp) &&
                    Objects.equals(dstIp, that.dstIp) &&
                    Objects.equals(isReachable, that.isReachable);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(srcIp, dstIp, isReachable);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("srcIp", srcIp)
                .add("dstIp", dstIp)
                .add("isReachable", isReachable)
                .toString();
    }

    /**
     * Obtains a reachability builder.
     *
     * @return reachability builder
     */
    public static Builder builder() {
        return new DefaultBuilder();
    }

    /**
     * A builder class for reachability.
     */
    public static final class DefaultBuilder implements Builder {

        private IpAddress srcIp;
        private IpAddress dstIp;
        private boolean isReachable;

        @Override
        public Builder srcIp(IpAddress srcIp) {
            this.srcIp = srcIp;
            return this;
        }

        @Override
        public Builder dstIp(IpAddress dstIp) {
            this.dstIp = dstIp;
            return this;
        }

        @Override
        public Builder isReachable(boolean isReachable) {
            this.isReachable = isReachable;
            return this;
        }

        @Override
        public Reachability build() {
            return new DefaultReachability(srcIp, dstIp, isReachable);
        }
    }
}
