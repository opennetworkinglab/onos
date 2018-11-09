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

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Flow statistic information.
 */
public class FlowStatInfo {
    String protocol;
    String hostAName;
    int hostAPort;
    String hostBName;
    int hostBPort;
    int detectedProtocol;
    String detectedProtocolName;
    long packets;
    long bytes;
    String hostServerName;

    /**
     * Constructor for default FlowStatInfo class.
     */
    public FlowStatInfo() {
        protocol = "";
        hostAName = "::";
        hostAPort = 0;
        hostBName = "";
        hostBPort = 0;
        detectedProtocol = 0;
        detectedProtocolName = "";
        packets = 0;
        bytes = 0;

        hostServerName = "";
    }

    /**
     * Constructor for FlowStatInfo class specified with flow statistic parameters.
     *
     * @param protocol protocol
     * @param hostAName host A name
     * @param hostAPort host A port
     * @param hostBName host B name
     * @param hostBPort host B port
     * @param detectedProtocol detected protocol
     * @param detectedProtocolName detected protocol name
     * @param packets packet count
     * @param bytes byte count
     */
    public FlowStatInfo(String protocol, String hostAName, int hostAPort, String hostBName, int hostBPort,
                        int detectedProtocol, String detectedProtocolName, long packets, long bytes) {
        this.protocol = protocol;
        this.hostAName = hostAName;
        this.hostAPort = hostAPort;
        this.hostBName = hostBName;
        this.hostBPort = hostBPort;
        this.detectedProtocol = detectedProtocol;
        this.detectedProtocolName = detectedProtocolName;
        this.packets = packets;
        this.bytes = bytes;

        hostServerName = "";
    }

    /**
     * Constructor for FlowStatInfo class specified with flow statistic parameters and hostServerName.
     *
     * @param protocol protocol
     * @param hostAName host A name
     * @param hostAPort host A port
     * @param hostBName host B name
     * @param hostBPort host B port
     * @param detectedProtocol detected protocol
     * @param detectedProtocolName detected protocol name
     * @param packets packet count
     * @param bytes byte count
     * @param hostServerName host server name
     */
    public FlowStatInfo(String protocol, String hostAName, int hostAPort, String hostBName, int hostBPort,
                        int detectedProtocol, String detectedProtocolName, long packets, long bytes,
                        String hostServerName) {
        this(protocol, hostAName, hostAPort, hostBName, hostBPort, detectedProtocol, detectedProtocolName,
             packets, bytes);

        this.hostServerName = hostServerName;
    }

    /**
     * Returns DPI flow protocol.
     *
     * @return protocol
     */
    public String protocol() {
        return protocol;
    }

    /**
     * Returns DPI flow host A name.
     *
     * @return hostAName
     */
    public String hostAName() {
        return hostAName;
    }

    /**
     * Returns DPI flow host A port.
     *
     * @return hostAPort
     */
    public int hostAPort() {
        return hostAPort;
    }


    /**
     * Returns DPI flow host B name.
     *
     * @return hostBName
     */
    public String hostBName() {
        return hostBName;
    }

    /**
     * Returns DPI flow host B Port.
     *
     * @return hostBPort
     */
    public int hostBPort() {
        return hostBPort;
    }

    /**
     * Returns DPI flow detected protocol.
     *
     * @return detectedProtocol
     */
    public int detectedProtocol() {
        return detectedProtocol;
    }

    /**
     * Returns DPI flow detected protocol name.
     *
     * @return detectedProtocolName
     */
    public String detectedProtocolName() {
        return detectedProtocolName;
    }

    /**
     * Returns DPI flow packets.
     *
     * @return packets
     */
    public long packets() {
        return packets;
    }

    /**
     * Returns DPI flow bytes.
     *
     * @return bytes
     */
    public long bytes() {
        return bytes;
    }

    /**
     * Returns DPI flow host server name.
     *
     * @return hostServerName
     */
    public String hostServerName() {
        return hostServerName;
    }


    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public void setHostAName(String hostAName) {
        this.hostAName = hostAName;
    }

    public void setHostAPort(int hostAPort) {
        this.hostAPort = hostAPort;
    }

    public void setHostBName(String hostBName) {
        this.hostBName = hostBName;
    }

    public void setHostBPort(int hostBPort) {
        this.hostBPort = hostBPort;
    }

    public void setDetectedProtocol(int detectedProtocol) {
        this.detectedProtocol = detectedProtocol;
    }

    public void setDetectedProtocolName(String detectedProtocolName) {
        this.detectedProtocolName = detectedProtocolName;
    }

    public void setPackets(long packets) {
        this.packets = packets;
    }

    public void setBytes(long bytes) {
        this.bytes = bytes;
    }

    public void setHostServerName(String hostServerName) {
        this.hostServerName = hostServerName;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("protocol", protocol)
                .add("hostAName", hostAName)
                .add("hostAPort", hostAPort)
                .add("hostBName", hostBName)
                .add("hostBPort", hostBPort)
                .add("detectedProtocol", detectedProtocol)
                .add("detectedProtocolName", detectedProtocolName)
                .add("packets", packets)
                .add("bytes", bytes)
                .add("hostServerName", hostServerName)
                .toString();
    }
}
