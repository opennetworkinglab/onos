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
}
