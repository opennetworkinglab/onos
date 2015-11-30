/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.iptopology.api;

import static com.google.common.base.MoreObjects.toStringHelper;

import java.util.Objects;

/**
 * This class provides implementation IS-IS and OSPF flags assigned to the prefix.
 */
public class IgpFlags {
    private final boolean isisUpDown;
    private final boolean ospfNoUnicast;
    private final boolean ospfLclAddr;
    private final boolean ospfNssa;

    /**
     * Constructor to initialize its parameters.
     *
     * @param isisUpDown IS-IS Up/Down
     * @param ospfNoUnicast OSPF no unicast
     * @param ospfLclAddr OSPF local address
     * @param ospfNssa OSPF propagate NSSA
     */
     public IgpFlags(boolean isisUpDown, boolean ospfNoUnicast, boolean ospfLclAddr,
                     boolean ospfNssa) {
         this.isisUpDown = isisUpDown;
         this.ospfNoUnicast = ospfNoUnicast;
         this.ospfLclAddr = ospfLclAddr;
         this.ospfNssa = ospfNssa;
     }

    /**
     * Provides information whether IS-IS is Up/Down.
     *
     * @return IS-IS Up/Down bit enabled or not
     */
    public boolean isisUpDown() {
        return isisUpDown;
    }

    /**
     * Provides information whether OSPF is unicast or not.
     *
     * @return OSPF no unicast Bit set or not
     */
    public boolean ospfNoUnicast() {
        return ospfNoUnicast;
    }

    /**
     * Provides information on OSPF local address.
     *
     * @return OSPF local address Bit set or not
     */
    public boolean ospfLclAddr() {
        return ospfLclAddr;
    }

    /**
     * Provides information on OSPF propagate NSSA.
     *
     * @return OSPF propagate NSSA Bit set or not
     */
    public boolean ospfNssa() {
        return ospfNssa;
    }

    @Override
    public int hashCode() {
        return Objects.hash(isisUpDown, ospfNoUnicast, ospfLclAddr,
                            ospfNssa);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof IgpFlags) {
            IgpFlags other = (IgpFlags) obj;
            return Objects.equals(isisUpDown, other.isisUpDown)
                    && Objects.equals(ospfNoUnicast, other.ospfNoUnicast)
                    && Objects.equals(ospfLclAddr, other.ospfLclAddr)
                    && Objects.equals(ospfNssa, other.ospfNssa);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("isisUpDown", isisUpDown)
                .add("ospfNoUnicast", ospfNoUnicast)
                .add("ospfLclAddr", ospfLclAddr)
                .add("ospfNssa", ospfNssa)
                .toString();
    }
}