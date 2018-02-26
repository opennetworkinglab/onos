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

import org.apache.commons.lang3.tuple.Pair;
import org.onlab.packet.MacAddress;
import org.onlab.util.Identifier;

/**
 * A representation of a Mac Address and Unsigned Integer as an MD identifier.
 */
public class MdIdMacUint extends Identifier<Pair<MacAddress, Integer>> implements MdId {
    private static final String MACUINT_PATTERN = "([a-fA-F0-9]{2}[\\-:]){6}[0-9]{1,5}";
    private static final int MAC_UINT_LENGTH_BYTES = 8;
    private static final int UINT_MIN = 0;
    private static final int UINT_MAX = 65535;

    protected MdIdMacUint(Pair<MacAddress, Integer> macAndUint) {
        super(macAndUint);
    }

    @Override
    public String mdName() {
        return identifier.getLeft().toString() + ":" + identifier.getRight();
    }

    @Override
    public String toString() {
        return mdName();
    }

    @Override
    public int getNameLength() {
        return MAC_UINT_LENGTH_BYTES;
    }

    public static MdId asMdId(String mdName) {
        if (mdName == null || !mdName.matches(MACUINT_PATTERN)) {
            throw new IllegalArgumentException("MD Name must follow pattern "
                    + MACUINT_PATTERN + " Rejecting: " + mdName);
        }
        MacAddress macAddress = MacAddress.valueOf(mdName.substring(0, 17));
        int uInt = Integer.parseInt(mdName.substring(18));

        return asMdId(macAddress, uInt);
    }

    @Override
    public MdNameType nameType() {
        return MdNameType.MACANDUINT;
    }

    public static MdId asMdId(MacAddress macAddress, int uInt) {
        if (uInt < UINT_MIN || uInt > UINT_MAX) {
            throw new IllegalArgumentException("uInt must be between " +
                    UINT_MIN + " and " + UINT_MAX + ". Rejecting: " + uInt);
        }
        return new MdIdMacUint(Pair.of(macAddress, uInt));
    }
}
