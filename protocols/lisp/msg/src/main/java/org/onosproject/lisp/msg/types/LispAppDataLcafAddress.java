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
package org.onosproject.lisp.msg.types;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Application data type LCAF address class.
 *
 * Application data type is defined in draft-ietf-lisp-lcaf-13
 * https://tools.ietf.org/html/draft-ietf-lisp-lcaf-13#page-26
 *
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |           AFI = 16387         |     Rsvd1     |     Flags     |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |   Type = 4    |     Rsvd2     |            12 + n             |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |       IP TOS, IPv6 TC, or Flow Label          |    Protocol   |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |    Local Port (lower-range)   |    Local Port (upper-range)   |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |   Remote Port (lower-range)   |   Remote Port (upper-range)   |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |              AFI = x          |         Address  ...          |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 */
public class LispAppDataLcafAddress extends LispLcafAddress {

    private final byte protocol;
    private final int ipTos;
    private final short localPort;
    private final short remotePort;
    private LispAfiAddress address;

    /**
     * Initializes application data type LCAF address.
     */
    public LispAppDataLcafAddress() {
        super(LispCanonicalAddressFormatEnum.APPLICATION_DATA);
        this.protocol = 0;
        this.ipTos = 0;
        this.localPort = 0;
        this.remotePort = 0;
    }

    /**
     * Initializes application data type LCAF address.
     *
     * @param protocol protocol number
     * @param ipTos ip type of service
     * @param localPort local port number
     * @param remotePort remote port number
     * @param address address
     */
    public LispAppDataLcafAddress(byte protocol, int ipTos, short localPort, short remotePort,
                                  LispAfiAddress address) {
        super(LispCanonicalAddressFormatEnum.APPLICATION_DATA);
        this.protocol = protocol;
        this.ipTos = ipTos;
        this.localPort = localPort;
        this.remotePort = remotePort;
        this.address = address;
    }

    /**
     * Obtains protocol number.
     *
     * @return protocol number
     */
    public byte getProtocol() {
        return protocol;
    }

    /**
     * Obtains IP type of service.
     *
     * @return IP type of service
     */
    public int getIpTos() {
        return ipTos;
    }

    /**
     * Obtains local port number.
     *
     * @return local port number
     */
    public short getLocalPort() {
        return localPort;
    }

    /**
     * Obtains remote port number.
     *
     * @return remote port number
     */
    public short getRemotePort() {
        return remotePort;
    }

    /**
     * Obtains address.
     *
     * @return address
     */
    public LispAfiAddress getAddress() {
        return address;
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, protocol, ipTos, localPort, remotePort);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof LispAppDataLcafAddress) {
            final LispAppDataLcafAddress other = (LispAppDataLcafAddress) obj;
            return Objects.equals(this.address, other.address) &&
                    Objects.equals(this.protocol, other.protocol) &&
                    Objects.equals(this.ipTos, other.ipTos) &&
                    Objects.equals(this.localPort, other.localPort) &&
                    Objects.equals(this.remotePort, other.remotePort);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("address", address)
                .add("protocol", protocol)
                .add("ip type of service", ipTos)
                .add("local port number", localPort)
                .add("remote port number", remotePort)
                .toString();
    }
}
