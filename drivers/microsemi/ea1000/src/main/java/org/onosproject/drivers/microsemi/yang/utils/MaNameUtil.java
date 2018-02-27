/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.drivers.microsemi.yang.utils;

import org.onlab.util.HexString;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaId2Octet;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdCharStr;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdIccY1731;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdPrimaryVid;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdRfc2685VpnId;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdShort;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;
import org.onosproject.yang.gen.v1.ietfyangtypes.rev20130715.ietfyangtypes.YangIdentifier;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.maintenancedomain.maintenanceassociation.MaNameAndTypeCombo;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.maintenancedomain.maintenanceassociation.manameandtypecombo.DefaultNameCharacterString;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.maintenancedomain.maintenanceassociation.manameandtypecombo.DefaultNamePrimaryVid;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.maintenancedomain.maintenanceassociation.manameandtypecombo.DefaultNameRfc2685VpnId;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.maintenancedomain.maintenanceassociation.manameandtypecombo.DefaultNameUint16;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.maintenancedomain.maintenanceassociation.manameandtypecombo.DefaultNameY1731Icc;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.maintenancedomain.maintenanceassociation.manameandtypecombo.NameCharacterString;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.maintenancedomain.maintenanceassociation.manameandtypecombo.nameprimaryvid.NamePrimaryVidUnion;
import org.onosproject.yang.gen.v1.mseatypes.rev20160229.mseatypes.Identifier45;

/**
 * Utility for translating between Maintenance Association names in the CFM API model and the device YANG.
 *
 * This has to be in a separate file as a workaround for Checkstyle issue.
 * https://github.com/checkstyle/checkstyle/issues/3850
 * There are two types of DefaultNameCharacterString - one for MA and another for MD
 * Putting both together in a file means that the full path has to be given which
 * will then fail checkstyle
 */
public final class MaNameUtil {

    private MaNameUtil() {
        //Hidden
    }

    /**
     * Convert CFM API MA identifier to the YANG model MA identifier.
     * @param maId Maintenance Association ID in CFM API
     * @return Maintenance Association ID in YANG API
     * @throws CfmConfigException If there's a problem with the name
     */
    public static MaNameAndTypeCombo getYangMaNameFromApiMaId(MaIdShort maId)
            throws CfmConfigException {
        MaNameAndTypeCombo maName;
        if (maId instanceof MaIdPrimaryVid) {
            maName = new DefaultNamePrimaryVid();
            ((DefaultNamePrimaryVid) maName).namePrimaryVid(NamePrimaryVidUnion.fromString(maId.maName()));
        } else if (maId instanceof MaId2Octet) {
            maName = new DefaultNameUint16();
            ((DefaultNameUint16) maName).nameUint16(Integer.valueOf(maId.maName()));
        } else if (maId instanceof MaIdRfc2685VpnId) {
            maName = new DefaultNameRfc2685VpnId();
            ((DefaultNameRfc2685VpnId) maName).nameRfc2685VpnId(HexString.fromHexString(maId.maName()));
        } else if (maId instanceof MaIdIccY1731) {
            maName = new DefaultNameY1731Icc();
            ((DefaultNameY1731Icc) maName).nameY1731Icc(YangIdentifier.of(maId.maName()));
        } else if (maId instanceof MaIdCharStr) {
            maName = new DefaultNameCharacterString();
            ((DefaultNameCharacterString) maName).name(Identifier45.fromString(maId.maName()));
        } else {
            throw new CfmConfigException("Unexpected error creating MD " +
                    maId.getClass().getSimpleName());
        }
        return maName;
    }

    /**
     * Convert YANG API MA identifier to the CFM API MA identifier.
     * @param nameAndTypeCombo Maintenance Association ID in YANG API
     * @return Maintenance Association ID in CFM API
     */
    public static MaIdShort getApiMaIdFromYangMaName(MaNameAndTypeCombo nameAndTypeCombo) {
        MaIdShort maId;
        if (nameAndTypeCombo instanceof DefaultNameCharacterString) {
            maId = MaIdCharStr.asMaId(
                    ((DefaultNameCharacterString) nameAndTypeCombo).name().string());
        } else if (nameAndTypeCombo instanceof DefaultNamePrimaryVid) {
            if (((DefaultNamePrimaryVid) nameAndTypeCombo).namePrimaryVid().enumeration() != null) {
                maId = MaIdPrimaryVid.asMaId(
                        ((DefaultNamePrimaryVid) nameAndTypeCombo).namePrimaryVid().enumeration().name());
            } else if (((DefaultNamePrimaryVid) nameAndTypeCombo).namePrimaryVid().vlanIdType() != null) {
                maId = MaIdPrimaryVid.asMaId(
                        ((DefaultNamePrimaryVid) nameAndTypeCombo).namePrimaryVid().vlanIdType().uint16());
            } else {
                throw new IllegalArgumentException("Unexpected primaryVid for " +
                        "MaNameAndTypeCombo: " + nameAndTypeCombo.toString());
            }
        } else if (nameAndTypeCombo instanceof DefaultNameUint16) {
            maId = MaId2Octet.asMaId(((DefaultNameUint16) nameAndTypeCombo).nameUint16());

        } else if (nameAndTypeCombo instanceof DefaultNameRfc2685VpnId) {
            maId = MaIdRfc2685VpnId.asMaIdHex(
                    HexString.toHexString(
                            ((DefaultNameRfc2685VpnId) nameAndTypeCombo).nameRfc2685VpnId()));
        } else if (nameAndTypeCombo instanceof DefaultNameY1731Icc) {
            maId = MaIdIccY1731.asMaId(((DefaultNameY1731Icc) nameAndTypeCombo).nameY1731Icc().string());

        } else {
            throw new IllegalArgumentException("Unexpected type for " +
                    "MaNameAndTypeCombo: " + nameAndTypeCombo.toString());
        }

        return maId;
    }

    /**
     * Cast the YANG generic type of MaNameAndTypeCombo specifically to char string.
     * @param maName a YANG generic MaNameAndTypeCombo
     * @return a YANG specific MaNameAndTypeCombo for Char string
     */
    public static NameCharacterString cast(MaNameAndTypeCombo maName) {
        return (NameCharacterString) maName;
    }
}
