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
package org.onosproject.incubator.net.l2monitoring.cfm.identifier;

import org.onlab.util.Identifier;

/**
 * A representation of a Rfc2685VpnId identifier in Hexadecimal as an MA identifier.
 */
public final class MaIdRfc2685VpnId extends Identifier<String> implements MaIdShort {
    private static final String VPNID_HEX_PATTERN = "([a-f|A-F|0-9]{2}:){6}[a-f|A-F|0-9]{2}";

    protected MaIdRfc2685VpnId(String maNameHex) {
        super(maNameHex);
    }

    @Override
    public String maName() {
        return identifier;
    }

    /**
     * Identifier will be in the format aa:bb:cc:dd:ee:ff:11.
     * Each pair of hex chars (and one colon) is one byte
     * To get the length in bytes add 1 (extra colon) and divide by 3
     * @return name length in bytes
     */
    @Override
    public int getNameLength() {
        return (identifier.length() + 1) / 3;
    }

    @Override
    public MaIdType nameType() {
        return MaIdType.RFC2685VPNID;
    }

    public static MaIdShort asMaIdHex(String hexString) {
        if (hexString == null || !hexString.matches(VPNID_HEX_PATTERN)) {
            throw new IllegalArgumentException("MA Name must follow pattern " +
                    VPNID_HEX_PATTERN + " Rejecting: " + hexString);
        }
        return new MaIdRfc2685VpnId(hexString.toLowerCase());
    }
}
