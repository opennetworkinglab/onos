/*
 * Copyright 2016-present Open Networking Foundation
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

import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;

/**
 * The factory of move treatment.
 */
public final class NiciraMoveTreatmentFactory {

    /**
     * Public constructor is prohibited.
     */
    private NiciraMoveTreatmentFactory() {

    }

    /**
     * Creates a move treatment that move arp sha to tha.
     *
     * @return ExtensionTreatment
     */
    public static ExtensionTreatment createNiciraMovArpShaToTha() {
        int srcOfs = 0;
        int dstOfs = 0;
        int nBits = 48;
        int srcSha = 0x00012206;
        int dstTha = 0x00012406;
        return new DefaultMoveExtensionTreatment(srcOfs, dstOfs, nBits, srcSha,
                                                 dstTha,
                                                 ExtensionTreatmentType.ExtensionTreatmentTypes
                                                 .NICIRA_MOV_ARP_SHA_TO_THA.type());
    }

    /**
     * Creates a move treatment that move arp spa to tpa.
     *
     * @return ExtensionTreatment
     */
    public static ExtensionTreatment createNiciraMovArpSpaToTpa() {
        int srcOfs = 0;
        int dstOfs = 0;
        int nBits = 32;
        int srcSpa = 0x00002004;
        int dstTpa = 0x00002204;
        return new DefaultMoveExtensionTreatment(srcOfs, dstOfs, nBits, srcSpa,
                                                 dstTpa,
                                                 ExtensionTreatmentType.ExtensionTreatmentTypes
                                                 .NICIRA_MOV_ARP_SPA_TO_TPA.type());
    }

    /**
     * Creates a move treatment that move eth src to dst.
     *
     * @return ExtensionTreatment
     */
    public static ExtensionTreatment createNiciraMovEthSrcToDst() {
        int srcOfs = 0;
        int dstOfs = 0;
        int nBits = 48;
        int srcEth = 0x00000406;
        int dstEth = 0x00000206;
        return new DefaultMoveExtensionTreatment(srcOfs, dstOfs, nBits, srcEth,
                                                 dstEth,
                                                 ExtensionTreatmentType.ExtensionTreatmentTypes
                                                 .NICIRA_MOV_ETH_SRC_TO_DST.type());
    }

    /**
     * Creates a move treatment that move ip src to dst.
     *
     * @return ExtensionTreatment
     */
    public static ExtensionTreatment createNiciraMovIpSrcToDst() {
        int srcOfs = 0;
        int dstOfs = 0;
        int nBits = 32;
        int srcIp = 0x00000e04;
        int dstIp = 0x00001004;
        return new DefaultMoveExtensionTreatment(srcOfs, dstOfs, nBits, srcIp,
                                                 dstIp,
                                                 ExtensionTreatmentType.ExtensionTreatmentTypes
                                                 .NICIRA_MOV_IP_SRC_TO_DST.type());
    }

    public static ExtensionTreatment createNiciraMovNshC1ToC1() {
        int srcOfs = 0;
        int dstOfs = 0;
        int nBits = 32;
        int srcC1 = 0x0001e604;
        int dstC1 = 0x0001e604;
        return new DefaultMoveExtensionTreatment(srcOfs, dstOfs, nBits, srcC1,
                                                 dstC1,
                                                 ExtensionTreatmentType.ExtensionTreatmentTypes
                                                 .NICIRA_MOV_NSH_C1_TO_C1.type());
    }

    public static ExtensionTreatment createNiciraMovNshC2ToC2() {
        int srcOfs = 0;
        int dstOfs = 0;
        int nBits = 32;
        int srcC2 = 0x0001e804;
        int dstC2 = 0x0001e804;
        return new DefaultMoveExtensionTreatment(srcOfs, dstOfs, nBits, srcC2,
                                                 dstC2,
                                                 ExtensionTreatmentType.ExtensionTreatmentTypes
                                                 .NICIRA_MOV_NSH_C2_TO_C2.type());
    }

    public static ExtensionTreatment createNiciraMovNshC3ToC3() {
        int srcOfs = 0;
        int dstOfs = 0;
        int nBits = 32;
        int srcC3 = 0x0001ea04;
        int dstC3 = 0x0001ea04;
        return new DefaultMoveExtensionTreatment(srcOfs, dstOfs, nBits, srcC3,
                                                 dstC3,
                                                 ExtensionTreatmentType.ExtensionTreatmentTypes
                                                 .NICIRA_MOV_NSH_C3_TO_C3.type());
    }

    public static ExtensionTreatment createNiciraMovNshC4ToC4() {
        int srcOfs = 0;
        int dstOfs = 0;
        int nBits = 32;
        int srcC4 = 0x0001ec04;
        int dstC4 = 0x0001ec04;
        return new DefaultMoveExtensionTreatment(srcOfs, dstOfs, nBits, srcC4,
                                                 dstC4,
                                                 ExtensionTreatmentType.ExtensionTreatmentTypes
                                                 .NICIRA_MOV_NSH_C4_TO_C4.type());
    }

    public static ExtensionTreatment createNiciraMovTunDstToTunDst() {
        int srcOfs = 0;
        int dstOfs = 0;
        int nBits = 32;
        int srcTunIpv4Dst = 0x00014004;
        int dstTunIpv4Dst = 0x00014004;
        return new DefaultMoveExtensionTreatment(srcOfs, dstOfs, nBits, srcTunIpv4Dst,
                                                 dstTunIpv4Dst,
                                                 ExtensionTreatmentType.ExtensionTreatmentTypes
                                                 .NICIRA_MOV_TUN_IPV4_DST_TO_TUN_IPV4_DST.type());
    }

    public static ExtensionTreatment createNiciraMovTunIdToTunId() {
        int srcOfs = 0;
        int dstOfs = 0;
        int nBits = 64;
        int srcTunId = 0x12008;
        int dstTunId = 0x12008; // 0x80004c08;
        return new DefaultMoveExtensionTreatment(srcOfs, dstOfs, nBits, srcTunId,
                                                 dstTunId,
                                                 ExtensionTreatmentType.ExtensionTreatmentTypes
                                                 .NICIRA_MOV_TUN_ID_TO_TUN_ID.type());
    }

    public static ExtensionTreatment createNiciraMovNshC2ToTunId() {
        int srcOfs = 0;
        int dstOfs = 0;
        int nBits = 32;
        int srcC2 = 0x0001e804;
        int dstTunId = 0x80004c08;
        return new DefaultMoveExtensionTreatment(srcOfs, dstOfs, nBits, srcC2,
                                                 dstTunId,
                                                 ExtensionTreatmentType.ExtensionTreatmentTypes
                                                 .NICIRA_MOV_NSH_C2_TO_TUN_ID.type());
    }
}
