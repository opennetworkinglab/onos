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
 * A representation of a string identifier as an MA identifier.
 */
public final class MaIdCharStr extends Identifier<String> implements MaIdShort {
    private static final String MANAME_PATTERN = "[a-zA-Z0-9\\-:]{1,48}";

    protected MaIdCharStr(String mdName) {
        super(mdName);
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
        return MaIdType.CHARACTERSTRING;
    }

    public static MaIdShort asMaId(String maName) {
        if (maName == null || !maName.matches(MANAME_PATTERN)) {
            throw new IllegalArgumentException("MA Name must follow pattern "
                    + MANAME_PATTERN + " Rejecting: " + maName);
        }
        return new MaIdCharStr(maName);
    }
}
