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
 * A representation of a Vid identifier as an int as an MA identifier.
 */
public class MaIdPrimaryVid extends Identifier<Integer> implements MaIdShort {
    private static final int PRIMARY_VID_BYTES = 2;
    protected static int lowerLimit = 0;
    protected static int upperLimit = 4095;

    protected MaIdPrimaryVid(int primaryVid) {
        super(primaryVid);
    }

    @Override
    public String maName() {
        return identifier.toString();
    }

    /**
     * Vid length of 12 bits will be rounded up to 2 bytes (16 bits).
     * @return 2 bytes
     */
    @Override
    public int getNameLength() {
        return PRIMARY_VID_BYTES;
    }

    @Override
    public MaIdType nameType() {
        return MaIdType.PRIMARYVID;
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

    public static MaIdShort asMaId(int id) {
        if (id <= lowerLimit || id > upperLimit) {
            throw new IllegalArgumentException("MA Id must be between " +
                    lowerLimit + " and " + upperLimit + ". Rejecting: " + id);
        }
        return new MaIdPrimaryVid(id);
    }
}
