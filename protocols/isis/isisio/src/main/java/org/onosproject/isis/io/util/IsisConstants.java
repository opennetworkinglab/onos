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

package org.onosproject.isis.io.util;

import org.onlab.packet.Ip4Address;

/**
 * Representation of ISIS Constants.
 */
public final class IsisConstants {
    public static final char PDU_LENGTH = 1497;
    public static final char CONFIG_LENGTH = 1498;
    public static final int MINIMUM_FRAME_LEN = 1521;
    public static final int METADATA_LEN = 7;
    public static final String SHOST = "127.0.0.1";
    public static final Ip4Address DEFAULTIP = Ip4Address.valueOf("0.0.0.0");
    public static final int SPORT = 3000;
    public static final byte L2 = 1;
    public static final int IRPDISCRIMINATOR = 131;
    public static final int ISISVERSION = 1;
    public static final int RESERVED = 0;
    public static final int PRIORITY = 0;
    public static final int MAXAREAADDRESS = 0;
    public static final int SYSTEMIDLENGTH = 0;
    public static final int PROTOCOLSUPPORTED = 204;
    public static final int LOCALCIRCUITIDFORP2P = 130;
    public static final int P2PHELLOHEADERLENGTH = 20;
    public static final int HELLOHEADERLENGTH = 27;
    public static final int CSNPDUHEADERLENGTH = 33;
    public static final int PSNPDUHEADERLENGTH = 17;
    public static final int PDULENGTHPOSITION = 17;
    public static final int COMMONHEADERLENGTH = 8;
    public static final int LSPMAXAGE = 1200;
    public static final int LSPREFRESH = 900;
    public static final int MAXSEQUENCENUMBER = Integer.MAX_VALUE;
    public static final int STARTLSSEQUENCENUM = 1;
    public static final int LENGTHPOSITION = 8;
    public static final int RESERVEDPOSITION = 6;
    public static final int CHECKSUMPOSITION = 24;
    public static final String REFRESHLSP = "refreshLsp";
    public static final String MAXAGELSP = "maxAgeLsp";
    public static final String DEFAULTLANID = "0000.0000.0000.00";
    public static final String PROCESSESID = "processId";
    public static final String INTERFACE = "interface";
    public static final String INTERFACEINDEX = "interfaceIndex";
    public static final String INTERMEDIATESYSTEMNAME = "intermediateSystemName";
    public static final String SYSTEMID = "systemId";
    public static final String RESERVEDPACKETCIRCUITTYPE = "reservedPacketCircuitType";
    public static final String CIRCUITID = "circuitId";
    public static final String NETWORKTYPE = "networkType";
    public static final String AREAADDRESS = "areaAddress";
    public static final String HOLDINGTIME = "holdingTime";
    public static final String HELLOINTERVAL = "helloInterval";
    public static final int PORT = 7000;
    public static final String LSPADDED = "LSP_ADDED";
    public static final String LSPREMOVED = "LSP_REMOVED";

    /**
     * Non parameterized constructor.
     */
    private IsisConstants() {
    }
}
