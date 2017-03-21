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
 * A representation of a string identifier as an MD identifier.
 */
public final class MdIdCharStr extends Identifier<String> implements MdId {
    private static final String MDNAME_PATTERN =
            "[a-zA-Z0-9\\-:\\.]{" + MD_NAME_MIN_LEN + "," + MD_NAME_MAX_LEN + "}";

    protected MdIdCharStr(String mdName) {
        super(mdName);
    }

    @Override
    public String mdName() {
        return identifier;
    }

    @Override
    public int getNameLength() {
        return identifier.length();
    }

    @Override
    public MdNameType nameType() {
        return MdNameType.CHARACTERSTRING;
    }

    public static MdId asMdId(String mdName) {
        if (mdName == null || !mdName.matches(MDNAME_PATTERN)) {
            throw new IllegalArgumentException("MD Name must follow pattern "
                    + MDNAME_PATTERN + " Rejecting: " + mdName);
        }
        return new MdIdCharStr(mdName);
    }
}
