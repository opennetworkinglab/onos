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

package org.onosproject.lisp.msg.protocols;

import org.onlab.packet.IP;
import org.onlab.packet.UDP;

/**
 * LISP Encapsulated Control Message (ECM) interface.
 * <p>
 * LISP ECM format is defined in RFC6830.
 * https://tools.ietf.org/html/rfc6830#section-6.1.8
 *
 * <pre>
 * {@literal
 *      0                   1                   2                   3
 *      0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * /   |                       IPv4 or IPv6 Header                     |
 * OH  |                      (uses RLOC addresses)                    |
 * \   |                                                               |
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * /   |       Source Port = xxxx      |       Dest Port = 4342        |
 * UDP +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * \   |           UDP Length          |        UDP Checksum           |
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * LH  |Type=8 |S|                  Reserved                           |
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * /   |                       IPv4 or IPv6 Header                     |
 * IH  |                  (uses RLOC or EID addresses)                 |
 * \   |                                                               |
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * /   |       Source Port = xxxx      |       Dest Port = yyyy        |
 * UDP +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * \   |           UDP Length          |        UDP Checksum           |
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * LCM |                      LISP Control Message                     |
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * }</pre>
 */
public interface LispEncapsulatedControl extends LispMessage {

    /**
     * Obtains security flag.
     * If this bit is set, the 'Reserved' field will have the authentication data.
     *
     * @return security flag
     */
    boolean isSecurity();

    /**
     * Obtains inner IP header.
     *
     * @return inner IP header
     */
    IP innerIpHeader();

    /**
     * Obtains inner LISP UDP header.
     *
     * @return inner LISP UDP header
     */
    UDP innerUdp();

    /**
     * Obtains inner LISP control message.
     * The format can be one of other LISP messages.
     *
     * @return Inner lisp control messages
     */

    LispMessage getControlMessage();

    /**
     * A builder of LISP map request message.
     */
    interface EcmBuilder extends Builder {

        /**
         * Sets security flag.
         *
         * @param security security flag
         * @return ECMBuilder object
         */
        EcmBuilder isSecurity(boolean security);

        /**
         * Sets inner IP header.
         *
         * @param innerIpHeader inner IP header in IPv4 or IPv6
         * @return ECMBuilder object
         */
        EcmBuilder innerIpHeader(IP innerIpHeader);

        /**
         * Sets inner UDP header.
         *
         * @param innerUdpHeader inner UDP packet
         * @return ECMBuilder object
         */
        EcmBuilder innerUdpHeader(UDP innerUdpHeader);

        /**
         * Sets inner LISP control message.
         *
         * @param msg the inner lisp message
         * @return ECMBuilder object
         */
        EcmBuilder innerLispMessage(LispMessage msg);

        /**
         * Builds LISP ECM message.
         *
         * @return LISP ECM message
         */
        LispEncapsulatedControl build();
    }
}
