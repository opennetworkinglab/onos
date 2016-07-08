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
package org.onosproject.net.behaviour;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

/**
 * Represent the object for the xml element of vpnTarget.
 */
public class NetconfVpnTarget {
    private final String vrfRTType;
    private final String vrfRTValue;

    /**
     * NetconfVpnTarget constructor.
     * 
     * @param vrfRTType vrfRTType
     * @param vrfRTValue vrfRTValue
     */
    public NetconfVpnTarget(String vrfRTType, String vrfRTValue) {
        checkNotNull(vrfRTType, "vrfRTType cannot be null");
        checkNotNull(vrfRTValue, "vrfRTValue cannot be null");
        this.vrfRTType = vrfRTType;
        this.vrfRTValue = vrfRTValue;
    }

    /**
     * Returns vrfRTType.
     * 
     * @return vrfRTType
     */
    public String vrfRTType() {
        return vrfRTType;
    }

    /**
     * Returns vrfRTValue.
     * 
     * @return vrfRTValue
     */
    public String vrfRTValue() {
        return vrfRTValue;
    }

    @Override
    public int hashCode() {
        return Objects.hash(vrfRTType, vrfRTValue);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NetconfVpnTarget) {
            final NetconfVpnTarget other = (NetconfVpnTarget) obj;
            return Objects.equals(this.vrfRTType, other.vrfRTType)
                    && Objects.equals(this.vrfRTValue, other.vrfRTValue);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("vrfRTType", vrfRTType)
                .add("vrfRTValue", vrfRTValue).toString();
    }
}
