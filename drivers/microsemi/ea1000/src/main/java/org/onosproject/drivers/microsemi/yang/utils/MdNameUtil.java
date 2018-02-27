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

import org.onlab.packet.IpAddress;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdId;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdIdCharStr;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdIdDomainName;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdIdMacUint;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdIdNone;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;
import org.onosproject.yang.gen.v1.ietfinettypes.rev20130715.ietfinettypes.DomainName;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.maintenancedomain.MdNameAndTypeCombo;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.maintenancedomain.mdnameandtypecombo.DefaultMacAddressAndUint;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.maintenancedomain.mdnameandtypecombo.DefaultNameCharacterString;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.maintenancedomain.mdnameandtypecombo.DefaultNameDomainName;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.maintenancedomain.mdnameandtypecombo.DefaultNameNone;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.maintenancedomain.mdnameandtypecombo.MacAddressAndUint;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.maintenancedomain.mdnameandtypecombo.NameCharacterString;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.maintenancedomain.mdnameandtypecombo.namedomainname.NameDomainNameUnion;
import org.onosproject.yang.gen.v1.mseatypes.rev20160229.mseatypes.Identifier45;
import org.onosproject.yang.gen.v1.mseatypes.rev20160229.mseatypes.MacAddressAndUintStr;

/**
 * Utility for translating between Maintenance Domain names in the CFM API model and the device YANG.
 *
 * This has to be in a separate file as a workaround for Checkstyle issue.
 * https://github.com/checkstyle/checkstyle/issues/3850
 * There are two types of DefaultNameCharacterString - one for MA and another for MD
 * Putting both together in a file means that the full path has to be given which
 * will then fail checkstyle
 */
public final class MdNameUtil {

    private MdNameUtil() {
        //Hidden
    }

    /**
     * Convert CFM API MD identifier to the YANG model MD identifier.
     * @param mdId Maintenance Domain ID in CFM API
     * @return Maintenance Domain ID in YANG API
     * @throws CfmConfigException If there's a problem with the name
     */
    public static MdNameAndTypeCombo getYangMdNameFromApiMdId(MdId mdId)
            throws CfmConfigException {
        MdNameAndTypeCombo mdName;
        if (mdId instanceof MdIdDomainName) {
            boolean isIpAddr = false;
            try {
                if (IpAddress.valueOf(mdId.mdName()) != null) {
                    isIpAddr = true;
                }
            } catch (IllegalArgumentException e) {
                //continue
            }
            if (isIpAddr) {
                mdName = new DefaultNameDomainName();
                ((DefaultNameDomainName) mdName).nameDomainName(NameDomainNameUnion.of(
                        org.onosproject.yang.gen.v1.ietfinettypes.rev20130715.ietfinettypes.
                                IpAddress.fromString(mdId.mdName())));
            } else {
                mdName = new DefaultNameDomainName();
                ((DefaultNameDomainName) mdName).nameDomainName(NameDomainNameUnion
                        .of(DomainName.fromString(mdId.mdName())));
            }
        } else if (mdId instanceof MdIdMacUint) {
            mdName = new DefaultMacAddressAndUint();
            ((DefaultMacAddressAndUint) mdName).nameMacAddressAndUint(MacAddressAndUintStr.fromString(mdId.mdName()));
        } else if (mdId instanceof MdIdNone) {
            mdName = new DefaultNameNone();
        } else if (mdId instanceof MdIdCharStr) {
            mdName = new DefaultNameCharacterString();
            ((DefaultNameCharacterString) mdName).name(Identifier45.fromString(mdId.mdName()));
        } else {
            throw new CfmConfigException("Unexpected error creating MD " +
                    mdId.getClass().getSimpleName());
        }
        return mdName;
    }

    /**
     * Convert YANG API MD identifier to the CFM API MD identifier.
     * @param nameAndTypeCombo Maintenance Domain ID in YANG API
     * @return Maintenance Domain ID in CFM API
     */
    public static MdId getApiMdIdFromYangMdName(MdNameAndTypeCombo nameAndTypeCombo) {
        MdId mdId;
        if (nameAndTypeCombo instanceof DefaultNameDomainName) {
            NameDomainNameUnion domainName =
                    ((DefaultNameDomainName) nameAndTypeCombo).nameDomainName();
            if (domainName.ipAddress() != null) {
                mdId = MdIdDomainName.asMdId(domainName.ipAddress().toString());
            } else if (domainName.domainName() != null) {
                mdId = MdIdDomainName.asMdId(domainName.domainName().string());
            } else {
                throw new IllegalArgumentException("Unexpected domainName for " +
                        "MdNameAndTypeCombo: " + nameAndTypeCombo.toString());
            }
        } else if (nameAndTypeCombo instanceof DefaultNameCharacterString) {
            mdId = MdIdCharStr.asMdId(
                    ((NameCharacterString) nameAndTypeCombo).name().string());

        } else if (nameAndTypeCombo instanceof DefaultMacAddressAndUint) {
            mdId = MdIdMacUint.asMdId(
                    ((MacAddressAndUint) nameAndTypeCombo).nameMacAddressAndUint().string());

        } else if (nameAndTypeCombo instanceof DefaultNameNone) {
            mdId = MdIdNone.asMdId();
        } else {
            throw new IllegalArgumentException("Unexpected type for " +
                    "MdNameAndTypeCombo: " + nameAndTypeCombo.toString());
        }

        return mdId;
    }

    /**
     * Cast the YANG generic type of MdNameAndTypeCombo specifically to char string.
     * @param maName a YANG generic MdNameAndTypeCombo
     * @return a YANG specific MdNameAndTypeCombo for Char string
     */
    public static NameCharacterString cast(MdNameAndTypeCombo maName) {
        return (NameCharacterString) maName;
    }
}
