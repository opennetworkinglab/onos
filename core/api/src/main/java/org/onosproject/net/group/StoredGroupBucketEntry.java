/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.net.group;

/**
 * Generic group bucket entry representation that is stored in a
 * group object. A group bucket entry provides additional info of
 * group bucket like statistics...etc
 */
public interface StoredGroupBucketEntry extends GroupBucket {
    /**
     * Sets number of packets processed by this group bucket entry.
     *
     * @param packets a long value
     */
    void setPackets(long packets);

    /**
     * Sets number of bytes processed by this group bucket entry.
     *
     * @param bytes a long value
     */
    void setBytes(long bytes);
}
