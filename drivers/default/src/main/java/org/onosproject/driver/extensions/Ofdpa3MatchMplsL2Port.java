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

package org.onosproject.driver.extensions;

import com.google.common.base.MoreObjects;
import org.onlab.util.KryoNamespace;
import org.onosproject.net.flow.AbstractExtension;
import org.onosproject.net.flow.criteria.ExtensionSelector;
import org.onosproject.net.flow.criteria.ExtensionSelectorType;

import java.util.Objects;

/**
 * OFDPA MPLS L2 Port extension match. MPLS L2 Port
 * is a logical port and it is represented with an
 * integer.
 */
public class Ofdpa3MatchMplsL2Port extends AbstractExtension implements ExtensionSelector {
    /**
     * Integer representing the logical port.
     */
    private int mplsL2Port;

    private static final KryoNamespace APPKRYO = new KryoNamespace.Builder()
            .build();

    /**
     * OFDPA MPLS L2 Port extension match.
     */
    protected Ofdpa3MatchMplsL2Port() {
        mplsL2Port = 0;
    }

    /**
     * Constructs a new MPLS L2 Port match with a given Integer.
     *
     * @param mplsl2port the MPLS L2 Port
     */
    public Ofdpa3MatchMplsL2Port(int mplsl2port) {
        this.mplsL2Port = mplsl2port;
    }

    /**
     * Gets the MPLS L2 Port.
     *
     * @return MPLS L2 Port
     */
    public int mplsL2Port() {
        return mplsL2Port;
    }

    @Override
    public ExtensionSelectorType type() {
        return ExtensionSelectorType.ExtensionSelectorTypes.OFDPA_MATCH_MPLS_L2_PORT.type();
    }

    @Override
    public byte[] serialize() {
        return APPKRYO.serialize(mplsL2Port);
    }

    @Override
    public void deserialize(byte[] data) {
        mplsL2Port = APPKRYO.deserialize(data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mplsL2Port);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Ofdpa3MatchMplsL2Port) {
            Ofdpa3MatchMplsL2Port that = (Ofdpa3MatchMplsL2Port) obj;
            return Objects.equals(mplsL2Port, that.mplsL2Port);

        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("mplsL2Port", mplsL2Port)
                .toString();
    }
}
