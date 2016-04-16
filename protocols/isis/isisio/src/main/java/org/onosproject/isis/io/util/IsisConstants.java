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

/**
 * Representation of ISIS constants.
 */
public final class IsisConstants {
    public static final char PDU_LENGTH = 1497; // mtu (1500) - (3) LLC
    public static final int MINIMUM_FRAME_LEN = 1521;
    public static final int METADATA_LEN = 7;
    public static final String SHOST = "127.0.0.1";
    public static final int SPORT = 3000;
    public static final byte L2 = 1;
    public static final int IRPDISCRIMINATOR = 131;
    public static final int ISISVERSION = 1;
    public static final int RESERVED = 0;
    public static final int MAXAREAADDRESS = 0;
    public static final int IDLENGTH = 0;
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

    /**
     * Non parameterized constructor.
     */
    private IsisConstants() {
    }
}
