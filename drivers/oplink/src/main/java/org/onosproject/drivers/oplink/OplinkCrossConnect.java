/*
 * Copyright 2016 Open Networking Laboratory
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

package org.onosproject.drivers.oplink;

import org.onosproject.net.PortNumber;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Oplink cross connect data unit.
 * The requirement members are input port, output port, wavelength channel and channel attenuation.
 */
public class OplinkCrossConnect {

    private final PortNumber inPort;
    private final PortNumber outPort;
    private final int channel;
    private final int attenuation;

    /**
     * OplinkConnection structure.
     * @param inPort the input port
     * @param outPort the output port
     * @param channel the channel
     * @param attenuation the attenuation
     */
    public OplinkCrossConnect(PortNumber inPort, PortNumber outPort, int channel, int attenuation) {
        this.inPort = inPort;
        this.outPort = outPort;
        this.channel = channel;
        this.attenuation = attenuation;
    }

    /**
     * Returns the input port of the cross connect.
     * @return input port
     */
    public PortNumber getInPort() {
        return inPort;
    }

    /**
     * Returns the output port of the cross connect.
     * @return output port
     */
    public PortNumber getOutPort() {
        return outPort;
    }

    /**
     * Returns the channel number of the cross connect.
     * @return channel number
     */
    public int getChannel() {
        return channel;
    }

    /**
     * Returns the channel attenuation of the cross connect.
     * @return attenuation
     */
    public int getAttenuation() {
        return attenuation;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("inPort", inPort)
                .add("outPort", outPort)
                .add("channel", channel)
                .add("attenuation", attenuation)
                .toString();
    }
}