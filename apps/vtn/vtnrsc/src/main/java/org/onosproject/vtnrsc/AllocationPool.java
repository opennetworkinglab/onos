/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.vtnrsc;

import org.onlab.packet.IpAddress;

/**
 * The continuous IP address range between the start address and the end address for the allocation pools.
 */
public interface AllocationPool {

    /**
     * The start address for the allocation pool.
     *
     * @return startIp
     */
    IpAddress startIp();

    /**
     * The end address for the allocation pool.
     *
     * @return endIp
     */
    IpAddress endIp();
}
