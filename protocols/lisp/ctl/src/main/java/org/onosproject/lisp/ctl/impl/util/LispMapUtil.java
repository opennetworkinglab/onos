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

package org.onosproject.lisp.ctl.impl.util;

import org.apache.commons.lang3.StringUtils;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onosproject.lisp.msg.protocols.LispEidRecord;
import org.onosproject.lisp.msg.types.LispIpAddress;
import org.onosproject.lisp.msg.types.LispIpv4Address;
import org.onosproject.lisp.msg.types.LispIpv6Address;

/**
 * A LISP map utility class includes various useful methods.
 */
public final class LispMapUtil {

    private static final int IPV4_BLOCK_LENGTH = 8;
    private static final int IPV6_BLOCK_LENGTH = 16;

    private static final String IPV4_DELIMITER = ".";
    private static final String IPV6_DELIMITER = ":";

    /**
     * Prevents object instantiation from external.
     */
    private LispMapUtil() {
    }

    /**
     * Generates CIDR style string from EID record.
     *
     * @param eidRecord EID record
     * @return CIDR style string
     */
    public static String cidrfy(LispEidRecord eidRecord) {
        StringBuilder sb = new StringBuilder();
        sb.append(eidRecord.getPrefix().toString());
        sb.append("/");
        sb.append(eidRecord.getMaskLength());
        return sb.toString();
    }

    /**
     * Checks whether the EID record is included in the given EID record.
     *
     * @param origin  the EID record to be compared
     * @param compare the EID record to compare
     * @return boolean result
     */
    public static boolean isInRange(LispEidRecord origin, LispEidRecord compare) {

        IpPrefix originIpPrefix = IpPrefix.valueOf(cidrfy(origin));
        IpPrefix compareIpPrefix = IpPrefix.valueOf(cidrfy(compare));

        return originIpPrefix.contains(compareIpPrefix);
    }

    /**
     * Obtains the EID record from an IP prefix.
     *
     * @param prefix IP prefix
     * @return EID record
     */
    public static LispEidRecord getEidRecordFromIpPrefix(IpPrefix prefix) {

        LispIpAddress eid = null;

        if (prefix.isIp4()) {
            eid = new LispIpv4Address(prefix.address());
        }

        if (prefix.isIp6()) {
            eid = new LispIpv6Address(prefix.address());
        }

        return new LispEidRecord((byte) prefix.prefixLength(), eid);
    }

    /**
     * Obtains the IP prefix from an EID record.
     *
     * @param eidRecord EID record
     * @return IP prefix
     */
    public static IpPrefix getIpPrefixFromEidRecord(LispEidRecord eidRecord) {
        return IpPrefix.valueOf(eidRecord.getPrefix().toString() +
                "/" + eidRecord.getMaskLength());
    }

    /**
     * Obtains the string formatted IP prefix.
     * For example, if the IP address is 10.1.1.1 and has 16 prefix length,
     * the resulting string is 10.1
     *
     * @param prefix IP prefix
     * @return string formatted IP prefix
     */
    public static String getPrefixString(IpPrefix prefix) {
        String addressString = prefix.address().toString();
        StringBuilder sb = new StringBuilder();
        String delimiter = "";
        int numOfBlock = 0;

        if (prefix.isIp4()) {
            delimiter = IPV4_DELIMITER;
            numOfBlock = prefix.prefixLength() / IPV4_BLOCK_LENGTH;
        }

        if (prefix.isIp6()) {
            delimiter = IPV6_DELIMITER;
            numOfBlock = prefix.prefixLength() / IPV6_BLOCK_LENGTH;
        }

        String[] octets = StringUtils.split(addressString, delimiter);

        for (int i = 0; i < numOfBlock; i++) {
            sb.append(octets[i]);

            if (i < numOfBlock - 1) {
                sb.append(delimiter);
            }
        }

        return sb.toString();
    }

    /**
     * Obtains the parent IP prefix of the given IP prefix.
     * For example, if the given IP prefix is 10.1.1.1/32, the parent IP prefix
     * will be 10.1.1.0/31.
     *
     * @param prefix IP prefix
     * @return parent IP prefix
     */
    public static IpPrefix getParentPrefix(IpPrefix prefix) {
        return IpPrefix.valueOf(IpAddress.makeMaskedAddress(prefix.address(),
                prefix.prefixLength() - 1), prefix.prefixLength() - 1);
    }
}