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
package org.onosproject.core;

import com.google.common.base.MoreObjects;

/**
 * Time provided by a Hybrid Logical Clock described in
 * this <a href="http://www.cse.buffalo.edu/tech-reports/2014-04.pdf">paper</a>.
 */
public class HybridLogicalTime {
    private final long logicalTime;
    private final long logicalCounter;

    public HybridLogicalTime(long logicalTime, long logicalCounter) {
        this.logicalTime = logicalTime;
        this.logicalCounter = logicalCounter;
    }

    /**
     * Returns the logical time component of a HLT.
     * @return logical time
     */
    public long logicalTime() {
        return logicalTime;
    }

    /**
     * Returns the logical counter component of a HLT.
     * @return logical counter
     */
    public long logicalCounter() {
        return logicalCounter;
    }

    /**
     * Returns the real system time represented by this HLT.
     * @return real system time
     */
    public long time() {
        return (logicalTime >> 16 << 16) | (logicalCounter << 48 >> 48);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("logicalTime", logicalTime)
                .add("logicalCounter", logicalCounter)
                .toString();
    }
}
