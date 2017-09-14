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

package org.onosproject.net.behaviour.trafficcontrol;

import com.google.common.annotations.Beta;

/**
 * Represents a stored token bucket.
 */
@Beta
public interface TokenBucketEntry {

    /**
     * Updates the number of packets seen by this token bucket.
     *
     * @param packets a packet count.
     */
    void setProcessedPackets(long packets);

    /**
     * Updates the number of bytes seen by this token bucket.
     *
     * @param bytes a byte counter.
     */
    void setProcessedBytes(long bytes);

}
