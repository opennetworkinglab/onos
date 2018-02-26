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
 * ICC-based MEG ID Format, thirteen octet field.
 *
 * It consists of two subfields: the ITU Carrier Code (ICC) followed by a unique
 * MEG ID code (UMC). The ITU Carrier Code consists of 1-6
 * left-justified characters, alphabetic, or leading alphabetic
 * with trailing numeric. The UMC code immediately follows the ICC
 * and shall consist of 7-12 characters, with trailing NULLs,
 * completing the 13-character MEG ID Value.
 * reference
 *  [Y.1731] Annex A;
 */
public final class MaIdIccY1731 extends Identifier<String> implements MaIdShort {
    private static final String ICC_PATTERN = "[a-z|A-Z|0-9]{1,6}";
    private static final String UMC_PATTERN = "[a-z|A-Z|0-9]{7,12}";
    private int iccLength = 0;

    protected MaIdIccY1731(String icc, String umc) {
        super(icc + umc);
        iccLength = icc.length();
    }

    @Override
    public String toString() {
        return identifier.substring(0, iccLength) + ":" + identifier.substring(iccLength);
    }

    @Override
    public String maName() {
        return identifier;
    }

    @Override
    public int getNameLength() {
        return identifier.length();
    }

    @Override
    public MaIdType nameType() {
        return MaIdType.ICCY1731;
    }

    public static MaIdShort asMaId(String icc, String umc) {
        if (icc == null || !icc.matches(ICC_PATTERN)) {
            throw new IllegalArgumentException("ICC part must follow pattern "
                    + ICC_PATTERN + " Rejecting: " + icc);
        } else if (umc == null || !umc.matches(UMC_PATTERN)) {
            throw new IllegalArgumentException("UMC part must follow pattern "
                    + UMC_PATTERN + " Rejecting: " + umc);
        }
        return new MaIdIccY1731(icc, umc);
    }

    public static MaIdShort asMaId(String iccAndUmc) {
        String[] nameParts = iccAndUmc.split(":");
        if (nameParts.length != 2) {
            throw new IllegalArgumentException("Expecting format like ICC:UMC");
        }
        return asMaId(nameParts[0], nameParts[1]);
    }
}
