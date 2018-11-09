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

package org.onosproject.incubator.net.dpi;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * DPI statistics with received time.
 */
public class DpiStatistics {
    private final String receivedTime;
    private final DpiStatInfo dpiStatInfo;

    /**
     * Constructor for DpiStatistics class.
     *
     * @param receivedTime dpiStatInfo received time
     * @param dpiStatInfo the dpi statistics info
     */
    public DpiStatistics(final String receivedTime, final DpiStatInfo dpiStatInfo) {
        checkNotNull(receivedTime, "Must specify receivedTime");
        checkNotNull(dpiStatInfo, "Must specify DpiStatInfo");

        this.receivedTime = receivedTime;
        this.dpiStatInfo = dpiStatInfo;
    }

    /**
     * Returns DPI statistics received time.
     *
     * @return receivedTime
     */
    public String receivedTime() {
        return receivedTime;
    }

    /**
     * Returns DPI statistics information.
     *
     * @return dpiStatInfo
     */
    public DpiStatInfo dpiStatInfo() {
        return dpiStatInfo;
    }

    @Override
    public int hashCode() {
        return Objects.hash(receivedTime, dpiStatInfo);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DpiStatistics other = (DpiStatistics) obj;
        if (!Objects.equals(this.receivedTime, other.receivedTime)) {
            return false;
        }
        if (!Objects.equals(this.dpiStatInfo, other.dpiStatInfo)) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("receivedTime", receivedTime)
                .add("dpiStatInfo", dpiStatInfo)
                .toString();
    }
}
