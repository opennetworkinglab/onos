/*
 * Copyright 2017-present Open Networking Laboratory
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

import org.onlab.packet.IpPrefix;
import org.onosproject.lisp.msg.protocols.LispEidRecord;

/**
 * A LISP map utility class includes various useful methods.
 */
public final class LispMapUtil {

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
}