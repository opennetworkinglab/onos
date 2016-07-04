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

package org.onosproject.yang.gen.v1.l3vpn.comm.type.rev20141225.nel3vpncommtype.l3vpncommonvrfrttype;

/**
 * Represents ENUM data of l3VpncommonVrfRtTypeEnum.
 */
public enum L3VpncommonVrfRtTypeEnum {

    /**
     * Represents exportExtcommunity.
     */
    EXPORT_EXTCOMMUNITY(0),

    /**
     * Represents importExtcommunity.
     */
    IMPORT_EXTCOMMUNITY(1);

    private int l3VpncommonVrfRtTypeEnum;

    /**
     * Creates an instance of l3VpncommonVrfRtTypeEnum.
     *
     * @param value value of l3VpncommonVrfRtTypeEnum
     */
    L3VpncommonVrfRtTypeEnum(int value) {
        l3VpncommonVrfRtTypeEnum = value;
    }

    /**
     * Returns the object of l3VpncommonVrfRtTypeEnumForTypeInt.
     *
     * @param value value of l3VpncommonVrfRtTypeEnumForTypeInt
     * @return Object of l3VpncommonVrfRtTypeEnumForTypeInt
     */
    public static L3VpncommonVrfRtTypeEnum of(int value) {
        switch (value) {
            case 0:
                return L3VpncommonVrfRtTypeEnum.EXPORT_EXTCOMMUNITY;
            case 1:
                return L3VpncommonVrfRtTypeEnum.IMPORT_EXTCOMMUNITY;
            default :
                return null;
        }
    }

    /**
     * Returns the attribute l3VpncommonVrfRtTypeEnum.
     *
     * @return value of l3VpncommonVrfRtTypeEnum
     */
    public int l3VpncommonVrfRtTypeEnum() {
        return l3VpncommonVrfRtTypeEnum;
    }

    /**
     * Returns the object of l3VpncommonVrfRtTypeEnum fromString input String.
     *
     * @param valInString input String
     * @return Object of l3VpncommonVrfRtTypeEnum
     */
    public static L3VpncommonVrfRtTypeEnum fromString(String valInString) {
        try {
            int tmpVal = Integer.parseInt(valInString);
            return of(tmpVal);
        } catch (Exception e) {
        }
        return null;
    }
}
