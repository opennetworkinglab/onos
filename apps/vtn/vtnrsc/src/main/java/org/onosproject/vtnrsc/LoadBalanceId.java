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
package org.onosproject.vtnrsc;

import org.onlab.util.Identifier;

import static com.google.common.base.Preconditions.checkArgument;

/*
 * Representation of 5 bit load balance identifier for a service function
 */
public final class LoadBalanceId extends Identifier<Byte> {

    private static final byte MAX_ID = 0x1F;

    /**
     * Default constructor.
     *
     * @param loadBalanceId service function chain path's load balance identifier
     */
    private LoadBalanceId(byte loadBalanceId) {
        super(loadBalanceId);
        checkArgument(loadBalanceId <= MAX_ID, "Load balance id should not be more than 5 bit identifier");
    }

    /**
     * Returns the SfcLoadBalanceId by setting its value.
     *
     * @param loadBalanceId service function chain path's load balance identifier
     * @return LoadBalanceId
     */
    public static LoadBalanceId of(byte loadBalanceId) {
        return new LoadBalanceId(loadBalanceId);
    }


    /**
     * Returns load balance identifier for a service function.
     *
     * @return loadBalanceId
     */
    public byte loadBalanceId() {
        return identifier;
    }
}
