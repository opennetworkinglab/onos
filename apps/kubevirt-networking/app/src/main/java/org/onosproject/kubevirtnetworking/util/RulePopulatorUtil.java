/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.kubevirtnetworking.util;

import org.onlab.packet.Ip4Address;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.ExtensionTreatmentResolver;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.instructions.ExtensionPropertyException;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.slf4j.Logger;

import static org.onosproject.net.flow.instructions.ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_LOAD;
import static org.onosproject.net.flow.instructions.ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_MOV_ARP_SHA_TO_THA;
import static org.onosproject.net.flow.instructions.ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_MOV_ARP_SPA_TO_TPA;
import static org.onosproject.net.flow.instructions.ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_MOV_ETH_SRC_TO_DST;
import static org.onosproject.net.flow.instructions.ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_MOV_IP_SRC_TO_DST;
import static org.onosproject.net.flow.instructions.ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_TUNNEL_DST;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provides common methods to help populating flow rules for SONA applications.
 */
public final class RulePopulatorUtil {

    private static final Logger log = getLogger(RulePopulatorUtil.class);

    private static final int OFF_SET_BIT = 0;
    private static final int REMAINDER_BIT = 8;

    private static final String OFF_SET_N_BITS = "ofsNbits";
    private static final String DESTINATION = "dst";
    private static final String VALUE = "value";
    private static final String TUNNEL_DST = "tunnelDst";

    // layer 3 nicira fields
    private static final int NXM_OF_IP_SRC = 0x00000e04;
    private static final int NXM_OF_IP_DST = 0x00001004;
    private static final int NXM_OF_IP_PROT = 0x00000c01;

    public static final int NXM_NX_IP_TTL = 0x00013a01;
    public static final int NXM_NX_IP_FRAG = 0x00013401;
    public static final int NXM_OF_ARP_OP = 0x00001e02;
    public static final int NXM_OF_ARP_SPA = 0x00002004;
    public static final int NXM_OF_ARP_TPA = 0x00002204;
    public static final int NXM_NX_ARP_SHA = 0x00012206;
    public static final int NXM_NX_ARP_THA = 0x00012406;

    // layer 4 nicira fields
    public static final int NXM_OF_TCP_SRC = 0x00001202;
    public static final int NXM_OF_TCP_DST = 0x00001402;
    public static final int NXM_NX_TCP_FLAGS = 0x00014402;
    public static final int NXM_OF_UDP_SRC = 0x00001602;
    public static final int NXM_OF_UDP_DST = 0x00001802;

    public static final int NXM_OF_ICMP_TYPE = 0x00001a01;
    public static final int NXM_OF_ICMP_CODE = 0x00001c01;

    private RulePopulatorUtil() {
    }

    /**
     * Returns the nicira load extension treatment.
     *
     * @param device        device instance
     * @param field         field code
     * @param value         value to load
     * @return load extension treatment
     */
    public static ExtensionTreatment buildLoadExtension(Device device,
                                                        long field,
                                                        long value) {
        if (!checkTreatmentResolver(device)) {
            return null;
        }

        ExtensionTreatmentResolver resolver = device.as(ExtensionTreatmentResolver.class);
        ExtensionTreatment treatment =
                resolver.getExtensionInstruction(NICIRA_LOAD.type());

        int ofsNbits = OFF_SET_BIT << 6 | (REMAINDER_BIT - 1);

        try {
            treatment.setPropertyValue(OFF_SET_N_BITS, ofsNbits);
            treatment.setPropertyValue(DESTINATION, field);
            treatment.setPropertyValue(VALUE, value);
            return treatment;
        } catch (ExtensionPropertyException e) {
            log.error("Failed to set nicira load extension treatment for {}",
                    device.id());
            return null;
        }
    }

    /**
     * Returns the nicira move source MAC to destination MAC extension treatment.
     *
     * @param device        device instance
     * @return move extension treatment
     */
    public static ExtensionTreatment buildMoveEthSrcToDstExtension(Device device) {
        if (!checkTreatmentResolver(device)) {
            return null;
        }

        ExtensionTreatmentResolver resolver = device.as(ExtensionTreatmentResolver.class);
        return resolver.getExtensionInstruction(NICIRA_MOV_ETH_SRC_TO_DST.type());
    }

    /**
     * Returns the nicira move source IP to destination IP extension treatment.
     *
     * @param device        device instance
     * @return move extension treatment
     */
    public static ExtensionTreatment buildMoveIpSrcToDstExtension(Device device) {
        if (!checkTreatmentResolver(device)) {
            return null;
        }

        ExtensionTreatmentResolver resolver = device.as(ExtensionTreatmentResolver.class);
        return resolver.getExtensionInstruction(NICIRA_MOV_IP_SRC_TO_DST.type());
    }

    /**
     * Returns the nicira move ARP SHA to THA extension treatment.
     *
     * @param device        device instance
     * @return move extension treatment
     */
    public static ExtensionTreatment buildMoveArpShaToThaExtension(Device device) {
        if (!checkTreatmentResolver(device)) {
            return null;
        }

        ExtensionTreatmentResolver resolver = device.as(ExtensionTreatmentResolver.class);
        return resolver.getExtensionInstruction(NICIRA_MOV_ARP_SHA_TO_THA.type());
    }

    /**
     * Returns the nicira move ARP SPA to TPA extension treatment.
     *
     * @param device        device instance
     * @return move extension treatment
     */
    public static ExtensionTreatment buildMoveArpSpaToTpaExtension(Device device) {
        if (!checkTreatmentResolver(device)) {
            return null;
        }

        ExtensionTreatmentResolver resolver = device.as(ExtensionTreatmentResolver.class);
        return resolver.getExtensionInstruction(NICIRA_MOV_ARP_SPA_TO_TPA.type());
    }

    private static boolean checkTreatmentResolver(Device device) {
        if (device == null || !device.is(ExtensionTreatmentResolver.class)) {
            log.warn("Nicira extension treatment is not supported");
            return false;
        }

        return true;
    }

    /**
     * Returns tunnel destination extension treatment object.
     *
     * @param deviceService driver service
     * @param deviceId device id to apply this treatment
     * @param remoteIp tunnel destination ip address
     * @return extension treatment
     */
    public static ExtensionTreatment buildExtension(DeviceService deviceService,
                                                    DeviceId deviceId,
                                                    Ip4Address remoteIp) {
        Device device = deviceService.getDevice(deviceId);
        if (!checkTreatmentResolver(device)) {
            return null;
        }

        if (device == null) {
            return null;
        }

        ExtensionTreatmentResolver resolver = device.as(ExtensionTreatmentResolver.class);
        ExtensionTreatment treatment =
                resolver.getExtensionInstruction(NICIRA_SET_TUNNEL_DST.type());
        try {
            treatment.setPropertyValue(TUNNEL_DST, remoteIp);
            return treatment;
        } catch (ExtensionPropertyException e) {
            log.warn("Failed to get tunnelDst extension treatment for {} " +
                    "because of {}", deviceId, e);
            return null;
        }
    }
}
