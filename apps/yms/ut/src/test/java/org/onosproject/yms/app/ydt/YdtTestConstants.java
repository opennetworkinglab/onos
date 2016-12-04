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

package org.onosproject.yms.app.ydt;

/**
 * Represents common constant utility for YANG data tree UT framework.
 */
final class YdtTestConstants {

    // No instantiation.
    private YdtTestConstants() {
    }

    public static final String BACKSLASH = "\"";
    public static final String PERIOD = ".";
    public static final String A = "92233720368547758.07";
    public static final String B = "92233720368547757";
    public static final String C = "-92233720368547758.08";
    public static final String D = "92233720368547757";
    public static final String E = "9.223372036854775807";
    public static final String F = "-922337203685477580.8";
    public static final String G = "922337203685477580.7";
    public static final String H = "-9.223372036854775808";
    public static final String I = "9223372036854775808";
    public static final String J = "9223372036854775807";
    public static final String K = "-9223372036854775808";
    public static final String L = "-9223372036854775809";
    public static final String M = "18446744073709551616";
    public static final String NWF = "negIntWithMaxFraction";
    public static final String PWF = "posIntWithMaxFraction";
    public static final String NIWMF = "negIntWithMinFraction";
    public static final String PIWMF = "posIntWithMinFraction";
    public static final String CAPSUINT8 = "UINT8";
    public static final String CAPSINT8 = "INT8";
    public static final String SMALLUINT8 = "uint8.";
    public static final String SMALLINT8 = "int8.";
    public static final String MAXUINT8 = "255";
    public static final String CAPSUINT16 = "UINT16";
    public static final String CAPSINT16 = "INT16";
    public static final String SUINT16 = "uint16.";
    public static final String SINT16 = "int16.";
    public static final String MAXUINT16 = "65535";
    public static final String CAPSUINT32 = "UINT32";
    public static final String CAPSINT32 = "INT32";
    public static final String SUINT32 = "uint32.";
    public static final String SINT32 = "int32.";
    public static final String MAXUINT32 = "4294967295";
    public static final String CAPSUINT64 = "UINT64";
    public static final String CAPSINT64 = "INT64";
    public static final String SMALLUINT64 = "uint64.";
    public static final String SMALLINT64 = "int64.";
    public static final String MAXUINT64 = "18446744073709551615";
    public static final String MINVALUE = "0";
    public static final String MINIWR = "minIntWithRange";
    public static final String MIDIWR = "midIntWithRange";
    public static final String MAXIWR = "maxIntWithRange";
    public static final String MINUIWR = "minUIntWithRange";
    public static final String MIDUIWR = "midUIntWithRange";
    public static final String MAXUIWR = "maxUIntWithRange";
    public static final String MRV = "multiRangeValidation";
    public static final String RUI = "revUnInteger";
    public static final String TYPE = "builtInType";
    public static final String INT8NS = "ydt.integer8";
    public static final String BIT = "BITS";
    public static final String BOOL = "BOOLEAN";
    public static final String EMPTY = "";
    public static final String ENUM = "ENUMERATION";
    public static final String LIST = "List";
    public static final String LWC = "listwithcontainer";
    public static final String INV = "invalidinterval";
    public static final String INT16NS = "ydt.integer16";
    public static final String INT32NS = "ydt.integer32";
    public static final String INT64NS = "ydt.integer64";
    public static final String BITNS = "ydt.bit";
    public static final String BOOLNS = "ydt.boolean";
    public static final String EMPTYNS = "ydt.emptydata";
    public static final String ENUMNS = "ydt.enumtest";
    public static final String LISTNS = "ydt.rootlist";
    public static final String A1 = "ydt.augment-topology1";
    public static final String A2 = "ydt.augment-topology2";
    public static final String A3 = "ydt.augment-topology3";
    public static final String A4 = "ydt.augment-topology4";
    public static final String A5 = "ydt.augment-topology5";
    public static final String A6 = "ydt.augment-topology6";
    public static final String A2L = "augment2leafList";
    public static final String A5L = "augment5leafList";
    public static final String A6L = "augment6leafList";
    public static final String MATERIALNS = "ydt.material-supervisor";
    public static final String PURCHASNS = "ydt.purchasing-supervisor";
    public static final String WAREHNS = "ydt.warehouse-supervisor";
    public static final String TRADNS = "ydt.trading-supervisor";
    public static final String EMPNS = "ydt.employee-id";
    public static final String COUSTOMNS = "ydt.customs-supervisor";
    public static final String MERCHNS = "ydt.Merchandiser-supervisor";
    public static final String STP = "supporting-termination-point";
    public static final String SLINK = "supporting-link";
    public static final String AUGSE = "/aug:l1";
    public static final String ELNS = "ydt.Empty.leafList";
    public static final String AUG1 = "/nd:networks/nd:network";
    public static final String AUG2 = "/nd:networks/nd:network/nd:node";
    public static final String AUG3 = "/nd:networks/nd:network/topo:link";
    public static final String AUG4 = "/nd:networks/nd:network/nd:node/" +
            "topo:t-point/supporting-termination-point";
    public static final String AUG5 = "/nd:networks/nd:network/topo:link/" +
            "aug2:augment2";
    public static final String AUG6 = "/nd:networks/nd:network/nd:node/" +
            "topo:t-point/supporting-termination-point/aug2:augment2";
    public static final String AUG7 = "/nd:networks/nd:network/topo:link/" +
            "aug2:augment2/aug3:augment3";
    public static final String AUG8 = "/nd:networks/nd:network/topo:link/" +
            "aug2:augment2/aug3:augment3/aug5:augment5";
    public static final String AUG9 = "/nd:networks/nd:network/topo:link/" +
            "aug2:augment2/aug5:augment5";
    public static final String AUG10 = "/aug:node/aug:cont1s/aug:cont1s";
    public static final String NETNS = "ydt.augmentNetwork";
    public static final String AUGNS = "ydt.augmentSequence1";
    public static final String IETFNS =
            "urn:ietf:params:xml:ns:yang:ietf-network";
    public static final String IETF = "yms-ietf-network";
    public static final String TOPONS =
            "urn:ietf:params:xml:ns:yang:ietf-network-topology";
    public static final String E_LEAF = "Exception has not occurred for " +
            "invalid leaf value with name ";
    public static final String E_LIST = "Exception has not occurred for " +
            "invalid node addition with the name ";
    public static final String E_TOPARENT = "Exception has not occurred " +
            "in traverse back to parent for multi instance node.";
    public static final String E_D64 = "YANG file error : value is not in" +
            " decimal64 range.";
}
