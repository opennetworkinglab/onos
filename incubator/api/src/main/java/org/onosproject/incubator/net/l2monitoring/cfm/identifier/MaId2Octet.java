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

/**
 * The 2 octet format of MA Short Name.
 * This is similar to primaryVid except range is 0 to 65535
 */
public final class MaId2Octet extends MaIdPrimaryVid {
    protected static int uintUpperLimit = 65535;

    protected MaId2Octet(int id2octet) {
        super(id2octet);
    }

    public static MaIdShort asMaId(int id) {
        if (id <= lowerLimit || id > uintUpperLimit) {
            throw new IllegalArgumentException("MA Id must be between " +
                    lowerLimit + " and " + uintUpperLimit + ". Rejecting: " + id);
        }
        return new MaId2Octet(id);
    }

    public static MaIdShort asMaId(String maName) {
        int id = 0;
        try {
            id = Integer.parseInt(maName);
            return asMaId(id);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("MA Name must be numeric. Rejecting: " +
                    maName + " " + e.getMessage());
        }
    }

    @Override
    public MaIdType nameType() {
        return MaIdType.TWOOCTET;
    }
}