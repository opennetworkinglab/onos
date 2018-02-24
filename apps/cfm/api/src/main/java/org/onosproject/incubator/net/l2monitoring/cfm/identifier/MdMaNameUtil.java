/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.incubator.net.l2monitoring.cfm.identifier;

/**
 * Utility for generating an MaIdShort or MdId from string.
 */
public final class MdMaNameUtil {

    private MdMaNameUtil() {
        //Singleton
    }

    /**
     * Create an MdId from a pair of strings.
     * @param mdNameType the name type
     * @param mdName the name
     * @return an MdId
     */
    public static MdId parseMdName(String mdNameType, String mdName) {
        MdId.MdNameType nameTypeEnum = MdId.MdNameType.valueOf(mdNameType);
        switch (nameTypeEnum) {
            case DOMAINNAME:
                return MdIdDomainName.asMdId(mdName);
            case MACANDUINT:
                return MdIdMacUint.asMdId(mdName);
            case NONE:
                return MdIdNone.asMdId();
            case CHARACTERSTRING:
            default:
                return MdIdCharStr.asMdId(mdName);
        }
    }

    /**
     * Create an MaIdShort from a pair of strings.
     * @param maNameType the name type
     * @param maName the name
     * @return an MaIdShort
     */
    public static MaIdShort parseMaName(String maNameType, String maName) {
        MaIdShort.MaIdType nameTypeEnum = MaIdShort.MaIdType.valueOf(maNameType);
        switch (nameTypeEnum) {
            case ICCY1731:
                return MaIdIccY1731.asMaId(maName);
            case PRIMARYVID:
                return MaIdPrimaryVid.asMaId(maName);
            case RFC2685VPNID:
                return MaIdRfc2685VpnId.asMaIdHex(maName);
            case TWOOCTET:
                return MaId2Octet.asMaId(maName);
            case CHARACTERSTRING:
            default:
                return MaIdCharStr.asMaId(maName);
        }
    }
}
